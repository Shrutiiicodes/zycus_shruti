package com.stockpulse.commerce;

import com.stockpulse.product.Category;
import com.stockpulse.recommendation.ChangeDirection;
import com.stockpulse.recommendation.TriggerReason;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class RuleBasedCommerceAdvisorTest {

    private final RuleBasedCommerceAdvisor advisor = new RuleBasedCommerceAdvisor();

    @Test
    void shouldRecommendTenPercentIncreaseWhenStockIsBelowThreshold() {
        ProductContext ctx = ProductContext.builder()
                .productId("PRD-001")
                .name("Test Product")
                .category(Category.ELECTRONICS)
                .currentPrice(new BigDecimal("100.00"))
                .stockLevel(5)
                .reorderThreshold(10)
                .demandVelocity(1)
                .categoryAverageVelocity(2.0)
                .build();

        TriggerContext trigger = TriggerContext.builder()
                .reason(TriggerReason.INVENTORY_LOW)
                .build();

        CommerceRecommendation rec = advisor.advise(ctx, trigger);

        assertEquals(new BigDecimal("110.00"), rec.getPricing().getRecommendedPrice());
        assertEquals(ChangeDirection.INCREASE, rec.getPricing().getDirection());
        assertEquals(25, rec.getReorder().getRecommendedQuantity()); // (10 * 3) - 5 = 25
    }

    @Test
    void shouldRecommendFivePercentIncreaseWhenDemandVelocitySpikes() {
        ProductContext ctx = ProductContext.builder()
                .productId("PRD-002")
                .name("Spike Product")
                .category(Category.ELECTRONICS)
                .currentPrice(new BigDecimal("50.00"))
                .stockLevel(30)
                .reorderThreshold(10)
                .demandVelocity(10)
                .categoryAverageVelocity(3.0)
                .build();

        TriggerContext trigger = TriggerContext.builder()
                .reason(TriggerReason.DEMAND_SPIKE)
                .build();

        CommerceRecommendation rec = advisor.advise(ctx, trigger);

        assertEquals(new BigDecimal("52.50"), rec.getPricing().getRecommendedPrice());
        assertEquals(ChangeDirection.INCREASE, rec.getPricing().getDirection());
    }

    @Test
    void shouldHoldPriceWhenNoSignalsTrigger() {
        ProductContext ctx = ProductContext.builder()
                .productId("PRD-003")
                .name("Normal Product")
                .category(Category.ELECTRONICS)
                .currentPrice(new BigDecimal("50.00"))
                .stockLevel(30)
                .reorderThreshold(10)
                .demandVelocity(2)
                .categoryAverageVelocity(3.0)
                .build();

        TriggerContext trigger = TriggerContext.builder()
                .reason(TriggerReason.MANUAL)
                .build();

        CommerceRecommendation rec = advisor.advise(ctx, trigger);

        assertEquals(new BigDecimal("50.00"), rec.getPricing().getRecommendedPrice());
        assertEquals(ChangeDirection.HOLD, rec.getPricing().getDirection());
    }
}
