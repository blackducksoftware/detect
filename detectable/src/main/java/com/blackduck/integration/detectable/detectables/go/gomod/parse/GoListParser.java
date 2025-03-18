package com.blackduck.integration.detectable.detectables.go.gomod.parse;

import java.util.LinkedList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.blackduck.integration.detectable.detectables.go.gomod.model.GoListAllData;
import com.blackduck.integration.detectable.detectables.go.gomod.model.GoListModule;

public class GoListParser {
    private final Gson gson;

    public GoListParser(Gson gson) {
        this.gson = gson;
    }

    public List<GoListAllData> parseGoListAllJsonOutput(List<String> listUJsonOutput) throws JsonSyntaxException {
        return parseGoListJsonToClass(listUJsonOutput, GoListAllData.class);
    }

    public List<GoListModule> parseGoListModuleJsonOutput(List<String> listModuleJsonOutput) throws JsonSyntaxException {
        return parseGoListJsonToClass(listModuleJsonOutput, GoListModule.class);
    }

    private <T> List<T> parseGoListJsonToClass(List<String> listJsonOutput, Class<T> classOfT) throws JsonSyntaxException {
        List<T> listEntries = new LinkedList<>();

        StringBuilder jsonEntry = new StringBuilder();
        for (String line : listJsonOutput) {
            jsonEntry.append(line);
            if (line.startsWith("}")) {
                T data = gson.fromJson(jsonEntry.toString(), classOfT);
                listEntries.add(data);
                // Reset to accumulate a new entry
                jsonEntry = new StringBuilder();
            }
        }

        return listEntries; //
    }
}
