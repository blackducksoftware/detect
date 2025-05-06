package com.blackduck.integration.detectable.detectables.cargo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CargoExtractor {
    private static final Logger logger = LoggerFactory.getLogger(CargoExtractor.class);
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

        String cargoTomlContents = FileUtils.readFileToString(cargoTomlFile, StandardCharsets.UTF_8);
        Map<String, String> excludableDependencyMap = cargoTomlParser.parseDependencyNameVersions(cargoTomlContents, cargoDetectableOptions);
        List<CargoLockPackageData> filteredPackages = excludeDependencies(cargoLockPackageDataList, excludableDependencyMap);

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

    private List<CargoLockPackageData> excludeDependencies(
        List<CargoLockPackageData> packages,
        Map<String, String> excludableDependencyMap
    ) {
        Set<String> excludedNames = new HashSet<>();

        List<CargoLockPackageData> filtered = packages.stream()
            .filter(pkg -> {
                String name = pkg.getName().orElse(null);
                String version = pkg.getVersion().orElse(null);
                if (name == null || version == null) return true;

                if (excludableDependencyMap.containsKey(name)) {
                    String constraint = excludableDependencyMap.get(name);
                    boolean matches = constraint == null || VersionUtils.versionMatches(constraint, version);
                    if (matches) {
                        logger.debug("Excluding package '{}' version '{}' due to constraint '{}'", name, version, constraint);
                        excludedNames.add(name);
                        return false;
                    }
                }

                return true;
            })
            .collect(Collectors.toList());

        return filtered.stream()
            .map(pkg -> new CargoLockPackageData(
                pkg.getName().orElse(null),
                pkg.getVersion().orElse(null),
                pkg.getSource().orElse(null),
                pkg.getChecksum().orElse(null),
                pkg.getDependencies()
                    .orElse(new ArrayList<>())
                    .stream()
                    .filter(dep -> !excludedNames.contains(dep))
                    .collect(Collectors.toList())
            ))
            .collect(Collectors.toList());
    }
}
