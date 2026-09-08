package com.stockpulse.recommendation;

import com.stockpulse.audit.AuditService;
import com.stockpulse.fulfillment.FulfillmentService;
import com.stockpulse.guardrails.GuardrailResult;
import com.stockpulse.guardrails.PricingPolicyEngine;
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
    private final PricingPolicyEngine pricingPolicyEngine;

    public SuggestionService(PricingSuggestionRepository pricingSuggestionRepository,
                              ReorderSuggestionRepository reorderSuggestionRepository,
                              ProductRepository productRepository,
                              AuditService auditService,
                              FulfillmentService fulfillmentService,
                              PricingPolicyEngine pricingPolicyEngine) {
        this.pricingSuggestionRepository = pricingSuggestionRepository;
        this.reorderSuggestionRepository = reorderSuggestionRepository;
        this.productRepository = productRepository;
        this.auditService = auditService;
        this.fulfillmentService = fulfillmentService;
        this.pricingPolicyEngine = pricingPolicyEngine;
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

            // Revalidate Pricing Guardrails against latest product state at acceptance time
            GuardrailResult guardrailResult = pricingPolicyEngine.validateAndEnforce(product, suggestion.getRecommendedPrice(), 0);
            BigDecimal finalPrice = guardrailResult.getValidatedPrice();

            BigDecimal oldPrice = product.getCurrentPrice();
            product.applyPriceChange(finalPrice);
            suggestion.setStatus(SuggestionStatus.ACCEPTED);
            pricingSuggestionRepository.save(suggestion);
            clearPriceReviewIfNoOtherPendingPricing(product);
            productRepository.save(product);

            auditService.recordPriceChange(
                    product.getId(),
                    oldPrice,
                    finalPrice,
                    "MERCHANDISER",
                    suggestion.getReasoning(),
                    suggestion.getId()
            );

            auditService.recordRecommendationAudit(
                    product.getId(),
                    "PRICING",
                    "COMMERCE_ENGINE",
                    suggestion.getTriggerReason(),
                    finalPrice,
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
            // Stale Recommendation Protection for Inventory Reorders
            if (product.getStockLevel() != suggestion.getCurrentStock()) {
                suggestion.setStatus(SuggestionStatus.EXPIRED);
                reorderSuggestionRepository.save(suggestion);
                throw new IllegalStateException("Suggestion is stale: inventory stock level has changed from "
                        + suggestion.getCurrentStock() + " to " + product.getStockLevel() + ". Please regenerate.");
            }

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