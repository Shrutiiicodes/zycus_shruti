package com.stockpulse.audit;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "price_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private BigDecimal oldPrice;

    @Column(nullable = false)
    private BigDecimal newPrice;

    @Column(nullable = false)
    private String changedBy; // USER | SYSTEM | AUTOMATED

    private String reason;

    private Long suggestionId;

    @Column(nullable = false)
    @Builder.Default
    private Instant timestamp = Instant.now();
}
