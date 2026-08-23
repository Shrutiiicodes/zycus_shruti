package com.stockpulse.commerce;

import com.stockpulse.recommendation.ChangeDirection;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PricingRecommendation {
    private final BigDecimal recommendedPrice;
    private final ChangeDirection direction;
    private final double confidence;
    private final String reasoning;
}