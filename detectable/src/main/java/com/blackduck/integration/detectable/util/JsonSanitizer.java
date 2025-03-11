package com.blackduck.integration.detectable.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class JsonSanitizer {
    public static JsonObject sanitize(String json) throws JsonSyntaxException {
        return JsonParser.parseString(json).getAsJsonObject();
    }
}