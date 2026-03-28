package dev.inscribe.core.engine;

import dev.inscribe.core.command.InsertItemCommand;
import dev.inscribe.core.event.*;
import dev.inscribe.core.spi.StepHandler;
import dev.inscribe.core.spi.StepResult;
import dev.inscribe.core.spi.WorkflowContext;
import dev.inscribe.core.workflow.WorkflowDefinition;
import dev.inscribe.core.workflow.WorkflowDefinitionLoader;
import org.redisson.api.RDeque;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Executes a single InsertItemCommand within its own @Transactional boundary.
 * Extracted from Orchestrator to avoid Spring self-invocation proxy bypass.
 */
@Component
public class CommandProcessor {

    private static final Logger log = LoggerFactory.getLogger(CommandProcessor.class);
    private static final String QUEUE_PREFIX = "inscribe:queue:container:";

    private final RedissonClient redisson;
    private final StepHandlerRegistry handlerRegistry;
    private final WorkflowDefinitionLoader workflowLoader;
    private final ApplicationEventPublisher eventPublisher;

    public CommandProcessor(RedissonClient redisson,
                            StepHandlerRegistry handlerRegistry,
                            WorkflowDefinitionLoader workflowLoader,
                            ApplicationEventPublisher eventPublisher) {
        this.redisson = redisson;
        this.handlerRegistry = handlerRegistry;
        this.workflowLoader = workflowLoader;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void process(InsertItemCommand command) {
        WorkflowDefinition workflow = workflowLoader.get(command.workflowName());
        UUID containerId = command.containerId();
        UUID correlationId = command.correlationId();

        log.info("[{}] Item requested: {} for container {}",
                correlationId, command.catalogItemId(), containerId);
        eventPublisher.publishEvent(new ItemRequestedEvent(
                containerId, null, command.workflowName(), command.catalogItemId(), correlationId));

        WorkflowContext ctx = new WorkflowContext(
                containerId, null,
                command.catalogItemId(), command.catalogItemName(),
                workflow.name(), correlationId, command.metadata());

        List<WorkflowDefinition.StepDef> steps = workflow.steps();
        for (int i = 0; i < steps.size(); i++) {
            WorkflowDefinition.StepDef stepDef = steps.get(i);
            StepHandler handler = handlerRegistry.get(stepDef.handler());

            log.info("[{}] Executing step '{}' via handler '{}'",
                    correlationId, stepDef.name(), stepDef.handler());

            StepResult result = handler.handle(ctx);

            switch (result) {
                case StepResult.Reject reject -> {
                    eventPublisher.publishEvent(new ItemRejectedEvent(
                            containerId, null, command.workflowName(), reject.reason(), correlationId));
                    log.info("[{}] Item {} rejected at step '{}': {}",
                            correlationId, command.catalogItemId(), stepDef.name(), reject.reason());
                    return;
                }
                case StepResult.Retry retry -> {
                    log.warn("[{}] Step '{}' requested retry: {} — re-enqueuing command",
                            correlationId, stepDef.name(), retry.reason());
                    RDeque<InsertItemCommand> queue = redisson.getDeque(QUEUE_PREFIX + containerId);
                    queue.addFirst(command);
                    return;
                }
                case StepResult.Success ignored -> {
                    log.debug("[{}] Step '{}' succeeded", correlationId, stepDef.name());
                    // After each successful step except the last, signal validation passed
                    if (i < steps.size() - 1) {
                        eventPublisher.publishEvent(new ItemValidatedEvent(
                                containerId, null, command.workflowName(), correlationId));
                    }
                }
            }
        }

        eventPublisher.publishEvent(new ItemInsertedEvent(
                containerId, null, command.workflowName(), correlationId));

        log.info("[{}] Item {} successfully processed for container {}",
                correlationId, command.catalogItemId(), containerId);
    }
}
