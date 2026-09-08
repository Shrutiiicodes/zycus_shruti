package com.stockpulse.guardrails;

import com.stockpulse.product.Category;
import com.stockpulse.product.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class PricingPolicyEngine {

    private static final BigDecimal DEFAULT_MARGIN_FLOOR_PERCENT = new BigDecimal("0.15"); // 15% margin floor
    private static final BigDecimal MAX_DECREASE_PERCENT = new BigDecimal("0.25"); // 25% max decrease
    private static final long COOLDOWN_HOURS = 24;

    public GuardrailResult validateAndEnforce(Product product, BigDecimal proposedPrice, int proposedQuantity) {
        List<String> rulesApplied = new ArrayList<>();

        // 1. Cooldown Check
        boolean cooldownActive = false;
        if (product.getLastPriceChangeTimestamp() != null) {
            long hoursSinceLastChange = Duration.between(product.getLastPriceChangeTimestamp(), Instant.now()).toHours();
            if (hoursSinceLastChange < COOLDOWN_HOURS) {
                cooldownActive = true;
                rulesApplied.add("Price cooldown active (last change " + hoursSinceLastChange + "h ago < " + COOLDOWN_HOURS + "h window): price change blocked.");
            }
        }

        BigDecimal adjustedPrice = cooldownActive ? product.getCurrentPrice() : proposedPrice;

        // 2. Margin Floor Enforcement
        BigDecimal cost = product.getCostPrice() != null ? product.getCostPrice() : product.getCurrentPrice().multiply(new BigDecimal("0.60"));
        BigDecimal marginFloor = cost.multiply(BigDecimal.ONE.add(DEFAULT_MARGIN_FLOOR_PERCENT)).setScale(2, RoundingMode.HALF_UP);

        if (adjustedPrice.compareTo(marginFloor) < 0) {
            adjustedPrice = marginFloor;
            rulesApplied.add("Enforced 15% margin floor: price raised from proposed to $" + marginFloor);
        }

        // 3. Psychological Price Rounding (.99)
        if (adjustedPrice.compareTo(new BigDecimal("10.00")) > 0) {
            BigDecimal rounded = adjustedPrice.setScale(0, RoundingMode.FLOOR).add(new BigDecimal("0.99"));
            if (rounded.compareTo(adjustedPrice) != 0) {
                adjustedPrice = rounded;
                rulesApplied.add("Applied .99 psychological price rounding");
            }
        }

        // 4. Category Increase Caps
        BigDecimal maxIncreasePercent = getCategoryMaxIncreasePercent(product.getCategory());
        BigDecimal maxPriceCap = product.getCurrentPrice().multiply(BigDecimal.ONE.add(maxIncreasePercent)).setScale(2, RoundingMode.HALF_UP);

        if (adjustedPrice.compareTo(maxPriceCap) > 0) {
            adjustedPrice = maxPriceCap;
            rulesApplied.add("Enforced category (" + product.getCategory() + ") max +" + (maxIncreasePercent.multiply(new BigDecimal("100")).intValue()) + "% price increase cap");
        }

        // 5. Max Decrease Cap
        BigDecimal maxDecreaseCap = product.getCurrentPrice().multiply(BigDecimal.ONE.subtract(MAX_DECREASE_PERCENT)).setScale(2, RoundingMode.HALF_UP);
        if (adjustedPrice.compareTo(maxDecreaseCap) < 0) {
            adjustedPrice = maxDecreaseCap;
            rulesApplied.add("Enforced max -25% price decrease limit");
        }

        // 6. Quantity Minimum (MOQ)
        int moq = product.getMinimumOrderQuantity() > 0 ? product.getMinimumOrderQuantity() : 25;
        int adjustedQuantity = proposedQuantity <= 0 ? 0 : Math.max(moq, proposedQuantity);
        if (proposedQuantity > 0 && adjustedQuantity > proposedQuantity) {
            rulesApplied.add("Enforced Minimum Order Quantity (MOQ: " + moq + ")");
        }

        return GuardrailResult.builder()
                .validatedPrice(adjustedPrice)
                .validatedQuantity(adjustedQuantity)
                .appliedRules(rulesApplied)
                .cooldownActive(cooldownActive)
                .build();
    }

    private BigDecimal getCategoryMaxIncreasePercent(Category category) {
        if (category == null) return new BigDecimal("0.15");
        return switch (category) {
            case ELECTRONICS -> new BigDecimal("0.15"); // Max +15%
            case APPAREL -> new BigDecimal("0.20");     // Max +20%
            case HOME -> new BigDecimal("0.10");        // Max +10%
        };
    }
}
