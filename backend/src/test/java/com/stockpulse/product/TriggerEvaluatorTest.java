package com.stockpulse.product;

import com.stockpulse.recommendation.TriggerReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TriggerEvaluatorTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private TriggerEvaluator triggerEvaluator;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(triggerEvaluator, "spikeMultiplier", 3.0);
    }

    @Test
    void shouldReturnInventoryLowWhenStockIsBelowThreshold() {
        Product product = Product.builder()
                .id("PRD-001")
                .sku("SKU-001")
                .name("Low Stock Item")
                .category(Category.ELECTRONICS)
                .currentPrice(new BigDecimal("99.99"))
                .stockLevel(5)
                .reorderThreshold(10)
                .demandVelocity(2)
                .status(ProductStatus.ACTIVE)
                .build();

        when(productRepository.findByCategoryAndIdNot(eq(Category.ELECTRONICS), eq("PRD-001")))
                .thenReturn(Collections.emptyList());

        List<TriggerReason> reasons = triggerEvaluator.evaluate(product);

        assertTrue(reasons.contains(TriggerReason.INVENTORY_LOW));
        assertFalse(reasons.contains(TriggerReason.DEMAND_SPIKE));
    }

    @Test
    void shouldReturnDemandSpikeWhenVelocityExceedsMultiplier() {
        Product peer = Product.builder()
                .id("PRD-002")
                .category(Category.ELECTRONICS)
                .demandVelocity(2)
                .build();

        Product target = Product.builder()
                .id("PRD-001")
                .sku("SKU-001")
                .name("Trending Item")
                .category(Category.ELECTRONICS)
                .currentPrice(new BigDecimal("49.99"))
                .stockLevel(50)
                .reorderThreshold(10)
                .demandVelocity(10) // 10 > 3.0 * 2.0 (Category average = 2.0)
                .status(ProductStatus.ACTIVE)
                .build();

        when(productRepository.findByCategoryAndIdNot(eq(Category.ELECTRONICS), eq("PRD-001")))
                .thenReturn(List.of(peer));

        List<TriggerReason> reasons = triggerEvaluator.evaluate(target);

        assertFalse(reasons.contains(TriggerReason.INVENTORY_LOW));
        assertTrue(reasons.contains(TriggerReason.DEMAND_SPIKE));
    }

    @Test
    void shouldReturnBothTriggersWhenBothConditionsAreMet() {
        Product peer = Product.builder()
                .id("PRD-002")
                .category(Category.APPAREL)
                .demandVelocity(2)
                .build();

        Product target = Product.builder()
                .id("PRD-003")
                .sku("SKU-003")
                .name("Viral Low Stock Item")
                .category(Category.APPAREL)
                .currentPrice(new BigDecimal("29.99"))
                .stockLevel(4) // Stock < 10
                .reorderThreshold(10)
                .demandVelocity(12) // Velocity 12 > 3.0 * 2.0
                .status(ProductStatus.ACTIVE)
                .build();

        when(productRepository.findByCategoryAndIdNot(eq(Category.APPAREL), eq("PRD-003")))
                .thenReturn(List.of(peer));

        List<TriggerReason> reasons = triggerEvaluator.evaluate(target);

        assertTrue(reasons.contains(TriggerReason.INVENTORY_LOW));
        assertTrue(reasons.contains(TriggerReason.DEMAND_SPIKE));
    }
}
