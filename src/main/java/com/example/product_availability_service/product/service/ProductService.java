package com.example.product_availability_service.product.service;

import com.example.product_availability_service.product.dto.CreateProductRequest;
import com.example.product_availability_service.product.dto.ProductResponse;
import com.example.product_availability_service.product.exceptions.ProductAlreadyExistsException;
import com.example.product_availability_service.product.model.Product;
import com.example.product_availability_service.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse create(CreateProductRequest request) {

        if (productRepository.existsBySku(request.sku())) {
            throw new ProductAlreadyExistsException(request.sku());
        }

        Product product = new Product(
                request.sku(),
                request.name(),
                request.category(),
                request.priceInCents(),
                request.stockQuantity()
        );

        Product savedProduct = productRepository.save(product);

        return new ProductResponse(
                savedProduct.getId(),
                savedProduct.getSku(),
                savedProduct.getName(),
                savedProduct.getCategory(),
                savedProduct.getPriceInCents(),
                savedProduct.getStockQuantity(),
                savedProduct.isAvailable()
        );
    }

}
