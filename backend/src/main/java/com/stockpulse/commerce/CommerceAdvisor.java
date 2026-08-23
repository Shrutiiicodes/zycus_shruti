package com.stockpulse.commerce;

/**
 * The one contract both HTTP on-demand endpoints and the async agentic loop call.
 * Implementations: RuleBasedCommerceAdvisor (Phase 2), AICommerceAdvisor (Phase 3).
 */
public interface CommerceAdvisor {
    CommerceRecommendation advise(ProductContext product, TriggerContext trigger);
}