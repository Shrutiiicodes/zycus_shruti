package com.stockpulse.recommendation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReorderSuggestionRepository extends JpaRepository<ReorderSuggestion, Long> {

    boolean existsByProductIdAndTriggerReasonAndStatus(
            String productId, TriggerReason triggerReason, SuggestionStatus status);

    List<ReorderSuggestion> findByStatus(SuggestionStatus status);

    List<ReorderSuggestion> findByProductIdAndStatus(String productId, SuggestionStatus status);
}