package com.stockpulse.commerce;

import com.stockpulse.recommendation.ChangeDirection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("rule-based")
public class RuleBasedCommerceAdvisor implements CommerceAdvisor {

    private static final BigDecimal INCREASE_10 = new BigDecimal("1.10");
    private static final BigDecimal INCREASE_5 = new BigDecimal("1.05");

    @Override
    public CommerceRecommendation advise(ProductContext p, TriggerContext trigger) {
        return CommerceRecommendation.builder()
                .pricing(pricingRecommendation(p))
                .reorder(reorderRecommendation(p))
                .build();
    }

    private PricingRecommendation pricingRecommendation(ProductContext p) {
        if (p.getStockLevel() < p.getReorderThreshold()) {
            BigDecimal recommended = p.getCurrentPrice().multiply(INCREASE_10).setScale(2, RoundingMode.HALF_UP);
            return PricingRecommendation.builder()
                    .recommendedPrice(recommended)
                    .direction(ChangeDirection.INCREASE)
                    .confidence(0.6)
                    .reasoning("Stock (" + p.getStockLevel() + ") is below reorder threshold ("
                            + p.getReorderThreshold() + "); rule-based baseline raises price 10% to protect remaining inventory.")
                    .build();
        }

        if (p.getCategoryAverageVelocity() > 0 && p.getDemandVelocity() > 2 * p.getCategoryAverageVelocity()) {
            BigDecimal recommended = p.getCurrentPrice().multiply(INCREASE_5).setScale(2, RoundingMode.HALF_UP);
            return PricingRecommendation.builder()
                    .recommendedPrice(recommended)
                    .direction(ChangeDirection.INCREASE)
                    .confidence(0.55)
                    .reasoning("Demand velocity (" + p.getDemandVelocity() + ") is more than 2x the category average ("
                            + p.getCategoryAverageVelocity() + "); rule-based baseline raises price 5% to capitalize on demand.")
                    .build();
        }

        return PricingRecommendation.builder()
                .recommendedPrice(p.getCurrentPrice())
                .direction(ChangeDirection.HOLD)
                .confidence(0.5)
                .reasoning("No inventory or demand signal crossed a threshold; holding current price.")
                .build();
    }

    private ReorderRecommendation reorderRecommendation(ProductContext p) {
        int quantity = Math.max(1, (p.getReorderThreshold() * 3) - p.getStockLevel());
        return ReorderRecommendation.builder()
                .recommendedQuantity(quantity)
                .suggestedLeadTimeDays(7)
                .confidence(0.5)
                .reasoning("Baseline target of 3x reorder threshold minus current stock ("
                        + p.getStockLevel() + "), floored at 1 unit.")
                .build();
    }
}