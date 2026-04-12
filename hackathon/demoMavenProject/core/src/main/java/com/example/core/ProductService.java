package com.example.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/** Service layer — owns product lifecycle logic. */
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final Map<String, Product> store = new LinkedHashMap<>();

    public Product save(Product product) {
        log.info("Saving product: {}", product);
        store.put(product.getId(), product);
        return product;
    }

    public Optional<Product> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Product> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(store.values()));
    }

    public boolean delete(String id) {
        return store.remove(id) != null;
    }
}

