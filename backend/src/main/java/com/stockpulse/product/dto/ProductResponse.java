package com.stockpulse.product.dto;

import com.stockpulse.product.Category;
import com.stockpulse.product.Product;
import com.stockpulse.product.ProductStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ProductResponse {
    private String id;
    private String sku;
    private String name;
    private Category category;
    private BigDecimal currentPrice;
    private int stockLevel;
    private int reorderThreshold;
    private int demandVelocity;
    private ProductStatus status;

    public static ProductResponse from(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .sku(p.getSku())
                .name(p.getName())
                .category(p.getCategory())
                .currentPrice(p.getCurrentPrice())
                .stockLevel(p.getStockLevel())
                .reorderThreshold(p.getReorderThreshold())
                .demandVelocity(p.getDemandVelocity())
                .status(p.getStatus())
                .build();
    }
}