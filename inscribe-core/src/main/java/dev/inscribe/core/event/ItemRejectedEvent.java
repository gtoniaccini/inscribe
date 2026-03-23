package dev.inscribe.core.event;

import java.time.Instant;
import java.util.UUID;

public record ItemRejectedEvent(
        UUID containerId,
        UUID itemId,
        String workflowName,
        String reason,
        UUID correlationId,
        Instant timestamp
) implements WorkflowEvent {
    public ItemRejectedEvent(UUID containerId, UUID itemId, String workflowName,
                             String reason, UUID correlationId) {
        this(containerId, itemId, workflowName, reason, correlationId, Instant.now());
    }
}
