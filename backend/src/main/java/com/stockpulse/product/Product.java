package com.stockpulse.product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    private int stockLevel;

    @Column(nullable = false)
    private int reorderThreshold;

    /** Orders placed in the last 24h — bumped by /orders, read by rule + AI strategies. */
    @Column(nullable = false)
    private int demandVelocity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    // --- Sprint 2 extension points — nullable, unused today ---
    private BigDecimal costPrice;
    private String supplierId;

    public void applyPriceChange(BigDecimal newPrice) {
        this.currentPrice = newPrice;
    }

    public void receiveStock(int quantity) {
        this.stockLevel += quantity;
        recomputeLifecycleFromStock();
    }

    public void decrementStock(int quantity) {
        this.stockLevel = Math.max(0, this.stockLevel - quantity);
        recomputeLifecycleFromStock();
    }

    /** OUT_OF_STOCK is derived from stock, never set directly by callers. */
    public void recomputeLifecycleFromStock() {
        if (this.stockLevel == 0) {
            this.status = ProductStatus.OUT_OF_STOCK;
        } else if (this.status == ProductStatus.OUT_OF_STOCK) {
            this.status = ProductStatus.ACTIVE;
        }
    }

    public boolean isBelowReorderThreshold() {
        return this.stockLevel < this.reorderThreshold;
    }
}