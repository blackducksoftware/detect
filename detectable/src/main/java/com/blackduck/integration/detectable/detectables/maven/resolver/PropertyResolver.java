package com.blackduck.integration.detectable.detectables.maven.resolver;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Resolves properties for maven models.
 */
class PropertyResolver {
    private Map<String, String> resolvedProperties;
    private Map<String, String> properties;
    private Map<String, String> parentProperties;
    private Function<String, String> envPropertiesProvider;

    public PropertyResolver(Map<String, String> resolvedProperties,
                            Map<String, String> properties,
                            Map<String, String> parentProperties,
                            Function<String, String> envPropertiesProvider) {
        this.resolvedProperties = resolvedProperties != null ? new HashMap<>(resolvedProperties) : new HashMap<>();
        this.properties = properties != null ? new HashMap<>(properties) : new HashMap<>();
        this.parentProperties = parentProperties != null ? new HashMap<>(parentProperties) : new HashMap<>();
        this.envPropertiesProvider = envPropertiesProvider;
    }

    /**
     * Resolves a property by name following Maven's property resolution order.
     *
     * @param propertyName The name of the property to resolve
     * @return The resolved property value, or null if not found
     */
    public String resolve(String propertyName) {
        // Check resolved properties first (highest priority)
        if (resolvedProperties.containsKey(propertyName)) {
            return resolvedProperties.get(propertyName);
        }

        // Check project properties
        if (properties.containsKey(propertyName)) {
            return properties.get(propertyName);
        }

        // Check parent properties
        if (parentProperties.containsKey(propertyName)) {
            return parentProperties.get(propertyName);
        }

        // Check environment variables (lowest priority)
        if (envPropertiesProvider != null) {
            return envPropertiesProvider.apply(propertyName);
        }

        return null;
    }

    /**
     * Returns all properties combined for use with StringSubstitutor.
     */
    public Map<String, String> getAllProperties() {
        Map<String, String> allProps = new HashMap<>();

        // Add in reverse priority order (lower priority first)
        if (parentProperties != null) {
            allProps.putAll(parentProperties);
        }
        if (properties != null) {
            allProps.putAll(properties);
        }
        if (resolvedProperties != null) {
            allProps.putAll(resolvedProperties);
        }

        return allProps;
    }

    // Getters
    public Map<String, String> getResolvedProperties() { return resolvedProperties; }
    public Map<String, String> getProperties() { return properties; }
    public Map<String, String> getParentProperties() { return parentProperties; }
}
