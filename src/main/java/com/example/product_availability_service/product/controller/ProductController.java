package com.example.product_availability_service.product.controller;

import com.example.product_availability_service.product.dto.CreateProductRequest;
import com.example.product_availability_service.product.dto.ProductResponse;
import com.example.product_availability_service.product.dto.TrendingProductResponse;
import com.example.product_availability_service.product.dto.UpdateStockRequest;
import com.example.product_availability_service.product.service.ProductService;
import com.example.product_availability_service.product.service.ProductViewService;
import com.example.product_availability_service.shared.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/products")
@Tag(
        name = "Products",
        description = "Operations for product availability, stock and popularity"
)
public class ProductController {
    private final ProductService productService;
    private final ProductViewService productViewService;

    public ProductController(
            ProductService productService,
            ProductViewService productViewService
    ) {
        this.productService = productService;
        this.productViewService = productViewService;
    }

    @Operation(
            summary = "Create a product",
            description = "Creates a new product with its initial stock quantity."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Product created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid product data",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "A product with the provided SKU already exists",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            )
    })
    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody CreateProductRequest request
    ) {
        ProductResponse product = productService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    @Operation(
            summary = "Get trending products",
            description = """
                Returns up to 10 products ranked by number of views.
                
                Product view counters are maintained using a Redis Sorted Set.
                Products with more views appear first.
                """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Trending products returned successfully"
    )
    @GetMapping("/trending")
    public ResponseEntity<List<TrendingProductResponse>> findTrendingProducts() {
        return ResponseEntity.ok(
                productService.findTrendingProducts()
        );
    }

    @Operation(
            summary = "Find product by SKU",
            description = """
                Returns a product by SKU.
                
                Product lookups are cached in Redis to reduce database reads.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Product found"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Product not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            )
    })
    @GetMapping("/{sku}")
    public ResponseEntity<ProductResponse> findBySku(
            @PathVariable String sku
    ) {
        ProductResponse product = productService.findBySku(sku);

        productViewService.registerView(sku);

        return ResponseEntity.ok(product);
    }

    @Operation(
            summary = "Update product stock",
            description = """
                Updates the current stock quantity for a product.
                
                Updating stock invalidates the cached product entry so that
                the next lookup retrieves the latest value.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Stock updated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid stock quantity",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Product not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            )
    })
    @PatchMapping("/{sku}/stock")
    public ResponseEntity<ProductResponse> updateStock(
            @PathVariable String sku,
            @Valid @RequestBody UpdateStockRequest request
    ) {
        return ResponseEntity.ok(productService.updateStock(sku, request));
    }

    @Operation(
            summary = "List all products",
            description = "Returns all products sorted alphabetically by name, ignoring case."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Products returned successfully"
    )
    @GetMapping
    public ResponseEntity<List<ProductResponse>> findAll() {
        return ResponseEntity.ok(productService.findAll());
    }
}
