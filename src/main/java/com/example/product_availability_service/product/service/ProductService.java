package com.example.product_availability_service.product.service;

import com.example.product_availability_service.product.dto.CreateProductRequest;
import com.example.product_availability_service.product.dto.ProductResponse;
import com.example.product_availability_service.product.dto.TrendingProductResponse;
import com.example.product_availability_service.product.dto.UpdateStockRequest;
import com.example.product_availability_service.product.exceptions.ProductAlreadyExistsException;
import com.example.product_availability_service.product.exceptions.ProductNotFoundException;
import com.example.product_availability_service.product.mapper.ProductMapper;
import com.example.product_availability_service.product.model.Product;
import com.example.product_availability_service.product.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductViewService productViewService;

    public ProductService(
            ProductRepository productRepository,
            ProductMapper productMapper,
            ProductViewService productViewService
    ) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.productViewService = productViewService;
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

    public List<TrendingProductResponse> findTrendingProducts() {
        var ranking = productViewService.getTopViewedProducts(10);

        if (ranking == null || ranking.isEmpty()) {
            return List.of();
        }

        List<String> skus = ranking.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .filter(Objects::nonNull)
                .toList();

        Map<String, Product> productsBySku =
                productRepository.findAllBySkuIn(skus)
                        .stream()
                        .collect(Collectors.toMap(
                                Product::getSku,
                                Function.identity()
                        ));

        return ranking.stream()
                .map(tuple -> {
                    String sku = tuple.getValue();
                    Product product = productsBySku.get(sku);

                    if (product == null) return null;

                    long views = tuple.getScore() != null
                            ? tuple.getScore().longValue()
                            : 0L;

                    return new TrendingProductResponse(
                            product.getSku(),
                            product.getName(),
                            views
                    );
                })
                .filter(Objects::nonNull)
                .toList();
    }

}
