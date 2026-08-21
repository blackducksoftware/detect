package com.blackduck.integration.detectable.detectables.bun.lockfile;

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

@DetectableInfo(name = "Bun Lockfile", language = "Node JS", forge = "npmjs", accuracy = DetectableAccuracyType.HIGH,
    requirementsMarkdown = "Files: package.json and bun.lock.")
public class BunLockfileDetectable extends Detectable {
    public static final String BUN_LOCK_FILENAME = "bun.lock";
    public static final String PACKAGE_JSON_FILENAME = "package.json";

    private final FileFinder fileFinder;
    private final BunLockfileExtractor bunLockfileExtractor;

    private File packageJson;
    private File bunLockFile;

    public BunLockfileDetectable(DetectableEnvironment environment, FileFinder fileFinder, BunLockfileExtractor bunLockfileExtractor) {
        super(environment);
        this.fileFinder = fileFinder;
        this.bunLockfileExtractor = bunLockfileExtractor;
    }

    @Override
    public DetectableResult applicable() {
        Requirements requirements = new Requirements(fileFinder, environment);
        packageJson = requirements.file(PACKAGE_JSON_FILENAME);
        bunLockFile = requirements.file(BUN_LOCK_FILENAME);
        return requirements.result();
    }

    @Override
    public DetectableResult extractable() {
        return new PassedDetectableResult();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) {
        return bunLockfileExtractor.extract(bunLockFile, packageJson);
    }
}
