package com.blackduck.integration.detectable.detectables.maven.resolver;

/**
 * Property resolution classes
 */

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

/**
 * Enables the creation of PropertyResolver objects.
 */
class PropertiesResolverProvider {
    private Map<String, String> userJvmProps;
    private Map<String, String> parentProperties;
    private Map<String, String> jvmProps;
    private Function<String, String> envPropertiesProvider;

    /**
     * Creates a new PropertiesResolverProvider.
     *
     * @param userJvmProps User defined properties via the jvm "-D" option
     * @param envPropertiesProvider Function that provides properties set by the environment
     */
    public PropertiesResolverProvider(Map<String, String> userJvmProps,
                                      Function<String, String> envPropertiesProvider) {
        this.userJvmProps = userJvmProps != null ? new HashMap<>(userJvmProps) : new HashMap<>();
        this.jvmProps = getJavaCalcProperties();
        this.envPropertiesProvider = envPropertiesProvider;
        this.parentProperties = new HashMap<>();
    }

    /**
     * Sets the parent properties for property resolution.
     */
    public void setParentProperties(Map<String, String> parentProps) {
        this.parentProperties = parentProps != null ? new HashMap<>(parentProps) : new HashMap<>();
    }

    /**
     * Creates a new PropertyResolver with the given properties.
     *
     * @param pomProps Properties sourced from the pom.xml file
     * @param projectStarProperties Well-known properties that don't need resolution
     * @return A new PropertyResolver instance
     */
    public PropertyResolver newResolver(Map<String, String> pomProps, Map<String, String> projectStarProperties) {
        // Clone to avoid mutations
        Map<String, String> clonedPomProps = pomProps != null ? new HashMap<>(pomProps) : new HashMap<>();

        Map<String, String> resolvedProps = new HashMap<>();

        // Add properties in priority order (lowest to highest)

        // 1. Java system properties (lowest priority for collisions with pom props)
        for (Map.Entry<String, String> entry : jvmProps.entrySet()) {
            if (!clonedPomProps.containsKey(entry.getKey())) {
                resolvedProps.put(entry.getKey(), entry.getValue());
            }
        }

        // 2. Parent properties (skip on pom props collision)
        for (Map.Entry<String, String> entry : parentProperties.entrySet()) {
            if (!clonedPomProps.containsKey(entry.getKey())) {
                resolvedProps.put(entry.getKey(), entry.getValue());
            }
        }

        // 3. User JVM properties (higher priority)
        resolvedProps.putAll(userJvmProps);

        // 4. Project star properties (highest priority)
        if (projectStarProperties != null) {
            resolvedProps.putAll(projectStarProperties);
        }

        return new PropertyResolver(resolvedProps, clonedPomProps, parentProperties, envPropertiesProvider);
    }

    /**
     * Gets calculated Java system properties.
     * This is a simplified version - in a full implementation, this would include
     * properties like ${java.version}, ${os.name}, etc.
     */
    private Map<String, String> getJavaCalcProperties() {
        Map<String, String> props = new HashMap<>();

        // Add some basic Java system properties
        props.put("java.version", System.getProperty("java.version", ""));
        props.put("java.home", System.getProperty("java.home", ""));
        props.put("os.name", System.getProperty("os.name", ""));
        props.put("os.arch", System.getProperty("os.arch", ""));
        props.put("user.name", System.getProperty("user.name", ""));
        props.put("user.home", System.getProperty("user.home", ""));
        props.put("file.separator", System.getProperty("file.separator", "/"));

        // Maven-specific calculated properties would be added here
        // like ${maven.build.timestamp}, ${project.basedir}, etc.

        return props;
    }

    // Getters
    public Map<String, String> getParentProperties() { return parentProperties; }
    public Map<String, String> getUserJvmProps() { return userJvmProps; }
}
