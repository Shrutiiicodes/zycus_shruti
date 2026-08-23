package com.stockpulse.ai;

import com.stockpulse.commerce.ProductContext;
import com.stockpulse.commerce.TriggerContext;
import com.stockpulse.recommendation.TriggerReason;
import org.springframework.stereotype.Component;

/**
 * Two genuinely different prompts, not one document with a field swapped in —
 * inventory-low and demand-spike are different merchandising decisions.
 */
@Component
public class PromptBuilder {

    private static final String OUTPUT_CONTRACT = """
            Respond with ONLY a single JSON object, no prose before or after, in exactly this shape:
            {"pricing":{"recommendedPrice":<number>,"direction":"INCREASE"|"DECREASE"|"HOLD","confidence":<0.0-1.0>,"reasoning":"<one or two sentences>"},
             "reorder":{"recommendedQuantity":<positive integer>,"suggestedLeadTimeDays":<integer>,"confidence":<0.0-1.0>,"reasoning":"<one or two sentences>"}}
            """;

    public String build(ProductContext p, TriggerContext trigger) {
        return switch (trigger.getReason()) {
            case INVENTORY_LOW -> lowStockPrompt(p);
            case DEMAND_SPIKE -> demandSpikePrompt(p);
            default -> generalPrompt(p, trigger);
        };
    }

    private String lowStockPrompt(ProductContext p) {
        return """
            You are a merchandising advisor for an online store. Stock has dropped below the reorder threshold.

            Product: %s (%s)
            Current price: %s
            Stock level: %d (reorder threshold: %d)
            Demand velocity: %d orders/24h (category average: %.1f)

            This is a genuinely ambiguous decision. Low stock could mean:
            - RAISE the price to protect remaining inventory from selling out before restock arrives, or
            - DISCOUNT to clear inventory if this looks like a slow mover that isn't worth restocking at current velocity.
            Use the demand velocity relative to the category average to judge which situation this is, and say so explicitly in your reasoning.

            Also recommend a reorder quantity and lead time to replenish stock.

            %s
            """.formatted(p.getName(), p.getCategory(), p.getCurrentPrice(), p.getStockLevel(),
                p.getReorderThreshold(), p.getDemandVelocity(), p.getCategoryAverageVelocity(), OUTPUT_CONTRACT);
    }

    private String demandSpikePrompt(ProductContext p) {
        return """
            You are a merchandising advisor for an online store. This product's demand velocity just spiked well above its category norm.

            Product: %s (%s)
            Current price: %s
            Stock level: %d (reorder threshold: %d)
            Demand velocity: %d orders/24h — this is significantly above the category average of %.1f orders/24h

            This looks like a trending/viral item. Consider a modest price increase to capitalize on demand without killing momentum — 
            this is different from a low-stock protection increase; the goal here is capturing upside, not preventing stockout panic.
            Also recommend a reorder quantity that accounts for the elevated velocity continuing, not just current stock level.

            %s
            """.formatted(p.getName(), p.getCategory(), p.getCurrentPrice(), p.getStockLevel(),
                p.getReorderThreshold(), p.getDemandVelocity(), p.getCategoryAverageVelocity(), OUTPUT_CONTRACT);
    }

    private String generalPrompt(ProductContext p, TriggerContext trigger) {
        String note = trigger.getReason() == TriggerReason.MANUAL
                ? "This is a manual, on-demand request from merchandising — not an automated trigger."
                : "This is the initial pricing/reorder assessment for a newly listed product.";
        return """
            You are a merchandising advisor for an online store. %s

            Product: %s (%s)
            Current price: %s
            Stock level: %d (reorder threshold: %d)
            Demand velocity: %d orders/24h (category average: %.1f)

            Recommend a price adjustment (or HOLD) and a reorder quantity, with brief reasoning for each.

            %s
            """.formatted(note, p.getName(), p.getCategory(), p.getCurrentPrice(), p.getStockLevel(),
                p.getReorderThreshold(), p.getDemandVelocity(), p.getCategoryAverageVelocity(), OUTPUT_CONTRACT);
    }
}