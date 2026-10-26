package com.blackduck.integration.configuration.property.types.enums;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.configuration.parse.ListValueParser;
import com.blackduck.integration.configuration.property.PropertyBuilder;
import com.blackduck.integration.configuration.property.base.ValuedAlikeListProperty;
import com.blackduck.integration.configuration.property.deprecation.DeprecatedValueUsage;
import com.blackduck.integration.configuration.util.EnumPropertyUtils;
import com.blackduck.integration.configuration.util.PropertyUtils;

public class EnumListProperty<E extends Enum<E>> extends ValuedAlikeListProperty<E> {
    @NotNull
    private final Class<E> enumClass;
    private final List<E> deprecatedValues = new ArrayList<>();

    public EnumListProperty(@NotNull String key, @NotNull List<E> defaultValue, @NotNull Class<E> enumClass) {
        super(key, new ListValueParser<>(new EnumValueParser<>(enumClass)), defaultValue);
        this.enumClass = enumClass;
    }

    public EnumListProperty<E> deprecateValue(E value, String reason) {
        deprecatedValues.add(value);
        addDeprecatedValueInfo(value.toString(), reason);
        return this;
    }

    @NotNull
    @Override
    public List<DeprecatedValueUsage> checkForDeprecatedValues(List<E> value) {
        return value.stream()
            .filter(deprecatedValues::contains)
            .map(element -> createDeprecatedValueUsageIfExists(element.toString()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    public static <E extends Enum<E>> PropertyBuilder<EnumListProperty<E>> newBuilder(@NotNull String key, @NotNull List<E> defaultValue, @NotNull Class<E> enumClass) {
        return new PropertyBuilder<EnumListProperty<E>>().setCreator(() -> new EnumListProperty<>(key, defaultValue, enumClass));
    }

    @Nullable
    @Override
    public String describeDefault() {
        return PropertyUtils.describeObjectList(getDefaultValue());
    }

    @Override
    public boolean isCaseSensitive() {
        return true;
    }

    @Nullable
    @Override
    public List<String> listExampleValues() {
        return EnumPropertyUtils.getEnumNames(enumClass);
    }

    @Override
    public boolean isOnlyExampleValues() {
        return true;
    }

    @Nullable
    @Override
    public String describeType() {
        return enumClass.getSimpleName() + " List";
    }
}