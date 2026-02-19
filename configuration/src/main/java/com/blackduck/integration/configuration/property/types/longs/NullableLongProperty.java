package com.blackduck.integration.configuration.property.types.longs;

import com.blackduck.integration.configuration.property.PropertyBuilder;
import com.blackduck.integration.configuration.property.types.integer.NullableIntegerProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.configuration.property.base.NullableAlikeProperty;

public class NullableLongProperty extends NullableAlikeProperty<Long> {
    public NullableLongProperty(@NotNull String key) {
        super(key, new LongValueParser());
    }

    public static PropertyBuilder<NullableLongProperty> newBuilder(@NotNull String key) {
        return new PropertyBuilder<NullableLongProperty>().setCreator(() -> new NullableLongProperty(key));
    }

    @Nullable
    @Override
    public String describeType() {
        return "Optional Long";
    }
}
