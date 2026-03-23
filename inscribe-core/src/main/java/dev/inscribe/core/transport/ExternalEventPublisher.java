package dev.inscribe.core.transport;

/**
 * Strategy interface for publishing events to an external message broker.
 * Implementations are activated via Spring profiles or configuration.
 */
public interface ExternalEventPublisher {
    void publish(String topic, String eventType, String payload);
}
