package com.blackduck.integration.detectable.detectables.npm.lockfile;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

@DetectableInfo(name = "NPM Shrinkwrap", language = "Node JS", forge = "npmjs", accuracy = DetectableAccuracyType.HIGH, requirementsMarkdown = "File: npm-shrinkwrap.json. Optionally for better results: package.json also.")
public class NpmShrinkwrapDetectable extends Detectable {
    public static final String SHRINKWRAP_JSON = "npm-shrinkwrap.json";
    public static final String PACKAGE_JSON = "package.json";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final FileFinder fileFinder;
    private final NpmLockfileExtractor npmLockfileExtractor;

    private File lockfile;
    private File packageJson;

    public NpmShrinkwrapDetectable(DetectableEnvironment environment, FileFinder fileFinder, NpmLockfileExtractor npmLockfileExtractor) {
        super(environment);
        this.fileFinder = fileFinder;
        this.npmLockfileExtractor = npmLockfileExtractor;
    }

    @Override
    public DetectableResult applicable() {
        Requirements requirements = new Requirements(fileFinder, environment);
        lockfile = requirements.file(SHRINKWRAP_JSON);
        packageJson = requirements.optionalFile(PACKAGE_JSON, () -> logger.warn("Npm applied but it could not find a package.json so dependencies may not be entirely accurate."));
        return requirements.result();
    }

    @Override
    public DetectableResult extractable() {
        return new PassedDetectableResult();
    }

    @Override
    public Extraction extract(ExtractionEnvironment environment) {
        return npmLockfileExtractor.extract(lockfile, packageJson);
    }

}
