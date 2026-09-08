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

    @Version
    private Long version;

    // --- Production Operational & Reorder Fields ---
    private BigDecimal costPrice;
    private String supplierId;

    @Builder.Default
    @Column(columnDefinition = "int default 7")
    private int leadTimeDays = 7;

    @Builder.Default
    @Column(columnDefinition = "int default 10")
    private int safetyStock = 10;

    @Builder.Default
    @Column(columnDefinition = "int default 0")
    private int incomingStock = 0;

    @Builder.Default
    @Column(columnDefinition = "int default 25")
    private int minimumOrderQuantity = 25;

    private java.time.Instant lastPriceChangeTimestamp;

    public void applyPriceChange(BigDecimal newPrice) {
        this.currentPrice = newPrice;
        this.lastPriceChangeTimestamp = java.time.Instant.now();
    }

    public void receiveStock(int quantity) {
        this.stockLevel += quantity;
        recomputeLifecycleFromStock();
    }

    public void decrementStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (quantity > stockLevel) {
            throw new IllegalStateException("Insufficient stock: requested " + quantity + ", available " + stockLevel);
        }
        this.stockLevel -= quantity;
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