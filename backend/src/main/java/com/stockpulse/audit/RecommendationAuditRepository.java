package com.stockpulse.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationAuditRepository extends JpaRepository<RecommendationAudit, Long> {
    List<RecommendationAudit> findByProductIdOrderByTimestampDesc(String productId);
    List<RecommendationAudit> findAllByOrderByTimestampDesc();
}
