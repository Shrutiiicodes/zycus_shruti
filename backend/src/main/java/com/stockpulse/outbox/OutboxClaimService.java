package com.stockpulse.outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class OutboxClaimService {

    private final OutboxRepository outboxRepository;

    public OutboxClaimService(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public boolean claimEventAtomically(Long eventId, String workerId, Instant now) {
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
}
