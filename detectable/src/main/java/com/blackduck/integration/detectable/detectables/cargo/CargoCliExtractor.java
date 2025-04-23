package com.blackduck.integration.detectable.detectables.cargo;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
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
import java.util.List;
import java.util.Optional;

public class CargoCliExtractor {
    private static final List<String> CARGO_TREE_COMMAND = Arrays.asList("tree", "--no-dedupe", "--prefix", "depth");
    private final DetectableExecutableRunner executableRunner;
    private final CargoDependencyGraphTransformer cargoDependencyTransformer;
    private final CargoTomlParser cargoTomlParser;

    public CargoCliExtractor(DetectableExecutableRunner executableRunner, CargoDependencyGraphTransformer cargoDependencyTransformer, CargoTomlParser cargoTomlParser) {
        this.executableRunner = executableRunner;
        this.cargoDependencyTransformer = cargoDependencyTransformer;
        this.cargoTomlParser = cargoTomlParser;
    }

    public Extraction extract(File directory, ExecutableTarget cargoExe, File cargoTomlFile, CargoCliDetectableOptions cargoCliDetectableOptions) throws ExecutableFailedException, IOException {
        List<String> cargoTreeCommand = new ArrayList<>(CARGO_TREE_COMMAND);

        if (cargoCliDetectableOptions.getDependencyTypeFilter().shouldExclude(CargoDependencyType.DEV)) {
            cargoTreeCommand.add("--edges");
            cargoTreeCommand.add("normal,build");
        }

        ExecutableOutput cargoOutput = executableRunner.executeSuccessfully(ExecutableUtils.createFromTarget(directory, cargoExe, cargoTreeCommand));
        List<String> cargoTreeOutput = cargoOutput.getStandardOutputAsList();

        DependencyGraph graph = cargoDependencyTransformer.transform(cargoTreeOutput);

        Optional<NameVersion> projectNameVersion = Optional.empty();
        if (cargoTomlFile != null) {
            String cargoTomlContents = FileUtils.readFileToString(cargoTomlFile, StandardCharsets.UTF_8);
            projectNameVersion = cargoTomlParser.parseNameVersionFromCargoToml(cargoTomlContents);
        }

        CodeLocation codeLocation = new CodeLocation(graph);

        return new Extraction.Builder()
            .success(codeLocation)
            .nameVersionIfPresent(projectNameVersion)
            .build();
    }
}
