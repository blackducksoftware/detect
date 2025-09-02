package com.blackduck.integration.configuration.property.types.json;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.blackduck.integration.configuration.parse.ValueParseException;
import com.blackduck.integration.configuration.parse.ValueParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * CHANGE: Added new JSON value parser to support detect.project.settings JSON property.
 * This parser uses Gson to parse JSON strings into JsonElement objects for property values.
 */

public class JsonValueParser extends ValueParser<JsonElement> {
    private final Gson gson = new Gson();

    @NotNull
    @Override
    public JsonElement parse(@Nullable String value) throws ValueParseException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValueParseException(value, "JsonElement", "JSON value cannot be null or empty");
        }

        try {
            return gson.fromJson(value, JsonElement.class);
        } catch (JsonSyntaxException e) {
            throw new ValueParseException(value, "JsonElement", "Failed to parse JSON: " + e.getMessage());
        }
    }
}