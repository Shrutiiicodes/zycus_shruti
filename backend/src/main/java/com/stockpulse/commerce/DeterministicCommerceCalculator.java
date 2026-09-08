package com.stockpulse.commerce;

import com.stockpulse.forecasting.DemandForecastService;
import com.stockpulse.product.Product;
import com.stockpulse.recommendation.ChangeDirection;
import com.stockpulse.recommendation.TriggerReason;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class DeterministicCommerceCalculator {

    private final DemandForecastService forecastService;

    public DeterministicCommerceCalculator(DemandForecastService forecastService) {
        this.forecastService = forecastService;
    }

    public CommerceRecommendation calculate(ProductContext p, TriggerContext trigger, Product product) {
        // --- 1. Deterministic Reorder Calculation ---
        // Formula: max(MOQ, (expectedLeadTimeDemand + safetyStock - currentStock - incomingStock))
        int leadTimeDemand = forecastService.calculateLeadTimeDemand(product);
        int safetyStock = product.getSafetyStock() > 0 ? product.getSafetyStock() : 10;
        int currentStock = product.getStockLevel();
        int incomingStock = product.getIncomingStock();
        int moq = product.getMinimumOrderQuantity() > 0 ? product.getMinimumOrderQuantity() : 25;

        int rawReorderQty = (leadTimeDemand + safetyStock) - (currentStock + incomingStock);
        int calculatedQuantity = rawReorderQty <= 0 ? 0 : Math.max(moq, rawReorderQty);
        String reorderReasoning = calculatedQuantity == 0
                ? "Current inventory (" + (currentStock + incomingStock) + ") satisfies lead-time demand (" + leadTimeDemand + ") and safety stock (" + safetyStock + "); no reorder required."
                : "Calculated based on expected lead-time demand (" + leadTimeDemand + " units) + safety stock (" + safetyStock + ") minus stock on hand/in-transit (" + (currentStock + incomingStock) + "), floored at MOQ (" + moq + ").";

        // --- 2. Deterministic Pricing Calculation ---
        BigDecimal currentPrice = p.getCurrentPrice();
        BigDecimal calculatedPrice = currentPrice;
        ChangeDirection direction = ChangeDirection.HOLD;
        String baseReasoning;

        if (trigger.getReason() == TriggerReason.INVENTORY_LOW || product.isBelowReorderThreshold()) {
            calculatedPrice = currentPrice.multiply(new BigDecimal("1.10")).setScale(2, RoundingMode.HALF_UP);
            direction = ChangeDirection.INCREASE;
            baseReasoning = "Inventory low (stock " + currentStock + " < threshold " + product.getReorderThreshold() + "); baseline 10% scarcity price adjustment.";
        } else if (trigger.getReason() == TriggerReason.DEMAND_SPIKE || forecastService.calculateVelocityRatio(product) > 2.0) {
            calculatedPrice = currentPrice.multiply(new BigDecimal("1.05")).setScale(2, RoundingMode.HALF_UP);
            direction = ChangeDirection.INCREASE;
            baseReasoning = "Demand velocity (" + product.getDemandVelocity() + ") exceeds category norm; baseline 5% price adjustment.";
        } else {
            baseReasoning = "No threshold crossed; price held steady.";
        }

        return CommerceRecommendation.builder()
                .pricing(PricingRecommendation.builder()
                        .recommendedPrice(calculatedPrice)
                        .direction(direction)
                        .confidence(0.85)
                        .reasoning(baseReasoning)
                        .build())
                .reorder(ReorderRecommendation.builder()
                        .recommendedQuantity(calculatedQuantity)
                        .suggestedLeadTimeDays(product.getLeadTimeDays() > 0 ? product.getLeadTimeDays() : 7)
                        .confidence(0.85)
                        .reasoning(reorderReasoning)
                        .build())
                .build();
    }
}
