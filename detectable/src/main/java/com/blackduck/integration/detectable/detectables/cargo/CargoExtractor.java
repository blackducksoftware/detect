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

        String cargoTomlContents = FileUtils.readFileToString(cargoTomlFile, StandardCharsets.UTF_8);
        Map<String, String> excludableDependencyMap = cargoTomlParser.parseDependencyNameVersions(cargoTomlContents, cargoDetectableOptions);
        excludeDependencies(cargoLockPackageDataList, excludableDependencyMap);

        List<CargoLockPackage> packages = cargoLockData.getPackages()
            .orElse(new ArrayList<>()).stream()
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

    private void excludeDependencies(List<CargoLockPackageData> cargoLockPackageDataList, Map<String, String> dependencyMap) {
        cargoLockPackageDataList.removeIf(packageData -> {
            String name = packageData.getName().orElse(null);
            String version = packageData.getVersion().orElse(null);

            if (!dependencyMap.containsKey(name)) {
                return false;
            }

            String devVersion = dependencyMap.get(name);
            return devVersion == null || devVersion.equals(version);
        });
    }
}
