package com.blackduck.integration.detectable.detectables.pipenv.tbuild;

import java.io.File;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.PassedResultBuilder;
import com.blackduck.integration.detectable.detectable.Requirements;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.resolver.PipenvResolver;
import com.blackduck.integration.detectable.detectable.executable.resolver.PythonResolver;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectable.result.FilesNotFoundDetectableResult;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;

@DetectableInfo(name = "Pipenv CLI", language = "Python", forge = "PyPi", accuracy = DetectableAccuracyType.HIGH, requirementsMarkdown = "Files: Pipfile or Pipfile.lock. Executables: python or python3, and pipenv.")
public class PipenvDetectable extends Detectable {
    public static final String SETUPTOOLS_DEFAULT_FILE_NAME = "setup.py";
    public static final String PIPFILE_FILE_NAME = "Pipfile";
    public static final String PIPFILE_DOT_LOCK_FILE_NAME = "Pipfile.lock";

    private final PipenvDetectableOptions pipenvDetectableOptions;
    private final FileFinder fileFinder;
    private final PythonResolver pythonResolver;
    private final PipenvResolver pipenvResolver;
    private final PipenvExtractor pipenvExtractor;

    private ExecutableTarget pythonExe;
    private ExecutableTarget pipenvExe;
    private File setupFile;

    public PipenvDetectable(
        DetectableEnvironment environment,
        PipenvDetectableOptions pipenvDetectableOptions,
        FileFinder fileFinder,
        PythonResolver pythonResolver,
        PipenvResolver pipenvResolver,
        PipenvExtractor pipenvExtractor
    ) {
        super(environment);
        this.pipenvDetectableOptions = pipenvDetectableOptions;
        this.fileFinder = fileFinder;
        this.pipenvResolver = pipenvResolver;
        this.pipenvExtractor = pipenvExtractor;
        this.pythonResolver = pythonResolver;
    }

    @Override
    public DetectableResult applicable() {
        File pipfile = fileFinder.findFile(environment.getDirectory(), PIPFILE_FILE_NAME);
        File pipfileDotLock = fileFinder.findFile(environment.getDirectory(), PIPFILE_DOT_LOCK_FILE_NAME);

        if (pipfile != null || pipfileDotLock != null) {
            PassedResultBuilder passedResultBuilder = new PassedResultBuilder();
            passedResultBuilder.foundNullableFile(pipfile);
            passedResultBuilder.foundNullableFile(pipfileDotLock);
            return passedResultBuilder.build();
        } else {
            return new FilesNotFoundDetectableResult(PIPFILE_FILE_NAME, PIPFILE_DOT_LOCK_FILE_NAME);
        }

    }

    @Override
    public DetectableResult extractable() throws DetectableException {
        Requirements requirements = new Requirements(fileFinder, environment);
        pythonExe = requirements.executable(pythonResolver::resolvePython, "python");
        pipenvExe = requirements.executable(pipenvResolver::resolvePipenv, "pipenv");

        setupFile = fileFinder.findFile(environment.getDirectory(), SETUPTOOLS_DEFAULT_FILE_NAME);
        requirements.explainNullableFile(setupFile);

        return requirements.result();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) {
        //TODO: Handle null better.
        return pipenvExtractor.extract(
            environment.getDirectory(),
            pythonExe,
            pipenvExe,
            setupFile,
            pipenvDetectableOptions.getPipProjectName().orElse(""),
            pipenvDetectableOptions.getPipProjectVersionName().orElse(""),
            pipenvDetectableOptions.isPipProjectTreeOnly()
        );
    }

}

