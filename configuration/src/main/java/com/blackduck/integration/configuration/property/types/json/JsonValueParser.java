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
            // First try parsing as-is (handles properly quoted JSON)
            return gson.fromJson(value, JsonElement.class);
        } catch (JsonSyntaxException e) {
            try {
                // If that fails, try normalizing unquoted JSON (handles shell-processed input)
                String normalizedJson = normalizeUnquotedJson(value.trim());
                return gson.fromJson(normalizedJson, JsonElement.class);
            } catch (JsonSyntaxException e2) {
                throw new ValueParseException(value, "JsonElement", "Failed to parse JSON: " + e.getMessage());
            }
        }
    }

    private String normalizeUnquotedJson(String value) {
        // Handle unquoted JSON by adding quotes around keys and string values
        // This is a simplified approach for common shell-processed JSON scenarios
        return value
            // Quote unquoted object keys
            .replaceAll("([{,]\\s*)([a-zA-Z_][a-zA-Z0-9_]*)(\\s*:)", "$1\"$2\"$3")
            // Quote unquoted string values (non-numeric, non-boolean, non-object/array)
            .replaceAll("(:\\s*)([a-zA-Z][a-zA-Z0-9\\s]*?)(?=\\s*[,}])", "$1\"$2\"");
    }
}