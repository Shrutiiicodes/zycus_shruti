package com.stockpulse.product;

import com.stockpulse.recommendation.TriggerReason;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Trigger semantics, made explicit (not left to event-ordering accident):
 * - INVENTORY_LOW: stock < reorderThreshold, evaluated after any stock mutation.
 * - DEMAND_SPIKE: velocity > spikeMultiplier x category average, evaluated after any order.
 * Both are evaluated independently — if a product satisfies both conditions at once,
 * both reasons are returned and each gets its own idempotent suggestion pair (see
 * CommerceEngineService's per-triggerReason dedup — ADR entry 7).
 */
@Component
public class TriggerEvaluator {

    private final ProductRepository productRepository;

    @Value("${commerce.demand-spike-multiplier:3}")
    private double spikeMultiplier;

    public TriggerEvaluator(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<TriggerReason> evaluate(Product product) {
        List<TriggerReason> reasons = new ArrayList<>();

        if (product.isBelowReorderThreshold()) {
            reasons.add(TriggerReason.INVENTORY_LOW);
        }

        double categoryAverage = categoryAverageVelocityExcluding(product);
        if (categoryAverage > 0 && product.getDemandVelocity() > spikeMultiplier * categoryAverage) {
            reasons.add(TriggerReason.DEMAND_SPIKE);
        }

        return reasons;
    }

    private double categoryAverageVelocityExcluding(Product product) {
        List<Product> peers = productRepository.findByCategoryAndIdNot(product.getCategory(), product.getId());
        return peers.isEmpty() ? 0.0
                : peers.stream().mapToInt(Product::getDemandVelocity).average().orElse(0.0);
    }
}