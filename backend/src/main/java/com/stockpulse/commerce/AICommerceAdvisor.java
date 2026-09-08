package com.stockpulse.commerce;

import com.stockpulse.ai.AIResponseParser;
import com.stockpulse.ai.AIResponseValidator;
import com.stockpulse.ai.LLMGateway;
import com.stockpulse.ai.PromptBuilder;
import com.stockpulse.guardrails.ConfidenceScorer;
import com.stockpulse.guardrails.GuardrailResult;
import com.stockpulse.guardrails.PricingPolicyEngine;
import com.stockpulse.product.Product;
import com.stockpulse.product.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Production AI Commerce Advisor Architecture:
 * 1. Deterministic calculation computes numerical prices and quantities mathematically.
 * 2. PricingPolicyEngine enforces margin floors, category caps, cooldowns, and MOQ.
 * 3. ConfidenceScorer computes system confidence mathematically.
 * 4. LLM provides contextual explanation for the validated numbers.
 */
@Component("ai")
public class AICommerceAdvisor implements CommerceAdvisor {

    private static final Logger log = LoggerFactory.getLogger(AICommerceAdvisor.class);

    private final LLMGateway llmGateway;
    private final PromptBuilder promptBuilder;
    private final AIResponseParser parser;
    private final AIResponseValidator validator;
    private final RuleBasedCommerceAdvisor fallback;
    private final DeterministicCommerceCalculator calculator;
    private final PricingPolicyEngine policyEngine;
    private final ConfidenceScorer confidenceScorer;
    private final ProductRepository productRepository;

    public AICommerceAdvisor(@Qualifier("qwen") LLMGateway llmGateway,
                              PromptBuilder promptBuilder,
                              AIResponseParser parser,
                              AIResponseValidator validator,
                              RuleBasedCommerceAdvisor fallback,
                              DeterministicCommerceCalculator calculator,
                              PricingPolicyEngine policyEngine,
                              ConfidenceScorer confidenceScorer,
                              ProductRepository productRepository) {
        this.llmGateway = llmGateway;
        this.promptBuilder = promptBuilder;
        this.parser = parser;
        this.validator = validator;
        this.fallback = fallback;
        this.calculator = calculator;
        this.policyEngine = policyEngine;
        this.confidenceScorer = confidenceScorer;
        this.productRepository = productRepository;
    }

    @Override
    public CommerceRecommendation advise(ProductContext productCtx, TriggerContext trigger) {
        try {
            Product product = productRepository.findById(productCtx.getProductId()).orElse(null);

            // Step 1: Deterministic Mathematical Calculation
            CommerceRecommendation calculatedRec = calculator.calculate(productCtx, trigger, product != null ? product : toFallbackProduct(productCtx));

            // Step 2: Enforce Business Guardrails
            GuardrailResult guardrails = policyEngine.validateAndEnforce(
                    product != null ? product : toFallbackProduct(productCtx),
                    calculatedRec.getPricing().getRecommendedPrice(),
                    calculatedRec.getReorder().getRecommendedQuantity()
            );

            // Step 3: Compute System Confidence Mathematically
            double confidence = confidenceScorer.calculateSystemConfidence(
                    product != null ? product : toFallbackProduct(productCtx),
                    guardrails
            );

            // Step 4: Optional LLM Explanation Layer
            String aiReasoning = getAiExplanationOrDefault(productCtx, trigger, calculatedRec.getPricing().getReasoning());

            String guardrailsNote = guardrails.getAppliedRules().isEmpty()
                    ? ""
                    : " [Guardrails Applied: " + String.join("; ", guardrails.getAppliedRules()) + "]";

            PricingRecommendation pricing = PricingRecommendation.builder()
                    .recommendedPrice(guardrails.getValidatedPrice())
                    .direction(calculatedRec.getPricing().getDirection())
                    .confidence(confidence)
                    .reasoning(aiReasoning + guardrailsNote)
                    .build();

            ReorderRecommendation reorder = ReorderRecommendation.builder()
                    .recommendedQuantity(guardrails.getValidatedQuantity())
                    .suggestedLeadTimeDays(calculatedRec.getReorder().getSuggestedLeadTimeDays())
                    .confidence(confidence)
                    .reasoning(calculatedRec.getReorder().getReasoning())
                    .build();

            CommerceRecommendation recommendation = CommerceRecommendation.builder()
                    .pricing(pricing)
                    .reorder(reorder)
                    .build();

            validator.validate(recommendation, productCtx.getCurrentPrice());
            return recommendation;

        } catch (Exception e) {
            log.warn("AI advisor fallback triggered for product {} ({}): {}",
                    productCtx.getProductId(), trigger.getReason(), e.getMessage());
            return fallback.advise(productCtx, trigger);
        }
    }

    private String getAiExplanationOrDefault(ProductContext productCtx, TriggerContext trigger, String defaultReasoning) {
        try {
            String prompt = promptBuilder.build(productCtx, trigger);
            String raw = llmGateway.call(prompt);
            AIResponseParser.CommerceRecommendation parsed = parser.parse(raw);
            if (parsed != null && parsed.getPricing() != null && parsed.getPricing().getReasoning() != null) {
                return parsed.getPricing().getReasoning();
            }
        } catch (Exception e) {
            log.debug("LLM explanation call skipped or failed, using deterministic reasoning: {}", e.getMessage());
        }
        return defaultReasoning;
    }

    private Product toFallbackProduct(ProductContext p) {
        return Product.builder()
                .id(p.getProductId())
                .name(p.getName())
                .category(p.getCategory())
                .currentPrice(p.getCurrentPrice())
                .stockLevel(p.getStockLevel())
                .reorderThreshold(p.getReorderThreshold())
                .demandVelocity(p.getDemandVelocity())
                .build();
    }
}