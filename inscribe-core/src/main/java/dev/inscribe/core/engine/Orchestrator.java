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

import java.util.UUID;

/**
 * Central orchestrator: receives commands, enqueues them per-container in Redis,
 * acquires a distributed lock, and runs the workflow steps sequentially.
 *
 * The core NEVER persists domain data. Each plugin's insertion StepHandler
 * writes to its own table. The orchestrator only manages flow and events.
 *
 * "containerId" is the UUID of the plugin's domain entity (Prescription, Cart).
 */
@Component
public class Orchestrator {

    private static final Logger log = LoggerFactory.getLogger(Orchestrator.class);
    private static final String QUEUE_PREFIX = "inscribe:queue:container:";

    private final RedissonClient redisson;
    private final LockManager lockManager;
    private final StepHandlerRegistry handlerRegistry;
    private final WorkflowDefinitionLoader workflowLoader;
    private final ApplicationEventPublisher eventPublisher;

    public Orchestrator(RedissonClient redisson,
                        LockManager lockManager,
                        StepHandlerRegistry handlerRegistry,
                        WorkflowDefinitionLoader workflowLoader,
                        ApplicationEventPublisher eventPublisher) {
        this.redisson = redisson;
        this.lockManager = lockManager;
        this.handlerRegistry = handlerRegistry;
        this.workflowLoader = workflowLoader;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Accept a command: enqueue it and attempt to drain the queue for this container.
     */
    public void submit(InsertItemCommand command) {
        RDeque<InsertItemCommand> queue = redisson.getDeque(QUEUE_PREFIX + command.containerId());
        queue.addLast(command);
        log.info("[{}] Command enqueued for container {}", command.correlationId(), command.containerId());
        drainQueue(command.containerId());
    }

    /**
     * Try to process all pending commands for a given container.
     * Only one pod at a time can drain a container's queue (distributed lock).
     */
    public void drainQueue(UUID containerId) {
        if (!lockManager.tryLock(containerId, 0)) {
            log.debug("Container {} is already being processed by another worker", containerId);
            return;
        }
        try {
            RDeque<InsertItemCommand> queue = redisson.getDeque(QUEUE_PREFIX + containerId);
            InsertItemCommand command;
            while ((command = queue.pollFirst()) != null) {
                processCommand(command);
            }
        } finally {
            lockManager.unlock(containerId);
        }
    }

    @Transactional
    protected void processCommand(InsertItemCommand command) {
        WorkflowDefinition workflow = workflowLoader.get(command.workflowName());
        UUID containerId = command.containerId();
        UUID correlationId = command.correlationId();

        // --- PHASE 1: REQUEST (no DB write) ---
        log.info("[{}] Item requested: {} for container {}",
                correlationId, command.catalogItemId(), containerId);
        eventPublisher.publishEvent(new ItemRequestedEvent(
                containerId, null, command.workflowName(), command.catalogItemId(), correlationId));

        // --- PHASE 2+3: VALIDATION & INSERTION (delegated to plugin handlers) ---
        WorkflowContext ctx = new WorkflowContext(
                containerId, null,
                command.catalogItemId(), command.catalogItemName(),
                workflow.name(), correlationId, command.metadata());

        for (WorkflowDefinition.StepDef stepDef : workflow.steps()) {
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
                    RDeque<InsertItemCommand> queue = redisson.getDeque(
                            QUEUE_PREFIX + containerId);
                    queue.addFirst(command);
                    return;
                }
                case StepResult.Success ignored -> {
                    log.debug("[{}] Step '{}' succeeded", correlationId, stepDef.name());
                }
            }
        }

        // All steps passed — the plugin's insertion handler has already persisted
        // the domain data in its own table. We only publish events here.
        eventPublisher.publishEvent(new ItemInsertedEvent(
                containerId, null, command.workflowName(), correlationId));

        log.info("[{}] Item {} successfully processed for container {}",
                correlationId, command.catalogItemId(), containerId);
    }
}
