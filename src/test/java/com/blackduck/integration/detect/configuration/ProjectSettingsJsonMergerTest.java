package com.blackduck.integration.detect.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

/**
 * Tests the ProjectSettingsJsonMerger class which extracts individual detect.project.*
 * property values from a consolidated JSON object.
 */
public class ProjectSettingsJsonMergerTest {
    private final ProjectSettingsJsonMerger merger = new ProjectSettingsJsonMerger();
    private final Gson gson = new Gson();

    @Test
    public void testBasicJsonPropertyExtraction() {
        String json = "{\n" +
                "  \"name\": \"MyProject\",\n" +
                "  \"description\": \"My test project\",\n" +
                "  \"tier\": 3,\n" +
                "  \"version\": {\n" +
                "    \"name\": \"1.0.0\",\n" +
                "    \"phase\": \"DEVELOPMENT\"\n" +
                "  }\n" +
                "}";

        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
        Map<String, String> properties = merger.extractPropertiesFromJson(jsonElement);

        assertEquals("MyProject", properties.get("detect.project.name"));
        assertEquals("My test project", properties.get("detect.project.description"));
        assertEquals("3", properties.get("detect.project.tier"));
        assertEquals("1.0.0", properties.get("detect.project.version.name"));
        assertEquals("DEVELOPMENT", properties.get("detect.project.version.phase"));
    }

    @Test
    public void testArrayPropertiesExtraction() {
        String json = "{\n" +
                "  \"tags\": [\"Critical\", \"Backend\"],\n" +
                "  \"userGroups\": [\"ProjectManagers\", \"TechLeads\"]\n" +
                "}";

        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
        Map<String, String> properties = merger.extractPropertiesFromJson(jsonElement);

        assertEquals("Critical,Backend", properties.get("detect.project.tags"));
        assertEquals("ProjectManagers,TechLeads", properties.get("detect.project.user.groups"));
    }

    @Test
    public void testNestedVersionObjectExtraction() {
        String json = "{\n" +
                "  \"version\": {\n" +
                "    \"nickname\": \"Release Candidate\",\n" +
                "    \"notes\": \"Release candidate for version 1.0\",\n" +
                "    \"license\": \"Apache License 2.0\"\n" +
                "  }\n" +
                "}";

        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
        Map<String, String> properties = merger.extractPropertiesFromJson(jsonElement);

        assertEquals("Release Candidate", properties.get("detect.project.version.nickname"));
        assertEquals("Release candidate for version 1.0", properties.get("detect.project.version.notes"));
        assertEquals("Apache License 2.0", properties.get("detect.project.version.license"));
    }

    @Test
    public void testEmptyJsonReturnsEmptyMap() {
        String json = "{}";
        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
        Map<String, String> properties = merger.extractPropertiesFromJson(jsonElement);
        
        assertTrue(properties.isEmpty());
    }

    @Test
    public void testNullJsonReturnsEmptyMap() {
        Map<String, String> properties = merger.extractPropertiesFromJson(null);
        assertTrue(properties.isEmpty());
    }

    @Test
    public void testDeepLicensePropertyExtraction() {
        String json = "{\n" +
                "  \"name\": \"TestProject\",\n" +
                "  \"deepLicense\": false\n" +
                "}";

        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
        Map<String, String> properties = merger.extractPropertiesFromJson(jsonElement);

        assertEquals("TestProject", properties.get("detect.project.name"));
        assertEquals("false", properties.get("detect.project.deep.license"));
    }

    @Test
    public void testAllProjectProperties() {
        String json = "{\n" +
                "  \"name\": \"ComprehensiveProject\",\n" +
                "  \"description\": \"A comprehensive test project\",\n" +
                "  \"tier\": 5,\n" +
                "  \"applicationId\": \"app-12345\",\n" +
                "  \"groupName\": \"MyGroup\",\n" +
                "  \"tags\": [\"Critical\", \"Production\"],\n" +
                "  \"userGroups\": [\"Admins\", \"Developers\"],\n" +
                "  \"levelAdjustments\": true,\n" +
                "  \"deepLicense\": true,\n" +
                "  \"cloneCategories\": \"ALL\",\n" +
                "  \"version\": {\n" +
                "    \"name\": \"2.0.0\",\n" +
                "    \"nickname\": \"Major Release\",\n" +
                "    \"notes\": \"Major version update\",\n" +
                "    \"phase\": \"RELEASED\",\n" +
                "    \"distribution\": \"EXTERNAL\",\n" +
                "    \"update\": false,\n" +
                "    \"license\": \"MIT License\"\n" +
                "  }\n" +
                "}";

        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
        Map<String, String> properties = merger.extractPropertiesFromJson(jsonElement);

        // Validate all properties are extracted correctly
        assertEquals("ComprehensiveProject", properties.get("detect.project.name"));
        assertEquals("A comprehensive test project", properties.get("detect.project.description"));
        assertEquals("5", properties.get("detect.project.tier"));
        assertEquals("app-12345", properties.get("detect.project.application.id"));
        assertEquals("MyGroup", properties.get("detect.project.group.name"));
        assertEquals("Critical,Production", properties.get("detect.project.tags"));
        assertEquals("Admins,Developers", properties.get("detect.project.user.groups"));
        assertEquals("true", properties.get("detect.project.level.adjustments"));
        assertEquals("true", properties.get("detect.project.deep.license"));
        assertEquals("ALL", properties.get("detect.project.clone.categories"));
        assertEquals("2.0.0", properties.get("detect.project.version.name"));
        assertEquals("Major Release", properties.get("detect.project.version.nickname"));
        assertEquals("Major version update", properties.get("detect.project.version.notes"));
        assertEquals("RELEASED", properties.get("detect.project.version.phase"));
        assertEquals("EXTERNAL", properties.get("detect.project.version.distribution"));
        assertEquals("false", properties.get("detect.project.version.update"));
        assertEquals("MIT License", properties.get("detect.project.version.license"));
    }
}