package com.example.product_availability_service.product.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductViewServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private ProductViewService productViewService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForZSet())
                .thenReturn(zSetOperations);
    }

    @Test
    void shouldRegisterProductView() {
        String sku = "MON-34";

        productViewService.registerView(sku);

        verify(zSetOperations)
                .incrementScore(
                        "product:views",
                        sku,
                        1
                );
    }

    @Test
    void shouldReturnTopViewedProducts() {
        Set<ZSetOperations.TypedTuple<String>> ranking =
                new LinkedHashSet<>();

        ranking.add(
                new DefaultTypedTuple<>("MON-34", 15D)
        );

        ranking.add(
                new DefaultTypedTuple<>("MOUSE-G502", 8D)
        );

        when(
                zSetOperations.reverseRangeWithScores(
                        "product:views",
                        0,
                        9
                )
        ).thenReturn(ranking);

        var result =
                productViewService.getTopViewedProducts(10);

        assertThat(result)
                .isEqualTo(ranking);

        verify(zSetOperations)
                .reverseRangeWithScores(
                        "product:views",
                        0,
                        9
                );
    }

    @Test
    void shouldRespectTrendingProductsLimit() {
        productViewService.getTopViewedProducts(5);

        verify(zSetOperations)
                .reverseRangeWithScores(
                        "product:views",
                        0,
                        4
                );
    }

    @Test
    void shouldReturnEmptyRankingWhenThereAreNoViews() {
        when(
                zSetOperations.reverseRangeWithScores(
                        "product:views",
                        0,
                        9
                )
        ).thenReturn(Set.of());

        var result =
                productViewService.getTopViewedProducts(10);

        assertThat(result)
                .isEmpty();
    }

}
