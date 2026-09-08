package com.stockpulse.audit;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/price-history")
    public List<PriceHistory> getPriceHistory(@RequestParam(required = false) String productId) {
        return auditService.getPriceHistory(productId);
    }

    @GetMapping("/inventory-transactions")
    public List<InventoryTransaction> getInventoryTransactions(@RequestParam(required = false) String productId) {
        return auditService.getInventoryTransactions(productId);
    }

    @GetMapping("/recommendations")
    public List<RecommendationAudit> getRecommendationAudits(@RequestParam(required = false) String productId) {
        return auditService.getRecommendationAudits(productId);
    }
}
