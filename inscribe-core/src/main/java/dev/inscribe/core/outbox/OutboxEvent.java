package dev.inscribe.core.outbox;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Persisted in the same JPA transaction as the domain data.
 * A scheduled relay job publishes PENDING events to the external broker.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant publishedAt;

    protected OutboxEvent() {}

    public OutboxEvent(String topic, String eventType, String payload) {
        this.topic = topic;
        this.eventType = eventType;
        this.payload = payload;
    }

    public UUID getId() { return id; }
    public String getTopic() { return topic; }
    public String getPayload() { return payload; }
    public String getEventType() { return eventType; }
    public OutboxStatus getStatus() { return status; }
    public void setStatus(OutboxStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }

    public enum OutboxStatus { PENDING, PUBLISHED, FAILED }
}
