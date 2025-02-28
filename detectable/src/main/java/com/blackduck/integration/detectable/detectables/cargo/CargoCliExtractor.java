package com.blackduck.integration.detectable.detectables.cargo;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.util.ToolVersionLogger;
import com.blackduck.integration.executable.ExecutableOutput;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class CargoCliExtractor {
    private static final Logger logger = LoggerFactory.getLogger(CargoCliExtractor.class.getName());
    private final DetectableExecutableRunner executableRunner;
    private final CargoMetadataParser cargoMetadataParser;
    private final CargoDependencyTransformer cargoDependencyTransformer;

    public CargoCliExtractor(DetectableExecutableRunner executableRunner, CargoMetadataParser cargoMetadataParser, CargoDependencyTransformer cargoDependencyTransformer) {
        this.executableRunner = executableRunner;
        this.cargoMetadataParser = cargoMetadataParser;
        this.cargoDependencyTransformer = cargoDependencyTransformer;
    }

    public Extraction extract(File directory, ExecutableTarget cargoExe) throws ExecutableFailedException {
        List<String> commandArguments = Arrays.asList("metadata", "--format-version=1");
        ExecutableOutput cargoOutput = executableRunner.executeSuccessfully(ExecutableUtils.createFromTarget(directory, cargoExe, commandArguments));
        String cargoMetadataJson = cargoOutput.getStandardOutput();

        JsonObject jsonObject = cargoMetadataParser.parseMetadata(cargoMetadataJson);
        DependencyGraph graph = cargoDependencyTransformer.transform(jsonObject);

        CodeLocation codeLocation = new CodeLocation(graph);
        return new Extraction.Builder()
            .success(codeLocation)
            .build();
    }
}
