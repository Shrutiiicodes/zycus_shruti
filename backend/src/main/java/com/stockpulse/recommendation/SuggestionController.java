package com.stockpulse.recommendation;

import com.stockpulse.recommendation.dto.PricingSuggestionResponse;
import com.stockpulse.recommendation.dto.ReorderSuggestionResponse;
import com.stockpulse.recommendation.dto.SuggestionDecisionRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SuggestionController {

    private final SuggestionService suggestionService;
    private final PricingSuggestionRepository pricingSuggestionRepository;
    private final ReorderSuggestionRepository reorderSuggestionRepository;

    public SuggestionController(SuggestionService suggestionService,
                                 PricingSuggestionRepository pricingSuggestionRepository,
                                 ReorderSuggestionRepository reorderSuggestionRepository) {
        this.suggestionService = suggestionService;
        this.pricingSuggestionRepository = pricingSuggestionRepository;
        this.reorderSuggestionRepository = reorderSuggestionRepository;
    }

    @GetMapping("/pricing-suggestions")
    public List<PricingSuggestionResponse> listPricing(@RequestParam(defaultValue = "PENDING") SuggestionStatus status) {
        return pricingSuggestionRepository.findByStatus(status).stream().map(PricingSuggestionResponse::from).toList();
    }

    @GetMapping("/reorder-suggestions")
    public List<ReorderSuggestionResponse> listReorder(@RequestParam(defaultValue = "PENDING") SuggestionStatus status) {
        return reorderSuggestionRepository.findByStatus(status).stream().map(ReorderSuggestionResponse::from).toList();
    }

    @PatchMapping("/pricing-suggestions/{id}")
    public PricingSuggestionResponse decidePricing(@PathVariable Long id, @Valid @RequestBody SuggestionDecisionRequest req) {
        boolean accept = req.getDecision() == SuggestionDecisionRequest.Decision.ACCEPT;
        return PricingSuggestionResponse.from(suggestionService.decidePricing(id, accept));
    }

    @PatchMapping("/reorder-suggestions/{id}")
    public ReorderSuggestionResponse decideReorder(@PathVariable Long id, @Valid @RequestBody SuggestionDecisionRequest req) {
        boolean accept = req.getDecision() == SuggestionDecisionRequest.Decision.ACCEPT;
        return ReorderSuggestionResponse.from(suggestionService.decideReorder(id, accept));
    }
}