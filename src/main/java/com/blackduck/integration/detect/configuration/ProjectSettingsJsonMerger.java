package com.blackduck.integration.detect.configuration;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * This class parses the detect.project.settings JSON and extracts individual property values.
 * Individual properties take precedence over JSON values.
 */
public class ProjectSettingsJsonMerger {
    private final Gson gson = new Gson();
    
    /**
     * Creates a map of property keys to values from the JSON project settings.
     * The JSON structure maps to detect.project.* property keys.
     * 
     * Expected JSON structure:
     * {
     *   "name": "project-name",
     *   "description": "project description",
     *   "tier": 3,
     *   "applicationId": "app-id",
     *   "groupName": "group-name",
     *   "tags": ["tag1", "tag2"],
     *   "userGroups": ["group1", "group2"],
     *   "levelAdjustments": true,
     *   "deepLicense": true,
     *   "detector": "GRADLE",
     *   "tool": ["DOCKER", "DETECTOR"],
     *   "cloneCategories": "ALL",
     *   "codelocation": {
     *     "prefix": "prefix",
     *     "suffix": "suffix"
     *   },
     *   "version": {
     *     "name": "1.0.0",
     *     "nickname": "nickname", 
     *     "notes": "notes",
     *     "phase": "DEVELOPMENT",
     *     "distribution": "EXTERNAL",
     *     "update": false,
     *     "license": "Apache License 2.0"
     *   }
     * }
     */
    public Map<String, String> extractPropertiesFromJson(JsonElement jsonElement) {
        Map<String, String> properties = new HashMap<>();
        
        if (jsonElement == null || !jsonElement.isJsonObject()) {
            return properties;
        }
        
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        
        // Direct mappings
        addIfPresent(properties, jsonObject, "name", "detect.project.name");
        addIfPresent(properties, jsonObject, "description", "detect.project.description");
        addIfPresent(properties, jsonObject, "tier", "detect.project.tier");
        addIfPresent(properties, jsonObject, "applicationId", "detect.project.application.id");
        addIfPresent(properties, jsonObject, "groupName", "detect.project.group.name");
        addIfPresent(properties, jsonObject, "levelAdjustments", "detect.project.level.adjustments");
        addIfPresent(properties, jsonObject, "deepLicense", "detect.project.deep.license");
        addIfPresent(properties, jsonObject, "detector", "detect.project.detector");
        addIfPresent(properties, jsonObject, "cloneCategories", "detect.project.clone.categories");
        
        // Array mappings (convert to comma-separated strings)
        addArrayIfPresent(properties, jsonObject, "tags", "detect.project.tags");
        addArrayIfPresent(properties, jsonObject, "userGroups", "detect.project.user.groups");
        addArrayIfPresent(properties, jsonObject, "tool", "detect.project.tool");
        
        // Nested codelocation object
        if (jsonObject.has("codelocation") && jsonObject.get("codelocation").isJsonObject()) {
            JsonObject codelocation = jsonObject.getAsJsonObject("codelocation");
            addIfPresent(properties, codelocation, "prefix", "detect.project.codelocation.prefix");
            addIfPresent(properties, codelocation, "suffix", "detect.project.codelocation.suffix");
        }
        
        // Nested version object
        if (jsonObject.has("version") && jsonObject.get("version").isJsonObject()) {
            JsonObject version = jsonObject.getAsJsonObject("version");
            addIfPresent(properties, version, "name", "detect.project.version.name");
            addIfPresent(properties, version, "nickname", "detect.project.version.nickname");
            addIfPresent(properties, version, "notes", "detect.project.version.notes");
            addIfPresent(properties, version, "phase", "detect.project.version.phase");
            addIfPresent(properties, version, "distribution", "detect.project.version.distribution");
            addIfPresent(properties, version, "update", "detect.project.version.update");
            addIfPresent(properties, version, "license", "detect.project.version.license");
        }
        
        return properties;
    }
    
    private void addIfPresent(Map<String, String> properties, JsonObject jsonObject, String jsonKey, String propertyKey) {
        if (jsonObject.has(jsonKey)) {
            JsonElement element = jsonObject.get(jsonKey);
            if (!element.isJsonNull()) {
                if (element.isJsonPrimitive()) {
                    properties.put(propertyKey, element.getAsString());
                } else {
                    // For non-primitive types, convert to JSON string
                    properties.put(propertyKey, gson.toJson(element));
                }
            }
        }
    }
    
    private void addArrayIfPresent(Map<String, String> properties, JsonObject jsonObject, String jsonKey, String propertyKey) {
        if (jsonObject.has(jsonKey)) {
            JsonElement element = jsonObject.get(jsonKey);
            if (!element.isJsonNull() && element.isJsonArray()) {
                StringBuilder sb = new StringBuilder();
                element.getAsJsonArray().forEach(item -> {
                    if (sb.length() > 0) sb.append(",");
                    sb.append(item.getAsString());
                });
                properties.put(propertyKey, sb.toString());
            }
        }
    }
}