package dev.inscribe.core.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "inscribe.transport", havingValue = "rabbit")
public class RabbitEventPublisher implements ExternalEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public RabbitEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(String topic, String eventType, String payload) {
        rabbitTemplate.convertAndSend(topic, eventType, payload);
        log.debug("Published to RabbitMQ exchange {}: {}", topic, eventType);
    }
}
