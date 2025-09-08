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
            
            // Direct mappings
            addIfNotNull(properties, projectSettings.getName(), DetectProperties.DETECT_PROJECT_NAME.getKey());
            addIfNotNull(properties, projectSettings.getDescription(), DetectProperties.DETECT_PROJECT_DESCRIPTION.getKey());
            addIfNotNull(properties, projectSettings.getTier(), DetectProperties.DETECT_PROJECT_TIER.getKey());
            addIfNotNull(properties, projectSettings.getApplicationId(), DetectProperties.DETECT_PROJECT_APPLICATION_ID.getKey());
            addIfNotNull(properties, projectSettings.getGroupName(), DetectProperties.DETECT_PROJECT_GROUP_NAME.getKey());
            addIfNotNull(properties, projectSettings.getLevelAdjustments(), DetectProperties.DETECT_PROJECT_LEVEL_ADJUSTMENTS.getKey());
            addIfNotNull(properties, projectSettings.getDeepLicense(), DetectProperties.DETECT_PROJECT_DEEP_LICENSE.getKey());
            addIfNotNull(properties, projectSettings.getCloneCategories(), DetectProperties.DETECT_PROJECT_CLONE_CATEGORIES.getKey());
            
            // Array mappings (convert to comma-separated strings)
            addListIfNotNull(properties, projectSettings.getTags(), DetectProperties.DETECT_PROJECT_TAGS.getKey());
            addListIfNotNull(properties, projectSettings.getUserGroups(), DetectProperties.DETECT_PROJECT_USER_GROUPS.getKey());

            // Version object mappings
            VersionSettings version = projectSettings.getVersion();
            if (version != null) {
                addIfNotNull(properties, version.getName(), DetectProperties.DETECT_PROJECT_VERSION_NAME.getKey());
                addIfNotNull(properties, version.getNickname(), DetectProperties.DETECT_PROJECT_VERSION_NICKNAME.getKey());
                addIfNotNull(properties, version.getNotes(), DetectProperties.DETECT_PROJECT_VERSION_NOTES.getKey());
                addIfNotNull(properties, version.getPhase(), DetectProperties.DETECT_PROJECT_VERSION_PHASE.getKey());
                addIfNotNull(properties, version.getDistribution(), DetectProperties.DETECT_PROJECT_VERSION_DISTRIBUTION.getKey());
                addIfNotNull(properties, version.getUpdate(), DetectProperties.DETECT_PROJECT_VERSION_UPDATE.getKey());
                addIfNotNull(properties, version.getLicense(), DetectProperties.DETECT_PROJECT_VERSION_LICENSE.getKey());
            }
            
        } catch (Exception e) {
            // If JSON parsing fails, return empty map
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