package com.stockpulse.outbox;

import com.stockpulse.commerce.CommerceEngineService;
import com.stockpulse.product.Category;
import com.stockpulse.product.Product;
import com.stockpulse.product.ProductRepository;
import com.stockpulse.product.ProductStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxWorkerTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CommerceEngineService commerceEngineService;

    @InjectMocks
    private OutboxWorker outboxWorker;

    @Test
    void shouldClaimAndProcessPendingEventSuccessfully() {
        OutboxEvent event = OutboxEvent.builder()
                .id(100L)
                .aggregateType("Product")
                .aggregateId("PRD-001")
                .eventType("INVENTORY_LOW")
                .status("PENDING")
                .build();

        Product product = Product.builder()
                .id("PRD-001")
                .sku("SKU-001")
                .name("Outbox Product")
                .category(Category.ELECTRONICS)
                .currentPrice(new BigDecimal("50.00"))
                .stockLevel(5)
                .reorderThreshold(10)
                .status(ProductStatus.ACTIVE)
                .build();

        when(outboxRepository.findClaimableEvents(eq("PENDING"), any(Instant.class)))
                .thenReturn(List.of(event));
        when(productRepository.findById("PRD-001")).thenReturn(Optional.of(product));

        outboxWorker.processPendingEvents();

        assertEquals("PROCESSED", event.getStatus());
        assertNotNull(event.getProcessedAt());
        assertNull(event.getLockedBy()); // Released after completion
    }
}
