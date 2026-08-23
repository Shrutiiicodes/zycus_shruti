package com.stockpulse.product.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderRequest {
    /** Units sold in this simulated order. Defaults to 1 if omitted. */
    @Min(1)
    private int quantity = 1;
}