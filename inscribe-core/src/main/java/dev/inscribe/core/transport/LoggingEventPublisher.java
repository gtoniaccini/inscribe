package dev.inscribe.core.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Default no-op publisher used when no external broker is configured.
 * Useful for local development and testing.
 */
@Component
@ConditionalOnMissingBean({KafkaEventPublisher.class, RabbitEventPublisher.class})
public class LoggingEventPublisher implements ExternalEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);

    @Override
    public void publish(String topic, String eventType, String payload) {
        log.info("[NO-OP] Would publish to '{}' — type={}, payload={}", topic, eventType, payload);
    }
}
