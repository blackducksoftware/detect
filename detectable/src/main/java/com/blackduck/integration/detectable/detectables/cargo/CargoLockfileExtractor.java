package com.blackduck.integration.detectable.detectables.cargo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

public class CargoLockfileExtractor {
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
        List<CargoLockPackageData> filteredPackages = new ArrayList<>(cargoLockPackageDataList);
        List<CargoLockPackageData> unfilteredPackages = new ArrayList<>(cargoLockPackageDataList);
        boolean exclusionEnabled = isDependencyExclusionEnabled(cargoDetectableOptions);
        String cargoTomlContents = null;

        Set<NameVersion> allRootDependencies = new HashSet<>();
        Set<NameVersion> resolvedRootDependencies = new HashSet<>();
        Map<NameVersion, List<CargoLockPackageData>> packageLookupMap = indexPackagesByNameVersion(filteredPackages); // Lookup map for quick access by NameVersion

        if(cargoTomlFile == null && exclusionEnabled) {
            return new Extraction.Builder()
                .failure("Cargo.toml file is required to exclude dependencies, but was not provided.")
                .build();
        }

        if (cargoTomlFile != null) {
            cargoTomlContents = FileUtils.readFileToString(cargoTomlFile, StandardCharsets.UTF_8);

            EnumListFilter<CargoDependencyType> filter = null;
            if (exclusionEnabled) {
                filter = cargoDetectableOptions.getDependencyTypeFilter();
            }

            // Only filter if Cargo.toml defines dependency sections.
            // Workspace root Cargo.toml files usually don’t, so skip filtering in that case.
            if (cargoTomlParser.hasDependencySections(cargoTomlContents)) {
                // Parses direct dependencies to include from Cargo.toml applying filter if provided
                Set<NameVersion> includedDependencies = cargoTomlParser.parseDependenciesToInclude(cargoTomlContents, filter);
                filteredPackages = resolveDirectAndTransitiveDependencies(cargoLockPackageDataList, includedDependencies, resolvedRootDependencies, packageLookupMap);

                // Collect all direct dependencies from Cargo.toml and their transitive dependencies
                // from Cargo.lock file to identify and collect orphan dependencies correctly
                Set<NameVersion> allDependencies = cargoTomlParser.parseDependenciesToInclude(cargoTomlContents, null);
                unfilteredPackages = resolveDirectAndTransitiveDependencies(cargoLockPackageDataList, allDependencies, allRootDependencies, packageLookupMap);
            }
        }

        // Packages that will be included in the dependency graph ultimately after filtering out NORMAL/BUILD/DEV
        List<CargoLockPackage> resolvedPackages = filteredPackages.stream()
            .map(cargoLockPackageDataTransformer::transform)
            .collect(Collectors.toList());
        resolvedPackages = resolveDependencyVersions(resolvedPackages, packageLookupMap);

        // All linked Packages via direct + transitive dependencies so as to isolate orphan dependencies
        List<CargoLockPackage> allConnectedPackages = unfilteredPackages.stream()
            .map(cargoLockPackageDataTransformer::transform)
            .collect(Collectors.toList());
        allConnectedPackages = resolveDependencyVersions(allConnectedPackages, packageLookupMap);

        List<CargoLockPackage> orphanedResolvedPackages = collectOrphanPackages(
            cargoLockPackageDataList,
            resolvedPackages,
            allConnectedPackages
        );

        DependencyGraph graph = cargoLockPackageTransformer.transformToGraph(resolvedPackages, orphanedResolvedPackages, resolvedRootDependencies);

        Optional<NameVersion> projectNameVersion = Optional.empty();
        if (cargoTomlFile != null) {
            projectNameVersion = cargoTomlParser.parseNameVersionFromCargoToml(cargoTomlContents);
        }
        CodeLocation codeLocation = new CodeLocation(graph); //TODO: Consider for producing a ProjectDependencyGraph

        return new Extraction.Builder()
            .success(codeLocation)
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

    private List<CargoLockPackage> collectOrphanPackages(
        List<CargoLockPackageData> allPackages,
        List<CargoLockPackage> resolvedPackages,
        List<CargoLockPackage> allConnectedPackages
    ) {

        Set<NameVersion> resolvedSet = resolvedPackages.stream()
            .map(CargoLockPackage::getPackageNameVersion)
            .collect(Collectors.toSet());

        Set<NameVersion> allConnectedSet = allConnectedPackages.stream()
            .map(CargoLockPackage::getPackageNameVersion)
            .collect(Collectors.toSet());

        // Orphaned = in lockfile but not in resolvedPackages and allConnectedSet
        return allPackages.stream()
            .map(cargoLockPackageDataTransformer::transform)
            .filter(pkg -> !allConnectedSet.contains(pkg.getPackageNameVersion()) &&
                !resolvedSet.contains(pkg.getPackageNameVersion()))
            .map(pkg -> new CargoLockPackage(pkg.getPackageNameVersion(), Collections.emptyList())) // clear dependencies
            .collect(Collectors.toList());
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
}
