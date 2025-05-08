package com.blackduck.integration.detectable.detectables.cargo;

import java.io.File;
import java.io.IOException;

import com.blackduck.integration.bdio.graph.builder.MissingExternalIdException;
import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.Requirements;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.result.CargoLockfileNotFoundDetectableResult;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectable.result.PassedDetectableResult;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;

//TODO: Rename to Cargo Lock
@DetectableInfo(name = "Cargo Lock", language = "Rust", forge = "crates", accuracy = DetectableAccuracyType.HIGH, requirementsMarkdown = "Files: Cargo.lock, Cargo.toml")
public class CargoLockDetectable extends Detectable {
    public static final String CARGO_LOCK_FILENAME = "Cargo.lock";
    public static final String CARGO_TOML_FILENAME = "Cargo.toml";

    private final FileFinder fileFinder;
    private final CargoExtractor cargoExtractor;
    private final CargoDetectableOptions cargoDetectableOptions;
    private File cargoLock;
    private File cargoToml;

    public CargoLockDetectable(DetectableEnvironment environment, FileFinder fileFinder, CargoExtractor cargoExtractor, CargoDetectableOptions cargoDetectableOptions) {
        super(environment);
        this.fileFinder = fileFinder;
        this.cargoExtractor = cargoExtractor;
        this.cargoDetectableOptions = cargoDetectableOptions;
    }

    @Override
    public DetectableResult applicable() {
        Requirements requirements = new Requirements(fileFinder, environment);
        requirements.eitherFile(CARGO_LOCK_FILENAME, CARGO_TOML_FILENAME, foundLock -> cargoLock = foundLock, foundToml -> cargoToml = foundToml);
        return requirements.result();
    }

    @Override
    public DetectableResult extractable() {
        if (cargoLock == null && cargoToml != null) {
            return new CargoLockfileNotFoundDetectableResult(environment.getDirectory().getAbsolutePath());
        }
        return new PassedDetectableResult();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) throws IOException, DetectableException, MissingExternalIdException {
        return cargoExtractor.extract(cargoLock, cargoToml, cargoDetectableOptions);
    }
}
