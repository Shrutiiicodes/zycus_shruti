package com.stockpulse.product;

import com.stockpulse.audit.AuditService;
import com.stockpulse.commerce.CommerceEngineService;
import com.stockpulse.outbox.OutboxService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductServiceCorrectnessTest {

    private ProductRepository productRepository;
    private CommerceEngineService commerceEngineService;
    private TriggerEvaluator triggerEvaluator;
    private OutboxService outboxService;
    private AuditService auditService;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        commerceEngineService = mock(CommerceEngineService.class);
        triggerEvaluator = mock(TriggerEvaluator.class);
        outboxService = mock(OutboxService.class);
        auditService = mock(AuditService.class);

        productService = new ProductService(
                productRepository,
                commerceEngineService,
                triggerEvaluator,
                outboxService,
                auditService
        );
    }

    @Test
    @DisplayName("Should throw IllegalStateException and prevent overselling when order quantity > stock")
    void testOversellPrevention() {
        Product product = Product.builder()
                .id("PRD-100")
                .sku("SKU-100")
                .name("Widget")
                .category(Category.ELECTRONICS)
                .currentPrice(new BigDecimal("100.00"))
                .stockLevel(5)
                .reorderThreshold(10)
                .status(ProductStatus.ACTIVE)
                .build();

        when(productRepository.findById("PRD-100")).thenReturn(Optional.of(product));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                productService.placeOrder("PRD-100", 10)
        );

        assertTrue(ex.getMessage().contains("Insufficient stock"));
        assertEquals(5, product.getStockLevel());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when placing order with zero or negative quantity")
    void testNegativeOrderQuantity() {
        Product product = Product.builder()
                .id("PRD-100")
                .stockLevel(10)
                .build();

        when(productRepository.findById("PRD-100")).thenReturn(Optional.of(product));

        assertThrows(IllegalArgumentException.class, () -> productService.placeOrder("PRD-100", 0));
        assertThrows(IllegalArgumentException.class, () -> productService.placeOrder("PRD-100", -5));
        assertEquals(10, product.getStockLevel());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when updating stock to negative level")
    void testNegativeStockUpdate() {
        assertThrows(IllegalArgumentException.class, () -> productService.updateStock("PRD-100", -10));
    }
}
