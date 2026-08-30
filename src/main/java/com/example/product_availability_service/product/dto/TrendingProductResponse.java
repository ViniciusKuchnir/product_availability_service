package com.example.product_availability_service.product.dto;

public record TrendingProductResponse(
        String sku,
        String name,
        long views
) {
}
