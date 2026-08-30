package com.example.product_availability_service.product.integration;

import com.example.product_availability_service.config.TestcontainersConfiguration;
import com.example.product_availability_service.product.dto.CreateProductRequest;
import com.example.product_availability_service.product.dto.ProductResponse;
import com.example.product_availability_service.product.dto.UpdateStockRequest;
import com.example.product_availability_service.product.repository.ProductRepository;
import com.example.product_availability_service.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.springframework.jdbc.core.JdbcTemplate;


import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class ProductCacheIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void shouldCacheProductAfterFirstLookup() {
        CreateProductRequest request =
                new CreateProductRequest(
                        "MON-34",
                        "Monitor Ultrawide 34",
                        "MONITORS",
                        189990L,
                        10
                );

        productService.create(request);

        ProductResponse result = productService.findBySku("MON-34");

        assertThat(result.sku()).isEqualTo("MON-34");

        assertThat(
                redisTemplate.hasKey("products::MON-34")
        ).isTrue();
    }

    @Test
    void shouldReturnCachedProductOnSubsequentLookup() {
        productService.create(
                new CreateProductRequest(
                        "MON-34",
                        "Monitor Ultrawide 34",
                        "MONITORS",
                        189990L,
                        10
                )
        );

        ProductResponse firstResponse =
                productService.findBySku("MON-34");

        assertThat(firstResponse.stockQuantity())
                .isEqualTo(10);

        jdbcTemplate.update("""
                UPDATE products
                SET stock_quantity = ?
                WHERE sku = ?
                """,
                99,
                "MON-34"
        );

        ProductResponse secondResponse = productService
                .findBySku("MON-34");

        assertThat(secondResponse.stockQuantity())
                .isEqualTo(10);

    }

    @Test
    void shouldEvictCacheWhenStockIsUpdated() {
        productService.create(
                new CreateProductRequest(
                        "MON-34",
                        "Monitor",
                        "MONITORS",
                        189990L,
                        10
                )
        );

        productService.findBySku("MON-34");

        assertThat(
                redisTemplate.hasKey("products::MON-34")
        ).isTrue();

        productService.updateStock(
                "MON-34",
                new UpdateStockRequest(25)
        );

        assertThat(
                redisTemplate.hasKey("products::MON-34")
        ).isFalse();
    }

    @Test
    void shouldReloadProductAfterCacheEviction() {
        productService.create(
                new CreateProductRequest(
                        "MON-34",
                        "Monitor",
                        "MONITORS",
                        189990L,
                        10
                )
        );

        productService.findBySku("MON-34");

        productService.updateStock(
                "MON-34",
                new UpdateStockRequest(25)
        );

        ProductResponse result =
                productService.findBySku("MON-34");

        assertThat(result.stockQuantity())
                .isEqualTo(25);

        assertThat(
                redisTemplate.hasKey("products::MON-34")
        ).isTrue();
    }

    @Test
    void shouldSetTtlForCachedProduct() {
        productService.create(
                new CreateProductRequest(
                        "MON-34",
                        "Monitor",
                        "MONITORS",
                        189990L,
                        10
                )
        );

        productService.findBySku("MON-34");

        Long ttl = redisTemplate.getExpire(
                "products::MON-34"
        );

        assertThat(ttl)
                .isBetween(250L, 300L); // 300 = 5 seconds
    }
}
