package com.stockpulse.commerce;

import com.stockpulse.product.Category;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ProductContext {
    private final String productId;
    private final String name;
    private final Category category;
    private final BigDecimal currentPrice;
    private final int stockLevel;
    private final int reorderThreshold;
    private final int demandVelocity;
    private final double categoryAverageVelocity;
}