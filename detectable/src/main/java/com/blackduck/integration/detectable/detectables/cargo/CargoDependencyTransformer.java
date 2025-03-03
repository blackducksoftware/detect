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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//TODO: Proposed CargoDependency Parser, Need to be implemented to parser

public class CargoDependencyTransformer {

    public DependencyGraph transform(JsonObject jsonObject) {
        BasicDependencyGraph graph = new BasicDependencyGraph();
        Map<String, Dependency> packageIdToDependency = new HashMap<>();
        Set<String> allDependencies = new HashSet<>();

        JsonArray packages = jsonObject.getAsJsonArray("packages");
        for (JsonElement pkg : packages) {
            JsonObject packageObj = pkg.getAsJsonObject();
            String packageId = packageObj.get("id").getAsString();
            String name = packageObj.get("name").getAsString();
            String version = packageObj.get("version").getAsString();

            ExternalId externalId = new ExternalId(Forge.CRATES);
            externalId.setName(name);
            externalId.setVersion(version);

            Dependency dep = new ProjectDependency(name, version, externalId);
            packageIdToDependency.put(packageId, dep);
        }

        for (JsonElement pkg : packages) {
            JsonObject packageObj = pkg.getAsJsonObject();
            String currentPackageId = packageObj.get("id").getAsString();
            Dependency currentDep = packageIdToDependency.get(currentPackageId);

            JsonArray deps = packageObj.getAsJsonArray("dependencies");
            for (JsonElement dep : deps) {
                JsonObject depObj = dep.getAsJsonObject();
                String depName = depObj.get("name").getAsString();
                String depReq = depObj.get("req").getAsString();

                String depPackageId = findPackageId(packages, depName, depReq);
                if (depPackageId != null) {
                    Dependency childDep = packageIdToDependency.get(depPackageId);
                    graph.addParentWithChild(currentDep, childDep);
                    allDependencies.add(depPackageId);
                }
            }
        }

        for (JsonElement pkg : packages) {
            JsonObject packageObj = pkg.getAsJsonObject();
            String packageId = packageObj.get("id").getAsString();
            Dependency dependency = packageIdToDependency.get(packageId);

            if (!allDependencies.contains(packageId)) {
                graph.addDirectDependency(dependency);
            }
        }

        return graph;
    }

    private String findPackageId(JsonArray packages, String name, String req) {
        for (JsonElement pkg : packages) {
            JsonObject packageObj = pkg.getAsJsonObject();
            String packageName = packageObj.get("name").getAsString();
            String packageVersion = packageObj.get("version").getAsString();

            if (packageName.equals(name) && satisfiesVersionRequirement(packageVersion, req)) {
                return packageObj.get("id").getAsString();
            }
        }
        return null;
    }

    private boolean satisfiesVersionRequirement(String version, String req) {
        return version.startsWith(req.replace("^", "").replace("~", ""));
    }
}