package com.example.product_availability_service.product.exceptions;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String sku) {
        super("Product with SKU '" + sku + "' was not found.");
    }
}
