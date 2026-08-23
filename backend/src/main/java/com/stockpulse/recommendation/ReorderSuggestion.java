package com.stockpulse.recommendation;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "reorder_suggestions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReorderSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private Integer currentStock;

    @Column(nullable = false)
    private Integer recommendedQuantity;

    private Integer suggestedLeadTimeDays;

    @Column(nullable = false)
    private Double confidence;

    @Column(length = 2000)
    private String reasoning;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuggestionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TriggerReason triggerReason;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}