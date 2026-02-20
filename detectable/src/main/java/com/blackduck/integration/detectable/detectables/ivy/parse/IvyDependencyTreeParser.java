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

    // Match exactly 1 space after [ivy:dependencytree], then capture indentation (pipes/spaces) before tree connector
    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile("^\\[ivy:dependencytree] ([|\\s]*)(\\+-|\\\\-)\\s+(.+)$");
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

            String connector = matcher.group(2);
            String dependencyString = matcher.group(3);

            // Calculate level by finding the position of the tree connector in the original line
            // This accurately determines depth regardless of pipes vs spaces in indentation
            int currentLevel = calculateLevel(line, connector);

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

    private int calculateLevel(String line, String connector) {
        // The level is determined by the POSITION where the tree connector appears
        // Find where the connector (+- or \-) starts in the line
        //
        // Examples from actual Ivy output:
        // "[ivy:dependencytree] +- dep"           -> connector at position 22 (after 1 space)  -> Level 0
        // "[ivy:dependencytree] |  +- dep"        -> connector at position 25 (after 1 + 3)   -> Level 1
        // "[ivy:dependencytree] |     \- dep"     -> connector at position 28 (after 1 + 6)   -> Level 2
        // "[ivy:dependencytree]    \- dep"        -> connector at position 25 (after 1 + 3)   -> Level 1
        // "[ivy:dependencytree] |  \- dep"        -> connector at position 25 (after 1 + 3)   -> Level 1
        //
        // The pattern: after "[ivy:dependencytree] " (22 chars), each level adds 3 characters
        // So: (connector_position - 22) / 3 = level

        int prefixLength = "[ivy:dependencytree] ".length();
        int connectorPosition = line.indexOf(connector, prefixLength);

        if (connectorPosition == -1) {
            logger.warn("Could not find connector '{}' in line: {}", connector, line);
            return 0;
        }

        // Calculate level based on how far the connector is from the prefix
        int indentationFromPrefix = connectorPosition - prefixLength;
        return indentationFromPrefix / 3;
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
            String versionString = matcher.group(3);

            // Handle Ivy version ranges - extract the first (lower bound) version
            String version = extractVersionFromRange(versionString);

            ExternalId externalId = externalIdFactory.createMavenExternalId(org, name, version);
            return new Dependency(name, version, externalId);
        }

        logger.debug("Could not parse Ivy dependency format: {}", dependencyString);
        return null;
    }

    private String extractVersionFromRange(String versionString) {
        // Ivy version ranges can be:
        // [1.81,1.82) - closed-open range
        // [1.0,2.0] - closed range
        // (1.0,2.0) - open range
        // [1.0,) - open-ended
        // 1.0 - exact version (no range)
        //
        // For ranges, we extract the first (lower bound) version

        if (versionString == null || versionString.isEmpty()) {
            return versionString;
        }

        // Check if it's a version range (starts with [ or ()
        if (versionString.startsWith("[") || versionString.startsWith("(")) {
            // Find the comma that separates the lower and upper bounds
            int commaIndex = versionString.indexOf(',');
            if (commaIndex > 0) {
                // Extract the lower bound version (skip the opening bracket/parenthesis)
                String lowerBound = versionString.substring(1, commaIndex).trim();
                logger.debug("Extracted version '{}' from range '{}'", lowerBound, versionString);
                return lowerBound;
            }
        }

        // Not a range, return as-is
        return versionString;
    }
}