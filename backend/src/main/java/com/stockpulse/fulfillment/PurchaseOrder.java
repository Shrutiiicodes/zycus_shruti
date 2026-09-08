package com.stockpulse.fulfillment;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "purchase_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private int quantityOrdered;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private POStatus status;

    private String supplierId;

    private Long suggestionId;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant expectedArrivalDate;

    private Instant receivedAt;
}
