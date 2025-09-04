package com.blackduck.integration.detectable.detectables.go.gomodfile.parse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoModFileContent;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoModuleInfo;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoReplaceDirective;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Resolves dependencies by applying exclude and replace directives.
 * This class processes the raw go.mod content and produces the final dependency graph
 * that respects all the directives.
 */
public class GoModDependencyResolver {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Resolves dependencies by applying exclude and replace directives.
     * 
     * @param goModContent The parsed go.mod file content
     * @return ResolvedDependencies containing the final dependency lists
     */
    public ResolvedDependencies resolveDependencies(GoModFileContent goModContent) {
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
        
        return new ResolvedDependencies(finalDirectDependencies, finalIndirectDependencies);
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
        
        Set<String> excludedModuleNames = excludedModules.stream()
                .map(GoModuleInfo::getName)
                .collect(Collectors.toSet());
        
        return dependencies.stream()
                .filter(dependency -> {
                    String dependencyKey = createModuleKey(dependency);
                    String moduleName = dependency.getName();
                    
                    // Check exact match first
                    if (excludedKeys.contains(dependencyKey)) {
                        logger.debug("Excluding dependency {} (exact match)", dependency);
                        return false;
                    }
                    
                    // Check module name only match
                    if (excludedModuleNames.contains(moduleName)) {
                        logger.debug("Excluding dependency {} (module name match)", dependency);
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
                .map(retracted -> retracted.getVersion())
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
        
        public ResolvedDependencies(List<GoModuleInfo> directDependencies, List<GoModuleInfo> indirectDependencies) {
            this.directDependencies = directDependencies;
            this.indirectDependencies = indirectDependencies;
        }
        
        public List<GoModuleInfo> getDirectDependencies() {
            return directDependencies;
        }
        
        public List<GoModuleInfo> getIndirectDependencies() {
            return indirectDependencies;
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
                    '}';
        }
    }
}
