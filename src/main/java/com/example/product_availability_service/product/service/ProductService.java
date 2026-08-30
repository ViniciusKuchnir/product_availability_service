package com.example.product_availability_service.product.service;

import com.example.product_availability_service.product.dto.CreateProductRequest;
import com.example.product_availability_service.product.dto.ProductResponse;
import com.example.product_availability_service.product.dto.UpdateStockRequest;
import com.example.product_availability_service.product.exceptions.ProductAlreadyExistsException;
import com.example.product_availability_service.product.exceptions.ProductNotFoundException;
import com.example.product_availability_service.product.mapper.ProductMapper;
import com.example.product_availability_service.product.model.Product;
import com.example.product_availability_service.product.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
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

        return productMapper.toResponse(savedProduct);
    }

    @Cacheable(value = "products", key = "#sku")
    public ProductResponse findBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ProductNotFoundException(sku));

        return productMapper.toResponse(product);
    }

    @CacheEvict(value = "products", key = "#sku")
    public ProductResponse updateStock(
            String sku,
            UpdateStockRequest request
    ) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ProductNotFoundException(sku));

        product.updateStock(request.quantity());

        Product savedProduct = productRepository.save(product);

        return productMapper.toResponse(savedProduct);
    }

}
