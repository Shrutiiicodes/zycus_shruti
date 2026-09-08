package com.stockpulse.outbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String aggregateType; // Product

    @Column(nullable = false)
    private String aggregateId;   // productId

    @Column(nullable = false)
    private String eventType;     // INVENTORY_LOW | DEMAND_SPIKE

    @Column(length = 2000)
    private String payloadJson;

    @Column(nullable = false)
    private String status;        // PENDING | PROCESSED | FAILED

    @Builder.Default
    private int retryCount = 0;

    private String lockedBy;
    private Instant lockedAt;
    private Instant leaseExpiry;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant processedAt;
}
