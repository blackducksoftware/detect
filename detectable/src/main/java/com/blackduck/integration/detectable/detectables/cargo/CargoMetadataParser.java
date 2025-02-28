package com.blackduck.integration.detectable.detectables.cargo;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CargoMetadataParser {
    private final Gson gson = new Gson();

    public JsonObject parseMetadata(String cargoMetadataJson) {
        return JsonParser.parseString(cargoMetadataJson).getAsJsonObject();
    }
}
