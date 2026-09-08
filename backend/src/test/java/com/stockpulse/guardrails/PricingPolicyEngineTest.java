package com.stockpulse.guardrails;

import com.stockpulse.product.Category;
import com.stockpulse.product.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PricingPolicyEngineTest {

    private final PricingPolicyEngine policyEngine = new PricingPolicyEngine();

    @Test
    void shouldEnforceMarginFloorWhenProposedPriceIsTooLow() {
        Product product = Product.builder()
                .id("PRD-001")
                .name("Low Margin Product")
                .category(Category.ELECTRONICS)
                .currentPrice(new BigDecimal("30.00"))
                .costPrice(new BigDecimal("25.00")) // Margin floor = 25 * 1.15 = 28.75 -> rounded to 28.99
                .minimumOrderQuantity(25)
                .build();

        // Proposed price $20.00 is below $28.75 floor
        GuardrailResult result = policyEngine.validateAndEnforce(product, new BigDecimal("20.00"), 10);

        assertEquals(new BigDecimal("28.99"), result.getValidatedPrice());
        assertEquals(25, result.getValidatedQuantity()); // MOQ enforced
        assertTrue(result.getAppliedRules().stream().anyMatch(r -> r.contains("margin floor")));
    }

    @Test
    void shouldCapPriceIncreaseToCategoryMaximum() {
        Product product = Product.builder()
                .id("PRD-002")
                .name("Electronics Product")
                .category(Category.ELECTRONICS) // Max +15%
                .currentPrice(new BigDecimal("100.00"))
                .costPrice(new BigDecimal("50.00"))
                .minimumOrderQuantity(25)
                .build();

        // Proposed price $150.00 exceeds +15% cap ($115.00)
        GuardrailResult result = policyEngine.validateAndEnforce(product, new BigDecimal("150.00"), 30);

        assertEquals(new BigDecimal("115.00"), result.getValidatedPrice());
        assertTrue(result.getAppliedRules().stream().anyMatch(r -> r.contains("max +15%")));
    }
}
