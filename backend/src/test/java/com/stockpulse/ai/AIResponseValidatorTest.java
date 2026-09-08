package com.stockpulse.ai;

import com.stockpulse.commerce.CommerceRecommendation;
import com.stockpulse.commerce.PricingRecommendation;
import com.stockpulse.commerce.ReorderRecommendation;
import com.stockpulse.recommendation.ChangeDirection;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AIResponseValidatorTest {

    private final AIResponseValidator validator = new AIResponseValidator();

    public AIResponseValidatorTest() {
        ReflectionTestUtils.setField(validator, "maxPriceMultiple", 5.0);
    }

    @Test
    void shouldPassValidRecommendation() {
        CommerceRecommendation rec = CommerceRecommendation.builder()
                .pricing(PricingRecommendation.builder()
                        .recommendedPrice(new BigDecimal("120.00"))
                        .direction(ChangeDirection.INCREASE)
                        .confidence(0.85)
                        .reasoning("Valid reasoning")
                        .build())
                .reorder(ReorderRecommendation.builder()
                        .recommendedQuantity(50)
                        .confidence(0.9)
                        .reasoning("Valid reorder reasoning")
                        .build())
                .build();

        assertDoesNotThrow(() -> validator.validate(rec, new BigDecimal("100.00")));
    }

    @Test
    void shouldThrowExceptionWhenPriceExceedsMaxMultiple() {
        CommerceRecommendation rec = CommerceRecommendation.builder()
                .pricing(PricingRecommendation.builder()
                        .recommendedPrice(new BigDecimal("600.00")) // 6x > 5x limit
                        .direction(ChangeDirection.INCREASE)
                        .confidence(0.85)
                        .reasoning("Absurd price increase")
                        .build())
                .reorder(ReorderRecommendation.builder()
                        .recommendedQuantity(50)
                        .confidence(0.9)
                        .build())
                .build();

        assertThrows(AIResponseValidator.AIValidationException.class, () ->
                validator.validate(rec, new BigDecimal("100.00"))
        );
    }

    @Test
    void shouldThrowExceptionWhenReorderQuantityNonPositive() {
        CommerceRecommendation rec = CommerceRecommendation.builder()
                .pricing(PricingRecommendation.builder()
                        .recommendedPrice(new BigDecimal("100.00"))
                        .direction(ChangeDirection.HOLD)
                        .confidence(0.8)
                        .build())
                .reorder(ReorderRecommendation.builder()
                        .recommendedQuantity(0) // Invalid
                        .confidence(0.8)
                        .build())
                .build();

        assertThrows(AIResponseValidator.AIValidationException.class, () ->
                validator.validate(rec, new BigDecimal("100.00"))
        );
    }

    @Test
    void shouldThrowExceptionWhenConfidenceOutOfRange() {
        CommerceRecommendation rec = CommerceRecommendation.builder()
                .pricing(PricingRecommendation.builder()
                        .recommendedPrice(new BigDecimal("100.00"))
                        .direction(ChangeDirection.HOLD)
                        .confidence(1.5) // Invalid confidence > 1.0
                        .build())
                .reorder(ReorderRecommendation.builder()
                        .recommendedQuantity(10)
                        .confidence(0.8)
                        .build())
                .build();

        assertThrows(AIResponseValidator.AIValidationException.class, () ->
                validator.validate(rec, new BigDecimal("100.00"))
        );
    }
}
