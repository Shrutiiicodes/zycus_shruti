package com.stockpulse.recommendation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReorderSuggestionRepository extends JpaRepository<ReorderSuggestion, Long> {

    boolean existsByProduct_IdAndTriggerReasonAndStatus(String productId, TriggerReason reason, SuggestionStatus status);

    List<ReorderSuggestion> findByStatus(SuggestionStatus status);

    List<ReorderSuggestion> findByProduct_IdAndStatus(String productId, SuggestionStatus status);
}