package com.blackduck.integration.detectable.detectables.go.gomodfile.parse;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectables.go.gomodfile.GoModFileDetectableOptions;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoDependencyNode;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoModFileContent;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoModFileHelpers;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoModuleInfo;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enhanced Go mod parser that handles all go.mod directives and produces a dependency graph
 * compatible with Black Duck's detection system.
 */
public class GoModParser {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ExternalIdFactory externalIdFactory;
    private final GoModFileParser fileParser;
    private final GoModDependencyResolver dependencyResolver;
    private final GoModFileHelpers goModFileHelpers;

    private static final String ORPHAN_PARENT_NAME = "Additional_Components";
    private static final String ORPHAN_PARENT_VERSION = "none";

    private static final String DEPENDENCY_SEPARATOR = StringUtils.repeat("-", 60 );

    public GoModParser(ExternalIdFactory externalIdFactory, GoModFileDetectableOptions options) {
        this.externalIdFactory = externalIdFactory;
        this.fileParser = new GoModFileParser();
        this.dependencyResolver = new GoModDependencyResolver(options);
        this.goModFileHelpers = new GoModFileHelpers(externalIdFactory);
    }

    /**
     * Parses a go.mod file and creates a dependency graph that respects all directives.
     * 
     * @param goModContents List of lines from the go.mod file
     * @return DependencyGraph with resolved dependencies
     */
    public DependencyGraph parseGoModFile(List<String> goModContents) {        
        // Parse the raw go.mod content
        GoModFileContent goModContent = fileParser.parseGoModFile(goModContents);
        logger.debug("Parsed go.mod: {}", goModContent);
        
        // Resolve dependencies by applying directives
        GoModDependencyResolver.ResolvedDependencies resolvedDependencies = 
            dependencyResolver.resolveDependencies(goModContent, externalIdFactory);
        logger.debug("Resolved dependencies: {}", resolvedDependencies);

        // Create dependency graph
        return createDependencyGraph(resolvedDependencies, goModContent);
    }
    
    /**
     * Gets detailed information about the parsed go.mod file.
     * This can be useful for debugging or detailed analysis.
     * 
     * @param goModContents List of lines from the go.mod file
     * @return GoModFileContent with all parsed information
     */
    public GoModFileContent getDetailedParseResult(List<String> goModContents) {
        return fileParser.parseGoModFile(goModContents);
    }

    private void printDependencyGraphOfIndirectDependency(Dependency dependency, List<GoDependencyNode> path) {
        logger.debug(DEPENDENCY_SEPARATOR);
        logger.debug("Dependency graph for indirect dependency: {}", dependency);
        logger.debug(DEPENDENCY_SEPARATOR);
        for(int idx=0; idx < path.size(); idx++) {
            GoDependencyNode pathEntry = path.get(idx);
            logger.debug("> {} {} {}", (idx > 0 ? StringUtils.repeat(" ", idx) : ""), pathEntry.getDependency().getName(), pathEntry.getDependency().getVersion());
        }
        logger.debug(DEPENDENCY_SEPARATOR);
    }

    private String createModuleKey(GoModuleInfo module) {
        return module.getName() + "@" + module.getVersion();
    }
    
    private DependencyGraph createDependencyGraph(GoModDependencyResolver.ResolvedDependencies resolvedDependencies, GoModFileContent goModContent) {
        DependencyGraph graph = new BasicDependencyGraph();
        Set<String> excludedKeys = createExcludedKeys(goModContent);
        
        addDirectDependencies(graph, resolvedDependencies.getDirectDependencies(), excludedKeys);
        List<Dependency> orphanDependencies = addIndirectDependencies(graph, resolvedDependencies, excludedKeys);
        addOrphanDependencies(graph, orphanDependencies);
        
        return graph;
    }

    private Set<String> createExcludedKeys(GoModFileContent goModContent) {
        return goModContent.getExcludedModules().stream()
                .map(this::createModuleKey)
                .collect(Collectors.toSet());
    }

    private void addDirectDependencies(DependencyGraph graph, List<GoModuleInfo> directDependencies, Set<String> excludedKeys) {
        for (GoModuleInfo directDep : directDependencies) {
            if (isExcluded(directDep, excludedKeys)) {
                logger.debug("Excluding mapping of direct dependency: {} as it is in the excluded modules list", directDep);
                continue;
            }
            Dependency dependency = goModFileHelpers.createDependency(directDep);
            graph.addDirectDependency(dependency);
            logger.debug("Added direct dependency: {} to the root module", dependency);
        }
    }

    private List<Dependency> addIndirectDependencies(DependencyGraph graph, GoModDependencyResolver.ResolvedDependencies resolvedDependencies, Set<String> excludedKeys) {
        List<Dependency> orphanDependencies = new ArrayList<>();
        
        for (GoModuleInfo indirectDep : resolvedDependencies.getIndirectDependencies()) {
            Dependency dependency = goModFileHelpers.createDependency(indirectDep);
            GoDependencyNode targetNode = new GoDependencyNode(false, dependency, new ArrayList<>());
            List<GoDependencyNode> path = getDependencyPathFromGraph(targetNode, resolvedDependencies.getDependencyGraph(), new ArrayList<>());
            
            if (path.isEmpty()) {
                handleOrphanDependency(dependency, orphanDependencies);
            } else {
                addDependencyPath(graph, dependency, path, excludedKeys);
            }
        }
        
        return orphanDependencies;
    }

    private void handleOrphanDependency(Dependency dependency, List<Dependency> orphanDependencies) {
        logger.warn("No path found for indirect dependency: {}. Hence, adding it to orphan dependencies", dependency);
        orphanDependencies.add(dependency);
    }

    private void addDependencyPath(DependencyGraph graph, Dependency dependency, List<GoDependencyNode> path, Set<String> excludedKeys) {
        if (logger.isDebugEnabled()) {
            printDependencyGraphOfIndirectDependency(dependency, path);
        }
        
        GoDependencyNode childDependencyNode = path.get(path.size() - 1);
        if (isExcluded(childDependencyNode.getDependency(), excludedKeys)) {
            logger.debug("Excluding mapping of transitive dependency: {} as it is in the excluded modules list", childDependencyNode.getDependency());
            return;
        }
        
        addPathToGraph(graph, path);
    }

    private void addPathToGraph(DependencyGraph graph, List<GoDependencyNode> path) {
        for (int idx = 0; idx < path.size() - 1; idx++) {
            GoDependencyNode parentDependency = path.get(idx);
            GoDependencyNode childDependency = path.get(idx + 1);
            graph.addChildWithParent(childDependency.getDependency(), parentDependency.getDependency());
            logger.debug("Mapped {} as child of {}", childDependency.getDependency(), parentDependency.getDependency());
        }
    }

    private boolean isExcluded(GoModuleInfo module, Set<String> excludedKeys) {
        return excludedKeys.contains(createModuleKey(module));
    }

    private boolean isExcluded(Dependency dependency, Set<String> excludedKeys) {
        return excludedKeys.contains(String.format("%s@%s", dependency.getName(), dependency.getVersion()));
    }

    private void addOrphanDependencies(DependencyGraph graph, List<Dependency> orphanDependencies) {
        if (orphanDependencies.isEmpty()) {
            return;
        }
        
        Dependency orphanParentDependency = goModFileHelpers.createDependency(new GoModuleInfo(ORPHAN_PARENT_NAME, ORPHAN_PARENT_VERSION));
        graph.addDirectDependency(orphanParentDependency);
        logger.debug("Created orphan parent dependency: {} for orphan dependencies", orphanParentDependency);
        
        for (Dependency orphanDep : orphanDependencies) {
            graph.addChildWithParent(orphanDep, orphanParentDependency);
            logger.debug("Mapped orphan dependency {} as child of {}", orphanDep, orphanParentDependency);
        }
    }

    public List<GoDependencyNode> getDependencyPathFromGraph(GoDependencyNode targetNode, GoDependencyNode graph, List<GoDependencyNode> visited) {
        boolean isFound = false;
        // Perform DFS to find the path from root to targetNode
        for (GoDependencyNode child : graph.getChildren()) {
            // Match only by name since replace directives may change versions
            if (child.getDependency().getName().equals(targetNode.getDependency().getName())) {
                visited.add(graph);
                // Add the child to the path because the replace directives would result in a different version to be added to the composition than the defined one in go.mod file.
                visited.add(child);
                isFound = true;
                break;
            }
        }
        if (isFound) {
            return visited;
        } else {
            if (!graph.isRootNode()) {
                visited.add(graph);
            }
            for (GoDependencyNode child : graph.getChildren()) {
                List<GoDependencyNode> childPath = getDependencyPathFromGraph(targetNode, child, visited);
                if (childPath.isEmpty()) {
                    visited.remove(visited.size() - 1);
                } else {
                    return childPath;
                }
            }
        }
        return new ArrayList<>(); // Return empty list if not found
    }
}
