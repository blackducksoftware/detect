package com.blackduck.integration.detectable.detectables.cargo;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class CargoDependencyTransformer {
    private static final Logger logger = LoggerFactory.getLogger(CargoDependencyTransformer.class);

    public DependencyGraph transform(List<String> cargoTreeOutput) {
        DependencyGraph graph = new BasicDependencyGraph();
        Deque<Dependency> dependencyStack = new ArrayDeque<>();

        for (String line : cargoTreeOutput) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            int currentLevel = extractDepth(line);
            String dependencyInfo = line.substring(String.valueOf(currentLevel).length()).trim();
            Dependency dependency = parseDependency(dependencyInfo);

            if (dependency != null) {
                while (!dependencyStack.isEmpty() && dependencyStack.size() >= currentLevel) {
                    dependencyStack.pop();
                }

                if (currentLevel == 1) {
                    graph.addDirectDependency(dependency);
                    // clearing stack to search for the next direct dependency
                    dependencyStack.clear();
                } else if (!dependencyStack.isEmpty()) {
                    graph.addParentWithChild(dependencyStack.peek(), dependency);
                } else {
                    logger.warn("No parent found for dependency: {} {}", dependency.getName(), dependency.getVersion());
                }
                dependencyStack.push(dependency);
            }
        }
        return graph;
    }

    private int extractDepth(String line) {
        int depth = 0;
        while (depth < line.length() && Character.isDigit(line.charAt(depth))) {
            depth++;
        }
        return Integer.parseInt(line.substring(0, depth));
    }

    private Dependency parseDependency(String dependencyInfo) {
        String[] parts = dependencyInfo.split(" ");
        if (parts.length < 2) {
            logger.warn("Unable to parse dependency from line: {}", dependencyInfo);
            return null;
        }

        String name = parts[0];
        String version = parts[1].replace("v", ""); // Remove 'v' prefix from version

        ExternalId externalId = new ExternalId(Forge.CRATES);
        externalId.setName(name);
        externalId.setVersion(version);

        return new Dependency(name, version, externalId);
    }
}