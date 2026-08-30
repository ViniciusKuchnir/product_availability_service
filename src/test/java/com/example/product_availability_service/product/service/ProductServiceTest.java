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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductViewService productViewService;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldCreateProduct() {

        CreateProductRequest request = new CreateProductRequest(
                "MON-ULTRA-34",
                "Monitor Ultrawide 34",
                "MONITORS",
                189990L,
                15
        );

        ProductResponse expectedResponse = new ProductResponse(
                null,
                "MON-ULTRA-34",
                "Monitor Ultrawide 34",
                "MONITORS",
                189990L,
                15,
                true
        );

        when(productRepository.existsBySku(request.sku()))
                .thenReturn(false);

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(productMapper.toResponse(any(Product.class)))
                .thenReturn(expectedResponse);


        ProductResponse result = productService.create(request);


        assertThat(result).isEqualTo(expectedResponse);

        ArgumentCaptor<Product> productCaptor =
                ArgumentCaptor.forClass(Product.class);

        verify(productRepository).save(productCaptor.capture());

        Product savedProduct = productCaptor.getValue();

        assertThat(savedProduct.getSku())
                .isEqualTo(request.sku());

        assertThat(savedProduct.getName())
                .isEqualTo(request.name());

        assertThat(savedProduct.getCategory())
                .isEqualTo(request.category());

        assertThat(savedProduct.getPriceInCents())
                .isEqualTo(request.priceInCents());

        assertThat(savedProduct.getStockQuantity())
                .isEqualTo(request.stockQuantity());

        assertThat(savedProduct.isAvailable())
                .isTrue();

        verify(productRepository).existsBySku(request.sku());
        verify(productMapper).toResponse(savedProduct);
    }

    @Test
    void shouldThrowProductAlreadyExistsExceptionWhenSkuAlreadyExists() {
        CreateProductRequest request = new CreateProductRequest(
                "MON-ULTRA-34",
                "Monitor Ultrawide 34",
                "MONITORS",
                189990L,
                15
        );

        when(productRepository.existsBySku(request.sku()))
                .thenReturn(true);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(ProductAlreadyExistsException.class)
                .hasMessageContaining(request.sku());

        verify(productRepository).existsBySku(request.sku());

        verify(productRepository, never())
                .save(any(Product.class));

        verifyNoInteractions(productMapper);
    }

    @Test
    void shouldReturnProductWhenSkuExists() {
        Product product = new Product(
                "MON-ULTRA-34",
                "Monitor Ultrawide 34",
                "MONITORS",
                189990L,
                15
        );

        ProductResponse expectedResponse = new ProductResponse(
                null,
                "MON-ULTRA-34",
                "Monitor Ultrawide 34",
                "MONITORS",
                189990L,
                15,
                true
        );

        when(productRepository.findBySku("MON-ULTRA-34"))
                .thenReturn(Optional.of(product));

        when(productMapper.toResponse(product))
                .thenReturn(expectedResponse);

        ProductResponse result =
                productService.findBySku("MON-ULTRA-34");

        assertThat(result)
                .isEqualTo(expectedResponse);

        verify(productRepository)
                .findBySku("MON-ULTRA-34");

        verify(productMapper)
                .toResponse(product);
    }

    @Test
    void shouldThrowProductNotFoundExceptionWhenSkuDoesNotExist() {
        String sku = "INVALID-SKU";

        when(productRepository.findBySku(sku))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findBySku(sku))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining(sku);

        verify(productRepository)
                .findBySku(sku);

        verifyNoInteractions(productMapper);
    }

    @Test
    void shouldUpdateProductStock() {
        String sku = "MON-ULTRA-34";

        Product product = new Product(
                sku,
                "Monitor Ultrawide 34",
                "MONITORS",
                189990L,
                15
        );

        UpdateStockRequest request =
                new UpdateStockRequest(25);

        ProductResponse expectedResponse = new ProductResponse(
                null,
                sku,
                "Monitor Ultrawide 34",
                "MONITORS",
                189990L,
                25,
                true
        );

        when(productRepository.findBySku(sku))
                .thenReturn(Optional.of(product));

        when(productRepository.save(product))
                .thenReturn(product);

        when(productMapper.toResponse(product))
                .thenReturn(expectedResponse);

        ProductResponse result =
                productService.updateStock(sku, request);

        assertThat(result)
                .isEqualTo(expectedResponse);

        assertThat(product.getStockQuantity())
                .isEqualTo(25);

        assertThat(product.isAvailable())
                .isTrue();

        verify(productRepository)
                .findBySku(sku);

        verify(productRepository)
                .save(product);

        verify(productMapper)
                .toResponse(product);
    }

    @Test
    void shouldMarkProductAsUnavailableWhenStockIsZero() {
        String sku = "MON-ULTRA-34";

        Product product = new Product(
                sku,
                "Monitor Ultrawide 34",
                "MONITORS",
                189990L,
                10
        );

        UpdateStockRequest request =
                new UpdateStockRequest(0);

        ProductResponse expectedResponse = new ProductResponse(
                null,
                sku,
                "Monitor Ultrawide 34",
                "MONITORS",
                189990L,
                0,
                false
        );

        when(productRepository.findBySku(sku))
                .thenReturn(Optional.of(product));

        when(productRepository.save(product))
                .thenReturn(product);

        when(productMapper.toResponse(product))
                .thenReturn(expectedResponse);

        ProductResponse result =
                productService.updateStock(sku, request);

        assertThat(product.getStockQuantity())
                .isZero();

        assertThat(product.isAvailable())
                .isFalse();

        assertThat(result.available())
                .isFalse();
    }

    @Test
    void shouldThrowProductNotFoundExceptionWhenUpdatingUnknownProduct() {
        String sku = "INVALID-SKU";

        UpdateStockRequest request =
                new UpdateStockRequest(20);

        when(productRepository.findBySku(sku))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> productService.updateStock(sku, request)
        )
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining(sku);

        verify(productRepository)
                .findBySku(sku);

        verify(productRepository, never())
                .save(any(Product.class));

        verifyNoInteractions(productMapper);
    }

    @Test
    void shouldRejectNegativeStockQuantity() {
        String sku = "MON-ULTRA-34";

        Product product = new Product(
                sku,
                "Monitor Ultrawide 34",
                "MONITORS",
                189990L,
                15
        );

        UpdateStockRequest request =
                new UpdateStockRequest(-1);

        when(productRepository.findBySku(sku))
                .thenReturn(Optional.of(product));

        assertThatThrownBy(
                () -> productService.updateStock(sku, request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock quantity cannot be negative");

        verify(productRepository, never())
                .save(any(Product.class));
    }

    @Test
    void shouldReturnAllProducts() {
        Product monitor = new Product(
                "MON-34",
                "Monitor",
                "MONITORS",
                189990L,
                10
        );

        Product mouse = new Product(
                "MOUSE-G502",
                "Mouse G502",
                "MOUSE",
                100000L,
                20
        );

        ProductResponse monitorResponse = new ProductResponse(
                null,
                "MON-34",
                "Monitor",
                "MONITORS",
                189990L,
                10,
                true
        );

        ProductResponse mouseResponse = new ProductResponse(
                null,
                "MOUSE-G502",
                "Mouse G502",
                "MOUSE",
                100000L,
                20,
                true
        );

        when(productRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(monitor, mouse));

        when(productMapper.toResponse(monitor))
                .thenReturn(monitorResponse);

        when(productMapper.toResponse(mouse))
                .thenReturn(mouseResponse);

        List<ProductResponse> result =
                productService.findAll();

        assertThat(result)
                .containsExactly(
                        monitorResponse,
                        mouseResponse
                );

        verify(productRepository)
                .findAll(any(Sort.class));
    }

    @Test
    void shouldReturnEmptyListWhenNoProductsExist() {
        when(productRepository.findAll(any(Sort.class)))
                .thenReturn(List.of());

        List<ProductResponse> result =
                productService.findAll();

        assertThat(result)
                .isEmpty();

        verifyNoInteractions(productMapper);
    }

    @Test
    void shouldReturnEmptyListWhenThereAreNoTrendingProducts() {
        when(productViewService.getTopViewedProducts(10))
                .thenReturn(Set.of());

        List<TrendingProductResponse> result =
                productService.findTrendingProducts();

        assertThat(result)
                .isEmpty();

        verify(productViewService)
                .getTopViewedProducts(10);

        verifyNoInteractions(productRepository);
    }

    @Test
    void shouldReturnTrendingProducts() {
        var monitorTuple =
                new DefaultTypedTuple<>("MON-34", 15D);

        var mouseTuple =
                new DefaultTypedTuple<>("MOUSE-G502", 8D);

        Set<ZSetOperations.TypedTuple<String>> ranking =
                new LinkedHashSet<>();

        ranking.add(
                new DefaultTypedTuple<>("MON-34", 15D)
        );

        ranking.add(
                new DefaultTypedTuple<>("MOUSE-G502", 8D)
        );

        when(productViewService.getTopViewedProducts(10))
                .thenReturn(ranking);

        Product monitor = new Product(
                "MON-34",
                "Monitor",
                "MONITORS",
                189990L,
                10
        );

        Product mouse = new Product(
                "MOUSE-G502",
                "Mouse G502",
                "MOUSE",
                100000L,
                20
        );

        when(productRepository.findAllBySkuIn(anyCollection()))
                .thenReturn(List.of(monitor, mouse));

        List<TrendingProductResponse> result =
                productService.findTrendingProducts();

        assertThat(result)
                .hasSize(2);

        assertThat(result)
                .extracting(TrendingProductResponse::sku)
                .containsExactly(
                        "MON-34",
                        "MOUSE-G502"
                );
    }

}
