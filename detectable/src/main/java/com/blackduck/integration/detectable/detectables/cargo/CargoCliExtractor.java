package com.blackduck.integration.detectable.detectables.cargo;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.detectables.cargo.parse.CargoTomlParser;
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
import java.util.List;
import java.util.Optional;

public class CargoCliExtractor {
    private final DetectableExecutableRunner executableRunner;
    private final CargoDependencyTransformer cargoTreeParser; // New parser for cargo tree output
    private final CargoTomlParser cargoTomlParser; // New parser for cargo tree output

    public CargoCliExtractor(DetectableExecutableRunner executableRunner, CargoDependencyTransformer cargoTreeParser, CargoTomlParser cargoTomlParser) {
        this.executableRunner = executableRunner;
        this.cargoTreeParser = cargoTreeParser;
        this.cargoTomlParser = cargoTomlParser;
    }

    public Extraction extract(File directory, ExecutableTarget cargoExe, File cargoTomlFile) throws ExecutableFailedException, IOException {
        List<String> commandArguments = Arrays.asList("tree", "--no-dedupe", "--prefix", "depth");
        ExecutableOutput cargoOutput = executableRunner.executeSuccessfully(ExecutableUtils.createFromTarget(directory, cargoExe, commandArguments));
        List<String> cargoTreeOutput = cargoOutput.getStandardOutputAsList();

        DependencyGraph graph = cargoTreeParser.transform(cargoTreeOutput);

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
