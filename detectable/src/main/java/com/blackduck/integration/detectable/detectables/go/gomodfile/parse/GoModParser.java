package com.blackduck.integration.detectable.detectables.go.gomodfile.parse;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoModFileContent;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoModuleInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public GoModParser(ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
        this.fileParser = new GoModFileParser();
        this.dependencyResolver = new GoModDependencyResolver();
    }

    /**
     * Parses a go.mod file and creates a dependency graph that respects all directives.
     * 
     * @param goModContents List of lines from the go.mod file
     * @return DependencyGraph with resolved dependencies
     */
    public DependencyGraph parseGoModFile(List<String> goModContents) {
        logger.debug("Parsing go.mod file with {} lines", goModContents.size());
        
        // Parse the raw go.mod content
        GoModFileContent goModContent = fileParser.parseGoModFile(goModContents);
        logger.info("Parsed go.mod: {}", goModContent);
        
        // Resolve dependencies by applying directives
        GoModDependencyResolver.ResolvedDependencies resolvedDependencies = 
            dependencyResolver.resolveDependencies(goModContent);
        logger.info("Resolved dependencies: {}", resolvedDependencies);
        
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
        ExternalId parentModuleExternalId = null;
        Dependency parentModuleDependency = null;

        // Create a dependency with parent module name and add it to root. This to to map transitive dependencies to the root module
        if (goModContent.getModuleName() != null) {
            parentModuleExternalId = externalIdFactory.createNameVersionExternalId(
                Forge.GOLANG, 
                goModContent.getModuleName(), 
                null
            );
            parentModuleDependency = new Dependency(
                goModContent.getModuleName(),
                null,
                parentModuleExternalId,
                null
            );
            graph.addDirectDependency(parentModuleDependency);
        }
        
        // Add direct dependencies
        for (GoModuleInfo directDep : resolvedDependencies.getDirectDependencies()) {
            Dependency dependency = createDependency(directDep);
            graph.addDirectDependency(dependency);
            logger.debug("Added direct dependency: {}", dependency.getName());
        }
        
        // Add indirect dependencies
        for (GoModuleInfo indirectDep : resolvedDependencies.getIndirectDependencies()) {
            Dependency dependency = createDependency(indirectDep);
            // Add dependency as a child to parentModuleDependency
            graph.addChildWithParent(dependency, parentModuleDependency);
            //graph.addDirectDependency(dependency); // Adding as direct for simplicity
            logger.debug("Added indirect dependency as child to root module: {}", dependency.getName());
        }
        
        logger.info("Created dependency graph with {} direct dependencies", 
                   graph.getDirectDependencies().size());
        
        return graph;
    }
    
    private Dependency createDependency(GoModuleInfo moduleInfo) {
        String cleanVersion = cleanVersionForExternalId(moduleInfo.getVersion());
        ExternalId externalId = externalIdFactory.createNameVersionExternalId(
            Forge.GOLANG, 
            moduleInfo.getName(), 
            cleanVersion
        );
        
        return new Dependency(
            moduleInfo.getName(), 
            cleanVersion, 
            externalId, 
            null
        );
    }
    
    private String cleanVersionForExternalId(String version) {
        if (version == null || version.isEmpty()) {
            return null;
        }
        
        // Remove any remaining incompatible markers
        version = version.replace("+incompatible", "").replace("%2Bincompatible", "");
        
        // Handle pseudo-versions and other Go-specific version formats
        if (version.contains("-")) {
            // For pseudo-versions like v0.0.0-20180917221912-90fa682c2a6e, 
            // extract just the hash part for better identification
            String[] parts = version.split("-");
            if (parts.length >= 3 && parts[parts.length - 1].length() >= 12) {
                // Take the last part if it looks like a git hash
                String lastPart = parts[parts.length - 1];
                if (lastPart.matches("[a-f0-9]{12,}")) {
                    return lastPart.substring(0, Math.min(12, lastPart.length()));
                }
            }
        }
        
        return version;
    }
}
