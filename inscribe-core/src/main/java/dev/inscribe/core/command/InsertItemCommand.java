package dev.inscribe.core.command;

import java.util.Map;
import java.util.UUID;

/**
 * Command to request the insertion of an item into a container.
 * Enqueued by the plugin controller or the AI resolver, processed by the Orchestrator.
 * <p>
 * {@code containerId} is the UUID of the plugin's domain entity (Prescription, Cart, etc.)
 * used for locking and queuing. {@code workflowName} tells the Orchestrator which
 * YAML workflow to execute.
 */
public record InsertItemCommand(
        UUID containerId,
        String workflowName,
        String catalogItemId,
        String catalogItemName,
        Map<String, Object> metadata,
        UUID correlationId
) {
    public InsertItemCommand(
            UUID containerId,
            String workflowName,
            String catalogItemId,
            String catalogItemName,
            Map<String, Object> metadata) {
        this(containerId, workflowName, catalogItemId, catalogItemName,
                metadata == null ? Map.of() : Map.copyOf(metadata), UUID.randomUUID());
    }

    public InsertItemCommand(
            UUID containerId,
            String workflowName,
            String catalogItemId,
            String catalogItemName) {
        this(containerId, workflowName, catalogItemId, catalogItemName, Map.of());
    }
}
