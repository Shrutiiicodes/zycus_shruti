package com.stockpulse.event;

import com.stockpulse.recommendation.TriggerReason;
import lombok.Getter;

/** Published when a product mutation crosses a trigger condition. Carries only an id — the listener re-fetches fresh state. */
@Getter
public class ProductSignalEvent {
    private final String productId;
    private final TriggerReason reason;

    public ProductSignalEvent(String productId, TriggerReason reason) {
        this.productId = productId;
        this.reason = reason;
    }
}