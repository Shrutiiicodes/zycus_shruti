package com.stockpulse.commerce;

import com.stockpulse.recommendation.TriggerReason;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TriggerContext {
    private final TriggerReason reason;
    private final String note;
}