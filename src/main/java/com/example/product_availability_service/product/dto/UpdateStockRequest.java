package com.example.product_availability_service.product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateStockRequest(
        @NotNull
        @PositiveOrZero
        Integer quantity
) {
}
