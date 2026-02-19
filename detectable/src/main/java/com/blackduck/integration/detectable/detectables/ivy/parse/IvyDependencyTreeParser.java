package com.blackduck.integration.detectable.detectables.ivy.parse;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;

public class IvyDependencyTreeParser {
    private static final Logger logger = LoggerFactory.getLogger(IvyDependencyTreeParser.class);

    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile("^\\[ivy:dependencytree]\\s+([|\\s]*)(\\+-|\\\\-)\\s+(.+)$");
    private static final Pattern IVY_DEPENDENCY_FORMAT = Pattern.compile("^([^#]+)#([^;]+);(.+)$");

    private final ExternalIdFactory externalIdFactory;

    public IvyDependencyTreeParser(ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
    }

    public DependencyGraph parse(List<String> dependencyTreeOutput) {
        DependencyGraph graph = new BasicDependencyGraph();
        Deque<Dependency> dependencyStack = new ArrayDeque<>();

        for (String line : dependencyTreeOutput) {
            if (StringUtils.isBlank(line)) {
                continue;
            }

            Matcher matcher = DEPENDENCY_PATTERN.matcher(line);
            if (!matcher.matches()) {
                continue;
            }

            String indentation = matcher.group(1);
            String dependencyString = matcher.group(3);

            // Calculate the level: count '|' characters in the indentation
            // Level 0 = direct dependency (no '|')
            // Level 1 = one '|' (transitive of direct)
            // Level 2 = two '|' (transitive of transitive), etc.
            int currentLevel = calculateLevel(indentation);

            Dependency dependency = parseDependency(dependencyString);
            if (dependency == null) {
                logger.debug("Failed to parse dependency: {}", dependencyString);
                continue;
            }

            updateDependencyStack(dependencyStack, currentLevel);

            if (currentLevel == 0) {
                // Direct dependency (top-level)
                graph.addDirectDependency(dependency);
                dependencyStack.clear();
                dependencyStack.push(dependency);
                logger.debug("Added direct dependency at level {}: {}", currentLevel, dependency.getExternalId());
            } else {
                // Transitive dependency - parent is at top of stack
                if (!dependencyStack.isEmpty()) {
                    Dependency parent = dependencyStack.peek();
                    graph.addParentWithChild(parent, dependency);
                    dependencyStack.push(dependency);
                    logger.debug("Added transitive dependency at level {}: {} -> {}", currentLevel, parent.getExternalId(), dependency.getExternalId());
                } else {
                    logger.warn("Found transitive dependency at level {} without parent: {}", currentLevel, dependencyString);
                }
            }
        }

        return graph;
    }

    private int calculateLevel(String indentation) {
        int level = 0;
        for (char c : indentation.toCharArray()) {
            if (c == '|') {
                level++;
            }
        }
        return level;
    }

    private void updateDependencyStack(Deque<Dependency> dependencyStack, int currentLevel) {
        // Keep the stack size at currentLevel
        // This ensures the parent at currentLevel - 1 is at the top
        while (dependencyStack.size() > currentLevel) {
            dependencyStack.pop();
        }
    }

    private Dependency parseDependency(String dependencyString) {
        Matcher matcher = IVY_DEPENDENCY_FORMAT.matcher(dependencyString.trim());
        if (matcher.matches()) {
            String org = matcher.group(1);
            String name = matcher.group(2);
            String version = matcher.group(3);

            ExternalId externalId = externalIdFactory.createMavenExternalId(org, name, version);
            return new Dependency(name, version, externalId);
        }

        logger.debug("Could not parse Ivy dependency format: {}", dependencyString);
        return null;
    }
}