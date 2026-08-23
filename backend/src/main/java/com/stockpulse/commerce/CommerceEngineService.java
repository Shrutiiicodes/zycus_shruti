package com.stockpulse.commerce;

import com.stockpulse.product.Product;
import com.stockpulse.product.ProductRepository;
import com.stockpulse.recommendation.*;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Single place where a Product turns into persisted suggestions.
 * Called synchronously from on-demand endpoints (Phase 2) and asynchronously
 * from the event listener (Phase 4) — same method, same contract either way.
 */
@Service
public class CommerceEngineService {

    private final StrategyRegistry strategyRegistry;
    private final ProductRepository productRepository;
    private final PricingSuggestionRepository pricingSuggestionRepository;
    private final ReorderSuggestionRepository reorderSuggestionRepository;

    public CommerceEngineService(StrategyRegistry strategyRegistry,
                                  ProductRepository productRepository,
                                  PricingSuggestionRepository pricingSuggestionRepository,
                                  ReorderSuggestionRepository reorderSuggestionRepository) {
        this.strategyRegistry = strategyRegistry;
        this.productRepository = productRepository;
        this.pricingSuggestionRepository = pricingSuggestionRepository;
        this.reorderSuggestionRepository = reorderSuggestionRepository;
    }

    public static class GeneratedSuggestions {
        public Optional<PricingSuggestion> pricing = Optional.empty();
        public Optional<ReorderSuggestion> reorder = Optional.empty();
    }

    /**
     * Idempotency rule: skip creating a suggestion if a PENDING one already exists
     * for the same product + triggerReason + suggestion type.
     */
    public GeneratedSuggestions generateSuggestions(Product product, TriggerReason reason) {
        GeneratedSuggestions result = new GeneratedSuggestions();

        ProductContext context = buildContext(product);
        TriggerContext trigger = TriggerContext.builder().reason(reason).build();
        CommerceRecommendation recommendation = strategyRegistry.activeAdvisor().advise(context, trigger);

        boolean pricingPending = pricingSuggestionRepository
                .existsByProduct_IdAndTriggerReasonAndStatus(product.getId(), reason, SuggestionStatus.PENDING);
        if (!pricingPending) {
            PricingSuggestion saved = pricingSuggestionRepository.save(PricingSuggestion.builder()
                    .product(product)
                    .currentPrice(product.getCurrentPrice())
                    .recommendedPrice(recommendation.getPricing().getRecommendedPrice())
                    .direction(recommendation.getPricing().getDirection())
                    .confidence(recommendation.getPricing().getConfidence())
                    .reasoning(recommendation.getPricing().getReasoning())
                    .status(SuggestionStatus.PENDING)
                    .triggerReason(reason)
                    .build());
            result.pricing = Optional.of(saved);
            if (product.getStatus() == com.stockpulse.product.ProductStatus.ACTIVE) {
                product.setStatus(com.stockpulse.product.ProductStatus.PRICE_REVIEW_PENDING);
                productRepository.save(product);
            }
        }

        boolean reorderPending = reorderSuggestionRepository
                .existsByProduct_IdAndTriggerReasonAndStatus(product.getId(), reason, SuggestionStatus.PENDING);
        if (!reorderPending) {
            ReorderSuggestion saved = reorderSuggestionRepository.save(ReorderSuggestion.builder()
                    .product(product)
                    .currentStock(product.getStockLevel())
                    .recommendedQuantity(recommendation.getReorder().getRecommendedQuantity())
                    .suggestedLeadTimeDays(recommendation.getReorder().getSuggestedLeadTimeDays())
                    .confidence(recommendation.getReorder().getConfidence())
                    .reasoning(recommendation.getReorder().getReasoning())
                    .status(SuggestionStatus.PENDING)
                    .triggerReason(reason)
                    .build());
            result.reorder = Optional.of(saved);
        }

        return result;
    }

    private ProductContext buildContext(Product product) {
        List<Product> peers = productRepository.findByCategoryAndIdNot(product.getCategory(), product.getId());
        double categoryAverageVelocity = peers.isEmpty() ? 0.0
                : peers.stream().mapToInt(Product::getDemandVelocity).average().orElse(0.0);

        return ProductContext.builder()
                .productId(product.getId())
                .name(product.getName())
                .category(product.getCategory())
                .currentPrice(product.getCurrentPrice())
                .stockLevel(product.getStockLevel())
                .reorderThreshold(product.getReorderThreshold())
                .demandVelocity(product.getDemandVelocity())
                .categoryAverageVelocity(categoryAverageVelocity)
                .build();
    }
}