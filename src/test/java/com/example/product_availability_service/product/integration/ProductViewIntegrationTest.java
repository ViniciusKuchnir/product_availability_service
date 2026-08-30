package com.example.product_availability_service.product.integration;

import com.example.product_availability_service.config.TestcontainersConfiguration;
import com.example.product_availability_service.product.dto.CreateProductRequest;
import com.example.product_availability_service.product.dto.TrendingProductResponse;
import com.example.product_availability_service.product.repository.ProductRepository;
import com.example.product_availability_service.product.service.ProductService;
import com.example.product_availability_service.product.service.ProductViewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class ProductViewIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductViewService productViewService;

    @Autowired
    private StringRedisTemplate redisTemplate;


    @Test
    void shouldIncrementProductViews() {
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
        productViewService.registerView("MON-34");

        productService.findBySku("MON-34");
        productViewService.registerView("MON-34");

        Double score = redisTemplate
                .opsForZSet()
                .score(
                        "product:views",
                        "MON-34"
                );

        assertThat(score)
                .isEqualTo(2D);
    }

    @Test
    void shouldReturnProductsOrderedByViews() {
        productService.create(
                new CreateProductRequest(
                        "MON-34",
                        "Monitor",
                        "MONITORS",
                        189990L,
                        10
                )
        );

        productService.create(
                new CreateProductRequest(
                        "MOUSE-G502",
                        "Mouse G502",
                        "MOUSE",
                        100000L,
                        20
                )
        );

        redisTemplate.opsForZSet()
                .add("product:views", "MON-34", 10);

        redisTemplate.opsForZSet()
                .add("product:views", "MOUSE-G502", 5);

        List<TrendingProductResponse> result =
                productService.findTrendingProducts();

        assertThat(result)
                .extracting(TrendingProductResponse::sku)
                .containsExactly(
                        "MON-34",
                        "MOUSE-G502"
                );
    }
}
