package com.stockpulse.commerce;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Resolves the active CommerceAdvisor by name. commerce.strategy is read on every
 * call (not injected once), so changing the property flips the strategy without a restart.
 * A future CompetitorAwareStrategy only needs @Component("competitor-aware") + this map grows automatically.
 */
@Component
public class StrategyRegistry {

    private final Map<String, CommerceAdvisor> advisorsByName;

    @Value("${commerce.strategy:rule-based}")
    private String activeStrategyName;

    public StrategyRegistry(Map<String, CommerceAdvisor> advisorsByName) {
        this.advisorsByName = advisorsByName;
    }

    public CommerceAdvisor activeAdvisor() {
        CommerceAdvisor advisor = advisorsByName.get(activeStrategyName);
        if (advisor == null) {
            throw new IllegalStateException("Unknown commerce.strategy: " + activeStrategyName
                    + " — available: " + advisorsByName.keySet());
        }
        return advisor;
    }
}