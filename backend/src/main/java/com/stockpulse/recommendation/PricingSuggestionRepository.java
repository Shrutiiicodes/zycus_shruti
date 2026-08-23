package com.stockpulse.recommendation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PricingSuggestionRepository extends JpaRepository<PricingSuggestion, Long> {

    boolean existsByProductIdAndTriggerReasonAndStatus(
            String productId, TriggerReason triggerReason, SuggestionStatus status);

    List<PricingSuggestion> findByStatus(SuggestionStatus status);

    List<PricingSuggestion> findByProductIdAndStatus(String productId, SuggestionStatus status);
}