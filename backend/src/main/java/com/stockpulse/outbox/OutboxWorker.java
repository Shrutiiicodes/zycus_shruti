package com.stockpulse.outbox;

import com.stockpulse.commerce.CommerceEngineService;
import com.stockpulse.product.Product;
import com.stockpulse.product.ProductRepository;
import com.stockpulse.recommendation.TriggerReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class OutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxWorker.class);

    private final OutboxRepository outboxRepository;
    private final ProductRepository productRepository;
    private final CommerceEngineService commerceEngineService;

    public OutboxWorker(OutboxRepository outboxRepository,
                        ProductRepository productRepository,
                        CommerceEngineService commerceEngineService) {
        this.outboxRepository = outboxRepository;
        this.productRepository = productRepository;
        this.commerceEngineService = commerceEngineService;
    }

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void processPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository.findTop10ByStatusOrderByCreatedAtAsc("PENDING");
        if (pendingEvents.isEmpty()) return;

        for (OutboxEvent event : pendingEvents) {
            try {
                Product product = productRepository.findById(event.getAggregateId()).orElse(null);
                if (product != null) {
                    TriggerReason reason = TriggerReason.valueOf(event.getEventType());
                    commerceEngineService.generateSuggestions(product, reason);
                }
                event.setStatus("PROCESSED");
                event.setProcessedAt(Instant.now());
            } catch (Exception e) {
                log.error("Failed to process outbox event {}: {}", event.getId(), e.getMessage());
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= 3) {
                    event.setStatus("FAILED");
                }
            }
            outboxRepository.save(event);
        }
    }
}
