package com.stockpulse.ai;

import org.springframework.stereotype.Component;

/**
 * Deterministic gateway for development/testing — proves the AI architecture
 * without depending on the real Qwen endpoint being reachable or correct.
 * Not registered under "ai" by default; wire it manually in tests, or flip
 * commerce/ai wiring to point here while the real endpoint is being sorted out.
 */
@Component("fake")
public class FakeLLMGateway implements LLMGateway {

    public enum Mode { VALID, MALFORMED_JSON, ABSURD_PRICE, TIMEOUT }

    private Mode mode = Mode.VALID;

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @Override
    public String call(String prompt) {
        return switch (mode) {
            case VALID -> """
                {"pricing":{"recommendedPrice":32.99,"direction":"INCREASE","confidence":0.82,"reasoning":"Stock is below threshold; a modest increase protects remaining units while demand stays healthy."},
                 "reorder":{"recommendedQuantity":45,"suggestedLeadTimeDays":5,"confidence":0.78,"reasoning":"Historical velocity suggests 45 units covers the next replenishment cycle."}}
                """;
            case MALFORMED_JSON -> "{\"pricing\": {\"recommendedPrice\": 32.99, \"direction\":"; // truncated on purpose
            case ABSURD_PRICE -> """
                {"pricing":{"recommendedPrice":999999.00,"direction":"INCREASE","confidence":0.4,"reasoning":"n/a"},
                 "reorder":{"recommendedQuantity":45,"suggestedLeadTimeDays":5,"confidence":0.4,"reasoning":"n/a"}}
                """;
            case TIMEOUT -> throw new RuntimeException("Simulated LLM timeout");
        };
    }
}