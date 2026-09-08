package com.stockpulse.recommendation;

import com.stockpulse.audit.AuditService;
import com.stockpulse.fulfillment.FulfillmentService;
import com.stockpulse.product.Category;
import com.stockpulse.product.Product;
import com.stockpulse.product.ProductRepository;
import com.stockpulse.product.ProductStatus;
import com.stockpulse.guardrails.PricingPolicyEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SuggestionServiceStaleTest {

    private PricingSuggestionRepository pricingSuggestionRepository;
    private ReorderSuggestionRepository reorderSuggestionRepository;
    private ProductRepository productRepository;
    private AuditService auditService;
    private FulfillmentService fulfillmentService;
    private PricingPolicyEngine pricingPolicyEngine;
    private SuggestionService suggestionService;

    @BeforeEach
    void setUp() {
        pricingSuggestionRepository = mock(PricingSuggestionRepository.class);
        reorderSuggestionRepository = mock(ReorderSuggestionRepository.class);
        productRepository = mock(ProductRepository.class);
        auditService = mock(AuditService.class);
        fulfillmentService = mock(FulfillmentService.class);
        pricingPolicyEngine = mock(PricingPolicyEngine.class);

        suggestionService = new SuggestionService(
                pricingSuggestionRepository,
                reorderSuggestionRepository,
                productRepository,
                auditService,
                fulfillmentService,
                pricingPolicyEngine
        );
    }

    @Test
    @DisplayName("Should expire suggestion and throw IllegalStateException when price has changed since generation")
    void testStalePricingSuggestionRejection() {
        Product product = Product.builder()
                .id("PRD-1")
                .sku("SKU-1")
                .name("Test Product")
                .category(Category.ELECTRONICS)
                .currentPrice(new BigDecimal("150.00")) // Price changed from 100 to 150
                .stockLevel(10)
                .status(ProductStatus.PRICE_REVIEW_PENDING)
                .build();

        PricingSuggestion suggestion = PricingSuggestion.builder()
                .id(1L)
                .product(product)
                .currentPrice(new BigDecimal("100.00")) // Snapshot at generation time
                .recommendedPrice(new BigDecimal("110.00"))
                .direction(ChangeDirection.INCREASE)
                .confidence(0.9)
                .status(SuggestionStatus.PENDING)
                .triggerReason(TriggerReason.INVENTORY_LOW)
                .build();

        when(pricingSuggestionRepository.findById(1L)).thenReturn(Optional.of(suggestion));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                suggestionService.decidePricing(1L, true)
        );

        assertTrue(ex.getMessage().contains("stale"));
        assertEquals(SuggestionStatus.EXPIRED, suggestion.getStatus());
        assertEquals(new BigDecimal("150.00"), product.getCurrentPrice()); // Price NOT overwritten to 110!
        verify(pricingSuggestionRepository).save(suggestion);
    }

    @Test
    @DisplayName("Should expire reorder suggestion and throw IllegalStateException when stock has changed since generation")
    void testStaleReorderSuggestionRejection() {
        Product product = Product.builder()
                .id("PRD-2")
                .sku("SKU-2")
                .name("Stock Item")
                .category(Category.ELECTRONICS)
                .stockLevel(50) // Stock changed from 5 to 50
                .incomingStock(0)
                .version(10L)
                .status(ProductStatus.ACTIVE)
                .build();

        ReorderSuggestion suggestion = ReorderSuggestion.builder()
                .id(2L)
                .product(product)
                .currentStock(5) // Snapshot at generation time
                .incomingStock(0)
                .productVersion(10L)
                .recommendedQuantity(100)
                .confidence(0.85)
                .status(SuggestionStatus.PENDING)
                .triggerReason(TriggerReason.INVENTORY_LOW)
                .build();

        when(reorderSuggestionRepository.findById(2L)).thenReturn(Optional.of(suggestion));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                suggestionService.decideReorder(2L, true)
        );

        assertTrue(ex.getMessage().contains("stale"));
        assertEquals(SuggestionStatus.EXPIRED, suggestion.getStatus());
        verify(reorderSuggestionRepository).save(suggestion);
        verify(fulfillmentService, never()).createPurchaseOrder(any(), anyInt(), any());
    }

    @Test
    @DisplayName("Should expire reorder suggestion when incomingStock or version has changed since generation")
    void testStaleReorderIncomingStockChange() {
        Product product = Product.builder()
                .id("PRD-3")
                .sku("SKU-3")
                .name("In Transit Item")
                .category(Category.ELECTRONICS)
                .stockLevel(5)
                .incomingStock(100) // PO was placed, incoming changed 0 -> 100
                .version(11L)
                .status(ProductStatus.ACTIVE)
                .build();

        ReorderSuggestion suggestion = ReorderSuggestion.builder()
                .id(3L)
                .product(product)
                .currentStock(5)
                .incomingStock(0) // Snapshot at 10:00 AM
                .productVersion(10L)
                .recommendedQuantity(100)
                .confidence(0.85)
                .status(SuggestionStatus.PENDING)
                .triggerReason(TriggerReason.INVENTORY_LOW)
                .build();

        when(reorderSuggestionRepository.findById(3L)).thenReturn(Optional.of(suggestion));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                suggestionService.decideReorder(3L, true)
        );

        assertTrue(ex.getMessage().contains("stale"));
        assertEquals(SuggestionStatus.EXPIRED, suggestion.getStatus());
        verify(reorderSuggestionRepository).save(suggestion);
        verify(fulfillmentService, never()).createPurchaseOrder(any(), anyInt(), any());
    }
}
