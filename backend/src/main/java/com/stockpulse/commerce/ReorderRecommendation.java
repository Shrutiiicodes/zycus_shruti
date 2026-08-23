package com.stockpulse.commerce;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReorderRecommendation {
    private final int recommendedQuantity;
    private final int suggestedLeadTimeDays;
    private final double confidence;
    private final String reasoning;
}