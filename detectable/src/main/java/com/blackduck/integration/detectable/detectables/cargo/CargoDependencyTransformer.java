package com.blackduck.integration.detectable.detectables.cargo;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;

import com.blackduck.integration.bdio.model.dependency.ProjectDependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

//TODO: Proposed CargoDependency Parser, Need to be implemented to parser
public class CargoDependencyTransformer {
    public DependencyGraph transform(JsonObject jsonObject) {
        BasicDependencyGraph graph = new BasicDependencyGraph();
        Map<String, Dependency> dependencies = new HashMap<>();

        JsonArray packages = jsonObject.getAsJsonArray("packages");
        if (packages != null) {
            for (JsonElement packageElement : packages) {
                JsonObject packageObj = packageElement.getAsJsonObject();
                String name = packageObj.get("name").getAsString();
                String version = packageObj.get("version").getAsString();
                String uniqueKey = name + ":" + version;

                ExternalId externalId = new ExternalId(Forge.CRATES);
                Dependency dependency = new ProjectDependency(name, version, externalId);
                dependencies.put(uniqueKey, dependency);
                graph.addDirectDependency(dependency);
            }
        }

        return graph;
    }
}
