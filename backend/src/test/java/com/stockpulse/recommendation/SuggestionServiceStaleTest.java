package com.stockpulse.recommendation;

import com.stockpulse.audit.AuditService;
import com.stockpulse.fulfillment.FulfillmentService;
import com.stockpulse.product.Category;
import com.stockpulse.product.Product;
import com.stockpulse.product.ProductRepository;
import com.stockpulse.product.ProductStatus;
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
    private SuggestionService suggestionService;

    @BeforeEach
    void setUp() {
        pricingSuggestionRepository = mock(PricingSuggestionRepository.class);
        reorderSuggestionRepository = mock(ReorderSuggestionRepository.class);
        productRepository = mock(ProductRepository.class);
        auditService = mock(AuditService.class);
        fulfillmentService = mock(FulfillmentService.class);

        suggestionService = new SuggestionService(
                pricingSuggestionRepository,
                reorderSuggestionRepository,
                productRepository,
                auditService,
                fulfillmentService
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
}
