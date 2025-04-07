package com.blackduck.integration.detectable.detectables.go.gomod.parse;

import java.util.*;
import java.util.stream.Collectors;

import com.blackduck.integration.detectable.detectables.go.gomod.model.GoListAllData;
import com.blackduck.integration.detectable.detectables.go.gomod.process.WhyListStructureTransform;


public class GoModuleDependencyHelper {
    
    private final WhyListStructureTransform whyListStructureTransform;
 
    public GoModuleDependencyHelper() {
        this.whyListStructureTransform = new WhyListStructureTransform();
    }

    /**
     * Takes a string that will be incorrectly computed to be a direct dependency and corrects it, such that a
     * true indirect dependency module will be associated with the module that is the true direct dependency.  
     * Eg. "main_module_name indirect_module" ->  "direct_dependency_module indirect_module"  - this will convert the
     * requirements graph to a dependency graph. True direct dependencies will be left unchanged.
     * @param main - The string name of the main go module
     * @param directs - The obtained list of the main module's direct dependency.
     * @param modWhyOutput - A list of all modules with their relationship to the main module
     * @param originalModGraphOutput - The list produced by "go mod graph"- the intended "target".
     * @return - the actual dependency list
     */
    public Set<String> computeDependencies(String main, List<String> directs, List<String> modWhyOutput, List<String> originalModGraphOutput, List<GoListAllData> allRequiredModulesData) { // has no unit tests! TODO TODO TODO
        Set<String> goModGraph = new HashSet<>();
        List<String> correctedDependencies = new ArrayList<>();
        List<String> allRequiredModules = extractAllRequiredModulePathsWithVersions(allRequiredModulesData);
        Map<String, List<String>> whyMap = whyListStructureTransform.convertWhyListToWhyMap(modWhyOutput); // confirmed
        /* Correct lines that get mis-interpreted as a direct dependency, given the list of direct deps, requirements graph etc.*/
        for (String grphLine : originalModGraphOutput) {
            boolean containsDirect = containsDirectDependencies(directs, main, grphLine);
            
            // Splitting here allows matching with less effort
            String[] splitLine = grphLine.split(" ");

            if(splitLine[1].startsWith("go@")) {
                continue;
            }
            
            // anything that falls in here isn't a direct dependency of main
            boolean needsRedux = !containsDirect && splitLine[0].equals(main); // this is just repeating the work in containsDirect isnt it?
            
            /* This searches for instances where the main module is apparently referring to itself.  
            This can step on the indirect dependency making it seem to be direct.*/ // DEFINITELY affects 4581 or relates to it. some other module could depend on main@v1 for example TODO TODO TODO TODO
            if (splitLine[0].startsWith(main) && splitLine[0].contains("@")) {
                boolean gotoNext = hasDependency(correctedDependencies, splitLine[1]);
                if (gotoNext) {
                    continue;
                }
            }

            if (needsRedux) {
                /* Redo the line to establish the direct reference module to this *indirect* module*/
                grphLine = this.getProperParentage(grphLine, splitLine, whyMap, directs, correctedDependencies, allRequiredModules);
            }
            
            goModGraph.add(grphLine);
        }
        return goModGraph;
    }
    
    private boolean containsDirectDependencies(List<String> directs, String main, String grphLine) {
        return grphLine.startsWith(main) && directs.stream().anyMatch(grphLine::contains);
    }
    
    private boolean hasDependency(List<String> correctedDependencies, String splitLinePart){
        for (String adep : correctedDependencies) {
            if (splitLinePart.startsWith(adep)) {
                return true;
            }
        }
        return false;
    }

    private String getProperParentage(String grphLine, String[] splitLine, Map<String, List<String>> whyMap, List<String> directs, List<String> correctedDependencies, List<String> allRequiredModules) {
        String childModulePath = splitLine[1].replaceAll("@.*", "");
        correctedDependencies.add(childModulePath); // keep track of ones we've fixed.

        // look up the 'why' results for the module...  This will tell us
        // the (directly or indirectly) required dependency item that pulled this item into the mix.
        List<String> trackPath = whyMap.get(childModulePath);
        if (trackPath != null && !trackPath.isEmpty() && !indicatesUnusedModule(trackPath)) {
            for (int i = trackPath.size() - 2; i >= 0 ; i--) {
                String tp = trackPath.get(i);
                String parent = allRequiredModules.stream()
                        .filter(requiredMod -> (tp.equalsIgnoreCase(requiredMod.replaceAll("@.*",""))))
                        .findFirst()
                        .orElse(null);
                if (parent != null) { // if real direct is found... otherwise do nothing
                    grphLine = grphLine.replace(splitLine[0], parent);
                    break;
                }
            }
        }
        return grphLine;
    }

    private boolean indicatesUnusedModule(List<String> trackPath) {
        return Arrays.stream(GoModWhyParser.UNUSED_MODULE_PREFIXES).anyMatch(trackPath.get(0)::contains);
    }

    private List<String> extractAllRequiredModulePathsWithVersions(List<GoListAllData> allRequiredModules) {
        return allRequiredModules.stream()
                .map(module -> module.getPath() + "@" + module.getVersion())
                .collect(Collectors.toList());
    }
}
