package com.stockpulse.audit;

import com.stockpulse.recommendation.TriggerReason;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "recommendation_audits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private String suggestionType; // PRICING | REORDER

    @Column(nullable = false)
    private String strategyUsed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TriggerReason triggerReason;

    private BigDecimal calculatedPrice;

    private Integer calculatedQuantity;

    private double systemConfidence;

    @Column(length = 2000)
    private String guardrailsAppliedJson;

    @Column(length = 2000)
    private String aiReasoning;

    @Column(nullable = false)
    private String status; // ACCEPTED | REJECTED | PENDING

    private String decidedBy;

    @Column(nullable = false)
    @Builder.Default
    private Instant timestamp = Instant.now();
}
