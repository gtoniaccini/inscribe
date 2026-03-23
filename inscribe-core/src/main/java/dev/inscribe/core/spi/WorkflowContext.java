package dev.inscribe.core.spi;

import java.util.Map;
import java.util.UUID;

/**
 * Immutable context passed to every {@link StepHandler} during workflow execution.
 */
public record WorkflowContext(
        UUID containerId,
        UUID itemId,
        String catalogItemId,
        String catalogItemName,
        String workflowName,
        UUID correlationId,
        Map<String, Object> metadata
) {
    public WorkflowContext(UUID containerId, UUID itemId, String catalogItemId,
                           String catalogItemName, String workflowName) {
        this(containerId, itemId, catalogItemId, catalogItemName,
                workflowName, UUID.randomUUID(), Map.of());
    }
}
