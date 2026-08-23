package com.stockpulse.commerce;

import com.stockpulse.ai.AIResponseParser;
import com.stockpulse.ai.AIResponseValidator;
import com.stockpulse.ai.LLMGateway;
import com.stockpulse.ai.PromptBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * AI implementation of the shared contract. On ANY failure — timeout, malformed
 * JSON, quota error, out-of-bounds values — falls back to the full rule-based
 * pair (never a partial/silent-drop result), per the unified-contract decision.
 */
@Component("ai")
public class AICommerceAdvisor implements CommerceAdvisor {

    private final LLMGateway llmGateway;
    private final PromptBuilder promptBuilder;
    private final AIResponseParser parser;
    private final AIResponseValidator validator;
    private final RuleBasedCommerceAdvisor fallback;

    public AICommerceAdvisor(@Qualifier("qwen") LLMGateway llmGateway,
                              PromptBuilder promptBuilder,
                              AIResponseParser parser,
                              AIResponseValidator validator,
                              RuleBasedCommerceAdvisor fallback) {
        this.llmGateway = llmGateway;
        this.promptBuilder = promptBuilder;
        this.parser = parser;
        this.validator = validator;
        this.fallback = fallback;
    }

    @Override
    public CommerceRecommendation advise(ProductContext product, TriggerContext trigger) {
        try {
            String prompt = promptBuilder.build(product, trigger);
            String raw = llmGateway.call(prompt);
            CommerceRecommendation recommendation = parser.parse(raw);
            validator.validate(recommendation, product.getCurrentPrice());
            return recommendation;
        } catch (Exception e) {
            return fallback.advise(product, trigger);
        }
    }
}