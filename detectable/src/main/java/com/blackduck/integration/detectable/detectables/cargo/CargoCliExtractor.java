package com.blackduck.integration.detectable.detectables.cargo;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.cargo.parse.CargoTomlParser;
import com.blackduck.integration.detectable.detectables.cargo.transform.CargoDependencyGraphTransformer;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.executable.ExecutableOutput;
import com.blackduck.integration.util.NameVersion;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.EnumMap;
import java.util.Set;

public class CargoCliExtractor {
    private static final List<String> CARGO_TREE_COMMAND = Arrays.asList("tree", "--prefix", "depth");
    private final DetectableExecutableRunner executableRunner;
    private final CargoDependencyGraphTransformer cargoDependencyTransformer;
    private final CargoTomlParser cargoTomlParser;

    public CargoCliExtractor(DetectableExecutableRunner executableRunner, CargoDependencyGraphTransformer cargoDependencyTransformer, CargoTomlParser cargoTomlParser) {
        this.executableRunner = executableRunner;
        this.cargoDependencyTransformer = cargoDependencyTransformer;
        this.cargoTomlParser = cargoTomlParser;
    }

    public Extraction extract(File directory, ExecutableTarget cargoExe, File cargoTomlFile, CargoDetectableOptions cargoDetectableOptions) throws ExecutableFailedException, IOException {
        Optional<NameVersion> projectNameVersion = Optional.empty();
        Set<String> workspaceMembers = new HashSet<>();

        if (cargoTomlFile != null) {
            File workspaceRoot = cargoTomlFile.getParentFile();
            String cargoTomlContents = FileUtils.readFileToString(cargoTomlFile, StandardCharsets.UTF_8);
            projectNameVersion = cargoTomlParser.parseNameVersionFromCargoToml(cargoTomlContents);
            workspaceMembers = cargoTomlParser.parseWorkspaceMembers(cargoTomlContents, workspaceRoot);
        }

        EnumListFilter<CargoDependencyType> dependencyTypeFilter = Optional.ofNullable(cargoDetectableOptions.getDependencyTypeFilter())
            .orElse(EnumListFilter.excludeNone());

        List<String> fullTreeOutput = runCargoTreeCommands(directory, cargoExe, cargoDetectableOptions, dependencyTypeFilter);

        List<CodeLocation> codeLocations = cargoDependencyTransformer.transform(fullTreeOutput, workspaceMembers);

        return new Extraction.Builder()
            .success(codeLocations)
            .nameVersionIfPresent(projectNameVersion)
            .build();
    }

    private List<String> runCargoTreeCommands(
        File directory,
        ExecutableTarget cargoExe,
        CargoDetectableOptions cargoDetectableOptions,
        EnumListFilter<CargoDependencyType> dependencyTypeFilter
    ) throws ExecutableFailedException {
        List<String> fullTreeOutput = new ArrayList<>();

        boolean ignoreAllWorkspaceMembers = cargoDetectableOptions.getCargoIgnoreAllWorkspacesMode();
        List<String> includedWorkspaces = cargoDetectableOptions.getIncludedWorkspaces();
        List<String> excludedWorkspaces = cargoDetectableOptions.getExcludedWorkspaces();
        List<String> effectiveInclusions = getEffectiveInclusions(includedWorkspaces, excludedWorkspaces);

        // Step 1: Run root workspace command (if not ignoring all workspaces)
        if (!ignoreAllWorkspaceMembers && !effectiveInclusions.isEmpty()) {
            List<String> rootCommand = new LinkedList<>(CARGO_TREE_COMMAND);
            if (!dependencyTypeFilter.shouldIncludeAll()) {
                addEdgeExclusions(rootCommand, cargoDetectableOptions);
            }
            List<String> rootOutput = runCargoTreeCommand(directory, cargoExe, rootCommand);
            fullTreeOutput.addAll(rootOutput);
            fullTreeOutput.add(""); // Add separator between workspaces
        }

        // Step 2: Run workspace-specific command
        List<String> workspaceCommand = new LinkedList<>(CARGO_TREE_COMMAND);
        addWorkspaceFlags(workspaceCommand, cargoDetectableOptions);
        if (!dependencyTypeFilter.shouldIncludeAll()) {
            addEdgeExclusions(workspaceCommand, cargoDetectableOptions);
        }
        List<String> workspaceOutput = runCargoTreeCommand(directory, cargoExe, workspaceCommand);
        fullTreeOutput.addAll(workspaceOutput);

        return fullTreeOutput;
    }

    private List<String> runCargoTreeCommand(File directory, ExecutableTarget cargoExe, List<String> commandArgs) throws ExecutableFailedException {
        ExecutableOutput output = executableRunner.executeSuccessfully(
            ExecutableUtils.createFromTarget(directory, cargoExe, commandArgs)
        );
        return output.getStandardOutputAsList();
    }

    private void addWorkspaceFlags(List<String> command, CargoDetectableOptions options) {
        boolean ignoreAllWorkspaceMembers = options.getCargoIgnoreAllWorkspacesMode();
        List<String> includedWorkspaces = options.getIncludedWorkspaces();
        List<String> excludedWorkspaces = options.getExcludedWorkspaces();

        // Exclusion dominates inclusion: filter out included workspaces that are also excluded
        List<String> effectiveInclusions = getEffectiveInclusions(includedWorkspaces, excludedWorkspaces);

        // Workspace flag logic (exclusion rules take precedence over inclusion):
        // 1. If specific workspaces are included (after exclusion filtering): use --package flags only
        // 2. If only exclusions exist: use --workspace with --exclude flags to include all except specified
        // 3. If neither inclusions nor exclusions: use --workspace to include all workspace members
        // Note: All cases skip workspace flags entirely when ignoreAllWorkspaceMembers is true
        if (!effectiveInclusions.isEmpty()) {
            for (String includeWorkspace : effectiveInclusions) {
                command.add("--package");
                command.add(includeWorkspace);
            }
        }  else if (excludedWorkspaces != null && !excludedWorkspaces.isEmpty() && !ignoreAllWorkspaceMembers) {
            command.add("--workspace");
            for (String excludedWorkspace : excludedWorkspaces) {
                command.add("--exclude");
                command.add(excludedWorkspace);
            }
        } else if (!ignoreAllWorkspaceMembers) {
            command.add("--workspace");
        }
    }

    private List<String> getEffectiveInclusions(List<String> included, List<String> excluded) {
        if (included == null || included.isEmpty()) {
            return new ArrayList<>();
        }

        if (excluded == null || excluded.isEmpty()) {
            return new LinkedList<>(included);
        }

        return included.stream()
            .filter(workspace -> !excluded.contains(workspace))
            .collect(java.util.stream.Collectors.toList());
    }

    private void addEdgeExclusions(List<String> cargoTreeCommand, CargoDetectableOptions options) {
        Map<CargoDependencyType, String> exclusionMap = new EnumMap<>(CargoDependencyType.class);
        exclusionMap.put(CargoDependencyType.NORMAL, "no-normal");
        exclusionMap.put(CargoDependencyType.BUILD, "no-build");
        exclusionMap.put(CargoDependencyType.DEV, "no-dev");
        exclusionMap.put(CargoDependencyType.PROC_MACRO, "no-proc-macro");

        List<String> exclusions = new LinkedList<>();
        for (Map.Entry<CargoDependencyType, String> entry : exclusionMap.entrySet()) {
            if (options.getDependencyTypeFilter().shouldExclude(entry.getKey())) {
                exclusions.add(entry.getValue());
            }
        }

        if (!exclusions.isEmpty()) {
            cargoTreeCommand.add("--edges");
            cargoTreeCommand.add(String.join(",", exclusions));
        }
    }
}
