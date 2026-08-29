package com.example.product_availability_service.product.model;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 100)
    private String category;

    @Column(name = "price_in_cents", nullable = false)
    private Long priceInCents;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    protected Product() {
    }

    public Product(String sku, String name, String category, Long priceInCents, Integer stockQuantity) {

        if (priceInCents == null || priceInCents <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero");
        }

        if (stockQuantity == null || stockQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }

        this.sku = sku;
        this.name = name;
        this.category = category;
        this.priceInCents = priceInCents;
        this.stockQuantity = stockQuantity;
    }

    public boolean isAvailable() {
        return stockQuantity > 0;
    }

    public void updateStock(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }

        this.stockQuantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public Long getPriceInCents() {
        return priceInCents;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }
}
