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
                "  \"projectTier\": 3,\n" +
                "  \"versionRequest\": {\n" +
                "    \"versionName\": \"1.0.0\",\n" +
                "    \"phase\": \"DEVELOPMENT\"\n" +
                "  }\n" +
                "}";

        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
        Map<String, String> properties = merger.extractPropertiesFromJson(jsonElement);

        assertEquals("MyProject", properties.get(DetectProperties.DETECT_PROJECT_NAME.getKey()));
        assertEquals("My test project", properties.get(DetectProperties.DETECT_PROJECT_DESCRIPTION.getKey()));
        assertEquals("3", properties.get(DetectProperties.DETECT_PROJECT_TIER.getKey()));
        assertEquals("1.0.0", properties.get(DetectProperties.DETECT_PROJECT_VERSION_NAME.getKey()));
        assertEquals("DEVELOPMENT", properties.get(DetectProperties.DETECT_PROJECT_VERSION_PHASE.getKey()));
    }

    @Test
    public void testNestedVersionObjectExtraction() {
        String json = "{\n" +
                "  \"versionRequest\": {\n" +
                "    \"nickname\": \"Release Candidate\"\n" +
                "  }\n" +
                "}";

        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
        Map<String, String> properties = merger.extractPropertiesFromJson(jsonElement);

        assertEquals("Release Candidate", properties.get(DetectProperties.DETECT_PROJECT_VERSION_NICKNAME.getKey()));
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
                "  \"deepLicenseDataEnabled\": false\n" +
                "}";

        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
        Map<String, String> properties = merger.extractPropertiesFromJson(jsonElement);

        assertEquals("TestProject", properties.get(DetectProperties.DETECT_PROJECT_NAME.getKey()));
        assertEquals("false", properties.get(DetectProperties.DETECT_PROJECT_DEEP_LICENSE.getKey()));
    }

    @Test
    public void testProjectSettingsObjectExtraction() {
        // Test the new method that works directly with ProjectSettings objects
        ProjectSettings projectSettings = new ProjectSettings();
        projectSettings.setName("TestProject");
        projectSettings.setDescription("Test description");
        projectSettings.setProjectTier(2);
        
        VersionSettings version = new VersionSettings();
        version.setVersionName("1.0.0");
        version.setPhase("DEVELOPMENT");
        projectSettings.setVersionRequest(version);
        
        Map<String, String> properties = merger.extractPropertiesFromProjectSettings(projectSettings);
        
        assertEquals("TestProject", properties.get(DetectProperties.DETECT_PROJECT_NAME.getKey()));
        assertEquals("Test description", properties.get(DetectProperties.DETECT_PROJECT_DESCRIPTION.getKey()));
        assertEquals("2", properties.get(DetectProperties.DETECT_PROJECT_TIER.getKey()));
        assertEquals("1.0.0", properties.get(DetectProperties.DETECT_PROJECT_VERSION_NAME.getKey()));
        assertEquals("DEVELOPMENT", properties.get(DetectProperties.DETECT_PROJECT_VERSION_PHASE.getKey()));
    }

    @Test
    public void testNullProjectSettingsReturnsEmptyMap() {
        Map<String, String> properties = merger.extractPropertiesFromProjectSettings(null);
        assertTrue(properties.isEmpty());
    }

    @Test
    public void testAllProjectProperties() {
        String json = "{\n" +
                "  \"name\": \"ComprehensiveProject\",\n" +
                "  \"description\": \"A comprehensive test project\",\n" +
                "  \"projectTier\": 5,\n" +
                "  \"projectLevelAdjustments\": true,\n" +
                "  \"deepLicenseDataEnabled\": true,\n" +
                "  \"cloneCategories\": \"ALL\",\n" +
                "  \"versionRequest\": {\n" +
                "    \"versionName\": \"2.0.0\",\n" +
                "    \"nickname\": \"Major Release\",\n" +
                "    \"phase\": \"RELEASED\",\n" +
                "    \"distribution\": \"EXTERNAL\",\n" +
                "    \"update\": false\n" +
                "  }\n" +
                "}";

        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
        Map<String, String> properties = merger.extractPropertiesFromJson(jsonElement);

        // Validate all properties are extracted correctly
        assertEquals("ComprehensiveProject", properties.get(DetectProperties.DETECT_PROJECT_NAME.getKey()));
        assertEquals("A comprehensive test project", properties.get(DetectProperties.DETECT_PROJECT_DESCRIPTION.getKey()));
        assertEquals("5", properties.get(DetectProperties.DETECT_PROJECT_TIER.getKey()));
        assertEquals("true", properties.get(DetectProperties.DETECT_PROJECT_LEVEL_ADJUSTMENTS.getKey()));
        assertEquals("true", properties.get(DetectProperties.DETECT_PROJECT_DEEP_LICENSE.getKey()));
        assertEquals("ALL", properties.get(DetectProperties.DETECT_PROJECT_CLONE_CATEGORIES.getKey()));
        assertEquals("2.0.0", properties.get(DetectProperties.DETECT_PROJECT_VERSION_NAME.getKey()));
        assertEquals("Major Release", properties.get(DetectProperties.DETECT_PROJECT_VERSION_NICKNAME.getKey()));
        assertEquals("RELEASED", properties.get(DetectProperties.DETECT_PROJECT_VERSION_PHASE.getKey()));
        assertEquals("EXTERNAL", properties.get(DetectProperties.DETECT_PROJECT_VERSION_DISTRIBUTION.getKey()));
        assertEquals("false", properties.get(DetectProperties.DETECT_PROJECT_VERSION_UPDATE.getKey()));
    }
}