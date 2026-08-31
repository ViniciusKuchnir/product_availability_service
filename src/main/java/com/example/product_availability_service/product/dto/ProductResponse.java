package com.example.product_availability_service.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProductResponse(
        @Schema(example = "1")
        Long id,

        @Schema(example = "MON-34")
        String sku,

        @Schema(example = "Monitor Ultrawide 34")
        String name,

        @Schema(example = "MONITORS")
        String category,

        @Schema(
                description = "Product price expressed in cents",
                example = "189990"
        )
        Long priceInCents,

        @Schema(example = "10")
        Integer stockQuantity,

        @Schema(
                description = "Indicates whether the product currently has stock available",
                example = "true"
        )
        boolean available
) {
}
