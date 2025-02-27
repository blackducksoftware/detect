package com.blackduck.integration.detectable.detectables.cargo;

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
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectables.cargo.parse.CargoTomlParser;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;
import com.blackduck.integration.detectable.detectable.executable.resolver.CargoResolver;
import com.blackduck.integration.executable.ExecutableRunner;
import com.blackduck.integration.executable.ExecutableRunnerException;

@DetectableInfo(name = "Cargo CLI", language = "Rust", forge = "crates", accuracy = DetectableAccuracyType.HIGH, requirementsMarkdown = "Command: cargo tree")
public class CargoCliDetectable extends Detectable {
    public static final String CARGO_LOCK_FILENAME = "Cargo.lock";
    public static final String CARGO_TOML_FILENAME = "Cargo.toml";

    private final FileFinder fileFinder;

    private final CargoResolver cargoResolver;
    private final ExecutableRunner executableRunner;
    private final CargoExtractor cargoExtractor;
    private final CargoTomlParser cargoTomlParser;
    private ExecutableTarget cargoExe;

//    private File cargoLock;
//    private File cargoToml;

    public CargoCliDetectable(DetectableEnvironment environment, FileFinder fileFinder, CargoResolver cargoResolver, ExecutableRunner executableRunner, CargoExtractor cargoExtractor, CargoTomlParser cargoTomlParser) {
        super(environment);
        this.fileFinder = fileFinder;
        this.cargoResolver = cargoResolver;
        this.executableRunner = executableRunner;
        this.cargoExtractor = cargoExtractor;
        this.cargoTomlParser = cargoTomlParser;
    }

    @Override
    public DetectableResult applicable() {
        Requirements requirements = new Requirements(fileFinder, environment);
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
        return null;
    }
}
