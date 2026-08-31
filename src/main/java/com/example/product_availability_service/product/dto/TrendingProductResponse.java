package com.example.product_availability_service.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TrendingProductResponse(

        @Schema(example = "MON-34")
        String sku,

        @Schema(example = "Monitor Ultrawide 34")
        String name,

        @Schema(
                description = "Number of recorded product views",
                example = "42"
        )
        long views
) {
}
