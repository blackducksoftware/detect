package com.blackduck.integration.detectable.detectables.cargo;

import java.io.File;
import java.io.IOException;

import com.blackduck.integration.bdio.graph.builder.MissingExternalIdException;
import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.Requirements;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectables.cargo.parse.CargoTomlParser;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;
import com.blackduck.integration.detectable.detectable.executable.resolver.CargoResolver;
import com.blackduck.integration.executable.ExecutableRunner;
import com.blackduck.integration.executable.ExecutableRunnerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DetectableInfo(name = "Cargo CLI", language = "Rust", forge = "crates", accuracy = DetectableAccuracyType.HIGH, requirementsMarkdown = "Command: cargo tree")
public class CargoCliDetectable extends Detectable {
    private static final Logger logger = LoggerFactory.getLogger(CargoCliDetectable.class);

    public static final String CARGO_LOCK_FILENAME = "Cargo.lock";
    public static final String CARGO_TOML_FILENAME = "Cargo.toml";

    private final FileFinder fileFinder;

    private final CargoResolver cargoResolver;
    private final CargoCliExtractor cargoCliExtractor;
    private ExecutableTarget cargoExe;
    private File cargoToml;

    public CargoCliDetectable(DetectableEnvironment environment, FileFinder fileFinder, CargoResolver cargoResolver, CargoCliExtractor cargoCliExtractor) {
        super(environment);
        this.fileFinder = fileFinder;
        this.cargoResolver = cargoResolver;
        this.cargoCliExtractor = cargoCliExtractor;
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
        return requirements.result();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) throws IOException, DetectableException, MissingExternalIdException, ExecutableRunnerException {
        try {
            return cargoCliExtractor.extract(environment.getDirectory(), cargoExe, cargoToml);
        } catch (Exception e) {
            logger.error("Failed to extract Cargo dependencies.", e);
            return new Extraction.Builder().failure("Cargo extraction failed due to an exception: " + e.getMessage()).build();
        }
    }
}
