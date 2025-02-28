package com.blackduck.integration.detectable.detectables.cargo;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.ProjectDependency;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectables.cargo.model.CargoLockPackage;
import com.blackduck.integration.util.NameVersion;
import com.blackduck.integration.detectable.util.NameOptionalVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CargoTreeParser {
    private final Gson gson = new Gson();
    private static final Logger logger = LoggerFactory.getLogger(CargoTreeParser.class.getName());

    public DependencyGraph parse(String cargoTreeJson) {
        JsonObject jsonObject = gson.fromJson(cargoTreeJson, JsonObject.class);
        JsonArray packages = jsonObject.getAsJsonArray("packages");
        Map<String, Dependency> dependencyMap = new HashMap<>();
        BasicDependencyGraph graph = new BasicDependencyGraph();

        for (JsonElement packageElement : packages) {
            JsonObject packageObject = packageElement.getAsJsonObject();
            String name = packageObject.get("name").getAsString();
            String version = packageObject.get("version").getAsString();
            String id = packageObject.get("id").getAsString();
            ExternalId externalId = new ExternalId(Forge.CRATES);
            Dependency dependency = new ProjectDependency(name, version, externalId);
            dependencyMap.put(id, dependency);
        }

        for (JsonElement packageElement : packages) {
            JsonObject packageObject = packageElement.getAsJsonObject();
            String id = packageObject.get("id").getAsString();
            JsonArray dependenciesArray = packageObject.getAsJsonArray("dependencies");
            Dependency parent = dependencyMap.get(id);

            if (dependenciesArray != null) {
                for (JsonElement dependencyElement : dependenciesArray) {
                    if (!dependencyElement.isJsonObject()) continue;
                    JsonObject dependencyObject = dependencyElement.getAsJsonObject();
                    if (!dependencyObject.has("id")) {
                        logger.warn("Missing 'id' field in dependency object.");
                        continue;
                    }
                    String dependencyId = dependencyObject.get("id").getAsString();
                    Dependency child = dependencyMap.get(dependencyId);
                    if (child != null) {
                        graph.addDirectDependency(child);
                    }
                }
            }
        }

        return graph;
    }
}
