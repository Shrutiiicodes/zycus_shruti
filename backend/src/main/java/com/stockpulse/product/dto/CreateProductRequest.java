package com.stockpulse.product.dto;

import com.stockpulse.product.Category;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateProductRequest {
    @NotBlank
    private String sku;
    @NotBlank
    private String name;
    @NotNull
    private Category category;
    @NotNull @Positive
    private BigDecimal currentPrice;
    @Min(0)
    private int stockLevel;
    @Min(0)
    private int reorderThreshold;
    @Min(0)
    private int demandVelocity;
}