package com.example.npci.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "outbox",
        indexes = {
                @Index(name = "idx_outbox_status_created",
                        columnList = "status, created_at"),

                @Index(name = "idx_outbox_retry_count",
                        columnList = "retry_count"),

                @Index(name = "idx_outbox_transaction_id",
                        columnList = "transaction_id"),

                @Index(name = "idx_outbox_processed_at",
                        columnList = "processed_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NpciOutbox {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = EventStatus.PENDING;
        this.retryCount = 0;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum EventStatus {
        PENDING,
        PROCESSING,
        SUCCESS,
        FAILED
    }
}