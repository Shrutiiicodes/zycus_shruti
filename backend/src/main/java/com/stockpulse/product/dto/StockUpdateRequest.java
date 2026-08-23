package com.stockpulse.product.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockUpdateRequest {
    /** New absolute stock level. */
    @NotNull
    private Integer stockLevel;
}