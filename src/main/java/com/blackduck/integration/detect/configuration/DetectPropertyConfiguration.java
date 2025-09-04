package com.blackduck.integration.detect.configuration;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.blackduck.integration.configuration.config.PropertyConfiguration;
import com.blackduck.integration.configuration.property.base.NullableProperty;
import com.blackduck.integration.configuration.property.base.PassthroughProperty;
import com.blackduck.integration.configuration.property.base.TypedProperty;
import com.blackduck.integration.configuration.property.base.ValuedProperty;
import com.blackduck.integration.configuration.property.types.path.NullablePathProperty;
import com.blackduck.integration.configuration.property.types.path.PathListProperty;
import com.blackduck.integration.configuration.property.types.path.PathProperty;
import com.blackduck.integration.configuration.property.types.path.PathResolver;
import com.blackduck.integration.configuration.source.MapPropertySource;

public class DetectPropertyConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(DetectPropertyConfiguration.class);
    
    private final PropertyConfiguration propertyConfiguration;
    private final PathResolver pathResolver;
    private final ProjectSettingsJsonMerger jsonMerger = new ProjectSettingsJsonMerger();
    private Map<String, String> cachedJsonProperties = null;

    public DetectPropertyConfiguration(PropertyConfiguration propertyConfiguration, PathResolver pathResolver) {
        this.propertyConfiguration = propertyConfiguration;
        this.pathResolver = pathResolver;
    }

    public Path getPathOrNull(NullablePathProperty property) {
        return propertyConfiguration.getValue(property).map(path -> path.resolvePath(pathResolver)).orElse(null);
    }

    public Path getPath(PathProperty property) {
        return propertyConfiguration.getValue(property).resolvePath(pathResolver);
    }

    public List<Path> getPaths(PathListProperty property) {
        return propertyConfiguration.getValue(property).stream()
            .map(path -> path.resolvePath(pathResolver))
            .collect(Collectors.toList());
    }

    public <V, R> R getNullableValue(NullableProperty<V, R> detectProperty) {
        return propertyConfiguration.getValue(detectProperty).orElse(null);
    }

    public <V, R> R getValue(ValuedProperty<V, R> detectProperty) {
        return propertyConfiguration.getValue(detectProperty);
    }

    @SafeVarargs
    public static <V, R> Optional<R> getFirstProvidedValueOrEmpty(PropertyConfiguration propertyConfiguration, NullableProperty<V, R>... properties) {
        for (NullableProperty<V, R> property : properties) {
            if (propertyConfiguration.wasPropertyProvided(property)) {
                return propertyConfiguration.getValueOrEmpty(property);
            }
        }

        return Optional.empty();
    }

    /**
     * Will get the first property in a list that was provided by the user.
     * If no property was provided, the default value of the first property will be used.
     */
    @SafeVarargs
    public static <V, R> R getFirstProvidedValueOrDefault(@NotNull PropertyConfiguration propertyConfiguration, @NotNull ValuedProperty<V, R>... properties) {
        Optional<R> value = getFirstProvidedValueOrEmpty(propertyConfiguration, properties);
        return value.orElseGet(() -> properties[0].convertValue(properties[0].getDefaultValue()));

    }

    /**
     * Will get the first property in a list that was provided by the user.
     * If no property was provided, the default will NOT be used.
     */
    @SafeVarargs
    public static <V, R> Optional<R> getFirstProvidedValueOrEmpty(@NotNull PropertyConfiguration propertyConfiguration, @NotNull ValuedProperty<V, R>... properties) {
        for (ValuedProperty<V, R> property : properties) {
            if (propertyConfiguration.wasPropertyProvided(property)) {
                return Optional.of(propertyConfiguration.getValueOrDefault(property));
            }
        }

        return Optional.empty();
    }

    public Map<String, String> getRaw() {
        return propertyConfiguration.getRaw();
    }

    public Map<String, String> getRaw(Set<String> keys) {
        return propertyConfiguration.getRaw(keys);
    }

    public Map<String, String> getRaw(PassthroughProperty passthroughProperty) {
        return propertyConfiguration.getRaw(passthroughProperty);
    }

    public <V, R> boolean wasPropertyProvided(TypedProperty<V, R> property) {
        return propertyConfiguration.wasPropertyProvided(property);
    }
    
    /**
     * Gets the value of a nullable property, checking JSON project settings as fallback.
     * Individual detect.project.* properties take precedence over JSON values.
     */
    public <V, R> R getNullableValueWithJsonFallback(NullableProperty<V, R> detectProperty) {
        return getValueWithJsonFallback(detectProperty, true);
    }
    
    /**
     * Gets the value of a valued property, checking JSON project settings as fallback.
     * Individual detect.project.* properties take precedence over JSON values.
     */
    public <V, R> R getValueWithJsonFallback(ValuedProperty<V, R> detectProperty) {
        return getValueWithJsonFallback(detectProperty, false);
    }
    
    /**
     * Common implementation for getting property values with JSON fallback.
     * @param detectProperty the property to get the value for
     * @param nullable true to return null for empty Optional values
     */
    private <V, R> R getValueWithJsonFallback(TypedProperty<V, R> detectProperty, boolean nullable) {
        // First check if the individual property was provided
        if (propertyConfiguration.wasPropertyProvided(detectProperty)) {
            if (nullable && detectProperty instanceof NullableProperty) {
                return propertyConfiguration.getValue((NullableProperty<V, R>) detectProperty).orElse(null);
            } else {
                return propertyConfiguration.getValue((ValuedProperty<V, R>) detectProperty);
            }
        }
        
        // If it's a project property, check JSON fallback
        if (detectProperty.getKey().startsWith("detect.project.")) {
            String jsonValue = getJsonPropertyValue(detectProperty.getKey());
            if (jsonValue != null) {
                try {
                    // Parse the JSON value using the property's parser
                    V parsedValue = detectProperty.getValueParser().parse(jsonValue);
                    return detectProperty.convertValue(parsedValue);
                } catch (Exception e) {
                    logger.warn("Failed to parse JSON value '{}' for property '{}': {}", 
                        jsonValue, detectProperty.getKey(), e.getMessage());
                }
            }
        }
        
        // Return default behavior based on property type
        if (nullable && detectProperty instanceof NullableProperty) {
            return propertyConfiguration.getValue((NullableProperty<V, R>) detectProperty).orElse(null);
        } else {
            return propertyConfiguration.getValue((ValuedProperty<V, R>) detectProperty);
        }
    }

    private String getJsonPropertyValue(String propertyKey) {
        if (cachedJsonProperties == null) {
            cachedJsonProperties = loadJsonProperties();
        }
        return cachedJsonProperties.get(propertyKey);
    }

    private Map<String, String> loadJsonProperties() {
        Optional<JsonElement> jsonSettings = propertyConfiguration.getValue(DetectProperties.DETECT_PROJECT_SETTINGS);
        if (jsonSettings.isPresent()) {
            try {
                logger.debug("Loading project settings from JSON");
                return jsonMerger.extractPropertiesFromJson(jsonSettings.get());
            } catch (Exception e) {
                logger.warn("Failed to parse JSON project settings: {}", e.getMessage());
            }
        }
        return new HashMap<>();
    }
}
