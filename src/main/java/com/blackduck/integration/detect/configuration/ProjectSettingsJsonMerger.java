package com.blackduck.integration.detect.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

/**
 * This class parses the detect.project.settings JSON and extracts individual property values.
 * Individual properties take precedence over JSON values.
 */
public class ProjectSettingsJsonMerger {
    private final Gson gson = new Gson();
    
    /**
     * Creates a map of property keys to values from the JSON project settings.
     * Uses POJO deserialization for type-safe parsing.
     */
    public Map<String, String> extractPropertiesFromJson(JsonElement jsonElement) {
        Map<String, String> properties = new HashMap<>();
        
        if (jsonElement == null || !jsonElement.isJsonObject()) {
            return properties;
        }
        
        try {
            ProjectSettings projectSettings = gson.fromJson(jsonElement, ProjectSettings.class);
            return extractPropertiesFromProjectSettings(projectSettings);
        } catch (Exception e) {
            // If JSON parsing fails, return empty map
            return properties;
        }
    }

    /**
     * Creates a map of property keys to values from a ProjectSettings object.
     * This method can be used with both JSON-parsed and directly created ProjectSettings objects.
     */
    public Map<String, String> extractPropertiesFromProjectSettings(ProjectSettings projectSettings) {
        Map<String, String> properties = new HashMap<>();
        
        if (projectSettings == null) {
            return properties;
        }
        
        // Direct mappings
        addIfNotNull(properties, projectSettings.getName(), DetectProperties.DETECT_PROJECT_NAME.getKey());
        addIfNotNull(properties, projectSettings.getDescription(), DetectProperties.DETECT_PROJECT_DESCRIPTION.getKey());
        addIfNotNull(properties, projectSettings.getProjectTier(), DetectProperties.DETECT_PROJECT_TIER.getKey());
        addIfNotNull(properties, projectSettings.getProjectLevelAdjustments(), DetectProperties.DETECT_PROJECT_LEVEL_ADJUSTMENTS.getKey());
        addIfNotNull(properties, projectSettings.getLicenseDeepLicense(), DetectProperties.DETECT_PROJECT_DEEP_LICENSE.getKey());
        addIfNotNull(properties, projectSettings.getCloneCategories(), DetectProperties.DETECT_PROJECT_CLONE_CATEGORIES.getKey());

        // Version object mappings
        VersionSettings version = projectSettings.getVersionRequest();
        if (version != null) {
            addIfNotNull(properties, version.getVersionName(), DetectProperties.DETECT_PROJECT_VERSION_NAME.getKey());
            addIfNotNull(properties, version.getNickname(), DetectProperties.DETECT_PROJECT_VERSION_NICKNAME.getKey());
            addIfNotNull(properties, version.getPhase(), DetectProperties.DETECT_PROJECT_VERSION_PHASE.getKey());
            addIfNotNull(properties, version.getDistribution(), DetectProperties.DETECT_PROJECT_VERSION_DISTRIBUTION.getKey());
            addIfNotNull(properties, version.getUpdate(), DetectProperties.DETECT_PROJECT_VERSION_UPDATE.getKey());
        }
        
        return properties;
    }
    
    private void addIfNotNull(Map<String, String> properties, Object value, String propertyKey) {
        if (value != null) {
            properties.put(propertyKey, value.toString());
        }
    }
    
    private void addListIfNotNull(Map<String, String> properties, List<String> list, String propertyKey) {
        if (list != null && !list.isEmpty()) {
            properties.put(propertyKey, String.join(",", list));
        }
    }
}