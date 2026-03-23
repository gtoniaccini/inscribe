package dev.inscribe.core.event;

import java.time.Instant;
import java.util.UUID;

public record ItemRequestedEvent(
        UUID containerId,
        UUID itemId,
        String workflowName,
        String catalogItemId,
        UUID correlationId,
        Instant timestamp
) implements WorkflowEvent {
    public ItemRequestedEvent(UUID containerId, UUID itemId, String workflowName,
                              String catalogItemId, UUID correlationId) {
        this(containerId, itemId, workflowName, catalogItemId, correlationId, Instant.now());
    }
}
