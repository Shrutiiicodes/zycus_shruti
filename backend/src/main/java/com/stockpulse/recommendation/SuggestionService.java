package com.stockpulse.recommendation;

import com.stockpulse.product.Product;
import com.stockpulse.product.ProductRepository;
import com.stockpulse.product.ProductStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * Owns the side effects of accept/reject. Suggestion status is workflow state;
 * Product owns commerce state (price, stock). Accept mutates both atomically.
 */
@Service
public class SuggestionService {

    private final PricingSuggestionRepository pricingSuggestionRepository;
    private final ReorderSuggestionRepository reorderSuggestionRepository;
    private final ProductRepository productRepository;

    public SuggestionService(PricingSuggestionRepository pricingSuggestionRepository,
                              ReorderSuggestionRepository reorderSuggestionRepository,
                              ProductRepository productRepository) {
        this.pricingSuggestionRepository = pricingSuggestionRepository;
        this.reorderSuggestionRepository = reorderSuggestionRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public PricingSuggestion decidePricing(Long id, boolean accept) {
        PricingSuggestion suggestion = pricingSuggestionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Pricing suggestion not found: " + id));
        requirePending(suggestion.getStatus());

        if (accept) {
            Product product = suggestion.getProduct();
            product.applyPriceChange(suggestion.getRecommendedPrice());
            clearPriceReviewIfNoOtherPendingPricing(product);
            productRepository.save(product);
            suggestion.setStatus(SuggestionStatus.ACCEPTED);
        } else {
            suggestion.setStatus(SuggestionStatus.REJECTED);
        }
        return pricingSuggestionRepository.save(suggestion);
    }

    @Transactional
    public ReorderSuggestion decideReorder(Long id, boolean accept) {
        ReorderSuggestion suggestion = reorderSuggestionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Reorder suggestion not found: " + id));
        requirePending(suggestion.getStatus());

        if (accept) {
            Product product = suggestion.getProduct();
            product.receiveStock(suggestion.getRecommendedQuantity());
            productRepository.save(product);
            suggestion.setStatus(SuggestionStatus.ACCEPTED);
        } else {
            suggestion.setStatus(SuggestionStatus.REJECTED);
        }
        return reorderSuggestionRepository.save(suggestion);
    }

    private void requirePending(SuggestionStatus status) {
        if (status != SuggestionStatus.PENDING) {
            throw new IllegalStateException("Suggestion is already " + status + " — cannot decide again.");
        }
    }

    /**
     * ADR 7 decision: PRICE_REVIEW_PENDING clears once no other PENDING pricing
     * suggestion remains for this product. Reorder suggestions don't affect Product.status.
     */
    private void clearPriceReviewIfNoOtherPendingPricing(Product product) {
        boolean otherPending = !pricingSuggestionRepository
                .findByProduct_IdAndStatus(product.getId(), SuggestionStatus.PENDING).isEmpty();
        if (!otherPending && product.getStatus() == ProductStatus.PRICE_REVIEW_PENDING) {
            product.setStatus(ProductStatus.ACTIVE);
        }
    }
}