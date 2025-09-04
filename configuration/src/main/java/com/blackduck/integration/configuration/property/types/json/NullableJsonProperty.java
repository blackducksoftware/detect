package com.blackduck.integration.configuration.property.types.json;

import com.google.gson.JsonElement;
import com.blackduck.integration.configuration.property.PropertyBuilder;
import com.blackduck.integration.configuration.property.base.NullableAlikeProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NullableJsonProperty extends NullableAlikeProperty<JsonElement> {
    public NullableJsonProperty(@NotNull String key) {
        super(key, new JsonValueParser());
    }

    public static PropertyBuilder<NullableJsonProperty> newBuilder(@NotNull String key) {
        return new PropertyBuilder<NullableJsonProperty>().setCreator(() -> new NullableJsonProperty(key));
    }

    @Nullable
    @Override
    public String describeType() {
        return "Optional JSON Object";
    }
}