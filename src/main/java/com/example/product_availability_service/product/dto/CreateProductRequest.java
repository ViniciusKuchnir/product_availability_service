package com.example.product_availability_service.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record CreateProductRequest(

        @Schema(
                description = "Unique product SKU",
                example = "MON-34"
        )
        @NotBlank
        @Size(max = 50)
        String sku,

        @Schema(
                description = "Product name",
                example = "Monitor Ultrawide 34"
        )
        @NotBlank
        @Size(max = 150)
        String name,

        @Schema(
                description = "Product category",
                example = "MONITORS"
        )
        @Size(max = 100)
        String category,

        @Schema(
                description = "Product price expressed in cents",
                example = "189990"
        )
        @NotNull
        @Positive
        Long priceInCents,

        @Schema(
                description = "Current available stock quantity",
                example = "10"
        )
        @NotNull
        @PositiveOrZero
        Integer stockQuantity
) {
}
