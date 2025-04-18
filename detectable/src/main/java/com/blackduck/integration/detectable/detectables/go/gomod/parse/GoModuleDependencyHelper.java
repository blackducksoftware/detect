package com.blackduck.integration.detectable.detectables.go.gomod.parse;

import java.util.*;

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
     * Takes a string that will be incorrectly computed to be a direct dependency and corrects it, such that a
     * true indirect dependency module will be associated with the required module that is its true parent.
     * Eg. "main_module_name indirect_module" ->  "required_parent_module indirect_module"  - this will convert the
     * requirements graph to a dependency graph. True direct dependencies will be left unchanged.
     * @param main - The string name of the main go module
     * @param directs - The obtained list of the main module's direct dependencies.
     * @param modWhyOutput - A list of all modules with their relationship to the main module
     * @param originalModGraphOutput - The list produced by "go mod graph"- the intended "target".
     * @return - the go mod why output cleaned up (duplicates removed + relationships corrected where applicable)
     */
    public Set<String> computeDependencies(String main, List<String> directs, List<String> modWhyOutput, List<String> originalModGraphOutput) {
        Set<String> goModGraph = new HashSet<>();
        List<String> correctedDependencies = new ArrayList<>();
        Map<String, List<String>> whyMap = whyListStructureTransform.convertWhyListToWhyMap(modWhyOutput);
        /* Correct lines that get mis-interpreted as a direct dependency, given the list of direct deps, requirements graph etc.*/
        for (String grphLine : originalModGraphOutput) {

            if (grphLine.equalsIgnoreCase("github.com/olekukonko/tablewriter@v0.0.0-20170122224234-a0225b3f23b5 github.com/mattn/go-runewidth@v0.0.7")) {
                System.out.println("weeeoooo");
            }
            // Splitting here allows matching with less effort
            String[] splitLine = grphLine.split(" ");
            // TODO check this is at least of size 2

            if(splitLine[1].startsWith("go@")) {
                continue;
            }

            boolean needsRedux = needsRedux(directs, splitLine[0], splitLine[1], main);

            if (needsRedux) {
                /* Redo the line to establish the direct reference module to this *indirect* module*/
                grphLine = this.getProperParentage(grphLine, splitLine, whyMap, correctedDependencies, main); // unnecessary work if we already found proper parent for req dep but now see (unreq -> req) so this is where we should call hasDependency() or just during the getParent method check we havent already corrected this one. Also add to correctedDependencies if its a direct module since that can appear again in the mod graph many times.
            }
            needsRedux(directs, splitLine[0], splitLine[1], main);
            goModGraph.add(grphLine);
        }
        return goModGraph;
    }


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

    private String getProperParentage(String grphLine, String[] splitLine, Map<String, List<String>> whyMap, List<String> correctedDependencies, String main) {
        String childModulePath = splitLine[1].replaceAll("@.*", "");
        correctedDependencies.add(childModulePath); // keep track of ones we've fixed.


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
