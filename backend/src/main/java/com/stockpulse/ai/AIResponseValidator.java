package com.stockpulse.ai;

import com.stockpulse.commerce.CommerceRecommendation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AIResponseValidator {

    @Value("${ai.max-price-multiple:5.0}")
    private double maxPriceMultiple;

    /** Throws AIValidationException if anything is out of sane bounds — caller decides fallback behavior. */
    public void validate(CommerceRecommendation rec, BigDecimal currentPrice) {
        var pricing = rec.getPricing();
        var reorder = rec.getReorder();

        if (pricing.getRecommendedPrice() == null || pricing.getRecommendedPrice().signum() <= 0) {
            throw new AIValidationException("Recommended price must be positive");
        }
        BigDecimal upperBound = currentPrice.multiply(BigDecimal.valueOf(maxPriceMultiple));
        if (pricing.getRecommendedPrice().compareTo(upperBound) > 0) {
            throw new AIValidationException("Recommended price " + pricing.getRecommendedPrice()
                    + " exceeds " + maxPriceMultiple + "x current price (" + currentPrice + ") without a documented justification");
        }
        if (pricing.getDirection() == null) {
            throw new AIValidationException("Missing price change direction");
        }
        if (pricing.getConfidence() < 0.0 || pricing.getConfidence() > 1.0) {
            throw new AIValidationException("Pricing confidence out of range: " + pricing.getConfidence());
        }

        if (reorder.getRecommendedQuantity() <= 0) {
            throw new AIValidationException("Recommended reorder quantity must be a positive integer");
        }
        if (reorder.getConfidence() < 0.0 || reorder.getConfidence() > 1.0) {
            throw new AIValidationException("Reorder confidence out of range: " + reorder.getConfidence());
        }
    }

    public static class AIValidationException extends RuntimeException {
        public AIValidationException(String message) { super(message); }
    }
}