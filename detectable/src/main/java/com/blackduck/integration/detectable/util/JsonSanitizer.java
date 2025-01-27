package com.blackduck.integration.detectable.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class JsonSanitizer {
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();

    public static String sanitize(String json) throws JsonSyntaxException {
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        return GSON.toJson(jsonObject);
    }
}