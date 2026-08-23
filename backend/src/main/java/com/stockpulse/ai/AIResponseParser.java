package com.stockpulse.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpulse.commerce.CommerceRecommendation;
import com.stockpulse.commerce.PricingRecommendation;
import com.stockpulse.commerce.ReorderRecommendation;
import com.stockpulse.recommendation.ChangeDirection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AIResponseParser {

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