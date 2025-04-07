package com.blackduck.integration.detectable.detectables.uv.buildexe;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.Requirements;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.resolver.UVResolver;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectable.result.ExecutableNotFoundDetectableResult;
import com.blackduck.integration.detectable.detectable.result.FailedDetectableResult;
import com.blackduck.integration.detectable.detectable.result.PassedDetectableResult;
import com.blackduck.integration.detectable.detectables.uv.UVDetectorOptions;
import com.blackduck.integration.detectable.detectables.uv.parse.UVTomlParser;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;
import com.blackduck.integration.executable.ExecutableRunnerException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

@DetectableInfo(name = "UV Build", language = "Python", forge = "PyPi", accuracy = DetectableAccuracyType.HIGH, requirementsMarkdown = "File: pyproject.toml file. Executable: uv.")
public class UVBuildDetectable extends Detectable {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String PYPROJECT_TOML = "pyproject.toml";
    private final FileFinder fileFinder;
    private final UVResolver uvResolver;
    private final UVBuildExtractor uvBuildExtractor;
    private final UVDetectorOptions uvDetectorOptions;
    private File uvTomlFile;
    private final UVTomlParser uvTomlParser;

    private ExecutableTarget uvExe;

    public UVBuildDetectable(DetectableEnvironment environment, FileFinder fileFinder, UVResolver uvResolver, UVBuildExtractor uvBuildExtractor, UVDetectorOptions uvDetectorOptions, UVTomlParser uvTomlParser) {
        super(environment);
        this.fileFinder = fileFinder;
        this.uvResolver = uvResolver;
        this.uvBuildExtractor = uvBuildExtractor;
        this.uvDetectorOptions = uvDetectorOptions;
        this.uvTomlParser = uvTomlParser;
    }

    @Override
    public DetectableResult applicable() {
        Requirements requirements = new Requirements(fileFinder, environment);
        uvTomlFile = requirements.file(PYPROJECT_TOML);
        return requirements.result();
    }

    @Override
    public DetectableResult extractable() throws DetectableException {
        uvExe = uvResolver.resolveUV();

        if(uvExe == null) {
            return new ExecutableNotFoundDetectableResult("uv");
        }

        if(!uvTomlParser.parseManagedKey(uvTomlFile)) {
            logger.warn("Skipping this detectable, since [tool.uv] managed is set to false.");
            return new FailedDetectableResult();
        }

        return new PassedDetectableResult();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) throws ExecutableRunnerException {
        return uvBuildExtractor.extract(uvExe, uvDetectorOptions, uvTomlFile);
    }
}
