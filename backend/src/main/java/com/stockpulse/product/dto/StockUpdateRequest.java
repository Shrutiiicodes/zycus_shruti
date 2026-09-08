package com.stockpulse.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockUpdateRequest {
    /** New absolute stock level. */
    @NotNull
    @Min(value = 0, message = "Stock level cannot be negative")
    private Integer stockLevel;
}