package com.blackduck.integration.detect.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.detect.configuration.DetectProperties;
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

        assertEquals("MyProject", properties.get(DetectProperties.DETECT_PROJECT_NAME.getKey()));
        assertEquals("My test project", properties.get(DetectProperties.DETECT_PROJECT_DESCRIPTION.getKey()));
        assertEquals("3", properties.get(DetectProperties.DETECT_PROJECT_TIER.getKey()));
        assertEquals("1.0.0", properties.get(DetectProperties.DETECT_PROJECT_VERSION_NAME.getKey()));
        assertEquals("DEVELOPMENT", properties.get(DetectProperties.DETECT_PROJECT_VERSION_PHASE.getKey()));
    }

    @Test
    public void testArrayPropertiesExtraction() {
        String json = "{\n" +
                "  \"tags\": [\"Critical\", \"Backend\"],\n" +
                "  \"userGroups\": [\"ProjectManagers\", \"TechLeads\"]\n" +
                "}";

        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
        Map<String, String> properties = merger.extractPropertiesFromJson(jsonElement);

        assertEquals("Critical,Backend", properties.get(DetectProperties.DETECT_PROJECT_TAGS.getKey()));
        assertEquals("ProjectManagers,TechLeads", properties.get(DetectProperties.DETECT_PROJECT_USER_GROUPS.getKey()));
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

        assertEquals("Release Candidate", properties.get(DetectProperties.DETECT_PROJECT_VERSION_NICKNAME.getKey()));
        assertEquals("Release candidate for version 1.0", properties.get(DetectProperties.DETECT_PROJECT_VERSION_NOTES.getKey()));
        assertEquals("Apache License 2.0", properties.get(DetectProperties.DETECT_PROJECT_VERSION_LICENSE.getKey()));
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

        assertEquals("TestProject", properties.get(DetectProperties.DETECT_PROJECT_NAME.getKey()));
        assertEquals("false", properties.get(DetectProperties.DETECT_PROJECT_DEEP_LICENSE.getKey()));
    }

    @Test
    public void testProjectSettingsObjectExtraction() {
        // Test the new method that works directly with ProjectSettings objects
        ProjectSettings projectSettings = new ProjectSettings();
        projectSettings.setName("TestProject");
        projectSettings.setDescription("Test description");
        projectSettings.setTier(2);
        
        VersionSettings version = new VersionSettings();
        version.setName("1.0.0");
        version.setPhase("DEVELOPMENT");
        projectSettings.setVersion(version);
        
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
        assertEquals("ComprehensiveProject", properties.get(DetectProperties.DETECT_PROJECT_NAME.getKey()));
        assertEquals("A comprehensive test project", properties.get(DetectProperties.DETECT_PROJECT_DESCRIPTION.getKey()));
        assertEquals("5", properties.get(DetectProperties.DETECT_PROJECT_TIER.getKey()));
        assertEquals("app-12345", properties.get(DetectProperties.DETECT_PROJECT_APPLICATION_ID.getKey()));
        assertEquals("MyGroup", properties.get(DetectProperties.DETECT_PROJECT_GROUP_NAME.getKey()));
        assertEquals("Critical,Production", properties.get(DetectProperties.DETECT_PROJECT_TAGS.getKey()));
        assertEquals("Admins,Developers", properties.get(DetectProperties.DETECT_PROJECT_USER_GROUPS.getKey()));
        assertEquals("true", properties.get(DetectProperties.DETECT_PROJECT_LEVEL_ADJUSTMENTS.getKey()));
        assertEquals("true", properties.get(DetectProperties.DETECT_PROJECT_DEEP_LICENSE.getKey()));
        assertEquals("ALL", properties.get(DetectProperties.DETECT_PROJECT_CLONE_CATEGORIES.getKey()));
        assertEquals("2.0.0", properties.get(DetectProperties.DETECT_PROJECT_VERSION_NAME.getKey()));
        assertEquals("Major Release", properties.get(DetectProperties.DETECT_PROJECT_VERSION_NICKNAME.getKey()));
        assertEquals("Major version update", properties.get(DetectProperties.DETECT_PROJECT_VERSION_NOTES.getKey()));
        assertEquals("RELEASED", properties.get(DetectProperties.DETECT_PROJECT_VERSION_PHASE.getKey()));
        assertEquals("EXTERNAL", properties.get(DetectProperties.DETECT_PROJECT_VERSION_DISTRIBUTION.getKey()));
        assertEquals("false", properties.get(DetectProperties.DETECT_PROJECT_VERSION_UPDATE.getKey()));
        assertEquals("MIT License", properties.get(DetectProperties.DETECT_PROJECT_VERSION_LICENSE.getKey()));
    }
}