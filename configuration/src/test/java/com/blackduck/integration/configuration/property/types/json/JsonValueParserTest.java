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
    void testComplexUnquotedJsonWithSpecialCharacters() throws ValueParseException {
        // Test case for JSON with hyphens, dots, spaces in values, and arrays
        String complexUnquotedJson = "{name:project-name,description:project description,tier:3,applicationId:app-id,groupName:group-name,tags:[tag1,tag2],userGroups:[group1,group2],levelAdjustments:true,deepLicense:true,cloneCategories:ALL,version:{name:1.0.0,nickname:nickname,notes:notes,phase:DEVELOPMENT,distribution:EXTERNAL,update:false,license:Apache License 2.0}}";
        JsonElement result = parser.parse(complexUnquotedJson);
        assertNotNull(result);
        assertTrue(result.isJsonObject());
        
        // Verify key values are parsed correctly
        assertEquals("project-name", result.getAsJsonObject().get("name").getAsString());
        assertEquals("project description", result.getAsJsonObject().get("description").getAsString());
        assertEquals("app-id", result.getAsJsonObject().get("applicationId").getAsString());
        assertEquals("Apache License 2.0", result.getAsJsonObject().get("version").getAsJsonObject().get("license").getAsString());
        
        // Verify arrays are parsed correctly
        assertTrue(result.getAsJsonObject().get("tags").isJsonArray());
        assertEquals(2, result.getAsJsonObject().get("tags").getAsJsonArray().size());
        assertEquals("tag1", result.getAsJsonObject().get("tags").getAsJsonArray().get(0).getAsString());
        assertEquals("tag2", result.getAsJsonObject().get("tags").getAsJsonArray().get(1).getAsString());
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