package com.example.product_availability_service.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateStockRequest(

        @Schema(
                description = "New absolute stock quantity",
                example = "25",
                minimum = "0"
        )
        @NotNull
        @PositiveOrZero
        Integer quantity
) {
}
