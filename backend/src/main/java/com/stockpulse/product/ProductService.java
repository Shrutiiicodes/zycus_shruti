package com.stockpulse.product;

import com.stockpulse.audit.AuditService;
import com.stockpulse.audit.TransactionType;
import com.stockpulse.commerce.CommerceEngineService;
import com.stockpulse.event.ProductSignalEvent;
import com.stockpulse.outbox.OutboxService;
import com.stockpulse.product.dto.CreateProductRequest;
import com.stockpulse.recommendation.PricingSuggestion;
import com.stockpulse.recommendation.ReorderSuggestion;
import com.stockpulse.recommendation.TriggerReason;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CommerceEngineService commerceEngineService;
    private final ApplicationEventPublisher eventPublisher;
    private final TriggerEvaluator triggerEvaluator;
    private final OutboxService outboxService;
    private final AuditService auditService;

    public ProductService(ProductRepository productRepository,
                           CommerceEngineService commerceEngineService,
                           ApplicationEventPublisher eventPublisher,
                           TriggerEvaluator triggerEvaluator,
                           OutboxService outboxService,
                           AuditService auditService) {
        this.productRepository = productRepository;
        this.commerceEngineService = commerceEngineService;
        this.eventPublisher = eventPublisher;
        this.triggerEvaluator = triggerEvaluator;
        this.outboxService = outboxService;
        this.auditService = auditService;
    }

    public Product create(CreateProductRequest req) {
        Product product = Product.builder()
                .id("PRD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .sku(req.getSku())
                .name(req.getName())
                .category(req.getCategory())
                .currentPrice(req.getCurrentPrice())
                .stockLevel(req.getStockLevel())
                .reorderThreshold(req.getReorderThreshold())
                .demandVelocity(req.getDemandVelocity())
                .status(req.getStockLevel() == 0 ? ProductStatus.OUT_OF_STOCK : ProductStatus.ACTIVE)
                .build();
        return productRepository.save(product);
    }

    public List<Product> list(Optional<ProductStatus> status, Optional<Category> category) {
        if (status.isPresent() && category.isPresent()) {
            return productRepository.findByStatusAndCategory(status.get(), category.get());
        }
        if (status.isPresent()) {
            return productRepository.findByStatus(status.get());
        }
        if (category.isPresent()) {
            return productRepository.findByCategory(category.get());
        }
        return productRepository.findAll();
    }

    public Product get(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + id));
    }

    public Product updateStock(String id, int newStockLevel) {
        Product product = get(id);
        int delta = newStockLevel - product.getStockLevel();
        if (newStockLevel > product.getStockLevel()) {
            product.receiveStock(delta);
        } else {
            product.decrementStock(-delta);
        }
        Product saved = productRepository.save(product);

        auditService.recordInventoryTransaction(id, delta, saved.getStockLevel(), TransactionType.STOCK_ADJUSTMENT, "MANUAL_ADJUSTMENT");
        publishTriggers(saved);
        return saved;
    }

    public Product placeOrder(String id, int quantity) {
        Product product = get(id);
        product.decrementStock(quantity);
        product.setDemandVelocity(product.getDemandVelocity() + quantity);
        Product saved = productRepository.save(product);

        auditService.recordInventoryTransaction(id, -quantity, saved.getStockLevel(), TransactionType.SALE, "ORDER-SIMULATION");
        publishTriggers(saved);
        return saved;
    }

    public PricingSuggestion suggestPricingOnDemand(String id) {
        Product product = get(id);
        var generated = commerceEngineService.generateSuggestions(product, TriggerReason.MANUAL);
        return generated.pricing.orElseThrow(() ->
                new IllegalStateException("A PENDING manual pricing suggestion already exists for " + id));
    }

    public ReorderSuggestion suggestReorderOnDemand(String id) {
        Product product = get(id);
        var generated = commerceEngineService.generateSuggestions(product, TriggerReason.MANUAL);
        return generated.reorder.orElseThrow(() ->
                new IllegalStateException("A PENDING manual reorder suggestion already exists for " + id));
    }

    private void publishTriggers(Product product) {
        for (TriggerReason reason : triggerEvaluator.evaluate(product)) {
            // Spring in-memory async event
            eventPublisher.publishEvent(new ProductSignalEvent(product.getId(), reason));
            // Durable transactional outbox event
            outboxService.publishEvent("Product", product.getId(), reason.name(), "{}");
        }
    }
}