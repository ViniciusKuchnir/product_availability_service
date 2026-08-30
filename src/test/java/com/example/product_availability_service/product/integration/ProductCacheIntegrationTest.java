package com.example.product_availability_service.product.integration;

import com.example.product_availability_service.config.TestcontainersConfiguration;
import com.example.product_availability_service.product.dto.CreateProductRequest;
import com.example.product_availability_service.product.dto.ProductResponse;
import com.example.product_availability_service.product.dto.UpdateStockRequest;
import com.example.product_availability_service.product.repository.ProductRepository;
import com.example.product_availability_service.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.springframework.jdbc.core.JdbcTemplate;


import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Isolated
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
                        "MSO-01",
                        "Mouse G501",
                        "MOUSE",
                        100000L,
                        10
                );

        productService.create(request);

        ProductResponse result = productService.findBySku("MSO-01");

        assertThat(result.sku()).isEqualTo("MSO-01");

        assertThat(
                redisTemplate.hasKey("products::MSO-01")
        ).isTrue();
    }

    @Test
    void shouldReturnCachedProductOnSubsequentLookup() {
        productService.create(
                new CreateProductRequest(
                        "MSO-02",
                        "Mouse G502",
                        "MOUSE",
                        100000L,
                        10
                )
        );

        ProductResponse firstResponse =
                productService.findBySku("MSO-02");

        assertThat(firstResponse.stockQuantity())
                .isEqualTo(10);

        jdbcTemplate.update("""
                UPDATE products
                SET stock_quantity = ?
                WHERE sku = ?
                """,
                99,
                "MSO-02"
        );

        ProductResponse secondResponse = productService
                .findBySku("MSO-02");

        assertThat(secondResponse.stockQuantity())
                .isEqualTo(10);

    }

    @Test
    void shouldEvictCacheWhenStockIsUpdated() {
        productService.create(
                new CreateProductRequest(
                        "MSO-03",
                        "Mouse G503",
                        "MOUSE",
                        100000L,
                        10
                )
        );

        ProductResponse firstResponse =
                productService.findBySku("MSO-03");

        assertThat(firstResponse.stockQuantity())
                .isEqualTo(10);

        productService.updateStock(
                "MSO-03",
                new UpdateStockRequest(25)
        );

        ProductResponse responseAfterUpdate =
                productService.findBySku("MSO-03");

        assertThat(responseAfterUpdate.stockQuantity())
                .isEqualTo(25);
    }

    @Test
    void shouldReloadProductAfterCacheEviction() {
        productService.create(
                new CreateProductRequest(
                        "MSO-04",
                        "Mouse G504",
                        "MOUSE",
                        100000L,
                        10
                )
        );

        productService.findBySku("MSO-04");

        productService.updateStock(
                "MSO-04",
                new UpdateStockRequest(25)
        );

        ProductResponse reloadedResponse =
                productService.findBySku("MSO-04");

        assertThat(reloadedResponse.stockQuantity())
                .isEqualTo(25);

        jdbcTemplate.update("""
            UPDATE products
            SET stock_quantity = ?
            WHERE sku = ?
            """,
                99,
                "MSO-04"
        );

        ProductResponse cachedResponse =
                productService.findBySku("MSO-04");

        assertThat(cachedResponse.stockQuantity())
                .isEqualTo(25);
    }

    @Test
    void shouldSetTtlForCachedProduct() {
        productService.create(
                new CreateProductRequest(
                        "MSO-05",
                        "Mouse G505",
                        "MOUSE",
                        100000L,
                        10
                )
        );

        productService.findBySku("MSO-05");

        Long ttl = redisTemplate.getExpire(
                "products::MSO-05"
        );

        assertThat(ttl)
                .isBetween(250L, 300L); // 300 seconds = 5 minutes
    }
}
