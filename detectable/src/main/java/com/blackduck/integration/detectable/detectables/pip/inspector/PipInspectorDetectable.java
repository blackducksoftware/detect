package com.blackduck.integration.detectable.detectables.pip.inspector;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.resolver.PipResolver;
import com.blackduck.integration.detectable.detectable.executable.resolver.PythonResolver;
import com.blackduck.integration.detectable.detectable.inspector.PipInspectorResolver;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectable.result.ExecutableNotFoundDetectableResult;
import com.blackduck.integration.detectable.detectable.result.FilesNotFoundDetectableResult;
import com.blackduck.integration.detectable.detectable.result.InspectorNotFoundDetectableResult;
import com.blackduck.integration.detectable.detectable.result.PassedDetectableResult;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;

@DetectableInfo(name = "PIP Native Inspector", language = "Python", forge = "Pypi", accuracy = DetectableAccuracyType.HIGH, requirementsMarkdown = "A setup.py file, or pyproject.toml file, or one or more requirements.txt files. Executables: python and pip, or python3 and pip3.")
public class PipInspectorDetectable extends Detectable {
    private static final String SETUPTOOLS_DEFAULT_FILE_NAME = "setup.py";
    private static final String REQUIREMENTS_DEFAULT_FILE_NAME = "requirements.txt";
    private static final String PYPROJECT_DEFAULT_FILE_NAME = "pyproject.toml";

    private final FileFinder fileFinder;
    private final PythonResolver pythonResolver;
    private final PipResolver pipResolver;
    private final PipInspectorResolver pipInspectorResolver;
    private final PipInspectorExtractor pipInspectorExtractor;
    private final PipInspectorDetectableOptions pipInspectorDetectableOptions;

    private ExecutableTarget pythonExe;
    private ExecutableTarget pipExe;
    private File pipInspector;
    private File setupFile;
    private List<Path> requirementsFiles;
    private File pyprojectToml;

    public PipInspectorDetectable(
        DetectableEnvironment environment,
        FileFinder fileFinder,
        PythonResolver pythonResolver,
        PipResolver pipResolver,
        PipInspectorResolver pipInspectorResolver,
        PipInspectorExtractor pipInspectorExtractor,
        PipInspectorDetectableOptions pipInspectorDetectableOptions
    ) {
        super(environment);
        this.fileFinder = fileFinder;
        this.pythonResolver = pythonResolver;
        this.pipResolver = pipResolver;
        this.pipInspectorResolver = pipInspectorResolver;
        this.pipInspectorExtractor = pipInspectorExtractor;
        this.pipInspectorDetectableOptions = pipInspectorDetectableOptions;
    }

    @Override
    public DetectableResult applicable() {
        setupFile = fileFinder.findFile(environment.getDirectory(), SETUPTOOLS_DEFAULT_FILE_NAME);
        boolean hasSetups = setupFile != null;

        pyprojectToml = fileFinder.findFile(environment.getDirectory(), PYPROJECT_DEFAULT_FILE_NAME);
        boolean hasPyprojectToml = pyprojectToml != null;

        requirementsFiles = pipInspectorDetectableOptions.getRequirementsFilePaths();
        if (CollectionUtils.isEmpty(pipInspectorDetectableOptions.getRequirementsFilePaths())) {
            requirementsFiles = fileFinder.findFiles(environment.getDirectory(), REQUIREMENTS_DEFAULT_FILE_NAME)
                .stream()
                .map(File::toPath)
                .collect(Collectors.toList());
        }
        boolean hasRequirements = CollectionUtils.isNotEmpty(requirementsFiles);

        if (hasSetups || hasRequirements || hasPyprojectToml) {
            return new PassedDetectableResult();
        } else {
            return new FilesNotFoundDetectableResult(SETUPTOOLS_DEFAULT_FILE_NAME, REQUIREMENTS_DEFAULT_FILE_NAME);
        }
    }

    @Override
    public DetectableResult extractable() throws DetectableException {
        pythonExe = pythonResolver.resolvePython();
        if (pythonExe == null) {
            return new ExecutableNotFoundDetectableResult("python");
        }

        pipExe = pipResolver.resolvePip();
        if (pipExe == null) {
            return new ExecutableNotFoundDetectableResult("pip");
        }

        pipInspector = pipInspectorResolver.resolvePipInspector();
        if (pipInspector == null) {
            return new InspectorNotFoundDetectableResult("pip-inspector.py");
        }

        return new PassedDetectableResult();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) {
        //TODO: Handle null better.
        return pipInspectorExtractor.extract(
            environment.getDirectory(),
            pythonExe,
            pipExe,
            pipInspector,
            setupFile,
            requirementsFiles,
            pipInspectorDetectableOptions.getPipProjectName().orElse(""),
            pyprojectToml
        );
    }
}
