package com.blackduck.integration.detectable.detectables.conda.parser;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.detectable.detectables.conda.model.CondaInfo;
import com.blackduck.integration.detectable.detectables.conda.model.CondaListElement;

public class CondaListParser {
    private final Gson gson;
    private final CondaDependencyCreator dependencyCreator;

    public CondaListParser(Gson gson, CondaDependencyCreator dependencyCreator) {
        this.gson = gson;
        this.dependencyCreator = dependencyCreator;
    }

    /**
     * Parses conda list and info JSON text into a dependency graph.
     * <p>
     * This method deserializes conda list JSON data into CondaListElement objects and conda info JSON
     * into a CondaInfo object. It then extracts the platform information and creates a dependency graph
     * by mapping each conda list element to a dependency, using the platform to ensure correct dependency
     * resolution.
     * </p>
     *
     * @param listJsonText the JSON text containing the list of conda packages and their details
     * @param infoJsonText the JSON text containing conda environment information including the platform
     * @return a DependencyGraph containing all parsed conda dependencies as root-level children
     */
    public DependencyGraph parse(String listJsonText, String infoJsonText) {
        Type listType = new TypeToken<ArrayList<CondaListElement>>() {
        }.getType();
        List<CondaListElement> condaList = gson.fromJson(listJsonText, listType);
        CondaInfo condaInfo = gson.fromJson(infoJsonText, CondaInfo.class);
        String platform = condaInfo.platform;

        DependencyGraph graph = new BasicDependencyGraph();
        condaList.stream()
            .map(condaListElement -> dependencyCreator.createFromCondaListElement(condaListElement, platform))
            .forEach(graph::addChildToRoot);

        return graph;
    }

    /**
     * Collects conda dependencies from JSON text into a map indexed by package name.
     * <p>
     * This method deserializes conda list JSON data into CondaListElement objects and creates
     * a map that indexes each package by its name. This map can be used for quick lookup of
     * dependency information during analysis.
     * </p>
     *
     * @param listJsonText the JSON text containing the list of conda packages and their details
     * @return a Map where keys are conda package names and values are the corresponding CondaListElement objects
     */
    public Map<String, CondaListElement> collectDependencies(String listJsonText) {
        Type listType = new TypeToken<ArrayList<CondaListElement>>() {
        }.getType();
        List<CondaListElement> condaList = gson.fromJson(listJsonText, listType);

        HashMap<String, CondaListElement> dependencies = new HashMap<>();

        condaList.forEach(element  -> {
              dependencies.put(element.name, element);
        });

       return dependencies;
    }

}
