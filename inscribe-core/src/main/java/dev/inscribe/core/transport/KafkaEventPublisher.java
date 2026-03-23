package dev.inscribe.core.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "inscribe.transport", havingValue = "kafka")
public class KafkaEventPublisher implements ExternalEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(String topic, String eventType, String payload) {
        kafkaTemplate.send(topic, eventType, payload);
        log.debug("Published to Kafka topic {}: {}", topic, eventType);
    }
}
