package com.blackduck.integration.detectable.detectables.conda.parser;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.detectable.detectables.conda.model.CondaListElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CondaTreeParser {
    private final CondaDependencyCreator condaDependencyCreator;
    private static final Logger logger = LoggerFactory.getLogger(CondaTreeParser.class);

    private static class DependencyDepth {
        final Dependency dependency;
        final int depth;

        DependencyDepth(Dependency dependency, int depth) {
            this.dependency = dependency;
            this.depth = depth;
        }
    }
    
    // Conda tree uses specific indentation patterns
    private static final List<String> TREE_PREFIXES = Arrays.asList("├─ ","│  ","└─ ","   ");
    private static final Pattern PACKAGE_VERSION_PATTERN = Pattern.compile("^([^\\s=]+)(?:==|\\s+)([^\\s]+)(?:\\s+.*)?");
    private static final Pattern PACKAGE_SUFFIX = Pattern.compile("\\s+\\[required:.*?\\]");
    private int depth;
    private DependencyGraph dependencyGraph;

    public CondaTreeParser(CondaDependencyCreator condaDependencyCreator) {
        this.condaDependencyCreator = condaDependencyCreator;
    }

    public DependencyGraph parse(List<String> condaTreeOutput, Map<String, CondaListElement> dependencies) {
        dependencyGraph = new BasicDependencyGraph();
        Deque<DependencyDepth> dependencyStack = new ArrayDeque<>();
        
        for (String line : condaTreeOutput) {
            if (line.trim().isEmpty() || line.contains("dependencies of")) {
                continue;
            }
            
            parseLine(line, dependencyStack, dependencies);
        }
        
        return dependencyGraph;
    }

    private void parseLine(String line, Deque<DependencyDepth> dependencyStack, Map<String, CondaListElement> dependencies) {
        String cleanedLine = calculateDepthAndCleanLine(line);
        // Dependency line
        Dependency dependency = parseDependency(cleanedLine, dependencies);
        if (dependency != null) {
            addDependencyToGraph(dependency, dependencyStack);
            dependencyStack.push(new DependencyDepth(dependency, depth));
        }
    }

    private Dependency parseDependency(String line, Map<String, CondaListElement> dependencies) {
        // Remove requirement specifications in brackets like [required: >=3.5]
        String cleanLine = line.replaceAll(PACKAGE_SUFFIX.pattern(), "");
        
        // Handle version constraints and extract package name and version
        Matcher matcher = PACKAGE_VERSION_PATTERN.matcher(cleanLine.trim());
        
        if (matcher.find()) {
            String packageName = matcher.group(1);

            if (!dependencies.isEmpty() && dependencies.containsKey(packageName)) {
                CondaListElement condaListElement = dependencies.get(packageName);
                return condaDependencyCreator.createFromCondaListElement(condaListElement, condaListElement.platform);
            } else {
                logger.warn("Not able to find dependency info properly: {}", packageName);
                return null;
            }
        } else {
            logger.warn("Unable to parse dependency from line: {}", line);
            return null;
        }
    }

    private void addDependencyToGraph(Dependency dependency, Deque<DependencyDepth> dependencyStack) {
        if (depth == 0) {
            // Direct dependency of root package
            dependencyGraph.addDirectDependency(dependency);
            dependencyStack.clear();
        } else if (!dependencyStack.isEmpty() && dependencyStack.peek().depth == depth) {
            // Sibling dependency - same level as previous
            dependencyStack.pop();
            dependencyGraph.addChildWithParent(dependency, dependencyStack.peek().dependency);
        } else if (!dependencyStack.isEmpty() && dependencyStack.peek().depth > depth) {
            // Moving up in the tree - adjust stack
            while (!dependencyStack.isEmpty() && dependencyStack.peek().depth > depth) {
                dependencyStack.pop();
            }
            dependencyGraph.addChildWithParent(dependency, dependencyStack.peek().dependency);
        } else {
            // Child of the previous dependency
            dependencyGraph.addChildWithParent(dependency, dependencyStack.peek().dependency);
        }
    }

    private String calculateDepthAndCleanLine(String line) {
        depth = 0;
        String cleanedLine = line;
        for(String prefix: TREE_PREFIXES) {
            while(cleanedLine.contains(prefix)) {
                depth++;
                cleanedLine = cleanedLine.replaceFirst(Pattern.quote(prefix), "");
            }
        }

        return cleanedLine;
    }
}
