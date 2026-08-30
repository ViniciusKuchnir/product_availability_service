package com.example.product_availability_service.product.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProductViewService {
    private static final String PRODUCT_VIEWS_KEY = "product:views";

    private final StringRedisTemplate redisTemplate;

    public ProductViewService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void registerView(String sku) {
        redisTemplate
                .opsForZSet()
                .incrementScore(PRODUCT_VIEWS_KEY, sku, 1);
    }
}
