package com.stockpulse.event;

import com.stockpulse.commerce.CommerceEngineService;
import com.stockpulse.product.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * The async half of the agentic loop. Runs off the request thread — the HTTP
 * call that published the event has already returned by the time this fires.
 * Uses the same CommerceAdvisor contract (via CommerceEngineService) as the
 * on-demand endpoints — no separate code path for automated vs manual.
 */
@Component
public class RecommendationEventListener {

    private static final Logger log = LoggerFactory.getLogger(RecommendationEventListener.class);

    private final CommerceEngineService commerceEngineService;
    private final ProductRepository productRepository;

    public RecommendationEventListener(CommerceEngineService commerceEngineService,
                                        ProductRepository productRepository) {
        this.commerceEngineService = commerceEngineService;
        this.productRepository = productRepository;
    }

    @Async("taskExecutor")
    @EventListener
    public void onProductSignal(ProductSignalEvent event) {
        productRepository.findById(event.getProductId()).ifPresentOrElse(
                product -> {
                    log.info("Agentic loop firing: product={} reason={}", product.getId(), event.getReason());
                    commerceEngineService.generateSuggestions(product, event.getReason());
                },
                () -> log.warn("Agentic loop: product {} not found, dropping signal", event.getProductId())
        );
    }
}