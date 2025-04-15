package com.blackduck.integration.detectable.detectables.uv.lockfile;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.Requirements;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectable.result.FailedDetectableResult;
import com.blackduck.integration.detectable.detectable.result.PassedDetectableResult;
import com.blackduck.integration.detectable.detectable.result.UVLockfileNotFoundDetectableResult;
import com.blackduck.integration.detectable.detectables.uv.UVDetectorOptions;
import com.blackduck.integration.detectable.detectables.uv.parse.UVTomlParser;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;
import com.blackduck.integration.executable.ExecutableRunnerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.IOException;

@DetectableInfo(name = "UV Lockfile", language = "Python", forge = "PyPi", accuracy = DetectableAccuracyType.HIGH, requirementsMarkdown = "File: pyproject.toml file. Lock File: uv.lock or requirements.txt file.")
public class UVLockFileDetectable extends Detectable {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String PYPROJECT_TOML = "pyproject.toml";
    private static final String UV_LOCK_FILE = "uv.lock";
    private static final String REQUIREMENTS_TXT = "requirements.txt";
    private final FileFinder fileFinder;
    private final UVDetectorOptions uvDetectorOptions;
    private File uvLockFile;
    private File requirementsTxtFile;
    private UVTomlParser uvTomlParser;
    private final UVLockfileExtractor uvLockfileExtractor;

    public UVLockFileDetectable(DetectableEnvironment environment, FileFinder fileFinder, UVDetectorOptions uvDetectorOptions, UVLockfileExtractor uvLockfileExtractor) {
        super(environment);
        this.fileFinder = fileFinder;
        this.uvDetectorOptions = uvDetectorOptions;
        this.uvLockfileExtractor = uvLockfileExtractor;
    }

    @Override
    public DetectableResult applicable() {
        Requirements requirements = new Requirements(fileFinder, environment);
        File uvTomlFile = requirements.file(PYPROJECT_TOML);

        // check [tool.uv] managed setting and if set to false, skip this detector
        if(uvTomlFile != null) {
            uvTomlParser = new UVTomlParser(uvTomlFile);
            if(!uvTomlParser.parseManagedKey()) {
                return new FailedDetectableResult();
            }
        }

        return requirements.result();
    }

    @Override
    public DetectableResult extractable() throws DetectableException {
        Requirements requirements = new Requirements(fileFinder, environment);
        uvLockFile = requirements.file(UV_LOCK_FILE);
        requirementsTxtFile = requirements.file(REQUIREMENTS_TXT);

        if(!uvLockFile.exists() && !requirementsTxtFile.exists()) {
            return new UVLockfileNotFoundDetectableResult(environment.getDirectory().getAbsolutePath());
        }

        return new PassedDetectableResult();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) throws ExecutableRunnerException, IOException {
        return uvLockfileExtractor.extract(uvDetectorOptions, uvTomlParser, uvLockFile, requirementsTxtFile);
    }
}
