package com.stockpulse.audit;

import com.stockpulse.recommendation.TriggerReason;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AuditService {

    private final PriceHistoryRepository priceHistoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final RecommendationAuditRepository recommendationAuditRepository;

    public AuditService(PriceHistoryRepository priceHistoryRepository,
                        InventoryTransactionRepository inventoryTransactionRepository,
                        RecommendationAuditRepository recommendationAuditRepository) {
        this.priceHistoryRepository = priceHistoryRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.recommendationAuditRepository = recommendationAuditRepository;
    }

    @Transactional
    public void recordPriceChange(String productId, BigDecimal oldPrice, BigDecimal newPrice, String changedBy, String reason, Long suggestionId) {
        PriceHistory history = PriceHistory.builder()
                .productId(productId)
                .oldPrice(oldPrice)
                .newPrice(newPrice)
                .changedBy(changedBy)
                .reason(reason)
                .suggestionId(suggestionId)
                .build();
        priceHistoryRepository.save(history);
    }

    @Transactional
    public void recordInventoryTransaction(String productId, int delta, int newStockLevel, TransactionType type, String refId) {
        InventoryTransaction tx = InventoryTransaction.builder()
                .productId(productId)
                .quantityDelta(delta)
                .newStockLevel(newStockLevel)
                .transactionType(type)
                .referenceId(refId)
                .build();
        inventoryTransactionRepository.save(tx);
    }

    @Transactional
    public void recordRecommendationAudit(String productId, String type, String strategy, TriggerReason reason,
                                           BigDecimal calcPrice, Integer calcQty, double confidence,
                                           String guardrailsJson, String aiReasoning, String status, String decidedBy) {
        RecommendationAudit audit = RecommendationAudit.builder()
                .productId(productId)
                .suggestionType(type)
                .strategyUsed(strategy)
                .triggerReason(reason)
                .calculatedPrice(calcPrice)
                .calculatedQuantity(calcQty)
                .systemConfidence(confidence)
                .guardrailsAppliedJson(guardrailsJson)
                .aiReasoning(aiReasoning)
                .status(status)
                .decidedBy(decidedBy)
                .build();
        recommendationAuditRepository.save(audit);
    }

    public List<PriceHistory> getPriceHistory(String productId) {
        return productId == null || productId.isBlank()
                ? priceHistoryRepository.findAllByOrderByTimestampDesc()
                : priceHistoryRepository.findByProductIdOrderByTimestampDesc(productId);
    }

    public List<InventoryTransaction> getInventoryTransactions(String productId) {
        return productId == null || productId.isBlank()
                ? inventoryTransactionRepository.findAllByOrderByTimestampDesc()
                : inventoryTransactionRepository.findByProductIdOrderByTimestampDesc(productId);
    }

    public List<RecommendationAudit> getRecommendationAudits(String productId) {
        return productId == null || productId.isBlank()
                ? recommendationAuditRepository.findAllByOrderByTimestampDesc()
                : recommendationAuditRepository.findByProductIdOrderByTimestampDesc(productId);
    }
}
