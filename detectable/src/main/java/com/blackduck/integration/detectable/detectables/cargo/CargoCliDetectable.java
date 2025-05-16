package com.blackduck.integration.detectable.detectables.cargo;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.blackduck.integration.bdio.graph.builder.MissingExternalIdException;
import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.Requirements;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectable.result.CargoExecutableVersionMismatchResult;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectable.result.ExecutableNotFoundDetectableResult;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;
import com.blackduck.integration.detectable.detectable.executable.resolver.CargoResolver;
import com.blackduck.integration.executable.ExecutableOutput;
import com.blackduck.integration.executable.ExecutableRunnerException;
import com.blackduck.integration.detectable.ExecutableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DetectableInfo(name = "Cargo CLI", language = "Rust", forge = "crates", accuracy = DetectableAccuracyType.HIGH, requirementsMarkdown = "Command: cargo tree")
public class CargoCliDetectable extends Detectable {
    private static final Logger logger = LoggerFactory.getLogger(CargoCliDetectable.class);
    public static final String CARGO_TOML_FILENAME = "Cargo.toml";
    private static final String MINIMUM_CARGO_VERSION = "1.44.0";
    private final FileFinder fileFinder;
    private final CargoResolver cargoResolver;
    private final CargoCliExtractor cargoCliExtractor;
    private final DetectableExecutableRunner executableRunner;
    private final CargoDetectableOptions cargoDetectableOptions;
    private ExecutableTarget cargoExe;
    private File cargoToml;

    public CargoCliDetectable(DetectableEnvironment environment, FileFinder fileFinder, CargoResolver cargoResolver, CargoCliExtractor cargoCliExtractor, DetectableExecutableRunner executableRunner, CargoDetectableOptions cargoDetectableOptions) {
        super(environment);
        this.fileFinder = fileFinder;
        this.cargoResolver = cargoResolver;
        this.cargoCliExtractor = cargoCliExtractor;
        this.executableRunner = executableRunner;
        this.cargoDetectableOptions = cargoDetectableOptions;
    }

    @Override
    public DetectableResult applicable() {
        Requirements requirements = new Requirements(fileFinder, environment);
        cargoToml = requirements.file(CARGO_TOML_FILENAME);
        return requirements.result();
    }

    @Override
    public DetectableResult extractable() throws DetectableException {
        Requirements requirements = new Requirements(fileFinder, environment);
        cargoExe = requirements.executable(() -> cargoResolver.resolveCargo(environment), "cargo");
        if (cargoExe == null) {
            return new ExecutableNotFoundDetectableResult("cargo");
        }

        if (!isCargoVersionValid(cargoExe)) {
            return new CargoExecutableVersionMismatchResult(environment.getDirectory().getAbsolutePath(), MINIMUM_CARGO_VERSION);
        }

        return requirements.result();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) throws IOException, DetectableException, MissingExternalIdException, ExecutableRunnerException {
        try {
            return cargoCliExtractor.extract(environment.getDirectory(), cargoExe, cargoToml, cargoDetectableOptions);
        } catch (Exception e) {
            logger.error("Failed to extract Cargo dependencies.", e);
            return new Extraction.Builder().failure("Cargo extraction failed due to an exception: " + e.getMessage()).build();
        }
    }

    private boolean isCargoVersionValid(ExecutableTarget cargoExe) {
        try {
            List<String> commandArguments = Collections.singletonList("--version");
            ExecutableOutput cargoOutput = executableRunner.executeSuccessfully(ExecutableUtils.createFromTarget(environment.getDirectory(), cargoExe, commandArguments));
            List<String> cargoVersionOutput = cargoOutput.getStandardOutputAsList();
            if (!cargoVersionOutput.isEmpty()) {
                String version = cargoVersionOutput.get(0).split(" ")[1];
                return VersionUtils.compareVersions(version, MINIMUM_CARGO_VERSION) >= 0;
            }
        } catch (Exception e) {
            logger.error("Failed to get Cargo version.", e);
        }
        return false;
    }
}
