package com.blackduck.integration.detectable.detectables.cran;

import java.io.File;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.Requirements;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectable.result.PassedDetectableResult;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;

@DetectableInfo(name = "Packrat Lock", language = "R", forge = "CRAN", accuracy = DetectableAccuracyType.HIGH, requirementsMarkdown = "File: packrat.lock.")
public class PackratLockDetectable extends Detectable {
    public static final String PACKRATLOCK_FILE_NAME = "packrat.lock";

    private final FileFinder fileFinder;
    private final PackratLockExtractor packratLockExtractor;

    private File packratLockFile;

    public PackratLockDetectable(DetectableEnvironment environment, FileFinder fileFinder, PackratLockExtractor packratLockExtractor) {
        super(environment);
        this.fileFinder = fileFinder;
        this.packratLockExtractor = packratLockExtractor;
    }

    @Override
    public DetectableResult applicable() {
        Requirements requirements = new Requirements(fileFinder, environment);
        packratLockFile = requirements.file(PACKRATLOCK_FILE_NAME);
        return requirements.result();
    }

    @Override
    public DetectableResult extractable() {
        return new PassedDetectableResult();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) {
        return packratLockExtractor.extract(environment.getDirectory(), packratLockFile);
    }

}
