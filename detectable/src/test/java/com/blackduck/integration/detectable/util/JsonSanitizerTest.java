package com.blackduck.integration.detectable.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JsonSanitizerTest {
    @Test
    void testValidJson() {
        String json = "{ " +
            "\"name\": \"my-project\", " +
            "\"version\": \"1.0.0\", " +
            "\"private\": true, " +
            "\"scripts\": { " +
            "\"start\": \"node index.js\", " +
            "\"test\": \"jest\"" +
            "}, " +
            "\"dependencies\": { " +
            "\"express\": \"^4.17.1\", " +
            "\"lodash\": \"^4.17.21\"" +
            "}, " +
            "\"devDependencies\": { " +
            "\"jest\": \"^27.0.0\", " +
            "\"eslint\": \"^7.32.0\"" +
            "}" +
            "}";

        JsonObject sanitized = JsonSanitizer.sanitize(json);

        assertEquals("my-project", sanitized.get("name").getAsString());
        assertEquals("1.0.0", sanitized.get("version").getAsString());
        assertTrue(sanitized.get("private").getAsBoolean());
        assertEquals("node index.js", sanitized.getAsJsonObject("scripts").get("start").getAsString());
        assertEquals("^4.17.1", sanitized.getAsJsonObject("dependencies").get("express").getAsString());
        assertEquals("^27.0.0", sanitized.getAsJsonObject("devDependencies").get("jest").getAsString());
    }

    @Test
    void testDuplicateKeysInDependencies() {
        String json = "{ " +
            "\"dependencies\": { " +
            "\"express\": \"^4.17.1\", " +
            "\"express\": \"^5.0.0\"" +
            "}" +
            "}";

        JsonObject sanitized = JsonSanitizer.sanitize(json);

        assertEquals("^5.0.0", sanitized.getAsJsonObject("dependencies").get("express").getAsString()); // Last value is kept
    }

    @Test
    void testNestedScriptsAndDependencies() {
        String json = "{ " +
            "\"scripts\": { " +
            "\"build\": \"tsc\", " +
            "\"start\": \"node dist/index.js\"" +
            "}, " +
            "\"dependencies\": { " +
            "\"typescript\": \"^4.0.0\", " +
            "\"node-fetch\": \"^2.6.1\"" +
            "}" +
            "}";

        JsonObject sanitized = JsonSanitizer.sanitize(json);

        assertEquals("tsc", sanitized.getAsJsonObject("scripts").get("build").getAsString());
        assertEquals("^4.0.0", sanitized.getAsJsonObject("dependencies").get("typescript").getAsString());
    }

    @Test
    void testInvalidJson() {
        String json = "{ " +
            "\"name\": \"my-project\", " +
            "\"version\": \"1.0.0\"";

        assertThrows(JsonSyntaxException.class, () -> JsonSanitizer.sanitize(json));
    }

    @Test
    void testEmptyPackageJson() {
        String json = "{}";

        JsonObject sanitized = JsonSanitizer.sanitize(json);
        assertTrue(sanitized.entrySet().isEmpty());
    }

    @Test
    void testJsonWithNullValues() {
        String json = "{ " +
            "\"name\": \"my-project\", " +
            "\"version\": null, " +
            "\"dependencies\": { " +
            "\"express\": \"^4.17.1\", " +
            "\"lodash\": null " +
            "} " +
            "}";

        JsonObject sanitized = JsonSanitizer.sanitize(json);

        assertTrue(sanitized.get("version").isJsonNull());
        assertTrue(sanitized.getAsJsonObject("dependencies").get("lodash").isJsonNull());
    }
}