package com.blackduck.integration.detectable.util;

import com.google.gson.JsonSyntaxException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class JsonSanitizerTest {

    @Test
    void testValidJsonNoDuplicates() {
        String input = "{\"name\": \"John\", \"age\": 30}";
        String expected = "{\"name\":\"John\",\"age\":30}";
        String actual = JsonSanitizer.sanitize(input);
        assertEquals(expected, actual);
    }

    @Test
    void testValidJsonWithDuplicates() {
        String input = "{\"name\": \"John\", \"name\": \"Doe\", \"age\": 30}";
        String expected = "{\"name\":\"Doe\",\"age\":30}";
        String actual = JsonSanitizer.sanitize(input);
        assertEquals(expected, actual);
    }

    @Test
    void testNestedJsonWithDuplicates() {
        String input = "{\"person\": {\"name\": \"John\", \"name\": \"Doe\"}, \"age\": 30}";
        String expected = "{\"person\":{\"name\":\"Doe\"},\"age\":30}";
        String actual = JsonSanitizer.sanitize(input);
        assertEquals(expected, actual);
    }

    @Test
    void testInvalidJson() {
        String invalidJson = "{\"name\":\"example\",\"version\":}";
        assertThrows(JsonSyntaxException.class, () -> {
            JsonSanitizer.sanitize(invalidJson);
        });
    }

    @Test
    void testEmptyJson() {
        String emptyJson = "{}";
        String sanitizedJson = JsonSanitizer.sanitize(emptyJson);
        assertEquals(emptyJson, sanitizedJson);
    }

    @Test
    void testJsonWithNullValues() {
        String input = "{\"name\": null, \"age\": 30}";
        String expected = "{\"name\":null,\"age\":30}";
        String actual = JsonSanitizer.sanitize(input);
        assertEquals(expected, actual);
    }
}