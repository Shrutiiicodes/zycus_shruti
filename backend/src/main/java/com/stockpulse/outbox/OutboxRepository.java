package com.stockpulse.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findTop10ByStatusOrderByCreatedAtAsc(String status);

    @Query("SELECT e FROM OutboxEvent e WHERE e.status = :status AND (e.lockedBy IS NULL OR e.leaseExpiry < :now) ORDER BY e.createdAt ASC")
    List<OutboxEvent> findClaimableEvents(@Param("status") String status, @Param("now") Instant now, Pageable pageable);

    @Query("SELECT e FROM OutboxEvent e WHERE e.status = :status AND (e.lockedBy IS NULL OR e.leaseExpiry < :now) ORDER BY e.createdAt ASC")
    List<OutboxEvent> findClaimableEvents(@Param("status") String status, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE OutboxEvent e SET e.lockedBy = :workerId, e.lockedAt = :now, e.leaseExpiry = :leaseExpiry WHERE e.id = :id AND e.status = 'PENDING' AND (e.lockedBy IS NULL OR e.leaseExpiry < :now)")
    int claimEvent(@Param("id") Long id, @Param("workerId") String workerId, @Param("now") Instant now, @Param("leaseExpiry") Instant leaseExpiry);
}
