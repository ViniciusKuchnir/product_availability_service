package com.example.product_availability_service.product.integration;

import com.example.product_availability_service.config.TestcontainersConfiguration;
import com.example.product_availability_service.product.dto.CreateProductRequest;
import com.example.product_availability_service.product.dto.TrendingProductResponse;
import com.example.product_availability_service.product.repository.ProductRepository;
import com.example.product_availability_service.product.service.ProductService;
import com.example.product_availability_service.product.service.ProductViewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Isolated
public class ProductViewIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductViewService productViewService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        redisTemplate
                .getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushAll();
    }


    @Test
    void shouldIncrementProductViews() {
        productService.create(
                new CreateProductRequest(
                        "CPU-01",
                        "CPU 01",
                        "CPU",
                        7000000L,
                        10
                )
        );

        productService.findBySku("CPU-01");
        productViewService.registerView("CPU-01");

        productService.findBySku("CPU-01");
        productViewService.registerView("CPU-01");

        Double score = redisTemplate
                .opsForZSet()
                .score(
                        "product:views",
                        "CPU-01"
                );

        assertThat(score)
                .isEqualTo(2D);
    }

    @Test
    void shouldReturnProductsOrderedByViews() {
        productService.create(
                new CreateProductRequest(
                        "CPU-02",
                        "CPU 02",
                        "CPU",
                        7000000L,
                        10
                )
        );

        productService.create(
                new CreateProductRequest(
                        "CPU-03",
                        "CPU 03",
                        "CPU",
                        4500000L,
                        20
                )
        );

        redisTemplate.opsForZSet()
                .add("product:views", "CPU-02", 10);

        redisTemplate.opsForZSet()
                .add("product:views", "CPU-03", 5);

        List<TrendingProductResponse> result =
                productService.findTrendingProducts();

        assertThat(result)
                .extracting(TrendingProductResponse::sku)
                .containsExactly(
                        "CPU-02",
                        "CPU-03"
                );
    }
}
