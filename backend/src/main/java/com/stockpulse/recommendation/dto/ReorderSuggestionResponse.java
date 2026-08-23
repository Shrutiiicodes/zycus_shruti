package com.stockpulse.recommendation.dto;

import com.stockpulse.recommendation.ReorderSuggestion;
import com.stockpulse.recommendation.SuggestionStatus;
import com.stockpulse.recommendation.TriggerReason;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReorderSuggestionResponse {
    private Long id;
    private String productId;
    private int currentStock;
    private int recommendedQuantity;
    private int suggestedLeadTimeDays;
    private double confidence;
    private String reasoning;
    private SuggestionStatus status;
    private TriggerReason triggerReason;

    public static ReorderSuggestionResponse from(ReorderSuggestion s) {
        return ReorderSuggestionResponse.builder()
                .id(s.getId())
                .productId(s.getProduct().getId())
                .currentStock(s.getCurrentStock())
                .recommendedQuantity(s.getRecommendedQuantity())
                .suggestedLeadTimeDays(s.getSuggestedLeadTimeDays())
                .confidence(s.getConfidence())
                .reasoning(s.getReasoning())
                .status(s.getStatus())
                .triggerReason(s.getTriggerReason())
                .build();
    }
}