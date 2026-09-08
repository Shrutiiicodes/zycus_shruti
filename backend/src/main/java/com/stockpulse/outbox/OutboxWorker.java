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
    public void processPendingEvents() {
        Instant now = Instant.now();
        List<OutboxEvent> candidates = outboxRepository.findClaimableEvents("PENDING", now, PageRequest.of(0, 10));
        if (candidates.isEmpty()) return;

        for (OutboxEvent candidate : candidates) {
            // Phase 1: Atomic Database Claim
            boolean claimed = claimEventAtomically(candidate.getId(), now);
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
                markProcessed(candidate.getId());
            } catch (Exception e) {
                log.error("Worker {} failed to process outbox event {}: {}", workerId, candidate.getId(), e.getMessage());
                markFailed(candidate.getId(), e.getMessage());
            }
        }
    }

    @Transactional
    public boolean claimEventAtomically(Long eventId, Instant now) {
        Instant leaseExpiry = now.plus(30, ChronoUnit.SECONDS);
        int rowsUpdated = outboxRepository.claimEvent(eventId, workerId, now, leaseExpiry);
        return rowsUpdated == 1;
    }

    @Transactional
    public void markProcessed(Long eventId) {
        outboxRepository.findById(eventId).ifPresent(event -> {
            event.setStatus("PROCESSED");
            event.setProcessedAt(Instant.now());
            event.setLockedBy(null);
            event.setLastError(null);
            outboxRepository.save(event);
        });
    }

    @Transactional
    public void markFailed(Long eventId, String errorMessage) {
        outboxRepository.findById(eventId).ifPresent(event -> {
            event.setRetryCount(event.getRetryCount() + 1);
            event.setLockedBy(null);
            if (errorMessage != null && errorMessage.length() > 2000) {
                event.setLastError(errorMessage.substring(0, 1997) + "...");
            } else {
                event.setLastError(errorMessage);
            }
            if (event.getRetryCount() >= 3) {
                event.setStatus("FAILED");
            }
            outboxRepository.save(event);
        });
    }

    public String getWorkerId() {
        return workerId;
    }
}
