package com.stockpulse.product;

import com.stockpulse.commerce.CommerceEngineService;
import com.stockpulse.product.dto.CreateProductRequest;
import com.stockpulse.recommendation.PricingSuggestion;
import com.stockpulse.recommendation.ReorderSuggestion;
import com.stockpulse.recommendation.TriggerReason;
import org.springframework.stereotype.Service;
import com.stockpulse.event.ProductSignalEvent;
import org.springframework.context.ApplicationEventPublisher;
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

    public ProductService(ProductRepository productRepository, CommerceEngineService commerceEngineService, ApplicationEventPublisher eventPublisher, TriggerEvaluator triggerEvaluator) {
        this.productRepository = productRepository;
        this.commerceEngineService = commerceEngineService;
        this.eventPublisher = eventPublisher;
        this.triggerEvaluator = triggerEvaluator;
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

    /** Phase 2: synchronous stock update, no event firing yet (added in Phase 4). */
    public Product updateStock(String id, int newStockLevel) {
        Product product = get(id);
        if (newStockLevel > product.getStockLevel()) {
            product.receiveStock(newStockLevel - product.getStockLevel());
        } else {
            product.decrementStock(product.getStockLevel() - newStockLevel);
        }
        Product saved = productRepository.save(product);
        publishTriggers(saved);
        return saved;
}

    /** Phase 2: synchronous order simulation, no event firing yet (added in Phase 4). */
    public Product placeOrder(String id, int quantity) {
        Product product = get(id);
        product.decrementStock(quantity);
        product.setDemandVelocity(product.getDemandVelocity() + quantity);
        Product saved = productRepository.save(product);
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
        eventPublisher.publishEvent(new ProductSignalEvent(product.getId(), reason));
    }
}
}