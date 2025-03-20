package com.blackduck.integration.configuration.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.SortedMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.blackduck.integration.configuration.config.resolution.NoPropertyResolution;
import com.blackduck.integration.configuration.config.resolution.PropertyResolution;
import com.blackduck.integration.configuration.config.resolution.PropertyResolutionInfo;
import com.blackduck.integration.configuration.config.resolution.SourcePropertyResolution;
import com.blackduck.integration.configuration.config.value.ExceptionPropertyValue;
import com.blackduck.integration.configuration.config.value.NoValuePropertyValue;
import com.blackduck.integration.configuration.config.value.PropertyValue;
import com.blackduck.integration.configuration.config.value.ValuedPropertyValue;
import com.blackduck.integration.configuration.parse.ValueParseException;
import com.blackduck.integration.configuration.property.Property;
import com.blackduck.integration.configuration.property.base.NullableProperty;
import com.blackduck.integration.configuration.property.base.PassthroughProperty;
import com.blackduck.integration.configuration.property.base.TypedProperty;
import com.blackduck.integration.configuration.property.base.ValuedProperty;
import com.blackduck.integration.configuration.source.PropertySource;

public class PropertyConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(PropertyConfiguration.class);
    private final Map<String, PropertyResolution> resolutionCache = new HashMap<>();
    private final Map<String, PropertyValue<?>> valueCache = new HashMap<>();
    private final List<PropertySource> orderedPropertySources;
    private SortedMap<String, String> scanSettingsProperties;
    private static final String SCAN_SETTINGS_FAILURE_MSG = "There was an error parsing the value from Scan Settings File";

    public PropertyConfiguration(@NotNull List<PropertySource> orderedPropertySources, SortedMap<String, String> scanSettingsProperties) {
        this.orderedPropertySources = orderedPropertySources;
        this.scanSettingsProperties = scanSettingsProperties;
    }

    public void setScanSettingsProperties(SortedMap<String, String> scanSettingsProperties) {
        this.scanSettingsProperties = scanSettingsProperties;
    }

    //region
    @NotNull
    public <V, R> Optional<R> getValueOrEmpty(@NotNull NullableProperty<V, R> property) {
        assertPropertyNotNull(property);
        try {
            return getValue(property);
        } catch (InvalidPropertyException e) {
            return Optional.empty();
        }
    }

    @NotNull
    public <V, R> R getValueOrDefault(@NotNull ValuedProperty<V, R> property) {
        assertPropertyNotNull(property);
        try {
            return getValue(property);
        } catch (InvalidPropertyException e) {
            V defaultValue = property.getDefaultValue();
            return property.convertValue(defaultValue);
        }
    }

    @NotNull
    public <V, R> Optional<R> getValue(@NotNull NullableProperty<V, R> property) throws InvalidPropertyException {
        assertPropertyNotNull(property);

        PropertyValue<V> value = valueFromCache(property);
        Optional<ValueParseException> parseException = value.getException();
        Optional<PropertyResolutionInfo> propertyResolutionInfo = value.getResolutionInfo();

        if (value.getValue().isPresent()) {
            return value.getValue()
                .map(property::convertValue);
        } else if (parseException.isPresent() && propertyResolutionInfo.isPresent()) {
            throw new InvalidPropertyException(property.getKey(), propertyResolutionInfo.get().getSource(), parseException.get());
        } else if(!scanSettingsProperties.isEmpty() && scanSettingsProperties.containsKey(property.getKey())) {
            String scanSettingsValue = scanSettingsProperties.get(property.getKey());
            try {
                V returnValue = property.getValueParser().parse(scanSettingsValue);
                return Optional.of(property.convertValue(returnValue));
            } catch (ValueParseException e) {
                throw new RuntimeException(SCAN_SETTINGS_FAILURE_MSG, e);
            }
        } else {
            return Optional.empty();
        }
    }

    @NotNull
    public <V, R> R getValue(@NotNull ValuedProperty<V, R> property) throws InvalidPropertyException {
        assertPropertyNotNull(property);
        PropertyValue<V> propertyValue = valueFromCache(property);

        Optional<V> value = propertyValue.getValue();
        Optional<ValueParseException> parseException = propertyValue.getException();
        Optional<PropertyResolutionInfo> propertyResolutionInfo = propertyValue.getResolutionInfo();

        if (value.isPresent()) {
            return value.map(property::convertValue).get();
        } else if (parseException.isPresent() && propertyResolutionInfo.isPresent()) {
            throw new InvalidPropertyException(property.getKey(), propertyResolutionInfo.get().getSource(), parseException.get());
        } else if(!scanSettingsProperties.isEmpty() && scanSettingsProperties.containsKey(property.getKey())) {
            String scanSettingsValue =  scanSettingsProperties.get(property.getKey());
            try {
                V returnValue = property.getValueParser().parse(scanSettingsValue);
                return property.convertValue(returnValue);
            } catch (ValueParseException e) {
                throw new RuntimeException(SCAN_SETTINGS_FAILURE_MSG, e);
            }
        } else {
            V defaultValue = property.getDefaultValue();
            return property.convertValue(defaultValue);
        }
    }

    @NotNull
    public <V, R> Optional<V> getProvidedParsedValue(@NotNull TypedProperty<V, R> property) throws InvalidPropertyException {
        assertPropertyNotNull(property);

        PropertyValue<V> value = valueFromCache(property);
        Optional<ValueParseException> parseException = value.getException();
        Optional<PropertyResolutionInfo> propertyResolutionInfo = value.getResolutionInfo();

        if (value.getValue().isPresent()) {
            return value.getValue();
        } else if (parseException.isPresent() && propertyResolutionInfo.isPresent()) {
            throw new InvalidPropertyException(property.getKey(), propertyResolutionInfo.get().getSource(), parseException.get());
        } else if(!scanSettingsProperties.isEmpty() && scanSettingsProperties.containsKey(property.getKey())) {
            String scanSettingsValue = scanSettingsProperties.get(property.getKey());
            try {
                return Optional.of(property.getValueParser().parse(scanSettingsValue));
            } catch (ValueParseException e) {
                throw new RuntimeException(SCAN_SETTINGS_FAILURE_MSG, e);
            }
        } else {
            return Optional.empty();
        }
    }

    public boolean wasKeyProvided(@NotNull String key) {
        Assert.notNull(key, "Must provide a property.");
        return resolveFromCache(key).getResolutionInfo().isPresent();
    }

    public <V, R> boolean wasPropertyProvided(@NotNull TypedProperty<V, R> property) {
        assertPropertyNotNull(property);
        return wasKeyProvided(property.getKey());
    }

    public Optional<String> getPropertySource(@NotNull Property property) {
        assertPropertyNotNull(property);
        return resolveFromCache(property.getKey()).getResolutionInfo().map(PropertyResolutionInfo::getSource);
    }

    public Optional<String> getPropertySource(@NotNull String key) {
        Assert.notNull(key, "You must provide a key");
        return resolveFromCache(key).getResolutionInfo().map(PropertyResolutionInfo::getSource);
    }

    public Optional<String> getPropertyOrigin(@NotNull Property property) {
        assertPropertyNotNull(property);
        return resolveFromCache(property.getKey()).getResolutionInfo().map(PropertyResolutionInfo::getOrigin);
    }

    @NotNull
    public Set<String> getKeys() {
        return orderedPropertySources.stream()
            .map(PropertySource::getKeys)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
    }

    public <V, R> Optional<ValueParseException> getPropertyException(@NotNull TypedProperty<V, R> property) {
        assertPropertyNotNull(property);
        return valueFromCache(property).getException();
    }

    //endregion

    //region Advanced Usage
    public Optional<String> getRaw(@NotNull Property property) {
        assertPropertyNotNull(property, "Must supply a property get raw values.");
        return resolveKey(property.getKey());
    }

    private Optional<String> resolveKey(String key) {
        PropertyResolution propertyResolution = resolveFromCache(key);
        return propertyResolution.getResolutionInfo().map(PropertyResolutionInfo::getRaw);
    }

    @NotNull
    public Map<String, String> getRaw() {
        return getRaw((String key) -> true);
    }

    @NotNull
    public Optional<String> getRaw(@NotNull String key) {
        Assert.notNull(key, "Must supply a key to get a raw value");
        return resolveFromCache(key).getResolutionInfo().map(PropertyResolutionInfo::getRaw);
    }

    @NotNull
    public Map<String, String> getRaw(@NotNull Set<String> keys) {
        Assert.notNull(keys, "Must supply a set of keys to get raw values");
        return getRaw(keys::contains);
    }

    @NotNull
    public Map<String, String> getRawValueMap(@NotNull Set<Property> properties) {
        return getMaskedRawValueMap(properties, key -> false);
    }

    @NotNull
    public Map<String, String> getMaskedRawValueMap(@NotNull Set<Property> properties, Predicate<String> shouldMask) {
        Map<String, String> rawMap = new HashMap<>();
        Set<String> validKeys = properties.stream().map(Property::getKey).collect(Collectors.toSet());
        Set<String> collectedKeys = getKeys();
        handleInvalidKeys(collectedKeys, validKeys);
        for (Property property : properties) {
            String rawKey = property.getKey();
            if (property instanceof PassthroughProperty) {
                getRaw((PassthroughProperty) property).entrySet()
                    .forEach(passThrough -> {
                            String fullPassThroughKey = String.format("%s.%s", rawKey, passThrough.getKey());
                            rawMap.put(fullPassThroughKey, maskValue(fullPassThroughKey, passThrough.getValue(), shouldMask));
                        }
                    );
            } else {
                getRaw(property).ifPresent(rawValue -> rawMap.put(rawKey, maskValue(rawKey, rawValue, shouldMask)));
            }
        }
        return rawMap;
    }

    public static void handleInvalidKeys(Set<String> collectedKeys, Set<String> validKeys) {
        for (String key : collectedKeys) {
            if (!validKeys.contains(key)) {
                List<String> similarKeys = findSimilarKeys(key, validKeys);
                if (!similarKeys.isEmpty()) {
                    logger.warn("Property key '{}' is not valid. The most similar keys are: {}", key, similarKeys);
                }
            }
        }
    }

    public static List<String> findSimilarKeys(String invalidKey, Set<String> validKeys) {
        LevenshteinDistance levenshtein = new LevenshteinDistance();
        return validKeys.stream()
                .filter(key -> levenshtein.apply(invalidKey, key) <= 3)
                .collect(Collectors.toList());
    }

    public String maskValue(String rawKey, String rawValue, Predicate<String> shouldMask) {
        String maskedValue = rawValue;
        if (shouldMask.test(rawKey)) {
            maskedValue = StringUtils.repeat('*', maskedValue.length());
        }
        return maskedValue;
    }

    @NotNull
    public Map<String, String> getRaw(@NotNull Predicate<String> predicate) {
        Assert.notNull(predicate, "Must supply a predicate to get raw keys");

        Set<String> keys = getKeys().stream()
            .filter(predicate)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        keys.addAll(scanSettingsProperties.keySet().stream()
                .filter(predicate)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));

        Map<String, String> keyMap = new HashMap<>();
        keys.forEach(key -> {
            Optional<String> rawValue = resolveKey(key);
            if(rawValue.isPresent()) {
                keyMap.put(key, rawValue.get());
            } else if (scanSettingsProperties.containsKey(key)) {
                keyMap.put(key, scanSettingsProperties.get(key));
            }
        });
        return keyMap;
    }

    // Takes in a 'passthrough.key' and returns key map (whose keys have that value removed)
    // So value 'passthrough.key.example' is returned as 'example'
    @NotNull
    public Map<String, String> getRaw(@NotNull PassthroughProperty property) {
        assertPropertyNotNull(property, "Must supply a passthrough to get raw keys");

        Map<String, String> rawValues = getRaw((String key) -> key.startsWith(property.getKey()));
        Map<String, String> trimmedKeys = new HashMap<>();
        for (Map.Entry<String, String> entry : rawValues.entrySet()) {
            if (entry.getValue() != null) {
                trimmedKeys.put(property.trimKey(entry.getKey()), entry.getValue());
            }
        }
        return trimmedKeys;
    }
    //endregion Advanced Usage

    //region Implementation Details

    private void assertPropertyNotNull(Property property) {
        assertPropertyNotNull(property, "Must provide a property.");
    }

    private void assertPropertyNotNull(Property property, @NotNull String message) {
        Assert.notNull(property, message);
    }

    private PropertyResolution resolveFromCache(@NotNull String key) {
        Assert.notNull(key, "Cannot resolve a null key.");
        if (!resolutionCache.containsKey(key)) {
            resolutionCache.put(key, resolveFromPropertySources(key));
        }

        PropertyResolution value = resolutionCache.get(key);
        Assert.notNull(value, "Could not resolve a value, something has gone wrong with properties!");
        return value;
    }

    private PropertyResolution resolveFromPropertySources(@NotNull String key) {
        Assert.notNull(key, "Cannot resolve a null key.");
        for (PropertySource propertySource : orderedPropertySources) {
            if (propertySource.hasKey(key)) {
                String rawValue = propertySource.getValue(key);
                if (rawValue != null) {
                    String name = propertySource.getName();
                    String origin = propertySource.getOrigin(key);
                    PropertyResolutionInfo propertyResolutionInfo = new PropertyResolutionInfo(name, origin, rawValue);
                    return new SourcePropertyResolution(propertyResolutionInfo);
                }
            }
        }
        return new NoPropertyResolution();
    }

    @NotNull
    private <V, R> PropertyValue<V> valueFromCache(@NotNull TypedProperty<V, R> property) {
        if (!valueCache.containsKey(property.getKey())) {
            valueCache.put(property.getKey(), valueFromResolution(property));
        }

        PropertyValue<V> value = (PropertyValue<V>) valueCache.get(property.getKey());
        Assert.notNull(value, "Could not source a value, something has gone wrong with properties!");
        return value;
    }

    @NotNull
    private <V, R> PropertyValue<V> valueFromResolution(@NotNull TypedProperty<V, R> property) {
        Assert.notNull(property, "Cannot resolve a null property.");
        Optional<PropertyResolutionInfo> propertyResolution = resolveFromCache(property.getKey()).getResolutionInfo();
        return propertyResolution
            .map(propertyResolutionInfo -> coerceValue(property, propertyResolutionInfo))
            .orElseGet(NoValuePropertyValue::new);
    }

    @NotNull
    private <V, R> PropertyValue<V> coerceValue(@NotNull TypedProperty<V, R> property, @NotNull PropertyResolutionInfo propertyResolutionInfo) {
        Assert.notNull(property, "Cannot resolve a null property.");
        Assert.notNull(propertyResolutionInfo, "Cannot coerce a null property resolution.");
        try {
            V value = property.getValueParser().parse(propertyResolutionInfo.getRaw());
            return new ValuedPropertyValue<>(value, propertyResolutionInfo);
        } catch (ValueParseException e) {
            return new ExceptionPropertyValue<>(e, propertyResolutionInfo);
        }
    }
    //endregion
}


