package com.blackduck.integration.detectable.detectables.go.gomod;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.JsonSyntaxException;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.detectables.go.gomod.model.GoGraphRelationship;
import com.blackduck.integration.detectable.detectables.go.gomod.model.GoListAllData;
import com.blackduck.integration.detectable.detectables.go.gomod.model.GoListModule;
import com.blackduck.integration.detectable.detectables.go.gomod.parse.GoGraphParser;
import com.blackduck.integration.detectable.detectables.go.gomod.parse.GoListParser;
import com.blackduck.integration.detectable.detectables.go.gomod.parse.GoModWhyParser;
import com.blackduck.integration.detectable.detectables.go.gomod.parse.GoVersionParser;
import com.blackduck.integration.detectable.detectables.go.gomod.parse.GoModuleDependencyHelper;
import com.blackduck.integration.detectable.detectables.go.gomod.process.GoModDependencyManager;
import com.blackduck.integration.detectable.detectables.go.gomod.process.GoModGraphGenerator;
import com.blackduck.integration.detectable.detectables.go.gomod.process.GoRelationshipManager;
import com.blackduck.integration.detectable.extraction.Extraction;

public class GoModCliExtractor {
    private final GoModCommandRunner goModCommandRunner;
    private final GoListParser goListParser;
    private final GoGraphParser goGraphParser;
    private final GoModWhyParser goModWhyParser;
    private final GoVersionParser goVersionParser;
    private final GoModGraphGenerator goModGraphGenerator;
    private final ExternalIdFactory externalIdFactory;
    private final GoModDependencyType excludedDependencyType;

    public GoModCliExtractor(
        GoModCommandRunner goModCommandRunner,
        GoListParser goListParser,
        GoGraphParser goGraphParser,
        GoModWhyParser goModWhyParser,
        GoVersionParser goVersionParser,
        GoModGraphGenerator goModGraphGenerator,
        ExternalIdFactory externalIdFactory,
        GoModDependencyType excludedDependencyType
    ) {
        this.goModCommandRunner = goModCommandRunner;
        this.goListParser = goListParser;
        this.goGraphParser = goGraphParser;
        this.goModWhyParser = goModWhyParser;
        this.goVersionParser = goVersionParser;
        this.goModGraphGenerator = goModGraphGenerator;
        this.externalIdFactory = externalIdFactory;
        this.excludedDependencyType = excludedDependencyType;
    }

    public Extraction extract(File directory, ExecutableTarget goExe) throws ExecutableFailedException, JsonSyntaxException, DetectableException {
        GoVersion goVersion = goVersion(directory, goExe);
        List<GoListModule> goListModules = listModules(directory, goExe); // only ever prints one module, the current module? Is this true even in "multi-module" projects? If they exist?
        List<GoListAllData> goListAllModules = listAllModules(directory, goExe, goVersion);
        List<GoGraphRelationship> goGraphRelationships = listAndCleanGraphRelationships(directory, goExe, goVersion, goListAllModules); // xxxxx****x*x*where we modify the go mod graph output
        Set<String> excludedModules = listExcludedModules(directory, goExe);

        GoRelationshipManager goRelationshipManager = new GoRelationshipManager(goGraphRelationships, excludedModules); // at this point the relationship mapping is correct now, 170 and 181 are independent.

        System.out.println("-->>");
        List something = goListAllModules.stream().filter(listmod -> listmod.getPath().contains("viper")).collect(Collectors.toList());

        GoModDependencyManager goModDependencyManager = new GoModDependencyManager(goListAllModules, externalIdFactory); // goListAllModules contains only required deps.
        List<CodeLocation> codeLocations = goListModules.stream() // goListModules corresponds to output of "go list -m -json" which should just print the main module, but iterating over stream suggests there could be more? need an example. looks like for each one we create a new code location
            .map(goListModule -> goModGraphGenerator.generateGraph(goListModule, goRelationshipManager, goModDependencyManager))
            .collect(Collectors.toList());

        // No project info - hoping git can help with that.
        return new Extraction.Builder().success(codeLocations).build();
    }

    private List<GoListModule> listModules(File directory, ExecutableTarget goExe) throws ExecutableFailedException, JsonSyntaxException {
        List<String> listOutput = goModCommandRunner.runGoList(directory, goExe);
        return goListParser.parseGoListModuleJsonOutput(listOutput);
    }

    private List<GoListAllData> listAllModules(File directory, ExecutableTarget goExe, GoVersion goVersion) throws ExecutableFailedException, JsonSyntaxException, DetectableException {
        List<String> listAllOutput = goModCommandRunner.runGoListAll(directory, goExe, goVersion);
        return goListParser.parseGoListAllJsonOutput(listAllOutput);
    }

    private List<GoGraphRelationship> listAndCleanGraphRelationships(File directory, ExecutableTarget goExe, GoVersion goVersion, List<GoListAllData> allRequiredModules) throws ExecutableFailedException {
        List<String> modGraphOutput = goModCommandRunner.runGoModGraph(directory, goExe);

        // Get the actual main module that produced this graph
        String mainMod = goModCommandRunner.runGoModGetMainModule(directory, goExe, goVersion); // Regardless of # of modules, each go mod execution targets one module (corresponding to a go.mod file directory)

        // Get the list of TRUE direct dependencies, then use the main mod name and
        // this list to create a TRUE dependency graph from the requirement graph
        List<String> directs = goModCommandRunner.runGoModDirectDeps(directory, goExe, goVersion); // its somehow failing to do this correctly
        List<String> modWhyOutput = goModCommandRunner.runGoModWhy(directory, goExe, false);
        
        GoModuleDependencyHelper goModDependencyHelper = new GoModuleDependencyHelper();
        Set<String> actualDependencyList = goModDependencyHelper.computeDependencies(mainMod, directs, modWhyOutput, modGraphOutput, allRequiredModules); // **********

        System.out.println("-->>");
        actualDependencyList.stream().filter(s -> s.contains("benbjohnson")).forEach(System.out::println);

        return goGraphParser.parseRelationshipsFromGoModGraph(actualDependencyList);
    }

    private GoVersion goVersion(File directory, ExecutableTarget goExe) throws ExecutableFailedException, DetectableException {
        String goVersionLine = goModCommandRunner.runGoVersion(directory, goExe);
        return goVersionParser.parseGoVersion(goVersionLine)
            .orElseThrow(() -> new DetectableException(String.format("Failed to find go version within output: %s", goVersionLine)));
    }

    private Set<String> listExcludedModules(File directory, ExecutableTarget goExe) throws ExecutableFailedException {
        List<String> modWhyOutput;
        if (excludedDependencyType.equals(GoModDependencyType.VENDORED)) {
            modWhyOutput = goModCommandRunner.runGoModWhy(directory, goExe, true);
        } else if (excludedDependencyType.equals(GoModDependencyType.UNUSED)) {
            modWhyOutput = goModCommandRunner.runGoModWhy(directory, goExe, false);
        } else {
            return Collections.emptySet();
        }
        return goModWhyParser.createModuleExclusionList(modWhyOutput);
    }

}
