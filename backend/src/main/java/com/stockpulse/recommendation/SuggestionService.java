package com.stockpulse.recommendation;

import com.stockpulse.audit.AuditService;
import com.stockpulse.fulfillment.FulfillmentService;
import com.stockpulse.product.Product;
import com.stockpulse.product.ProductRepository;
import com.stockpulse.product.ProductStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final AuditService auditService;
    private final FulfillmentService fulfillmentService;

    public SuggestionService(PricingSuggestionRepository pricingSuggestionRepository,
                              ReorderSuggestionRepository reorderSuggestionRepository,
                              ProductRepository productRepository,
                              AuditService auditService,
                              FulfillmentService fulfillmentService) {
        this.pricingSuggestionRepository = pricingSuggestionRepository;
        this.reorderSuggestionRepository = reorderSuggestionRepository;
        this.productRepository = productRepository;
        this.auditService = auditService;
        this.fulfillmentService = fulfillmentService;
    }

    @Transactional
    public PricingSuggestion decidePricing(Long id, boolean accept) {
        PricingSuggestion suggestion = pricingSuggestionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Pricing suggestion not found: " + id));
        requirePending(suggestion.getStatus());

        Product product = suggestion.getProduct();
        if (accept) {
            // Stale Recommendation Protection: verify current product price matches suggestion snapshot
            if (product.getCurrentPrice().compareTo(suggestion.getCurrentPrice()) != 0) {
                suggestion.setStatus(SuggestionStatus.EXPIRED);
                pricingSuggestionRepository.save(suggestion);
                clearPriceReviewIfNoOtherPendingPricing(product);
                productRepository.save(product);
                throw new IllegalStateException("Suggestion is stale: product price has changed from "
                        + suggestion.getCurrentPrice() + " to " + product.getCurrentPrice() + ". Please regenerate.");
            }

            BigDecimal oldPrice = product.getCurrentPrice();
            product.applyPriceChange(suggestion.getRecommendedPrice());
            suggestion.setStatus(SuggestionStatus.ACCEPTED);
            pricingSuggestionRepository.save(suggestion);
            clearPriceReviewIfNoOtherPendingPricing(product);
            productRepository.save(product);

            auditService.recordPriceChange(
                    product.getId(),
                    oldPrice,
                    suggestion.getRecommendedPrice(),
                    "MERCHANDISER",
                    suggestion.getReasoning(),
                    suggestion.getId()
            );

            auditService.recordRecommendationAudit(
                    product.getId(),
                    "PRICING",
                    "COMMERCE_ENGINE",
                    suggestion.getTriggerReason(),
                    suggestion.getRecommendedPrice(),
                    null,
                    suggestion.getConfidence(),
                    "{}",
                    suggestion.getReasoning(),
                    "ACCEPTED",
                    "MERCHANDISER"
            );

            return suggestion;
        } else {
            suggestion.setStatus(SuggestionStatus.REJECTED);
            PricingSuggestion saved = pricingSuggestionRepository.save(suggestion);
            clearPriceReviewIfNoOtherPendingPricing(product);
            productRepository.save(product);

            auditService.recordRecommendationAudit(
                    product.getId(),
                    "PRICING",
                    "COMMERCE_ENGINE",
                    suggestion.getTriggerReason(),
                    suggestion.getRecommendedPrice(),
                    null,
                    suggestion.getConfidence(),
                    "{}",
                    suggestion.getReasoning(),
                    "REJECTED",
                    "MERCHANDISER"
            );

            return saved;
        }
    }

    @Transactional
    public ReorderSuggestion decideReorder(Long id, boolean accept) {
        ReorderSuggestion suggestion = reorderSuggestionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Reorder suggestion not found: " + id));
        requirePending(suggestion.getStatus());

        Product product = suggestion.getProduct();
        if (accept) {
            suggestion.setStatus(SuggestionStatus.ACCEPTED);
            ReorderSuggestion saved = reorderSuggestionRepository.save(suggestion);

            // Realistic Purchase Order Workflow: Creates PO & updates incomingStock
            fulfillmentService.createPurchaseOrder(product.getId(), suggestion.getRecommendedQuantity(), suggestion.getId());

            auditService.recordRecommendationAudit(
                    product.getId(),
                    "REORDER",
                    "COMMERCE_ENGINE",
                    suggestion.getTriggerReason(),
                    null,
                    suggestion.getRecommendedQuantity(),
                    suggestion.getConfidence(),
                    "{}",
                    suggestion.getReasoning(),
                    "ACCEPTED",
                    "MERCHANDISER"
            );

            return saved;
        } else {
            suggestion.setStatus(SuggestionStatus.REJECTED);
            ReorderSuggestion saved = reorderSuggestionRepository.save(suggestion);

            auditService.recordRecommendationAudit(
                    product.getId(),
                    "REORDER",
                    "COMMERCE_ENGINE",
                    suggestion.getTriggerReason(),
                    null,
                    suggestion.getRecommendedQuantity(),
                    suggestion.getConfidence(),
                    "{}",
                    suggestion.getReasoning(),
                    "REJECTED",
                    "MERCHANDISER"
            );

            return saved;
        }
    }

    private void requirePending(SuggestionStatus status) {
        if (status != SuggestionStatus.PENDING) {
            throw new IllegalStateException("Suggestion is already " + status + " — cannot decide again.");
        }
    }

    private void clearPriceReviewIfNoOtherPendingPricing(Product product) {
        boolean otherPending = !pricingSuggestionRepository
                .findByProduct_IdAndStatus(product.getId(), SuggestionStatus.PENDING).isEmpty();
        if (!otherPending && product.getStatus() == ProductStatus.PRICE_REVIEW_PENDING) {
            product.setStatus(ProductStatus.ACTIVE);
        }
    }
}