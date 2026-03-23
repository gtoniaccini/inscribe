package dev.inscribe.core.event;

import java.time.Instant;
import java.util.UUID;

/** Marker interface for all Inscribe domain events. */
public sealed interface WorkflowEvent permits
        ItemRequestedEvent, ItemValidatedEvent, ItemRejectedEvent, ItemInsertedEvent {

    UUID containerId();
    UUID itemId();
    String workflowName();
    UUID correlationId();
    Instant timestamp();
}
