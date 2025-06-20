package com.blackduck.integration.detectable.detectables.cargo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
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
            Map<NameVersion, String> excludableDependencyMap = cargoTomlParser.parseDependenciesToExclude(cargoTomlContents, cargoDetectableOptions.getDependencyTypeFilter());
            filteredPackages = excludeDependencies(cargoLockPackageDataList, excludableDependencyMap);
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

    private List<CargoLockPackageData> excludeDependencies(
        List<CargoLockPackageData> packages,
        Map<NameVersion, String> excludableDependencyMap
    ) {
        Set<NameVersion> dependenciesToExclude = initializeExclusionSet(excludableDependencyMap); // collecting the direct dependencies to exclude
        processTransitiveDependencies(packages, dependenciesToExclude); // collecting the direct transitive dependencies to exclude
        return filterExcludedPackages(packages, dependenciesToExclude); // filtering out direct and transitive dependencies
    }

    // Seed the exclusion set from excludableDependencyMap
    private Set<NameVersion> initializeExclusionSet(Map<NameVersion, String> excludableDependencyMap) {
        Set<NameVersion> toExclude = new HashSet<>();
        for (Map.Entry<NameVersion, String> entry : excludableDependencyMap.entrySet()) {
            NameVersion nv = entry.getKey();
            String constraint = entry.getValue();
            if (nv.getVersion() != null && (constraint == null || VersionUtils.versionMatches(constraint, nv.getVersion()))) {
                toExclude.add(nv);
            }
        }
        return toExclude;
    }

    // Recursively excluding transitive dependencies
    private void processTransitiveDependencies(List<CargoLockPackageData> packages, Set<NameVersion> dependenciesToExclude) {
        Deque<NameVersion> queue = new ArrayDeque<>(dependenciesToExclude);
        while (!queue.isEmpty()) {
            NameVersion current = queue.pop();
            CargoLockPackageData currentPkg = findPackageByNameVersion(current, packages);
            if (currentPkg != null) {
                currentPkg.getDependencies().ifPresent(dependencies -> {
                    for (String depStr : dependencies) {
                        NameVersion nameVersion = extractPackageNameVersion(depStr, packages);
                        if (nameVersion != null && dependenciesToExclude.add(nameVersion)) {
                            queue.add(nameVersion);
                        }
                    }
                });
            }
        }
    }

    // Filter out dependencies dependencies
    private List<CargoLockPackageData> filterExcludedPackages(List<CargoLockPackageData> packages, Set<NameVersion> toExclude) {
        return packages.stream()
            .filter(pkg -> !toExclude.contains(new NameVersion(pkg.getName().orElse(null), pkg.getVersion().orElse(null))))
            .map(pkg -> new CargoLockPackageData(
                pkg.getName().orElse(null),
                pkg.getVersion().orElse(null),
                pkg.getSource().orElse(null),
                pkg.getChecksum().orElse(null),
                pkg.getDependencies().orElse(Collections.emptyList())
                    .stream()
                    .filter(depStr -> {
                        NameVersion dep = extractPackageNameVersion(depStr, packages);
                        return dep == null || !toExclude.contains(dep);
                    })
                    .collect(Collectors.toList())
            ))
            .collect(Collectors.toList());
    }

    // Extracting NameVersion from
    private NameVersion extractPackageNameVersion(String dependencyString, List<CargoLockPackageData> packages) {
        if (dependencyString == null || dependencyString.isEmpty()) {
            return null;
        }

        String[] parts = dependencyString.split(" ");
        String depName = parts[0].trim();
        String depVersion = (parts.length > 1) ? parts[1].trim() : null;

        for (CargoLockPackageData pkg : packages) {
            String name = pkg.getName().orElse(null);
            String version = pkg.getVersion().orElse(null);

            if (!depName.equals(name)) {
                continue;
            }

            if (depVersion == null || depVersion.equals(version)) {
                return new NameVersion(name, version);
            }
        }

        return null;
    }

    private CargoLockPackageData findPackageByNameVersion(NameVersion nv, List<CargoLockPackageData> packages) {
        for (CargoLockPackageData pkg : packages) {
            String name = pkg.getName().orElse(null);
            String version = pkg.getVersion().orElse(null);

            // Matching name and use VersionUtils to check if the versions are compatible
            if (nv.getName().equals(name)
                && version != null
                && VersionUtils.versionMatches(VersionUtils.stripBuildMetadata(nv.getVersion()), VersionUtils.stripBuildMetadata(version))) {
                return pkg;
            }
        }
        return null;
    }
}
