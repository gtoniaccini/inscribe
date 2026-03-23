package dev.inscribe.core.event;

import java.time.Instant;
import java.util.UUID;

public record ItemInsertedEvent(
        UUID containerId,
        UUID itemId,
        String workflowName,
        UUID correlationId,
        Instant timestamp
) implements WorkflowEvent {
    public ItemInsertedEvent(UUID containerId, UUID itemId, String workflowName, UUID correlationId) {
        this(containerId, itemId, workflowName, correlationId, Instant.now());
    }
}
