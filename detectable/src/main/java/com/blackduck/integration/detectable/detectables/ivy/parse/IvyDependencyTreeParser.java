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
        int previousLevel = 0;

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

            int currentLevel = indentation.length() / 3;

            Dependency dependency = parseDependency(dependencyString);
            if (dependency == null) {
                logger.debug("Failed to parse dependency: {}", dependencyString);
                continue;
            }

            // Handle level changes similar to Maven's approach
            if (currentLevel == 0) {
                // Direct dependency (top-level)
                dependencyStack.clear();
                graph.addDirectDependency(dependency);
                dependencyStack.push(dependency);
            } else {
                // Transitive dependency - adjust stack based on level changes
                if (currentLevel == previousLevel) {
                    // Sibling of previous dependency - pop previous and use same parent
                    dependencyStack.pop();
                } else if (currentLevel < previousLevel) {
                    // Moving back up the tree - pop back to correct parent level
                    for (int i = previousLevel; i >= currentLevel; i--) {
                        dependencyStack.pop();
                    }
                }
                // For currentLevel > previousLevel, previous dependency becomes the parent (already on stack)

                if (!dependencyStack.isEmpty()) {
                    Dependency parent = dependencyStack.peek();
                    graph.addParentWithChild(parent, dependency);
                    dependencyStack.push(dependency);
                } else {
                    // Shouldn't happen, but handle gracefully
                    logger.warn("Found transitive dependency without parent: {}", dependencyString);
                    graph.addDirectDependency(dependency);
                    dependencyStack.push(dependency);
                }
            }

            previousLevel = currentLevel;
        }

        return graph;
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