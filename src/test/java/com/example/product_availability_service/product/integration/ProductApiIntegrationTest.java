package com.example.product_availability_service.product.integration;

import com.example.product_availability_service.config.TestcontainersConfiguration;
import com.example.product_availability_service.product.dto.CreateProductRequest;
import com.example.product_availability_service.product.model.Product;
import com.example.product_availability_service.product.repository.ProductRepository;
import com.example.product_availability_service.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Isolated
public class ProductApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        Cache productsCache = cacheManager.getCache("products");

        if (productsCache != null) {
            productsCache.clear();
        }

        redisTemplate.delete("product:views");
    }

    private void createProduct(
            String sku,
            String name,
            String category,
            long priceInCents,
            int stockQuantity
    ) {
        productService.create(
                new CreateProductRequest(
                        sku,
                        name,
                        category,
                        priceInCents,
                        stockQuantity
                )
        );
    }

    @Test
    void shouldCreateProduct() throws Exception {

        String request = """
            {
                "sku": "MON-31",
                "name": "Monitor Ultrawide 34",
                "category": "MONITORS",
                "priceInCents": 189990,
                "stockQuantity": 10
            }
            """;

        mockMvc.perform(
                        post("/api/v1/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sku").value("MON-31"))
                .andExpect(jsonPath("$.name").value("Monitor Ultrawide 34"))
                .andExpect(jsonPath("$.category").value("MONITORS"))
                .andExpect(jsonPath("$.priceInCents").value(189990))
                .andExpect(jsonPath("$.stockQuantity").value(10))
                .andExpect(jsonPath("$.available").value(true));

        assertThat(
                productRepository.findBySku("MON-31")
        ).isPresent();
    }

    @Test
    void shouldReturnConflictWhenSkuAlreadyExists() throws Exception {

        String request = """
            {
                "sku": "MON-32",
                "name": "Monitor Ultrawide 34",
                "category": "MONITORS",
                "priceInCents": 189990,
                "stockQuantity": 10
            }
            """;

        mockMvc.perform(
                        post("/api/v1/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/v1/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("PRODUCT_ALREADY_EXISTS"))
                .andExpect(
                        jsonPath("$.message")
                                .value("Product with SKU MON-32 already exists")
                );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
            {
                "sku": "",
                "name": "Mouse",
                "priceInCents": 10000,
                "stockQuantity": 10
            }
            """,
            """
            {
                "sku": "MOUSE-01",
                "name": "",
                "priceInCents": 10000,
                "stockQuantity": 10
            }
            """,
            """
            {
                "sku": "MOUSE-01",
                "name": "Mouse",
                "priceInCents": 0,
                "stockQuantity": 10
            }
            """,
            """
            {
                "sku": "MOUSE-01",
                "name": "Mouse",
                "priceInCents": 10000,
                "stockQuantity": -1
            }
            """
    })
    void shouldRejectInvalidProductRequests(String request) throws Exception {
        mockMvc.perform(
                        post("/api/v1/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCreateUnavailableProductWhenStockIsZero() throws Exception {

        String request = """
            {
                "sku": "SSD-1TB",
                "name": "SSD NVMe 1TB",
                "category": "STORAGE",
                "priceInCents": 49990,
                "stockQuantity": 0
            }
            """;

        mockMvc.perform(
                        post("/api/v1/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockQuantity").value(0))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void shouldReturnProductBySku() throws Exception {
        createProduct(
                "MON-33",
                "Monitor Ultrawide 33",
                "MONITORS",
                189990L,
                10
        );

        mockMvc.perform(
                        get("/api/v1/products/MON-33")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("MON-33"))
                .andExpect(jsonPath("$.name").value("Monitor Ultrawide 33"))
                .andExpect(jsonPath("$.stockQuantity").value(10))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
        mockMvc.perform(
                        get("/api/v1/products/INVALID-SKU")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void shouldUpdateProductStock() throws Exception {
        createProduct(
                "MON-35",
                "Monitor Ultrawide 35",
                "MONITORS",
                189990L,
                10
        );

        String request = """
            {
                "quantity": 25
            }
            """;

        mockMvc.perform(
                        patch("/api/v1/products/MON-35/stock")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("MON-35"))
                .andExpect(jsonPath("$.stockQuantity").value(25))
                .andExpect(jsonPath("$.available").value(true));

        Product product = productRepository
                .findBySku("MON-35")
                .orElseThrow();

        assertThat(product.getStockQuantity())
                .isEqualTo(25);
    }

    @Test
    void shouldReturnBadRequestWhenStockIsNegative() throws Exception {
        createProduct(
                "MON-36",
                "Monitor",
                "MONITORS",
                189990L,
                10
        );

        String request = """
            {
                "quantity": -1
            }
            """;

        mockMvc.perform(
                        patch("/api/v1/products/MON-36/stock")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isBadRequest());

        Product product = productRepository
                .findBySku("MON-36")
                .orElseThrow();

        assertThat(product.getStockQuantity())
                .isEqualTo(10);
    }

    @Test
    void shouldRegisterViewsEvenWhenProductIsRetrievedFromCache() throws Exception {
        createProduct(
                "MON-37",
                "Monitor",
                "MONITORS",
                189990L,
                10
        );

        mockMvc.perform(
                        get("/api/v1/products/MON-37")
                )
                .andExpect(status().isOk());

        assertThat(
                redisTemplate.hasKey("products::MON-37")
        ).isTrue();

        mockMvc.perform(
                        get("/api/v1/products/MON-37")
                )
                .andExpect(status().isOk());

        Double views = redisTemplate
                .opsForZSet()
                .score(
                        "product:views",
                        "MON-37"
                );

        assertThat(views)
                .isEqualTo(2D);
    }

    @Test
    void shouldReturnProductsSortedByNameIgnoringCase() throws Exception {
        createProduct(
                "MOUSE-G502",
                "MOUSE G502",
                "MOUSE",
                100000L,
                35
        );

        createProduct(
                "SSD-1TB",
                "SSD NVMe 1TB",
                "STORAGE",
                49990L,
                20
        );

        createProduct(
                "MON-28",
                "Monitor Ultrawide 28",
                "MONITORS",
                189990L,
                5
        );

        mockMvc.perform(
                        get("/api/v1/products")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Monitor Ultrawide 28"))
                .andExpect(jsonPath("$[1].name").value("MOUSE G502"))
                .andExpect(jsonPath("$[2].name").value("SSD NVMe 1TB"));
    }

    @Test
    void shouldReturnTrendingProductsOrderedByViews() throws Exception {
        createProduct(
                "MON-38",
                "Monitor Ultrawide",
                "MONITORS",
                189990L,
                10
        );

        createProduct(
                "MOUSE-G502",
                "Mouse G502",
                "MOUSE",
                100000L,
                20
        );

        // Monitor = 3 views
        mockMvc.perform(get("/api/v1/products/MON-38"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/products/MON-38"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/products/MON-38"))
                .andExpect(status().isOk());

        // Mouse = 2 views
        mockMvc.perform(get("/api/v1/products/MOUSE-G502"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/products/MOUSE-G502"))
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/v1/products/trending")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("MON-38"))
                .andExpect(jsonPath("$[0].views").value(3))
                .andExpect(jsonPath("$[1].sku").value("MOUSE-G502"))
                .andExpect(jsonPath("$[1].views").value(2));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingUnknownProduct() throws Exception {
        String request = """
            {
                "quantity": 25
            }
            """;

        mockMvc.perform(
                        patch("/api/v1/products/INVALID-SKU/stock")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void shouldReturnEmptyListWhenNoProductsExist() throws Exception {
        mockMvc.perform(
                        get("/api/v1/products")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturnEmptyTrendingListWhenThereAreNoViews() throws Exception {
        mockMvc.perform(
                        get("/api/v1/products/trending")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldMarkProductAsUnavailableWhenStockIsUpdatedToZero() throws Exception {
        createProduct(
                "MON-39",
                "Monitor",
                "MONITORS",
                189990L,
                10
        );

        String request = """
            {
                "quantity": 0
            }
            """;

        mockMvc.perform(
                        patch("/api/v1/products/MON-39/stock")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(0))
                .andExpect(jsonPath("$.available").value(false));

        Product product = productRepository
                .findBySku("MON-39")
                .orElseThrow();

        assertThat(product.getStockQuantity())
                .isZero();

        assertThat(product.isAvailable())
                .isFalse();
    }

    @Test
    void shouldNotRegisterViewWhenProductDoesNotExist() throws Exception {
        mockMvc.perform(
                        get("/api/v1/products/UNKNOWN-01")
                )
                .andExpect(status().isNotFound());

        Double views = redisTemplate
                .opsForZSet()
                .score(
                        "product:views",
                        "UNKNOWN-01"
                );

        assertThat(views).isNull();
    }

    @Test
    void shouldReturnAtMostTenTrendingProducts() throws Exception {
        for (int i = 1; i <= 11; i++) {
            String sku = "TREND-" + i;

            createProduct(
                    sku,
                    "Product " + i,
                    "TEST",
                    10000L + i,
                    10
            );

            redisTemplate
                    .opsForZSet()
                    .add(
                            "product:views",
                            sku,
                            i
                    );
        }

        mockMvc.perform(
                        get("/api/v1/products/trending")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].sku").value("TREND-11"))
                .andExpect(jsonPath("$[9].sku").value("TREND-2"));
    }

}
