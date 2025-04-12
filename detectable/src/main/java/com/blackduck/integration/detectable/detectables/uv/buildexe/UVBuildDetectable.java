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
import org.tomlj.internal.TomlParser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

@DetectableInfo(name = "UV Build", language = "Python", forge = "PyPI", accuracy = DetectableAccuracyType.HIGH, requirementsMarkdown = "File: pyproject.toml file. Executable: uv.")
public class UVBuildDetectable extends Detectable {

    private static final String PYPROJECT_TOML = "pyproject.toml";
    private final FileFinder fileFinder;
    private final UVResolver uvResolver;
    private final UVBuildExtractor uvBuildExtractor;
    private final UVDetectorOptions uvDetectorOptions;
    private UVTomlParser uvTomlParser;

    private ExecutableTarget uvExe;

    public UVBuildDetectable(DetectableEnvironment environment, FileFinder fileFinder, UVResolver uvResolver, UVBuildExtractor uvBuildExtractor, UVDetectorOptions uvDetectorOptions) {
        super(environment);
        this.fileFinder = fileFinder;
        this.uvResolver = uvResolver;
        this.uvBuildExtractor = uvBuildExtractor;
        this.uvDetectorOptions = uvDetectorOptions;
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
        uvExe = uvResolver.resolveUV();

        if(uvExe == null) {
            return new ExecutableNotFoundDetectableResult("uv");
        }

        return new PassedDetectableResult();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) throws ExecutableRunnerException {
        return uvBuildExtractor.extract(uvExe, uvDetectorOptions, uvTomlParser);
    }
}
