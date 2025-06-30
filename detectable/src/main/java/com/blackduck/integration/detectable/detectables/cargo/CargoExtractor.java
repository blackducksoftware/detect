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

import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.cargo.data.CargoLockPackageData;
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

public class CargoExtractor {
    private final CargoTomlParser cargoTomlParser;
    private final CargoLockPackageDataTransformer cargoLockPackageDataTransformer;
    private final CargoLockPackageTransformer cargoLockPackageTransformer;

    public CargoExtractor(
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
        List<CargoLockPackageData> filteredPackages = cargoLockPackageDataList;
        boolean exclusionEnabled = isDependencyExclusionEnabled(cargoDetectableOptions);
        String cargoTomlContents = null;

        if(cargoTomlFile == null && exclusionEnabled) {
            return new Extraction.Builder()
                .failure("Cargo.toml file is required to exclude dependencies, but was not provided.")
                .build();
        }

        if (cargoTomlFile != null) {
            cargoTomlContents = FileUtils.readFileToString(cargoTomlFile, StandardCharsets.UTF_8);
        }

        if (cargoTomlFile != null && exclusionEnabled) {
            Set<NameVersion> dependenciesToInclude = cargoTomlParser.parseDependenciesToInclude(
                    cargoTomlContents, cargoDetectableOptions.getDependencyTypeFilter());
            filteredPackages = includeDependencies(cargoLockPackageDataList, dependenciesToInclude);
        }

        List<CargoLockPackage> packages = filteredPackages.stream()
            .map(cargoLockPackageDataTransformer::transform)
            .collect(Collectors.toList());

        DependencyGraph graph = cargoLockPackageTransformer.transformToGraph(packages);

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


    private List<CargoLockPackageData> includeDependencies(
            List<CargoLockPackageData> packages,
            Set<NameVersion> dependenciesToInclude
    ) {

        Map<String, List<CargoLockPackageData>> packageLookupMap = indexPackagesByName(packages); // Create lookup map (multi-map) for each name
        processTransitiveDependenciesForInclusion(dependenciesToInclude, packageLookupMap); // Collect all transitive dependencies to include
        return filterPackagesByInclusion(packages, dependenciesToInclude); // Only keep direct and transitive dependencies
    }

    private Map<String, List<CargoLockPackageData>> indexPackagesByName(List<CargoLockPackageData> packages) {
        return packages.stream()
            .filter(pkg -> pkg.getName().isPresent())
            .collect(Collectors.groupingBy(pkg -> pkg.getName().get()));
    }

    private void processTransitiveDependenciesForInclusion(
        Set<NameVersion> dependenciesToInclude,
        Map<String, List<CargoLockPackageData>> packageLookupMap
    ) {

        Set<NameVersion> resolvedRootDependencies = resolveRootDependencies(dependenciesToInclude, packageLookupMap);
        dependenciesToInclude.clear();
        dependenciesToInclude.addAll(resolvedRootDependencies);

        Deque<NameVersion> queue = new ArrayDeque<>(dependenciesToInclude);
        while (!queue.isEmpty()) {
            NameVersion current = queue.pop();
            CargoLockPackageData currentPkg = findPackageByNameVersion(current, packageLookupMap);
            if (currentPkg != null) {
                currentPkg.getDependencies().ifPresent(dependencies -> {
                    for (String depStr : dependencies) {
                        NameVersion nameVersion = extractPackageNameVersion(depStr, packageLookupMap);
                        if (nameVersion != null && dependenciesToInclude.add(nameVersion)) {
                            queue.add(nameVersion);
                        }
                    }
                });
            }
        }
    }

    private Set<NameVersion> resolveRootDependencies(
        Set<NameVersion> dependenciesToInclude,
        Map<String, List<CargoLockPackageData>> packageLookupMap
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
        Map<String, List<CargoLockPackageData>> packageLookupMap
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
        List<CargoLockPackageData> possiblePackages = packageLookupMap.get(depName);
        if (possiblePackages != null) {
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
        Map<String, List<CargoLockPackageData>> packageLookupMap
    ) {
        List<CargoLockPackageData> possiblePackages = packageLookupMap.get(nv.getName());
        if (possiblePackages == null) return null;

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
}
