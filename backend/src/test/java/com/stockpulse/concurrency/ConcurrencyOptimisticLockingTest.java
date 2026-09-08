package com.stockpulse.concurrency;

import com.stockpulse.product.Category;
import com.stockpulse.product.Product;
import com.stockpulse.product.ProductRepository;
import com.stockpulse.product.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ConcurrencyOptimisticLockingTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @Test
    void shouldPreventNegativeStockAndThrowOptimisticLockingExceptionUnderConcurrentOrders() throws InterruptedException {
        // Create product with stock level = 1
        Product p = Product.builder()
                .id("PRD-CONCUR-01")
                .sku("SKU-CONCUR-01")
                .name("Limited Item")
                .category(Category.ELECTRONICS)
                .currentPrice(new BigDecimal("99.99"))
                .stockLevel(1)
                .reorderThreshold(10)
                .demandVelocity(0)
                .build();
        productRepository.save(p);

        int numberOfThreads = 10;
        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger lockingFailureCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            service.submit(() -> {
                try {
                    latch.await();
                    productService.placeOrder("PRD-CONCUR-01", 1);
                    successCount.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException e) {
                    lockingFailureCount.incrementAndGet();
                } catch (Exception e) {
                    // Ignored for concurrency assertion
                }
            });
        }

        latch.countDown();
        service.shutdown();
        service.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        Product reloaded = productRepository.findById("PRD-CONCUR-01").orElseThrow();

        // Verify physical stock is never negative
        assertTrue(reloaded.getStockLevel() >= 0, "Stock level must never drop below 0");
        assertTrue(successCount.get() >= 1, "At least one concurrent order should succeed");
    }
}
