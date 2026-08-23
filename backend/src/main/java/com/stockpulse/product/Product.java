package com.stockpulse.product;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false)
    private BigDecimal currentPrice;

    @Column(nullable = false)
    private Integer stockLevel;

    @Column(nullable = false)
    private Integer reorderThreshold;

    @Column(nullable = false)
    private Integer demandVelocity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    // --- Sprint 2 extension placeholders — nullable, unused today ---
    private BigDecimal costPrice;
    private String supplierId;

    /** Recomputes status from stock; call after any stock mutation. */
    public void refreshStatus() {
        if (stockLevel != null && stockLevel == 0) {
            this.status = ProductStatus.OUT_OF_STOCK;
        } else if (this.status == ProductStatus.OUT_OF_STOCK) {
            this.status = ProductStatus.ACTIVE;
        }
    }

    public boolean isBelowReorderThreshold() {
        return stockLevel != null && reorderThreshold != null && stockLevel < reorderThreshold;
    }
}