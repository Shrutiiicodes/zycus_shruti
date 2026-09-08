package com.stockpulse.guardrails;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class GuardrailResult {
    private final BigDecimal validatedPrice;
    private final int validatedQuantity;
    private final List<String> appliedRules;
    private final boolean cooldownActive;
}
