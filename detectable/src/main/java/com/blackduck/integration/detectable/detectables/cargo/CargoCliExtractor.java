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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(CargoCliExtractor.class);
    private static final List<String> CARGO_TREE_COMMAND = Arrays.asList("tree", "--prefix", "depth");
    private final DetectableExecutableRunner executableRunner;
    private final CargoDependencyGraphTransformer cargoDependencyTransformer;
    private final CargoTomlParser cargoTomlParser;
    public static final String CARGO_TOML_FILENAME = "Cargo.toml";
    private static final String VIRTUAL_WORKSPACE_EXCLUSION_WARNING =
        "Cannot exclude all workspace members for virtual manifest. " +
            "Please check your workspace configuration (detect.cargo.ignore.all.workspaces or exclude properties). " +
            "Zero components will be reported in SBOM.";

    public CargoCliExtractor(DetectableExecutableRunner executableRunner, CargoDependencyGraphTransformer cargoDependencyTransformer, CargoTomlParser cargoTomlParser) {
        this.executableRunner = executableRunner;
        this.cargoDependencyTransformer = cargoDependencyTransformer;
        this.cargoTomlParser = cargoTomlParser;
    }

    public Extraction extract(File directory, ExecutableTarget cargoExe, File cargoTomlFile, CargoDetectableOptions cargoDetectableOptions) throws ExecutableFailedException, IOException {
        Optional<NameVersion> projectNameVersion = Optional.empty();
        Set<String> workspaceMemberPaths = new HashSet<>();
        Set<String> allWorkspaceMembers = new HashSet<>();
        Set<String> activeWorkspaceMembers = new HashSet<>();
        boolean isVirtualWorkspace = false;

        if (cargoTomlFile != null) {
            File workspaceRoot = cargoTomlFile.getParentFile();
            String cargoTomlContents = FileUtils.readFileToString(cargoTomlFile, StandardCharsets.UTF_8);

            projectNameVersion = cargoTomlParser.parseNameVersionFromCargoToml(cargoTomlContents);
            isVirtualWorkspace = cargoTomlParser.isVirtualWorkspace(cargoTomlContents);

            // Get workspace member paths
            workspaceMemberPaths = cargoTomlParser.parseAllWorkspaceMembers(cargoTomlContents, workspaceRoot);

            // Resolve paths to package names
            allWorkspaceMembers.addAll(resolveWorkspaceMemberNames(workspaceMemberPaths, workspaceRoot));

            // Calculate active members after exclusions
            activeWorkspaceMembers = calculateActiveMembers(
                allWorkspaceMembers,
                cargoDetectableOptions.getIncludedWorkspaces(),
                cargoDetectableOptions.getExcludedWorkspaces()
            );
        }

        EnumListFilter<CargoDependencyType> dependencyTypeFilter = Optional.ofNullable(cargoDetectableOptions.getDependencyTypeFilter())
            .orElse(EnumListFilter.excludeNone());

        List<String> fullTreeOutput = runCargoTreeCommand(
            directory,
            cargoExe,
            cargoDetectableOptions,
            dependencyTypeFilter,
            activeWorkspaceMembers,
            isVirtualWorkspace
        );

        List<CodeLocation> codeLocations = cargoDependencyTransformer.transform(fullTreeOutput, workspaceMemberPaths);

        return new Extraction.Builder()
            .success(codeLocations)
            .nameVersionIfPresent(projectNameVersion)
            .build();
    }

    private Set<String> calculateActiveMembers(
        Set<String> allMembers,
        List<String> includedWorkspaces,
        List<String> excludedWorkspaces
    ) {
        Set<String> activeMembers = new HashSet<>(allMembers);

        if (includedWorkspaces != null && !includedWorkspaces.isEmpty()) {
            activeMembers.retainAll(includedWorkspaces);
        }

        if (excludedWorkspaces != null && !excludedWorkspaces.isEmpty()) {
            activeMembers.removeAll(excludedWorkspaces);
        }

        return activeMembers;
    }

    private Set<String> resolveWorkspaceMemberNames(Set<String> workspaceMemberPaths, File workspaceRoot) throws IOException {
        Set<String> packageNames = new HashSet<>();

        for (String memberPath : workspaceMemberPaths) {
            File memberDir = new File(workspaceRoot, memberPath);
            File memberCargoToml = new File(memberDir, CARGO_TOML_FILENAME);

            if (memberCargoToml.exists()) {
                String memberTomlContents = FileUtils.readFileToString(memberCargoToml, StandardCharsets.UTF_8);
                String packageName = cargoTomlParser.parsePackageNameFromCargoToml(memberTomlContents);

                if (packageName != null) {
                    packageNames.add(packageName);
                }
            }
        }

        return packageNames;
    }

    private List<String> runCargoTreeCommand(
        File directory,
        ExecutableTarget cargoExe,
        CargoDetectableOptions cargoDetectableOptions,
        EnumListFilter<CargoDependencyType> dependencyTypeFilter,
        Set<String> activeWorkspaceMembers,
        boolean isVirtualWorkspace
    ) throws ExecutableFailedException {

        boolean shouldIgnoreAllWorkspaceMembers = cargoDetectableOptions.getCargoIgnoreAllWorkspacesMode();
        List<String> includedWorkspaces = cargoDetectableOptions.getIncludedWorkspaces();
        List<String> excludedWorkspaces = cargoDetectableOptions.getExcludedWorkspaces();

        boolean hasInclusions = includedWorkspaces != null && !includedWorkspaces.isEmpty();
        boolean hasExclusions = excludedWorkspaces != null && !excludedWorkspaces.isEmpty();
        boolean noActiveWorkspaceMembers = hasExclusions && activeWorkspaceMembers.isEmpty();

        // Case: User excluded all workspace members (via property or exclude config) for virtual workspace
        if (shouldSkipVirtualWorkspace(isVirtualWorkspace, shouldIgnoreAllWorkspaceMembers, noActiveWorkspaceMembers)) {
            logger.warn(VIRTUAL_WORKSPACE_EXCLUSION_WARNING);
            return new LinkedList<>();
        }

        // Special case: inclusions without exclusions - run two separate commands
        // This ensures we get root package + included packages
        if (hasInclusions && !hasExclusions) {

            // For virtual workspaces, only run the included packages command
            // Virtual workspaces have no root package dependencies
            if (isVirtualWorkspace) {
                List<String> includedCommand = buildPackageCommand(includedWorkspaces, dependencyTypeFilter, cargoDetectableOptions);
                return runCargoTreeCommand(directory, cargoExe, includedCommand);
            }

            // Command 1: Get root package dependencies
            List<String> rootCommand = new LinkedList<>(CARGO_TREE_COMMAND);
            if (!dependencyTypeFilter.shouldIncludeAll()) {
                addEdgeExclusions(rootCommand, cargoDetectableOptions);
            }
            List<String> combinedOutput = new LinkedList<>(runCargoTreeCommand(directory, cargoExe, rootCommand));

            // Add line break separator
            combinedOutput.add("");

            // Command 2: Get included packages dependencies
            List<String> includedCommand = buildPackageCommand(includedWorkspaces, dependencyTypeFilter, cargoDetectableOptions);

            // Add features flags if required
            addFeatureFlags(includedCommand, cargoDetectableOptions);

            if (!dependencyTypeFilter.shouldIncludeAll()) {
                addEdgeExclusions(includedCommand, cargoDetectableOptions);
            }
            combinedOutput.addAll(runCargoTreeCommand(directory, cargoExe, includedCommand));

            return combinedOutput;
        }

        // Single-command logic for all other cases:
        // - No filters (default workspace)
        // - Only exclusions (--workspace --exclude)
        // - Both inclusions AND exclusions (--package --workspace --exclude)
        List<String> command = new LinkedList<>(CARGO_TREE_COMMAND);

        if (!shouldIgnoreAllWorkspaceMembers) {
            addWorkspaceFlags(command, cargoDetectableOptions);
        }

        // Add features flags if required
        addFeatureFlags(command, cargoDetectableOptions);

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

    private boolean shouldSkipVirtualWorkspace(boolean isVirtualWorkspace, boolean shouldIgnoreAllWorkspaceMembers, boolean noActiveWorkspaceMembers) {
        return isVirtualWorkspace && (shouldIgnoreAllWorkspaceMembers || noActiveWorkspaceMembers);
    }

    private List<String> buildPackageCommand(
        List<String> packages,
        EnumListFilter<CargoDependencyType> dependencyTypeFilter,
        CargoDetectableOptions options
    ) {
        List<String> command = new LinkedList<>(CARGO_TREE_COMMAND);

        for (String packageName : packages) {
            command.add("--package");
            command.add(packageName);
        }

        if (!dependencyTypeFilter.shouldIncludeAll()) {
            addEdgeExclusions(command, options);
        }

        return command;
    }

    private void addWorkspaceFlags(List<String> command, CargoDetectableOptions options) {
        List<String> includedWorkspaces = options.getIncludedWorkspaces();
        List<String> excludedWorkspaces = options.getExcludedWorkspaces();

        boolean hasInclusions = includedWorkspaces != null && !includedWorkspaces.isEmpty();
        boolean hasExclusions = excludedWorkspaces != null && !excludedWorkspaces.isEmpty();

        // Add --workspace flag when:
        // 1. No filters specified (default: scan entire workspace)
        // 2. Exclusions are present (cargo requires --workspace with --exclude)
        if (!hasInclusions || hasExclusions) {
            command.add("--workspace");
        }

        // Add --package flags for each explicitly included workspace member
        if (hasInclusions) {
            for (String includeWorkspace : includedWorkspaces) {
                command.add("--package");
                command.add(includeWorkspace);
            }
        }

        // Add --exclude flags for workspace members to skip
        if (hasExclusions) {
            for (String excludedWorkspace : excludedWorkspaces) {
                command.add("--exclude");
                command.add(excludedWorkspace);
            }
        }
    }

    private void addFeatureFlags(List<String> command, CargoDetectableOptions options) {
        List<String> features = options.getIncludedFeatures();
        boolean isDefaultFeaturesDisabled = options.isDefaultFeaturesDisabled();

        // Handle --no-default-features flag (independent of specific features)
        if (isDefaultFeaturesDisabled) {
            command.add("--no-default-features");
        }

        // Handle feature specifications
        if (features != null && !features.isEmpty()) {
            // Check for special keywords (case-insensitive)
            boolean includeAllFeatures = features.stream()
                .anyMatch(feature -> "ALL".equalsIgnoreCase(feature.trim()));
            boolean includeNoFeatures = features.stream()
                .anyMatch(feature -> "NONE".equalsIgnoreCase(feature.trim()));

            if (includeNoFeatures) {
                // NONE keyword: skip all feature processing
                return;
            } else if (includeAllFeatures) {
                command.add("--all-features");
            } else {
                // Add each feature with its own --features flag
                // This handles edge cases like features starting with digits (e.g., "2d", "3d")
                for (String feature : features) {
                    String trimmedFeature = feature.trim();
                    if (!trimmedFeature.isEmpty()) {
                        command.add("--features");
                        command.add(trimmedFeature);
                    }
                }
            }
        }
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
