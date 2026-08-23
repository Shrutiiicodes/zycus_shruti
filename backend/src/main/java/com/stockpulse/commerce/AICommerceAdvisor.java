package com.stockpulse.commerce;

import com.stockpulse.ai.AIResponseParser;
import com.stockpulse.ai.AIResponseValidator;
import com.stockpulse.ai.LLMGateway;
import com.stockpulse.ai.PromptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("ai")
public class AICommerceAdvisor implements CommerceAdvisor {

    private static final Logger log = LoggerFactory.getLogger(AICommerceAdvisor.class);

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
            log.warn("AI advisor failed for product {} ({}), falling back to rule-based: {}",
                    product.getProductId(), trigger.getReason(), e.getMessage());
            return fallback.advise(product, trigger);
        }
    }
}