package com.example.testutils;

import com.example.core.Product;

/**
 * Fluent builder for creating {@link Product} test fixtures.
 *
 * <p>Used by unit tests across all modules.
 * This class (and this whole module) should NOT appear in a production BOM.</p>
 */
public class ProductTestBuilder {

    private String id    = "test-id-001";
    private String name  = "Test Product";
    private double price = 9.99;

    public static ProductTestBuilder aProduct() {
        return new ProductTestBuilder();
    }

    public ProductTestBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public ProductTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ProductTestBuilder withPrice(double price) {
        this.price = price;
        return this;
    }

    public Product build() {
        return new Product(id, name, price);
    }
}

