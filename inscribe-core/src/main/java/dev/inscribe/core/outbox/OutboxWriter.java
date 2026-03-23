package dev.inscribe.core.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.inscribe.core.event.ItemInsertedEvent;
import dev.inscribe.core.event.ItemRejectedEvent;
import dev.inscribe.core.workflow.WorkflowDefinition;
import dev.inscribe.core.workflow.WorkflowDefinitionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listens to internal Spring events and writes OutboxEvent rows
 * in the same transaction as the domain changes.
 */
@Component
public class OutboxWriter {

    private static final Logger log = LoggerFactory.getLogger(OutboxWriter.class);

    private final OutboxEventRepository outboxRepository;
    private final WorkflowDefinitionLoader workflowLoader;
    private final ObjectMapper objectMapper;

    public OutboxWriter(OutboxEventRepository outboxRepository,
                        WorkflowDefinitionLoader workflowLoader,
                        ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.workflowLoader = workflowLoader;
        this.objectMapper = objectMapper;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onItemInserted(ItemInsertedEvent event) {
        WorkflowDefinition def = workflowLoader.get(event.workflowName());
        if (def.onComplete() != null) {
            saveOutbox(def.onComplete().topic(), "ItemInserted", event);
        }
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onItemRejected(ItemRejectedEvent event) {
        WorkflowDefinition def = workflowLoader.get(event.workflowName());
        if (def.onReject() != null) {
            saveOutbox(def.onReject().topic(), "ItemRejected", event);
        }
    }

    private void saveOutbox(String topic, String eventType, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            outboxRepository.save(new OutboxEvent(topic, eventType, payload));
            log.debug("Outbox event written: {} -> {}", eventType, topic);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize outbox event", e);
        }
    }
}
