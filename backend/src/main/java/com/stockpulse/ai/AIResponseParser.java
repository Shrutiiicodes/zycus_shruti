package com.stockpulse.ai;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class AIResponseParser {

    public enum ChangeDirection { INCREASE, DECREASE, HOLD }

    public static final class CommerceRecommendation {
        private final PricingRecommendation pricing;
        private final ReorderRecommendation reorder;

        private CommerceRecommendation(PricingRecommendation pricing, ReorderRecommendation reorder) {
            this.pricing = pricing;
            this.reorder = reorder;
        }

        public PricingRecommendation getPricing() { return pricing; }
        public ReorderRecommendation getReorder() { return reorder; }

        public static Builder builder() { return new Builder(); }
        public static final class Builder {
            private PricingRecommendation pricing;
            private ReorderRecommendation reorder;
            public Builder pricing(PricingRecommendation value) { pricing = value; return this; }
            public Builder reorder(ReorderRecommendation value) { reorder = value; return this; }
            public CommerceRecommendation build() { return new CommerceRecommendation(pricing, reorder); }
        }
    }

    public static final class PricingRecommendation {
        private BigDecimal recommendedPrice;
        private ChangeDirection direction;
        private double confidence;
        private String reasoning;
        public static Builder builder() { return new Builder(); }
        public static final class Builder {
            private final PricingRecommendation value = new PricingRecommendation();
            public Builder recommendedPrice(BigDecimal v) { value.recommendedPrice = v; return this; }
            public Builder direction(ChangeDirection v) { value.direction = v; return this; }
            public Builder confidence(double v) { value.confidence = v; return this; }
            public Builder reasoning(String v) { value.reasoning = v; return this; }
            public PricingRecommendation build() { return value; }
        }
    }

    public static final class ReorderRecommendation {
        private int recommendedQuantity;
        private int suggestedLeadTimeDays;
        private double confidence;
        private String reasoning;
        public static Builder builder() { return new Builder(); }
        public static final class Builder {
            private final ReorderRecommendation value = new ReorderRecommendation();
            public Builder recommendedQuantity(int v) { value.recommendedQuantity = v; return this; }
            public Builder suggestedLeadTimeDays(int v) { value.suggestedLeadTimeDays = v; return this; }
            public Builder confidence(double v) { value.confidence = v; return this; }
            public Builder reasoning(String v) { value.reasoning = v; return this; }
            public ReorderRecommendation build() { return value; }
        }
    }

    private final ObjectMapper mapper = new ObjectMapper();

    /** Throws AIParsingException on anything malformed — caller decides fallback behavior. */
    public CommerceRecommendation parse(String rawResponse) {
        try {
            String json = extractJsonObject(rawResponse);
            JsonNode root = mapper.readTree(json);

            JsonNode pricingNode = requireField(root, "pricing");
            JsonNode reorderNode = requireField(root, "reorder");

            PricingRecommendation pricing = PricingRecommendation.builder()
                    .recommendedPrice(new BigDecimal(requireField(pricingNode, "recommendedPrice").asText()))
                    .direction(ChangeDirection.valueOf(requireField(pricingNode, "direction").asText()))
                    .confidence(requireField(pricingNode, "confidence").asDouble())
                    .reasoning(requireField(pricingNode, "reasoning").asText())
                    .build();

            ReorderRecommendation reorder = ReorderRecommendation.builder()
                    .recommendedQuantity(requireField(reorderNode, "recommendedQuantity").asInt())
                    .suggestedLeadTimeDays(requireField(reorderNode, "suggestedLeadTimeDays").asInt())
                    .confidence(requireField(reorderNode, "confidence").asDouble())
                    .reasoning(requireField(reorderNode, "reasoning").asText())
                    .build();

            return CommerceRecommendation.builder().pricing(pricing).reorder(reorder).build();
        } catch (Exception e) {
            throw new AIParsingException("Could not parse AI response: " + e.getMessage(), e);
        }
    }

    private JsonNode requireField(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            throw new AIParsingException("Missing required field: " + field);
        }
        return value;
    }

    /** LLMs sometimes wrap JSON in prose or code fences despite instructions — grab the outermost {...}. */
    private String extractJsonObject(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start == -1 || end == -1 || end < start) {
            throw new AIParsingException("No JSON object found in response");
        }
        return raw.substring(start, end + 1);
    }

    public static class AIParsingException extends RuntimeException {
        public AIParsingException(String message) { super(message); }
        public AIParsingException(String message, Throwable cause) { super(message, cause); }
    }
}