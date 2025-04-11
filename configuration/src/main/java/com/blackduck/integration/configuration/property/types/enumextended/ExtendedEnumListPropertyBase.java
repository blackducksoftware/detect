package com.blackduck.integration.configuration.property.types.enumextended;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.configuration.parse.ListValueParser;
import com.blackduck.integration.configuration.property.base.ValuedListProperty;
import com.blackduck.integration.configuration.property.deprecation.DeprecatedValueUsage;
import com.blackduck.integration.configuration.util.EnumPropertyUtils;
import com.blackduck.integration.configuration.util.PropertyUtils;

public abstract class ExtendedEnumListPropertyBase<E extends Enum<E>, B extends Enum<B>, R> extends ValuedListProperty<ExtendedEnumValue<E, B>, R> {
    private final List<String> allOptions;
    protected final Class<B> bClass;
    protected final Class<E> eClass;

    public ExtendedEnumListPropertyBase(@NotNull String key, @NotNull List<ExtendedEnumValue<E, B>> defaultValue, @NotNull Class<E> eClass, @NotNull Class<B> bClass) {
        super(key, new ListValueParser<>(new ExtendedEnumValueParser<>(eClass, bClass)), defaultValue);

        allOptions = new ArrayList<>();
        allOptions.addAll(EnumPropertyUtils.getEnumNames(eClass));
        allOptions.addAll(EnumPropertyUtils.getEnumNames(bClass));
        this.bClass = bClass;
        this.eClass = eClass;
    }

    @Nullable
    @Override
    public String describeDefault() {
        return PropertyUtils.describeObjectList(getDefaultValue());
    }

    @Override
    public boolean isCaseSensitive() {
        return false;
    }

    @Nullable
    @Override
    public List<String> listExampleValues() {
        return allOptions;
    }

    @Override
    public boolean isOnlyExampleValues() {
        return true;
    }

    @Nullable
    @Override
    public String describeType() {
        return bClass.getSimpleName() + " List";
    }

    List<E> deprecatedExtendedValues = new ArrayList<>();

    public void deprecateExtendedValue(E value, String reason) {
        deprecatedExtendedValues.add(value);
        addDeprecatedValueInfo(value.toString(), reason);
    }

    @Override
    @NotNull
    public List<DeprecatedValueUsage> checkForDeprecatedValues(List<ExtendedEnumValue<E, B>> value) {
        return value.stream()
            .filter(element -> element.getExtendedValue().isPresent())
            .map(element -> element.getExtendedValue().get())
            .filter(deprecatedExtendedValues::contains)
            .map(element -> createDeprecatedValueUsageIfExists(element.toString()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }
}
