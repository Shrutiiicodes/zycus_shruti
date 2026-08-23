package com.stockpulse.recommendation.dto;

import com.stockpulse.recommendation.ChangeDirection;
import com.stockpulse.recommendation.PricingSuggestion;
import com.stockpulse.recommendation.SuggestionStatus;
import com.stockpulse.recommendation.TriggerReason;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PricingSuggestionResponse {
    private Long id;
    private String productId;
    private BigDecimal currentPrice;
    private BigDecimal recommendedPrice;
    private ChangeDirection direction;
    private double confidence;
    private String reasoning;
    private SuggestionStatus status;
    private TriggerReason triggerReason;

    public static PricingSuggestionResponse from(PricingSuggestion s) {
        return PricingSuggestionResponse.builder()
                .id(s.getId())
                .productId(s.getProduct().getId())
                .currentPrice(s.getCurrentPrice())
                .recommendedPrice(s.getRecommendedPrice())
                .direction(s.getDirection())
                .confidence(s.getConfidence())
                .reasoning(s.getReasoning())
                .status(s.getStatus())
                .triggerReason(s.getTriggerReason())
                .build();
    }
}