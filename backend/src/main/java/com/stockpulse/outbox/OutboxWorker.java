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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Component
public class OutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxWorker.class);

    private final OutboxRepository outboxRepository;
    private final ProductRepository productRepository;
    private final CommerceEngineService commerceEngineService;
    private final String workerId = "WORKER-" + UUID.randomUUID().toString().substring(0, 8);

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
        Instant now = Instant.now();
        List<OutboxEvent> claimableEvents = outboxRepository.findClaimableEvents("PENDING", now);
        if (claimableEvents.isEmpty()) return;

        for (OutboxEvent event : claimableEvents) {
            // Multi-instance Event Claiming with Lease
            event.setLockedBy(workerId);
            event.setLockedAt(now);
            event.setLeaseExpiry(now.plus(30, ChronoUnit.SECONDS));
            outboxRepository.save(event);

            try {
                Product product = productRepository.findById(event.getAggregateId()).orElse(null);
                if (product != null) {
                    TriggerReason reason = TriggerReason.valueOf(event.getEventType());
                    commerceEngineService.generateSuggestions(product, reason);
                }
                event.setStatus("PROCESSED");
                event.setProcessedAt(Instant.now());
                event.setLockedBy(null);
            } catch (Exception e) {
                log.error("Worker {} failed to process outbox event {}: {}", workerId, event.getId(), e.getMessage());
                event.setRetryCount(event.getRetryCount() + 1);
                event.setLockedBy(null);
                if (event.getRetryCount() >= 3) {
                    event.setStatus("FAILED");
                }
            }
            outboxRepository.save(event);
        }
    }

    public String getWorkerId() {
        return workerId;
    }
}
