package com.example.product_availability_service.product.mapper;

import com.example.product_availability_service.product.dto.ProductResponse;
import com.example.product_availability_service.product.model.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getCategory(),
                product.getPriceInCents(),
                product.getStockQuantity(),
                product.isAvailable()
        );
    }
}
