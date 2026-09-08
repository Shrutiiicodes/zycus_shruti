package com.stockpulse.guardrails;

import com.stockpulse.product.Product;
import org.springframework.stereotype.Component;

@Component
public class ConfidenceScorer {

    public double calculateSystemConfidence(Product product, GuardrailResult guardrails) {
        double score = 0.50; // Base score

        // Data completeness
        if (product.getCostPrice() != null) score += 0.10;
        if (product.getSupplierId() != null) score += 0.05;

        // Signal strength & threshold precision
        if (product.isBelowReorderThreshold()) {
            double ratio = (double) product.getStockLevel() / Math.max(1, product.getReorderThreshold());
            score += Math.max(0.10, (1.0 - ratio) * 0.25);
        } else if (product.getDemandVelocity() > 0) {
            score += 0.15;
        }

        // Guardrails compliance penalty
        if (guardrails.isCooldownActive()) {
            score -= 0.15;
        }
        if (!guardrails.getAppliedRules().isEmpty()) {
            score += 0.05; // Confidence increases when mathematical policy rules explicitly validate bounds
        }

        return Math.min(0.95, Math.max(0.40, Math.round(score * 100.0) / 100.0));
    }
}
