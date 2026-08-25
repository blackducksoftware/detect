package com.blackduck.integration.configuration.property.types.enumallnone.property;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.blackduck.integration.configuration.property.PropertyBuilder;
import com.blackduck.integration.configuration.property.types.enumallnone.enumeration.NoneEnum;
import com.blackduck.integration.configuration.property.types.enumallnone.list.NoneEnumList;
import com.blackduck.integration.configuration.property.types.enumextended.ExtendedEnumListPropertyBase;
import com.blackduck.integration.configuration.property.types.enumextended.ExtendedEnumValue;

public class NoneEnumListProperty<B extends Enum<B>> extends ExtendedEnumListPropertyBase<NoneEnum, B, NoneEnumList<B>> {
    public NoneEnumListProperty(@NotNull String key, List<ExtendedEnumValue<NoneEnum, B>> defaultValue, @NotNull Class<B> eClass) {
        super(key, defaultValue, NoneEnum.class, eClass);
    }

    public static <B extends Enum<B>> PropertyBuilder<NoneEnumListProperty<B>> newBuilder(
        @NotNull String key,
        List<ExtendedEnumValue<NoneEnum, B>> defaultValue,
        @NotNull Class<B> eClass
    ) {
        return new PropertyBuilder<NoneEnumListProperty<B>>().setCreator(() -> new NoneEnumListProperty<>(key, defaultValue, eClass));
    }

    public static <B extends Enum<B>> PropertyBuilder<NoneEnumListProperty<B>> newBuilder(@NotNull String key, @NotNull NoneEnum noneValue, @NotNull Class<B> eClass) {
        return new PropertyBuilder<NoneEnumListProperty<B>>().setCreator(() -> new NoneEnumListProperty<>(
            key,
            Collections.singletonList(ExtendedEnumValue.ofExtendedValue(noneValue)),
            eClass
        ));
    }

    public static <B extends Enum<B>> PropertyBuilder<NoneEnumListProperty<B>> newBuilder(@NotNull String key, @NotNull B extendedValue, @NotNull Class<B> eClass) {
        return new PropertyBuilder<NoneEnumListProperty<B>>().setCreator(() -> new NoneEnumListProperty<>(
            key,
            Collections.singletonList(ExtendedEnumValue.ofBaseValue(extendedValue)),
            eClass
        ));
    }

    public NoneEnumListProperty<B> deprecateValue(B value, String reason) {
        addDeprecatedBaseValue(value, reason);
        return this;
    }

    public NoneEnumList<B> toList(List<ExtendedEnumValue<NoneEnum, B>> values) {
        return new NoneEnumList<>(values, bClass);
    }

    @Override
    @NotNull
    public NoneEnumList<B> convertValue(List<ExtendedEnumValue<NoneEnum, B>> value) {
        return new NoneEnumList<>(value, bClass);
    }
}
