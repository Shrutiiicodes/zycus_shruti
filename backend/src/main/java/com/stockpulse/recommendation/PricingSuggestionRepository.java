package com.stockpulse.recommendation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PricingSuggestionRepository extends JpaRepository<PricingSuggestion, Long> {

    boolean existsByProduct_IdAndTriggerReasonAndStatus(String productId, TriggerReason reason, SuggestionStatus status);

    List<PricingSuggestion> findByStatus(SuggestionStatus status);

    List<PricingSuggestion> findByProduct_IdAndStatus(String productId, SuggestionStatus status);
}