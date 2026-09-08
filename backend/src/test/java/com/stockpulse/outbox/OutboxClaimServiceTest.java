package com.stockpulse.outbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxClaimServiceTest {

    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private OutboxClaimService outboxClaimService;

    @Test
    void shouldClaimEventAtomically() {
        when(outboxRepository.claimEvent(eq(100L), eq("W-1"), any(Instant.class), any(Instant.class)))
                .thenReturn(1);

        boolean result = outboxClaimService.claimEventAtomically(100L, "W-1", Instant.now());
        assertTrue(result);
    }

    @Test
    void shouldMarkProcessed() {
        OutboxEvent event = OutboxEvent.builder()
                .id(100L)
                .status("PENDING")
                .lockedBy("W-1")
                .build();

        when(outboxRepository.findById(100L)).thenReturn(Optional.of(event));

        outboxClaimService.markProcessed(100L);

        assertEquals("PROCESSED", event.getStatus());
        assertNull(event.getLockedBy());
        assertNotNull(event.getProcessedAt());
        verify(outboxRepository).save(event);
    }
}
