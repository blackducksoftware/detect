package com.blackduck.integration.detectable.detectables.go.gomodfile.parse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectables.go.gomodfile.GoModFileDetectableOptions;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoDependencyNode;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoModFileContent;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoModFileHelpers;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoModuleInfo;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoProxyModuleResolver;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoReplaceDirective;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves dependencies by applying exclude and replace directives.
 * This class processes the raw go.mod content and produces the final dependency graph
 * that respects all the directives.
 */
public class GoModDependencyResolver {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final GoProxyModuleResolver goProxyModuleResolver;
    private final GoModFileParser goModFileParser = new GoModFileParser();
    private GoModFileHelpers goModFileHelpers;

    public GoModDependencyResolver(GoModFileDetectableOptions options) {
        this.goProxyModuleResolver = new GoProxyModuleResolver(options);
    }

    private Map<GoDependencyNode, List<GoDependencyNode>> visitedNodes = new HashMap<>();

    /**
     * Resolves dependencies by applying exclude and replace directives.
     * Additionally, this method now builds a recursive dependency graph by fetching
     * and parsing go.mod files for each direct dependency using GoProxyModuleResolver.
     * 
     * The recursive resolution process:
     * 1. Applies all directives (replace, exclude, retract) to the current go.mod
     * 2. For each direct dependency, fetches its go.mod file from proxy.golang.org
     * 3. Recursively parses and resolves dependencies for each fetched go.mod file
     * 4. Builds a complete dependency tree with parent-child relationships
     * 5. Prevents infinite recursion by tracking visited modules
     * 
     * @param goModContent The parsed go.mod file content
     * @return ResolvedDependencies containing the final dependency lists and recursive graph
     */
    public ResolvedDependencies resolveDependencies(GoModFileContent goModContent, ExternalIdFactory externalIdFactory) {
        // Start with all dependencies (direct and indirect)
        List<GoModuleInfo> allDependencies = new ArrayList<>();
        allDependencies.addAll(goModContent.getDirectDependencies());
        allDependencies.addAll(goModContent.getIndirectDependencies());
        
        // Apply replace directives first
        List<GoModuleInfo> replacedDependencies = applyReplaceDirectives(allDependencies, goModContent.getReplaceDirectives());
        
        // Apply exclude directives
        List<GoModuleInfo> finalDependencies = applyExcludeDirectives(replacedDependencies, goModContent.getExcludedModules());
        
        // Filter retracted versions
        finalDependencies = filterRetractedVersions(finalDependencies, goModContent.getRetractedVersions());
        
        // Separate back into direct and indirect
        List<GoModuleInfo> finalDirectDependencies = finalDependencies.stream()
                .filter(dep -> !dep.isIndirect())
                .collect(Collectors.toList());
        
        List<GoModuleInfo> finalIndirectDependencies = finalDependencies.stream()
                .filter(GoModuleInfo::isIndirect)
                .collect(Collectors.toList());
        
        goModFileHelpers = new GoModFileHelpers(externalIdFactory);
        
        ExternalId parentModuleExternalId = null;
        Dependency parentModuleDependency = null;

        // Create a dependency with parent module name and add it to root. This to to map transitive dependencies to the root module
        if (goModContent.getModuleName() != null) {
            parentModuleDependency = new Dependency(
                goModContent.getModuleName(),
                null,
                parentModuleExternalId,
                null
            );
        }

        GoDependencyNode rootNode = new GoDependencyNode(true, parentModuleDependency, new ArrayList<>());
        for (GoModuleInfo directDep : finalDirectDependencies) {
            GoDependencyNode childNode = new GoDependencyNode(false, goModFileHelpers.createDependency(directDep), new ArrayList<>());
            rootNode.addChild(childNode);
        }

        List<GoDependencyNode> rootTransitives = new ArrayList<>();
        for (GoModuleInfo indirectDep : finalIndirectDependencies) {
            GoDependencyNode childNode = new GoDependencyNode(false, goModFileHelpers.createDependency(indirectDep), new ArrayList<>());
            rootTransitives.add(childNode);
        }

        // Check connectivity to Go proxy
        if (!goProxyModuleResolver.checkConnectivity()) {
            logger.warn("Cannot connect to Go proxy at {}. Skipping recursive dependency resolution.", goProxyModuleResolver.options.getGoProxyUrl());
            return new ResolvedDependencies(finalDirectDependencies, finalIndirectDependencies, rootNode);
        }
        // Build a recursive dependency graph
        GoDependencyNode recursiveGraphNode = computeDependencyTree(rootNode, rootTransitives, externalIdFactory);

        return new ResolvedDependencies(finalDirectDependencies, finalIndirectDependencies, recursiveGraphNode);
    }

    private ResolvedDependencies parseGoModFile(GoModFileContent goModContent) {
        // Start with all dependencies (direct and indirect)
        List<GoModuleInfo> allDependencies = new ArrayList<>();
        allDependencies.addAll(goModContent.getDirectDependencies());
        allDependencies.addAll(goModContent.getIndirectDependencies());
        
        // Apply replace directives first
        List<GoModuleInfo> replacedDependencies = applyReplaceDirectives(allDependencies, goModContent.getReplaceDirectives());
        
        // Apply exclude directives
        List<GoModuleInfo> finalDependencies = applyExcludeDirectives(replacedDependencies, goModContent.getExcludedModules());
        
        // Filter retracted versions
        finalDependencies = filterRetractedVersions(finalDependencies, goModContent.getRetractedVersions());
        
        // Separate back into direct and indirect
        List<GoModuleInfo> finalDirectDependencies = finalDependencies.stream()
                .filter(dep -> !dep.isIndirect())
                .collect(Collectors.toList());
        
        List<GoModuleInfo> finalIndirectDependencies = finalDependencies.stream()
                .filter(GoModuleInfo::isIndirect)
                .collect(Collectors.toList());
        return new ResolvedDependencies(finalDirectDependencies, finalIndirectDependencies, null);
    }

    private GoDependencyNode computeDependencyTree(GoDependencyNode node, List<GoDependencyNode> rootTransitives, ExternalIdFactory externalIdFactory) {
        if (!node.isRootNode()) {
            processNonRootNode(node);
        }

        processChildNodes(node, rootTransitives, externalIdFactory);
        return node;
    }

    private void processNonRootNode(GoDependencyNode node) {
        if (visitedNodes.containsKey(node)) {
            node.appendChildren(visitedNodes.get(node));
            return;
        }

        List<GoDependencyNode> children = fetchAndParseChildren(node);
        visitedNodes.put(node, children);
        node.appendChildren(children);
    }

    private List<GoDependencyNode> fetchAndParseChildren(GoDependencyNode node) {
        String goModFileContent = goProxyModuleResolver.getGoModFileOfTheDependency(node.getDependency());
        if (goModFileContent == null) {
            return new ArrayList<>();
        }

        GoModFileContent childGoModContent = goModFileParser.parseGoModFile(goModFileContent);
        ResolvedDependencies childResolvedDeps = parseGoModFile(childGoModContent);
        
        List<GoDependencyNode> children = new ArrayList<>();
        for (GoModuleInfo childInfo : childResolvedDeps.getDirectDependencies()) {
            Dependency childDep = goModFileHelpers.createDependency(childInfo);
            GoDependencyNode childNode = new GoDependencyNode(false, childDep, new ArrayList<>());
            children.add(childNode);
        }
        return children;
    }

    private void processChildNodes(GoDependencyNode node, List<GoDependencyNode> rootTransitives, ExternalIdFactory externalIdFactory) {
        for (GoDependencyNode child : node.getChildren()) {
            if (shouldProcessChild(node, child, rootTransitives)) {
                computeDependencyTree(child, rootTransitives, externalIdFactory);
            }
        }
    }

    private boolean shouldProcessChild(GoDependencyNode node, GoDependencyNode child, List<GoDependencyNode> rootTransitives) {
        if (node.isRootNode()) {
            return true;
        }
        
        return rootTransitives.stream()
                .anyMatch(item -> child.getDependency().getName().equals(item.getDependency().getName()));
    }
    
    private List<GoModuleInfo> applyReplaceDirectives(List<GoModuleInfo> dependencies, List<GoReplaceDirective> replaceDirectives) {
        if (replaceDirectives.isEmpty()) {
            return new ArrayList<>(dependencies);
        }
        
        // Create a map for quick lookup of replacements
        Map<String, GoReplaceDirective> replacementMap = new HashMap<>();
        for (GoReplaceDirective directive : replaceDirectives) {
            String key = createModuleKey(directive.getOldModule());
            replacementMap.put(key, directive);
        }
        
        List<GoModuleInfo> replacedDependencies = new ArrayList<>();
        
        for (GoModuleInfo dependency : dependencies) {
            String dependencyKey = createModuleKey(dependency);
            
            if (replacementMap.containsKey(dependencyKey)) {
                GoReplaceDirective replacement = replacementMap.get(dependencyKey);
                GoModuleInfo newModule = replacement.getNewModule();
                
                // Create a new dependency with the replaced module info but preserve indirect flag
                GoModuleInfo replacedDependency = new GoModuleInfo(
                    newModule.getName(),
                    newModule.getVersion(),
                    dependency.isIndirect()
                );
                
                replacedDependencies.add(replacedDependency);
                logger.debug("Replaced dependency {} with {}", dependency, replacedDependency);
            } else {
                // Check for module-only replacement (without version matching)
                String moduleOnlyKey = dependency.getName();
                GoReplaceDirective moduleReplacement = findModuleOnlyReplacement(moduleOnlyKey, replaceDirectives);
                
                if (moduleReplacement != null) {
                    GoModuleInfo newModule = moduleReplacement.getNewModule();
                    GoModuleInfo replacedDependency = new GoModuleInfo(
                        newModule.getName(),
                        newModule.getVersion(),
                        dependency.isIndirect()
                    );
                    
                    replacedDependencies.add(replacedDependency);
                    logger.debug("Replaced dependency {} with {} (module-only replacement)", dependency, replacedDependency);
                } else {
                    replacedDependencies.add(dependency);
                }
            }
        }
        
        return replacedDependencies;
    }
    
    private GoReplaceDirective findModuleOnlyReplacement(String moduleName, List<GoReplaceDirective> replaceDirectives) {
        for (GoReplaceDirective directive : replaceDirectives) {
            if (directive.getOldModule().getName().equals(moduleName)) {
                return directive;
            }
        }
        return null;
    }
    
    private List<GoModuleInfo> applyExcludeDirectives(List<GoModuleInfo> dependencies, Set<GoModuleInfo> excludedModules) {
        if (excludedModules.isEmpty()) {
            return dependencies;
        }
        
        Set<String> excludedKeys = excludedModules.stream()
                .map(this::createModuleKey)
                .collect(Collectors.toSet());
        
        
        return dependencies.stream()
                .filter(dependency -> {
                    String dependencyKey = createModuleKey(dependency);
                    
                    // Check exact match first
                    if (excludedKeys.contains(dependencyKey)) {
                        logger.debug("Excluding dependency {} (exact match)", dependencyKey);
                        return false;
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
    }
    
    private List<GoModuleInfo> filterRetractedVersions(List<GoModuleInfo> dependencies, Set<GoModuleInfo> retractedVersions) {
        if (retractedVersions.isEmpty()) {
            return dependencies;
        }
        
        Set<String> retractedVersionKeys = retractedVersions.stream()
                .map(GoModuleInfo::getVersion)
                .collect(Collectors.toSet());
        
        return dependencies.stream()
                .filter(dependency -> {
                    if (retractedVersionKeys.contains(dependency.getVersion())) {
                        logger.debug("Filtering retracted version {} for module {}", 
                                    dependency.getVersion(), dependency.getName());
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }
    
    private String createModuleKey(GoModuleInfo module) {
        return module.getName() + "@" + module.getVersion();
    }
    
    /**
     * Represents the final resolved dependencies after applying all directives.
     */
    public static class ResolvedDependencies {
        private final List<GoModuleInfo> directDependencies;
        private final List<GoModuleInfo> indirectDependencies;
        private final GoDependencyNode dependencyGraph;
        
        public ResolvedDependencies(List<GoModuleInfo> directDependencies, List<GoModuleInfo> indirectDependencies) {
            this(directDependencies, indirectDependencies, null);
        }
        
        public ResolvedDependencies(List<GoModuleInfo> directDependencies, List<GoModuleInfo> indirectDependencies, GoDependencyNode dependencyGraph) {
            this.directDependencies = directDependencies;
            this.indirectDependencies = indirectDependencies;
            this.dependencyGraph = dependencyGraph;
        }
        
        public List<GoModuleInfo> getDirectDependencies() {
            return directDependencies;
        }
        
        public List<GoModuleInfo> getIndirectDependencies() {
            return indirectDependencies;
        }
        
        public GoDependencyNode getDependencyGraph() {
            return dependencyGraph;
        }
        
        public List<GoModuleInfo> getAllDependencies() {
            List<GoModuleInfo> all = new ArrayList<>();
            all.addAll(directDependencies);
            all.addAll(indirectDependencies);
            return all;
        }
        
        @Override
        public String toString() {
            return "ResolvedDependencies{" +
                    "directDependencies=" + directDependencies.size() +
                    ", indirectDependencies=" + indirectDependencies.size() +
                    ", hasRecursiveGraph=" + (dependencyGraph != null) +
                    '}';
        }
    }
}
