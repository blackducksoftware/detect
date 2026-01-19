package com.blackduck.integration.detectable.detectables.cargo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.cargo.data.CargoLockPackageData;
import com.blackduck.integration.detectable.util.NameOptionalVersion;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;

import com.moandjiezana.toml.Toml;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.graph.builder.MissingExternalIdException;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectables.cargo.data.CargoLockData;
import com.blackduck.integration.detectable.detectables.cargo.model.CargoLockPackage;
import com.blackduck.integration.detectable.detectables.cargo.parse.CargoTomlParser;
import com.blackduck.integration.detectable.detectables.cargo.transform.CargoLockPackageDataTransformer;
import com.blackduck.integration.detectable.detectables.cargo.transform.CargoLockPackageTransformer;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.util.NameVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CargoLockfileExtractor {
    private static final Logger logger = LoggerFactory.getLogger(CargoLockfileExtractor.class);
    private final CargoTomlParser cargoTomlParser;
    private final CargoLockPackageDataTransformer cargoLockPackageDataTransformer;
    private final CargoLockPackageTransformer cargoLockPackageTransformer;

    public CargoLockfileExtractor(
        CargoTomlParser cargoTomlParser,
        CargoLockPackageDataTransformer cargoLockPackageDataTransformer,
        CargoLockPackageTransformer cargoLockPackageTransformer
    ) {
        this.cargoTomlParser = cargoTomlParser;
        this.cargoLockPackageDataTransformer = cargoLockPackageDataTransformer;
        this.cargoLockPackageTransformer = cargoLockPackageTransformer;
    }

    public Extraction extract(File cargoLockFile, @Nullable File cargoTomlFile, CargoDetectableOptions cargoDetectableOptions) throws IOException, DetectableException, MissingExternalIdException {
        CargoLockData cargoLockData = new Toml().read(cargoLockFile).to(CargoLockData.class);
        List<CargoLockPackageData> cargoLockPackageDataList = cargoLockData.getPackages().orElse(new ArrayList<>());
        boolean exclusionEnabled = isDependencyExclusionEnabled(cargoDetectableOptions);

        if (exclusionEnabled && cargoDetectableOptions.getDependencyTypeFilter().shouldExclude(CargoDependencyType.PROC_MACRO)) {
            logger.warn(
                "PROC_MACRO exclusion is not supported by the Cargo Lockfile Detector and will be ignored. " +
                    "Supported exclusions for Cargo Lockfile Detector: [NORMAL, BUILD, DEV]. "
            );
        }

        if (cargoTomlFile == null && exclusionEnabled) {
            return new Extraction.Builder()
                .failure("Cargo.toml file is required to exclude dependencies, but was not provided.")
                .build();
        }

        Map<NameVersion, List<CargoLockPackageData>> packageLookupMap = indexPackagesByNameVersion(cargoLockPackageDataList);
        EnumListFilter<CargoDependencyType> filter = exclusionEnabled ? cargoDetectableOptions.getDependencyTypeFilter() : null;

        List<CodeLocation> codeLocations = new ArrayList<>();
        Optional<NameVersion> projectNameVersion = Optional.empty();

        if(cargoTomlFile != null) {
            String cargoTomlContents = FileUtils.readFileToString(cargoTomlFile, StandardCharsets.UTF_8);
            File workspaceRoot = cargoTomlFile.getParentFile();
            Set<String> workspaceMembers = cargoTomlParser.parseActiveWorkspaceMembers(cargoTomlContents, workspaceRoot);

            if(cargoDetectableOptions != null) {
                // Step-1: Process all workspace members and their Cargo.toml first
                processWorkspaceMembers(workspaceMembers, cargoDetectableOptions, workspaceRoot, cargoLockPackageDataList, packageLookupMap, filter, codeLocations);
            }

            // Step-2: Process single root Cargo.toml. Only filter if Cargo.toml defines dependency sections.
            // Workspace root Cargo.toml files usually don’t, so skip filtering in that case.
            if (cargoTomlParser.hasDependencySections(cargoTomlContents)) {
                CodeLocation rootCodeLocation = buildCodeLocationFromCargoToml(
                    cargoTomlFile,
                    cargoLockPackageDataList,
                    packageLookupMap,
                    null,
                    filter
                );
                codeLocations.add(rootCodeLocation);
            }
            projectNameVersion = cargoTomlParser.parseNameVersionFromCargoToml(cargoTomlContents);
        }

        // Fallback: if no Cargo.toml found or CodeLocations were created, treat all Cargo.lock packages as direct dependencies
        if (cargoTomlFile == null || codeLocations.isEmpty()) {
            List<CargoLockPackage> allPackages = cargoLockPackageDataList.stream()
                .map(cargoLockPackageDataTransformer::transform)
                .collect(Collectors.toList());
            allPackages = resolveDependencyVersions(allPackages, packageLookupMap);

            Set<NameVersion> allRootDependencies = allPackages.stream()
                .map(CargoLockPackage::getPackageNameVersion)
                .collect(Collectors.toSet());

            DependencyGraph graph = cargoLockPackageTransformer.transformToGraph(
                allPackages,
                allRootDependencies
            );

            CodeLocation codeLocation = new CodeLocation(graph);
            codeLocations.add(codeLocation);
        }

        return new Extraction.Builder()
            .success(codeLocations)
            .nameVersionIfPresent(projectNameVersion)
            .build();
    }

    private boolean isDependencyExclusionEnabled(CargoDetectableOptions options) {
        if (options == null) {
            return false;
        }

        EnumListFilter<CargoDependencyType> filter = options.getDependencyTypeFilter();
        return filter != null && !filter.shouldIncludeAll();
    }

    private List<CargoLockPackageData> resolveDirectAndTransitiveDependencies(
            List<CargoLockPackageData> packages,
            Set<NameVersion> dependenciesToInclude,
            Set<NameVersion> resolvedRootDependencies,
            Map<NameVersion, List<CargoLockPackageData>> packageLookupMap
    ) {
        processTransitiveDependenciesForInclusion(dependenciesToInclude, packageLookupMap, resolvedRootDependencies); // Collect all transitive dependencies to include
        return filterPackagesByInclusion(packages, dependenciesToInclude); // Only keep direct and transitive dependencies
    }

    private Map<NameVersion, List<CargoLockPackageData>> indexPackagesByNameVersion(List<CargoLockPackageData> packages) {
        return packages.stream()
            .filter(pkg -> pkg.getName().isPresent() && pkg.getVersion().isPresent())
            .collect(Collectors.groupingBy(pkg -> new NameVersion(
                pkg.getName().get(),
                VersionUtils.stripBuildMetadata(pkg.getVersion().get())
            )));
    }

    private void processTransitiveDependenciesForInclusion(
        Set<NameVersion> dependenciesToInclude,
        Map<NameVersion, List<CargoLockPackageData>> packageLookupMap,
        Set<NameVersion> resolvedRootDependenciesOut
    ) {
        Set<NameVersion> resolvedRootDependencies = resolveRootDependencies(dependenciesToInclude, packageLookupMap);

        // Preserve resolved root dependencies
        resolvedRootDependenciesOut.addAll(resolvedRootDependencies);

        // Copy to avoid modifying the original root dependencies
        Set<NameVersion> allDependenciesToInclude = new HashSet<>(resolvedRootDependencies);
        Deque<NameVersion> queue = new ArrayDeque<>(resolvedRootDependencies);

        while (!queue.isEmpty()) {
            NameVersion current = queue.pop();
            CargoLockPackageData currentPkg = findPackageByNameVersion(current, packageLookupMap);
            if (currentPkg != null) {
                currentPkg.getDependencies().ifPresent(dependencies -> {
                    for (String depStr : dependencies) {
                        NameVersion nameVersion = extractPackageNameVersion(depStr, packageLookupMap);
                        if (nameVersion != null && allDependenciesToInclude.add(nameVersion)) {
                            queue.add(nameVersion);
                        }
                    }
                });
            }
        }

        // Mutate dependenciesToInclude to include all resolved dependencies
        dependenciesToInclude.clear();
        dependenciesToInclude.addAll(allDependenciesToInclude);
    }

    private Set<NameVersion> resolveRootDependencies(
        Set<NameVersion> dependenciesToInclude,
        Map<NameVersion, List<CargoLockPackageData>> packageLookupMap
    ) {
        Set<NameVersion> resolvedRootDependencies = new HashSet<>();
        for (NameVersion nv : dependenciesToInclude) {
            CargoLockPackageData pkg = findPackageByNameVersion(nv, packageLookupMap);
            if (pkg != null) {
                String name = pkg.getName().orElse(null);
                String version = pkg.getVersion().orElse(null);
                resolvedRootDependencies.add(new NameVersion(name, version));
            } else {
                resolvedRootDependencies.add(nv);
            }
        }
        return resolvedRootDependencies;
    }

    private List<CargoLockPackage> resolveDependencyVersions(
            List<CargoLockPackage> packages,
            Map<NameVersion, List<CargoLockPackageData>> packageLookupMap
    ) {
        List<CargoLockPackage> resolvedPackages = new ArrayList<>();
        for (CargoLockPackage pkg : packages) {
            List<NameOptionalVersion> resolvedDependencies = new ArrayList<>();
            for (NameOptionalVersion dep : pkg.getDependencies()) {
                if (!dep.getVersion().isPresent()) {
                    NameVersion resolved = extractPackageNameVersion(dep.getName(), packageLookupMap);
                    if (resolved != null) {
                        resolvedDependencies.add(new NameOptionalVersion(resolved.getName(), resolved.getVersion()));
                    } else {
                        resolvedDependencies.add(dep);
                    }
                } else {
                    resolvedDependencies.add(dep);
                }
            }
            String name = pkg.getPackageNameVersion().getName();
            String version = pkg.getPackageNameVersion().getVersion();
            resolvedPackages.add(new CargoLockPackage(new NameVersion(name, version), resolvedDependencies));
        }
        return resolvedPackages;
    }

    private List<CargoLockPackageData> filterPackagesByInclusion(
            List<CargoLockPackageData> packages,
            Set<NameVersion> dependenciesToInclude
    ) {
        List<CargoLockPackageData> result = new ArrayList<>();
        for (CargoLockPackageData pkg : packages) {
            String name = pkg.getName().orElse(null);
            String version = VersionUtils.stripBuildMetadata(pkg.getVersion().orElse(null));
            for (NameVersion include : dependenciesToInclude) {
                String includeName = include.getName();
                String includeVersion = VersionUtils.stripBuildMetadata(include.getVersion());
                if (Objects.equals(name, includeName) && VersionUtils.versionMatches(includeVersion, version)) {
                    result.add(pkg);
                    break;
                }
            }
        }
        return result;
    }

    private NameVersion extractPackageNameVersion(
        String dependencyString,
        Map<NameVersion, List<CargoLockPackageData>> packageLookupMap
    ) {
        if (dependencyString == null || dependencyString.isEmpty()) {
            return null;
        }

        String[] parts = dependencyString.split(" ");
        String depName = parts[0].trim();
        String depVersion = (parts.length > 1) ? parts[1].trim() : null;

        // If version is specified, return as-is (original behavior)
        if (depVersion != null && !depVersion.isEmpty()) {
            return new NameVersion(depName, depVersion);
        }

        // If depVersion is null or empty, find by name only.
        // Otherwise, find by name and version.
        // Original name-only lookup behavior replicated exactly
        List<CargoLockPackageData> possiblePackages = findPackagesByName(depName, packageLookupMap);
        if (!possiblePackages.isEmpty()) {
            for (CargoLockPackageData pkg : possiblePackages) {
                String name = pkg.getName().orElse(null);
                if (depName.equals(name)) {
                    return new NameVersion(name, pkg.getVersion().orElse(null));
                }
            }
        }

        return new NameVersion(depName, depVersion);
    }

    private CargoLockPackageData findPackageByNameVersion(
        NameVersion nv,
        Map<NameVersion, List<CargoLockPackageData>> packageLookupMap
    ) {
        List<CargoLockPackageData> possiblePackages = packageLookupMap.get(nv);
        if (possiblePackages == null) {
            possiblePackages = findPackagesByName(nv.getName(), packageLookupMap);
        }

        for (CargoLockPackageData pkg : possiblePackages) {
            String version = pkg.getVersion().orElse(null);
            if (version != null && VersionUtils.versionMatches(
                VersionUtils.stripBuildMetadata(nv.getVersion()),
                VersionUtils.stripBuildMetadata(version))) {
                return pkg;
            }
        }
        return null;
    }

    private List<CargoLockPackageData> findPackagesByName(
        String name,
        Map<NameVersion, List<CargoLockPackageData>> packageLookupMap
    ) {
        List<CargoLockPackageData> out = new ArrayList<>();
        for (Map.Entry<NameVersion, List<CargoLockPackageData>> e : packageLookupMap.entrySet()) {
            if (Objects.equals(name, e.getKey().getName())) {
                out.addAll(e.getValue());
            }
        }
        return out;
    }

    private void processWorkspaceMembers(
        Set<String> workspaceMembers,
        CargoDetectableOptions cargoDetectableOptions,
        File workspaceRoot,
        List<CargoLockPackageData> cargoLockPackageDataList,
        Map<NameVersion, List<CargoLockPackageData>> packageLookupMap,
        EnumListFilter<CargoDependencyType> filter,
        List<CodeLocation> codeLocations
    ) throws IOException, MissingExternalIdException, DetectableException {

        boolean ignoreAllWorkspaceMembers = cargoDetectableOptions.getCargoIgnoreAllWorkspacesMode();
        List<String> includedWorkspaces = cargoDetectableOptions.getIncludedWorkspaces();
        List<String> excludedWorkspaces = cargoDetectableOptions.getExcludedWorkspaces();

        if(ignoreAllWorkspaceMembers || workspaceMembers.isEmpty()) {
            logger.info("Ignoring all workspace members as per configuration.");
            return;
        }

        Set<String> effectiveWorkspaces = getEffectiveWorkspaces(workspaceMembers, includedWorkspaces, excludedWorkspaces, workspaceRoot);

        for (String workspace : effectiveWorkspaces) {
            File workspaceToml = new File(workspaceRoot, workspace + File.separator + "Cargo.toml");
            if (workspaceToml.exists()) {
                CodeLocation workspaceCodeLocation = buildCodeLocationFromCargoToml(
                    workspaceToml,
                    cargoLockPackageDataList,
                    packageLookupMap,
                    workspace,
                    filter
                );
                codeLocations.add(workspaceCodeLocation);
            } else {
                logger.warn("Workspace member Cargo.toml not found: {}", workspaceToml.getPath());
            }
        }
    }

    private Set<String> getEffectiveWorkspaces(
        Set<String> allWorkspaces,
        List<String> includedWorkspaces,
        List<String> excludedWorkspaces,
        File workspaceRoot
    ) {
        Set<String> effective = new HashSet<>(allWorkspaces);

        // If specific inclusions exist, filter by matching base names
        if (includedWorkspaces != null && !includedWorkspaces.isEmpty()) {
            effective = allWorkspaces.stream()
                .filter(workspace -> matchesAnyWorkspace(workspace, includedWorkspaces, workspaceRoot))
                .collect(Collectors.toSet());
        }

        // Apply exclusions (exclusion dominates) - also match by base name
        if (excludedWorkspaces != null && !excludedWorkspaces.isEmpty()) {
            effective = effective.stream()
                .filter(workspace -> !matchesAnyWorkspace(workspace, excludedWorkspaces, workspaceRoot))
                .collect(Collectors.toSet());
        }

        return effective;
    }

    private CodeLocation buildCodeLocationFromCargoToml(
        File cargoTomlFile,
        List<CargoLockPackageData> cargoLockPackageDataList,
        Map<NameVersion, List<CargoLockPackageData>> packageLookupMap,
        @Nullable String workspacePath,
        EnumListFilter<CargoDependencyType> filter
    ) throws IOException, MissingExternalIdException, DetectableException {

        String cargoTomlContents = FileUtils.readFileToString(cargoTomlFile, StandardCharsets.UTF_8);
        Set<NameVersion> includedDependencies = cargoTomlParser.parseDependenciesToInclude(cargoTomlContents, filter);
        Set<NameVersion> resolvedRootDependencies = new HashSet<>();

        List<CargoLockPackageData> filteredPackages = resolveDirectAndTransitiveDependencies(
            cargoLockPackageDataList,
            includedDependencies,
            resolvedRootDependencies,
            packageLookupMap
        );

        List<CargoLockPackage> resolvedPackages = filteredPackages.stream()
            .map(cargoLockPackageDataTransformer::transform)
            .collect(Collectors.toList());
        resolvedPackages = resolveDependencyVersions(resolvedPackages, packageLookupMap);

        DependencyGraph graph = cargoLockPackageTransformer.transformToGraph(
            resolvedPackages,
            resolvedRootDependencies
        );

        Optional<NameVersion> projectNameVersion = cargoTomlParser.parseNameVersionFromCargoToml(cargoTomlContents);
        // Use workspace path as name if provided, otherwise use Cargo.toml name
        String name = (workspacePath != null) ? workspacePath : projectNameVersion.map(NameVersion::getName).orElse("unknown");
        String version = projectNameVersion.map(NameVersion::getVersion).orElse(null);

        ExternalIdFactory externalIdFactory = new ExternalIdFactory();
        ExternalId externalId = externalIdFactory.createNameVersionExternalId(Forge.CRATES, name, version);
        return externalId != null? new CodeLocation(graph, externalId): new CodeLocation(graph);
    }

    private boolean matchesAnyWorkspace(String workspacePath, List<String> workspaceNames, File workspaceRoot) {
        String baseName = extractWorkspaceBaseName(workspacePath);

        for (String name : workspaceNames) {
            // Exact matches
            if (name.equals(baseName) || name.equals(workspacePath)) {
                return true;
            }

            // Try to resolve workspace path to package name from Cargo.toml
            String packageName = resolveWorkspaceToPackageName(workspacePath, workspaceRoot);
            if (packageName != null && packageName.equals(name)) {
                return true;
            }

            // Suffix matching as fallback
            if (name.endsWith(baseName) || baseName.endsWith(name)) {
                return true;
            }
        }

        return false;
    }

    private String resolveWorkspaceToPackageName(String workspacePath, File workspaceRoot) {
        if (workspaceRoot == null || workspacePath == null) {
            return null;
        }

        File workspaceToml = new File(workspaceRoot, workspacePath + File.separator + "Cargo.toml");
        if (!workspaceToml.exists()) {
            return null;
        }

        try {
            String tomlContents = FileUtils.readFileToString(workspaceToml, StandardCharsets.UTF_8);
            return cargoTomlParser.parsePackageNameFromCargoToml(tomlContents);
        } catch (IOException e) {
            logger.warn("Failed to read Cargo.toml for workspace: {}", workspacePath, e);
            return null;
        }
    }

    private String extractWorkspaceBaseName(String workspacePath) {
        int lastSlash = workspacePath.lastIndexOf('/');
        if (lastSlash >= 0) {
            return workspacePath.substring(lastSlash + 1);
        }
        return workspacePath;
    }
}
