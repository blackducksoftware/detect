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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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
    
    private DependencyGraph createDependencyGraph(GoModDependencyResolver.ResolvedDependencies resolvedDependencies, 
                                                 GoModFileContent goModContent) {
        DependencyGraph graph = new BasicDependencyGraph();
        
        // Add direct dependencies
        for (GoModuleInfo directDep : resolvedDependencies.getDirectDependencies()) {
            Dependency dependency = goModFileHelpers.CreateDependency(directDep);
            graph.addDirectDependency(dependency);
            logger.debug("Added direct dependency: {} to the root module", dependency.toString());
        }
        
        // Add indirect dependencies
        for (GoModuleInfo indirectDep : resolvedDependencies.getIndirectDependencies()) {
            Dependency dependency = goModFileHelpers.CreateDependency(indirectDep);
            GoDependencyNode targetNode = new GoDependencyNode(false, dependency, new ArrayList<>());
            // Use DFS to find targetNode from resolvedDependencies.getDependencyGraph() of type GoDependencyNode
            List<GoDependencyNode> path = GetDependencyPathFromGraph(targetNode, resolvedDependencies.getDependencyGraph(), new ArrayList<>());
            if (!path.isEmpty()) {
                logger.debug("-----------------------------------------------------------");
                logger.debug("Dependency graph for indirect dependency: {}", dependency.toString());
                logger.debug("-----------------------------------------------------------");
                for(int idx=0; idx < path.size(); idx++) {
                    GoDependencyNode pathEntry = path.get(idx);
                    logger.debug(">" + " ".repeat(idx) + pathEntry.getDependency().getName() + " " + pathEntry.getDependency().getVersion());
                }
                logger.debug("-----------------------------------------------------------");
                for(int idx=0; idx < path.size() - 1; idx++) {
                    GoDependencyNode parentDependency = path.get(idx);
                    GoDependencyNode childDependency = path.get(idx + 1);
                    graph.addChildWithParent(childDependency.getDependency(), parentDependency.getDependency());
                    logger.debug("Mapped {} as child of {}", childDependency.getDependency().toString(), parentDependency.getDependency().toString());
                }
            } else {
                logger.warn("No path found for indirect dependency: {}", dependency.toString());
            }
        }
        
        return graph;
    }

    public List<GoDependencyNode> GetDependencyPathFromGraph(GoDependencyNode targetNode, GoDependencyNode graph, List<GoDependencyNode> visited) {
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
                List<GoDependencyNode> childPath = GetDependencyPathFromGraph(targetNode, child, visited);
                if (childPath.isEmpty()) {
                    visited.remove(visited.size() - 1);
                }
                if (!childPath.isEmpty()) {
                    return childPath;
                }
            }
        }
        return new ArrayList<>(); // Return empty list if not found
    }
}
