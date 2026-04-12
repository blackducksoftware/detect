package com.example.it;

import com.example.api.ProductController;
import com.example.core.Product;
import com.example.core.ProductService;
import com.example.testutils.ProductTestBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that wires the full API → service stack.
 *
 * <p>In a real project this would use Testcontainers or Spring Boot's test slice.
 * This module (and its heavy test-framework dependencies) should NOT appear in a
 * production BOM — exclude via --detect.maven.excluded.modules=integration-tests.</p>
 */
class ProductIntegrationTest {

    @Test
    void createAndRetrieveProduct() {
        ProductService service = new ProductService();
        ProductController controller = new ProductController(service);

        Product product = ProductTestBuilder.aProduct()
            .withId("p-1")
            .withName("Widget")
            .withPrice(19.99)
            .build();

        controller.create(product);

        var response = controller.get("p-1");
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        assertEquals("Widget", response.getBody().getName());
    }
}

