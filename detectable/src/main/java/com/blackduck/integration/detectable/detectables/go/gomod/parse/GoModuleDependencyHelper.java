package com.blackduck.integration.detectable.detectables.go.gomod.parse;

import java.util.*;
import java.util.stream.Collectors;

import com.blackduck.integration.detectable.detectables.go.gomod.model.GoListAllData;
import com.blackduck.integration.detectable.detectables.go.gomod.process.WhyListStructureTransform;


public class GoModuleDependencyHelper {
    
    private final WhyListStructureTransform whyListStructureTransform;
    private final Map<String, String> allRequiredModulesPathsAndVersions = new HashMap<>();
 
    public GoModuleDependencyHelper(List<GoListAllData> allRequiredModulesData) {
        this.whyListStructureTransform = new WhyListStructureTransform();
        processRequiredModulesIntoPathAndPathWithVersionMap(allRequiredModulesData);
    }

    /**
     * Takes the output of go mod graph, and for each line of the form "moduleA moduleB", cleans it up such that the
     * following modifications are made where applicable using the output of go mod why:
     * "main_module indirect_module" ->  "true_parent_module indirect_module"
     * "parent_but_unrequired_module required_moduleA" -> "parent_and_required_module required_moduleA"
     * Where a required module refers to a module name and version that was actually resolved for the build.
     * Ultimately this method takes in the requirements graph and outputs a dependency graph.
     * @param main - The string name of the main go module
     * @param directs - The obtained list of the main module's direct dependencies.
     * @param modWhyOutput - A list of all modules with their relationship to the main module
     * @param originalModGraphOutput - The list produced by "go mod graph"- the requirements graph
     * @return - the go mod why output cleaned up (duplicates removed + relationships corrected where applicable)
     */
    public Set<String> computeDependencies(String main, List<String> directs, List<String> modWhyOutput, List<String> originalModGraphOutput) {
        Set<String> goModGraph = new HashSet<>();
        Map<String, List<String>> whyMap = whyListStructureTransform.convertWhyListToWhyMap(modWhyOutput);
        // Add all true direct dependencies to correctedDependencies to begin with so we avoid unnecessary work
        Set<String> correctedDependencies = directs.stream().map(directModuleNameAndVersion -> directModuleNameAndVersion.split("@")[0]).collect(Collectors.toSet());

        for (String grphLine : originalModGraphOutput) {
            // Splitting here allows matching with less effort
            String[] splitLine = grphLine.split(" ");

            if (splitLine.length != 2) {
                continue;
            }

            if(splitLine[1].startsWith("go@") || splitLine[1].startsWith("toolchain@go")) {
                continue;
            }

            boolean needsRedux = needsRedux(directs, splitLine[0], splitLine[1], main);

            if (needsRedux) {
                /* Redo the line to establish the appropriate parent for the indirect module before we begin graph building */
                grphLine = this.getProperParentage(grphLine, splitLine, whyMap, correctedDependencies);
            }
            goModGraph.add(grphLine);
        }
        return goModGraph;
    }

    /**
     * @param directs
     * @param parent
     * @param child
     * @param main
     * @return true for requirements graph entries of the form:
     * "main_module indirect_module" or "unrequired_module required_module"
     */
    private boolean needsRedux(List<String> directs, String parent, String child, String main) {
        if ( (!isDirect(directs, child) && parent.equalsIgnoreCase(main)) || (isRequired(child) && !isRequired(parent)) )
            return true;
        else
            return false;
    }

    private boolean isRequired(String childPathWithVersion){
        return allRequiredModulesPathsAndVersions.containsValue(childPathWithVersion);
    }

    private boolean isDirect(List<String> directs, String modulePathWithVersion) {
        return directs.contains(modulePathWithVersion);
    }

    
    private boolean hasDependency(List<String> correctedDependencies, String splitLinePart){
        for (String adep : correctedDependencies) {
            if (splitLinePart.startsWith(adep)) {
                return true;
            }
        }
        return false;
    }

    private String getProperParentage(String grphLine, String[] splitLine, Map<String, List<String>> whyMap, Set<String> correctedDependencies) {
        String childModulePath = splitLine[1].replaceAll("@.*", "");
        // If we have already found the correct parent for this module, don't try to find it again
        if (!correctedDependencies.add(childModulePath)) return grphLine;


        // look up the 'why' results for the module...  This will tell us
        // the (directly or indirectly) required dependency item that pulled this item into the mix.
        List<String> trackPath = whyMap.get(childModulePath);
        if (trackPath != null && !trackPath.isEmpty() && !indicatesUnusedModule(trackPath)) {
            for (int i = trackPath.size() - 2; i >= 0 ; i--) {
                String tp = trackPath.get(i);
                String parentPath = allRequiredModulesPathsAndVersions.keySet().stream()
                        .filter(requiredMod -> (tp.contains(requiredMod)))
                        .findFirst()
                        .orElse(null);
                String parentWithVersion = allRequiredModulesPathsAndVersions.get(parentPath);
                if (parentWithVersion != null) { // if real direct is found... otherwise do nothing
                    grphLine = grphLine.replace(splitLine[0], parentWithVersion);
                    break;
                }
            }
        }

        return grphLine;
    }

    private boolean indicatesUnusedModule(List<String> trackPath) {
        return Arrays.stream(GoModWhyParser.UNUSED_MODULE_PREFIXES).anyMatch(trackPath.get(0)::contains);
    }

    private void processRequiredModulesIntoPathAndPathWithVersionMap(List<GoListAllData> allRequiredModules) {
        for (GoListAllData module : allRequiredModules) {
            String path = module.getPath();
            String version = module.getVersion();
            if (version == null) {
                allRequiredModulesPathsAndVersions.putIfAbsent(path, path);
            } else {
                allRequiredModulesPathsAndVersions.putIfAbsent(path, path + "@" + version);
            }
        }
    }
}
