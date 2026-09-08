package com.stockpulse.idempotency;

import com.stockpulse.commerce.CommerceEngineService;
import com.stockpulse.product.Category;
import com.stockpulse.product.Product;
import com.stockpulse.product.ProductRepository;
import com.stockpulse.recommendation.PricingSuggestionRepository;
import com.stockpulse.recommendation.ReorderSuggestionRepository;
import com.stockpulse.recommendation.SuggestionStatus;
import com.stockpulse.recommendation.TriggerReason;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DatabaseIdempotencyTest {

    @Autowired
    private CommerceEngineService commerceEngineService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PricingSuggestionRepository pricingSuggestionRepository;

    @Autowired
    private ReorderSuggestionRepository reorderSuggestionRepository;

    @Test
    void shouldPreventDuplicatePendingSuggestionsForSameTrigger() {
        Product p = Product.builder()
                .id("PRD-IDEMP-01")
                .sku("SKU-IDEMP-01")
                .name("Idempotent Item")
                .category(Category.ELECTRONICS)
                .currentPrice(new BigDecimal("100.00"))
                .stockLevel(5)
                .reorderThreshold(10)
                .demandVelocity(0)
                .status(com.stockpulse.product.ProductStatus.ACTIVE)
                .build();
        productRepository.save(p);

        // Run 1: Generates suggestions
        commerceEngineService.generateSuggestions(p, TriggerReason.INVENTORY_LOW);

        long pricingCountInitial = pricingSuggestionRepository
                .findByProduct_IdAndStatus("PRD-IDEMP-01", SuggestionStatus.PENDING).size();
        long reorderCountInitial = reorderSuggestionRepository
                .findByProduct_IdAndStatus("PRD-IDEMP-01", SuggestionStatus.PENDING).size();

        assertEquals(1, pricingCountInitial);
        assertEquals(1, reorderCountInitial);

        // Run 2: Duplicate trigger attempt -> Should skip duplicate creation
        commerceEngineService.generateSuggestions(p, TriggerReason.INVENTORY_LOW);

        long pricingCountAfterDuplicate = pricingSuggestionRepository
                .findByProduct_IdAndStatus("PRD-IDEMP-01", SuggestionStatus.PENDING).size();
        long reorderCountAfterDuplicate = reorderSuggestionRepository
                .findByProduct_IdAndStatus("PRD-IDEMP-01", SuggestionStatus.PENDING).size();

        assertEquals(1, pricingCountAfterDuplicate, "Duplicate trigger must not create additional PENDING pricing suggestion");
        assertEquals(1, reorderCountAfterDuplicate, "Duplicate trigger must not create additional PENDING reorder suggestion");
    }
}
