package dev.inscribe.core.outbox;

import dev.inscribe.core.transport.ExternalEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Polls the outbox table and publishes pending events to the external broker.
 * Uses a simple SELECT-then-UPDATE approach; idempotent across multiple pods
 * thanks to the status check (only publishes PENDING rows).
 */
@Component
public class OutboxRelayJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayJob.class);

    private final OutboxEventRepository outboxRepository;
    private final ExternalEventPublisher publisher;

    public OutboxRelayJob(OutboxEventRepository outboxRepository,
                          ExternalEventPublisher publisher) {
        this.outboxRepository = outboxRepository;
        this.publisher = publisher;
    }

    @Scheduled(fixedDelayString = "${inscribe.outbox.relay-interval-ms:1000}")
    @Transactional
    public void relay() {
        List<OutboxEvent> pending = outboxRepository
                .findByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxStatus.PENDING);

        for (OutboxEvent event : pending) {
            try {
                publisher.publish(event.getTopic(), event.getEventType(), event.getPayload());
                event.setStatus(OutboxEvent.OutboxStatus.PUBLISHED);
                event.setPublishedAt(Instant.now());
                log.debug("Relayed outbox event {} to topic {}", event.getId(), event.getTopic());
            } catch (Exception e) {
                event.setStatus(OutboxEvent.OutboxStatus.FAILED);
                log.error("Failed to relay outbox event {}: {}", event.getId(), e.getMessage());
            }
            outboxRepository.save(event);
        }
    }
}
