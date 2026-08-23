package com.stockpulse.commerce;

import lombok.Builder;
import lombok.Getter;

/** What every CommerceAdvisor returns — the caller never knows if this came from rules or an LLM. */
@Getter
@Builder
public class CommerceRecommendation {
    private final PricingRecommendation pricing;
    private final ReorderRecommendation reorder;
}