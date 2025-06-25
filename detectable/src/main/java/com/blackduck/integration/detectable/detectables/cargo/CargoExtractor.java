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
            Set<NameVersion> dependenciesToExclude = cargoTomlParser.parseDependenciesToExclude(cargoTomlContents, cargoDetectableOptions.getDependencyTypeFilter());
            filteredPackages = excludeDependencies(cargoLockPackageDataList, dependenciesToExclude);
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
        Set<NameVersion> dependenciesToExclude
    ) {
        processTransitiveDependencies(packages, dependenciesToExclude); // Collect transitive dependencies to exclude
        return filterExcludedPackages(packages, dependenciesToExclude); // Filter out direct and transitive dependencies
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
        List<CargoLockPackageData> result = new ArrayList<>();

        for (CargoLockPackageData pkg : packages) {
            String name = pkg.getName().orElse(null);
            String version = VersionUtils.stripBuildMetadata(pkg.getVersion().orElse(null));

            if (!shouldInclude(name, version, toExclude)) {
                continue;
            }

            CargoLockPackageData filteredPkg = createFilteredPackage(pkg, packages, toExclude);
            result.add(filteredPkg);
        }

        return result;
    }

    private CargoLockPackageData createFilteredPackage(
        CargoLockPackageData pkg,
        List<CargoLockPackageData> allPackages,
        Set<NameVersion> toExclude
    ) {
        List<String> filteredDependencies = new ArrayList<>();

        for (String depStr : pkg.getDependencies().orElse(Collections.emptyList())) {
            NameVersion nameVersion = extractPackageNameVersion(depStr, allPackages);

            if (nameVersion == null) {
                filteredDependencies.add(depStr);
                continue;
            }

            String depName = nameVersion.getName();
            String depVersion = VersionUtils.stripBuildMetadata(nameVersion.getVersion());

            if (shouldInclude(depName, depVersion, toExclude)) {
                filteredDependencies.add(depStr);
            }
        }

        return new CargoLockPackageData(
            pkg.getName().orElse(null),
            pkg.getVersion().orElse(null),
            pkg.getSource().orElse(null),
            pkg.getChecksum().orElse(null),
            filteredDependencies
        );
    }

    private boolean shouldInclude(String name, String version, Set<NameVersion> toExclude) {
        for (NameVersion nameVersion : toExclude) {
            String exName = nameVersion.getName();
            String exVersion = VersionUtils.stripBuildMetadata(nameVersion.getVersion());

            if (Objects.equals(name, exName) && VersionUtils.versionMatches(exVersion, version)) {
                return false;
            }
        }
        return true;
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
