package com.stockpulse.outbox;

import com.stockpulse.commerce.CommerceEngineService;
import com.stockpulse.product.Product;
import com.stockpulse.product.ProductRepository;
import com.stockpulse.recommendation.TriggerReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class OutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxWorker.class);

    private final OutboxRepository outboxRepository;
    private final ProductRepository productRepository;
    private final CommerceEngineService commerceEngineService;
    private final OutboxClaimService outboxClaimService;
    private final String workerId = "WORKER-" + UUID.randomUUID().toString().substring(0, 8);

    public OutboxWorker(OutboxRepository outboxRepository,
                        ProductRepository productRepository,
                        CommerceEngineService commerceEngineService,
                        OutboxClaimService outboxClaimService) {
        this.outboxRepository = outboxRepository;
        this.productRepository = productRepository;
        this.commerceEngineService = commerceEngineService;
        this.outboxClaimService = outboxClaimService;
    }

    @Scheduled(fixedDelay = 3000)
    public void processPendingEvents() {
        Instant now = Instant.now();
        List<OutboxEvent> candidates = outboxRepository.findClaimableEvents("PENDING", now, PageRequest.of(0, 10));
        if (candidates.isEmpty()) return;

        for (OutboxEvent candidate : candidates) {
            // Phase 1: Atomic Database Claim via proxy service
            boolean claimed = outboxClaimService.claimEventAtomically(candidate.getId(), workerId, now);
            if (!claimed) {
                continue; // Claimed by another concurrent worker instance
            }

            // Phase 2: Compute suggestions & AI explanations without long DB lock
            try {
                Product product = productRepository.findById(candidate.getAggregateId()).orElse(null);
                if (product != null) {
                    TriggerReason reason = TriggerReason.valueOf(candidate.getEventType());
                    commerceEngineService.generateSuggestions(product, reason);
                }
                outboxClaimService.markProcessed(candidate.getId());
            } catch (Exception e) {
                log.error("Worker {} failed to process outbox event {}: {}", workerId, candidate.getId(), e.getMessage());
                outboxClaimService.markFailed(candidate.getId(), e.getMessage());
            }
        }
    }

    public String getWorkerId() {
        return workerId;
    }
}
