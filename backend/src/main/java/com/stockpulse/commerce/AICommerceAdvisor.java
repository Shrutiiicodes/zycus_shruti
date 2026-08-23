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
            AIResponseParser.CommerceRecommendation parsed = parser.parse(raw);
            CommerceRecommendation recommendation = toCommerceRecommendation(parsed);
            validator.validate(recommendation, product.getCurrentPrice());
            return recommendation;
        } catch (Exception e) {
            log.warn("AI advisor failed for product {} ({}), falling back to rule-based: {}",
                    product, trigger, e.getMessage());
            return fallback.advise(product, trigger);
        }
    }

    private CommerceRecommendation toCommerceRecommendation(AIResponseParser.CommerceRecommendation parsed) {
        try {
            for (java.lang.reflect.Constructor<?> constructor : CommerceRecommendation.class.getConstructors()) {
                java.lang.reflect.Parameter[] parameters = constructor.getParameters();
                java.lang.reflect.RecordComponent[] components = parsed.getClass().getRecordComponents();
                if (components != null && parameters.length == components.length) {
                    Object[] values = new Object[components.length];
                    for (int i = 0; i < components.length; i++) {
                        values[i] = components[i].getAccessor().invoke(parsed);
                    }
                    return (CommerceRecommendation) constructor.newInstance(values);
                }
            }
            throw new IllegalStateException("No compatible CommerceRecommendation constructor");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to convert AI recommendation", e);
        }
    }
}