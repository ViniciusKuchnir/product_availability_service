package com.example.product_availability_service.product.dto;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String category,
        Long priceInCents,
        Integer stockQuantity,
        boolean available
) {
}
