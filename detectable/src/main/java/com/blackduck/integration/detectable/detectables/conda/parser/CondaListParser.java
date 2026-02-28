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

    public Map<String, CondaListElement> getDependencies(String listJsonText) {
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
