package com.blackduck.integration.detectable.detectables.poetry;

import java.io.File;
import java.util.Set;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectable.result.FileNotFoundDetectableResult;
import com.blackduck.integration.detectable.detectable.result.FilesNotFoundDetectableResult;
import com.blackduck.integration.detectable.detectable.result.PassedDetectableResult;
import com.blackduck.integration.detectable.detectable.result.PoetryLockfileNotFoundDetectableResult;
import com.blackduck.integration.detectable.detectable.result.SectionNotFoundDetectableResult;
import com.blackduck.integration.detectable.detectables.poetry.parser.ToolPoetrySectionParser;
import com.blackduck.integration.detectable.detectables.poetry.parser.ToolPoetrySectionResult;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;

@DetectableInfo(name = "Poetry Lock", language = "Python", forge = "pypi", accuracy = DetectableAccuracyType.HIGH, requirementsMarkdown = "Files: Poetry.lock, pyproject.toml")
public class PoetryDetectable extends Detectable {
    private static final String PYPROJECT_TOML_FILE_NAME = "pyproject.toml";
    private static final String POETRY_LOCK = "poetry.lock";

    private final FileFinder fileFinder;
    private final PoetryExtractor poetryExtractor;
    private final ToolPoetrySectionParser poetrySectionParser;
    private final PoetryOptions poetryOptions;

    private File pyprojectToml;
    private File poetryLock;
    private ToolPoetrySectionResult toolPoetrySectionResult;

    public PoetryDetectable(DetectableEnvironment environment, FileFinder fileFinder, PoetryExtractor poetryExtractor, ToolPoetrySectionParser tomlPoetrySectionParser, PoetryOptions options) {
        super(environment);
        this.fileFinder = fileFinder;
        this.poetryExtractor = poetryExtractor;
        this.toolPoetrySectionResult = null;
        this.poetrySectionParser = tomlPoetrySectionParser;
        this.poetryOptions = options;
    }

    @Override
    public DetectableResult applicable() {
        poetryLock = fileFinder.findFile(environment.getDirectory(), POETRY_LOCK);
        pyprojectToml = fileFinder.findFile(environment.getDirectory(), PYPROJECT_TOML_FILE_NAME);
        if (poetryLock == null && pyprojectToml == null) {
            return new FilesNotFoundDetectableResult(PYPROJECT_TOML_FILE_NAME, POETRY_LOCK);
        }

        toolPoetrySectionResult = poetrySectionParser.parseToolPoetrySection(pyprojectToml);

        if (poetryLock == null && !toolPoetrySectionResult.wasFound()) {
            return new SectionNotFoundDetectableResult(pyprojectToml.getName(), ToolPoetrySectionParser.TOOL_POETRY_KEY);
        }
        return new PassedDetectableResult();
    }

    @Override
    public DetectableResult extractable() {
        if (poetryLock == null && pyprojectToml != null) {
            return new PoetryLockfileNotFoundDetectableResult(environment.getDirectory().getAbsolutePath());
        }
        
        if (!poetryOptions.getExcludedGroups().isEmpty() && pyprojectToml == null) {
            return new FileNotFoundDetectableResult(PYPROJECT_TOML_FILE_NAME);
        }

        return new PassedDetectableResult();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) {
        Set<String> rootPackages = poetrySectionParser.parseRootPackages(pyprojectToml, poetryOptions);
        return poetryExtractor.extract(
            poetryLock,
            toolPoetrySectionResult.getToolPoetrySection().orElse(null),
            poetrySectionParser.parseProjectSection(pyprojectToml),
            rootPackages
        );
    }
}
