package com.example.product_availability_service.product.exceptions;

public class ProductAlreadyExistsException extends RuntimeException{

    public ProductAlreadyExistsException(String sku) {
        super("Product with SKU " + sku + " already exists");
    }
}
