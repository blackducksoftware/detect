package com.blackduck.integration.detectable.detectables.cargo.transform;

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
            processLine(line.trim(), graph, dependencyStack);
        }

        return graph;
    }

    private void processLine(String line, DependencyGraph graph, Deque<Dependency> dependencyStack) {
        if (line.isEmpty()) {
            return;
        }

        int currentLevel = extractDepth(line);
        String dependencyInfo = line.substring(String.valueOf(currentLevel).length()).trim();
        Dependency dependency = parseDependency(dependencyInfo);

        if (dependency != null) {
            updateDependencyStack(dependencyStack, currentLevel);
            addDependencyToGraph(graph, dependencyStack, dependency, currentLevel);
            dependencyStack.push(dependency);
        }
    }

    private void updateDependencyStack(Deque<Dependency> dependencyStack, int currentLevel) {
        while (!dependencyStack.isEmpty() && dependencyStack.size() >= currentLevel) {
            dependencyStack.pop();
        }
    }

    private void addDependencyToGraph(DependencyGraph graph, Deque<Dependency> dependencyStack, Dependency dependency, int currentLevel) {
        if (currentLevel == 1) {
            graph.addDirectDependency(dependency);
            dependencyStack.clear();
        } else if (!dependencyStack.isEmpty()) {
            graph.addParentWithChild(dependencyStack.peek(), dependency);
        } else {
            logger.warn("No parent found for dependency: {} {}", dependency.getName(), dependency.getVersion());
        }
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
        String version = parts[1].replace("v", "");
        ExternalId externalId = createExternalId(name, version);

        return new Dependency(name, version, externalId);
    }

    private ExternalId createExternalId(String name, String version) {
        ExternalId externalId = new ExternalId(Forge.CRATES);
        externalId.setName(name);
        externalId.setVersion(version);
        return externalId;
    }
}