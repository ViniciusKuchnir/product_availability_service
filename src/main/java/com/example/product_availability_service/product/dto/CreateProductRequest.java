package com.example.product_availability_service.product.dto;

import jakarta.validation.constraints.*;

public record CreateProductRequest(

        @NotBlank
        @Size(max = 50)
        String sku,

        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 100)
        String category,

        @NotNull
        @Positive
        Long priceInCents,

        @NotNull
        @PositiveOrZero
        Integer stockQuantity
) {
}
