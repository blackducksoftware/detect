package com.blackduck.integration.configuration.property.types.json;

import com.google.gson.JsonElement;
import com.blackduck.integration.configuration.parse.ValueParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonValueParserTest {
    private final JsonValueParser parser = new JsonValueParser();

    @Test
    void testValidQuotedJson() throws ValueParseException {
        String validJson = "{\"name\":\"MyProject\",\"version\":{\"name\":\"1.0.0\"},\"description\":\"My project\"}";
        JsonElement result = parser.parse(validJson);
        assertNotNull(result);
        assertTrue(result.isJsonObject());
        assertEquals("MyProject", result.getAsJsonObject().get("name").getAsString());
    }

    @Test
    void testUnquotedJsonFromShell() throws ValueParseException {
        String unquotedJson = "{name:MyProject,version:{name:1.0.0},description:My project}";
        JsonElement result = parser.parse(unquotedJson);
        assertNotNull(result);
        assertTrue(result.isJsonObject());
        assertEquals("MyProject", result.getAsJsonObject().get("name").getAsString());
        assertEquals("My project", result.getAsJsonObject().get("description").getAsString());
    }

    @Test
    void testNullValue() {
        assertThrows(ValueParseException.class, () -> parser.parse(null));
    }

    @Test
    void testEmptyValue() {
        assertThrows(ValueParseException.class, () -> parser.parse(""));
    }

    @Test
    void testCompletelyInvalidJson() {
        assertThrows(ValueParseException.class, () -> parser.parse("not json at all"));
    }
}