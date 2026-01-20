package com.blackduck.integration.detectable.detectables.cargo;

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
            workspaceMembers = cargoTomlParser.parseAllWorkspaceMembers(cargoTomlContents, workspaceRoot);
        }

        EnumListFilter<CargoDependencyType> dependencyTypeFilter = Optional.ofNullable(cargoDetectableOptions.getDependencyTypeFilter())
            .orElse(EnumListFilter.excludeNone());

        List<String> fullTreeOutput = runCargoTreeCommand(directory, cargoExe, cargoDetectableOptions, dependencyTypeFilter);

        List<CodeLocation> codeLocations = cargoDependencyTransformer.transform(fullTreeOutput, workspaceMembers);

        return new Extraction.Builder()
            .success(codeLocations)
            .nameVersionIfPresent(projectNameVersion)
            .build();
    }

    private List<String> runCargoTreeCommand(
        File directory,
        ExecutableTarget cargoExe,
        CargoDetectableOptions cargoDetectableOptions,
        EnumListFilter<CargoDependencyType> dependencyTypeFilter
    ) throws ExecutableFailedException {
        List<String> command = new LinkedList<>(CARGO_TREE_COMMAND);

        boolean shouldIgnoreAllWorkspaceMembers = cargoDetectableOptions.getCargoIgnoreAllWorkspacesMode();

        // Add workspace flags only if not ignoring all workspaces
        if (!shouldIgnoreAllWorkspaceMembers) {
            addWorkspaceFlags(command, cargoDetectableOptions);
        }

        // Add features flags if required
        addFeatureFlags(command, cargoDetectableOptions);

        // Always add edge exclusions if needed
        if (!dependencyTypeFilter.shouldIncludeAll()) {
            addEdgeExclusions(command, cargoDetectableOptions);
        }

        return runCargoTreeCommand(directory, cargoExe, command);
    }

    private List<String> runCargoTreeCommand(File directory, ExecutableTarget cargoExe, List<String> commandArgs) throws ExecutableFailedException {
        ExecutableOutput output = executableRunner.executeSuccessfully(
            ExecutableUtils.createFromTarget(directory, cargoExe, commandArgs)
        );
        return output.getStandardOutputAsList();
    }

    private void addWorkspaceFlags(List<String> command, CargoDetectableOptions options) {
        List<String> includedWorkspaces = options.getIncludedWorkspaces();
        List<String> excludedWorkspaces = options.getExcludedWorkspaces();

        boolean hasInclusions = includedWorkspaces != null && !includedWorkspaces.isEmpty();
        boolean hasExclusions = excludedWorkspaces != null && !excludedWorkspaces.isEmpty();

        // Add --workspace flag when:
        // 1. No filters specified (default: scan entire workspace)
        if (!hasInclusions && !hasExclusions) {
            command.add("--workspace");
            return;
        }

        // Add --workspace flag in two cases:
        // 1. When no specific packages are included (default: scan entire workspace)
        // 2. When exclusions are present (cargo requires --workspace with --exclude)
        if (!hasInclusions || hasExclusions) {
            command.add("--workspace");
        }

        // Add --package flags for each explicitly included workspace member
        // Note: Can be used alone (without --workspace) when no exclusions are present
        if (hasInclusions) {
            for (String includeWorkspace : includedWorkspaces) {
                command.add("--package");
                command.add(includeWorkspace);
            }
        }

        // Add --exclude flags for workspace members to skip
        // Note: Must be used with --workspace flag (enforced by cargo CLI)
        if (hasExclusions) {
            for (String excludedWorkspace : excludedWorkspaces) {
                command.add("--exclude");
                command.add(excludedWorkspace);
            }
        }
    }

    private void addFeatureFlags(List<String> command, CargoDetectableOptions options) {
        List<String> features = options.getIncludedFeatures();

        // Check if user wants all features (support "ALL" keyword or empty list meaning all)
        if (features != null && !features.isEmpty()) {
            // Check for special "ALL" keyword (case-insensitive)
            boolean includeAllFeatures = features.stream()
                .anyMatch(feature -> "ALL".equalsIgnoreCase(feature.trim()));

            if (includeAllFeatures) {
                command.add("--all-features");
            } else {
                // Add specific features
                command.add("--features");
                command.add(String.join(",", features));
            }
        }
        // If features is null or empty, use cargo's default behavior (no feature flags)
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
