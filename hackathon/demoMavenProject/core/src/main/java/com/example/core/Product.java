package com.example.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/** Domain model representing a product in the catalogue. */
public class Product {

    private final String id;
    private final String name;
    private final double price;

    public Product(
            @JsonProperty("id")    String id,
            @JsonProperty("name")  String name,
            @JsonProperty("price") double price) {
        this.id    = Objects.requireNonNull(id,    "id must not be null");
        this.name  = Objects.requireNonNull(name,  "name must not be null");
        this.price = price;
    }

    public String getId()    { return id; }
    public String getName()  { return name; }
    public double getPrice() { return price; }

    @Override
    public String toString() {
        return "Product{id='" + id + "', name='" + name + "', price=" + price + '}';
    }
}

