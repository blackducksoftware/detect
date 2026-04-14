package com.blackduck.integration.detectable.detectables.nuget;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.PassedResultBuilder;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.explanation.FoundInspector;
import com.blackduck.integration.detectable.detectable.inspector.nuget.NugetInspectorResolver;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectable.result.FilesNotFoundDetectableResult;
import com.blackduck.integration.detectable.detectable.result.InspectorNotFoundDetectableResult;
import com.blackduck.integration.detectable.detectable.result.PassedDetectableResult;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;

@DetectableInfo(name = "NuGet Solution Native Inspector", language = "C#", forge = "NuGet.org", accuracy = DetectableAccuracyType.HIGH, requirementsMarkdown = "File: a solution file with a .sln extension.")
public class NugetSolutionDetectable extends Detectable {
    private static final List<String> SUPPORTED_SOLUTION_PATTERNS = Collections.singletonList("*.sln");

    private final FileFinder fileFinder;
    private final NugetInspectorResolver nugetInspectorResolver;
    private final NugetInspectorExtractor nugetInspectorExtractor;

    private final NugetInspectorOptions nugetInspectorOptions;
    private ExecutableTarget inspector;
    private List<File> solutionFiles = new ArrayList<>();

    public NugetSolutionDetectable(
        DetectableEnvironment environment,
        FileFinder fileFinder,
        NugetInspectorResolver nugetInspectorManager,
        NugetInspectorExtractor nugetInspectorExtractor,
        NugetInspectorOptions nugetInspectorOptions
    ) {
        super(environment);
        this.fileFinder = fileFinder;
        this.nugetInspectorExtractor = nugetInspectorExtractor;
        this.nugetInspectorResolver = nugetInspectorManager;
        this.nugetInspectorOptions = nugetInspectorOptions;
    }

    @Override
    public DetectableResult applicable() {
        solutionFiles = fileFinder.findFiles(environment.getDirectory(), SUPPORTED_SOLUTION_PATTERNS);

        if (solutionFiles != null && solutionFiles.size() > 0) {
            PassedResultBuilder passedResultBuilder = new PassedResultBuilder();
            solutionFiles.forEach(passedResultBuilder::foundFile);
            return passedResultBuilder.build();
        } else {
            return new FilesNotFoundDetectableResult(SUPPORTED_SOLUTION_PATTERNS);
        }
    }

    @Override
    public DetectableResult extractable() throws DetectableException {
        inspector = nugetInspectorResolver.resolveNugetInspector(nugetInspectorOptions);

        if (inspector == null) {
            return new InspectorNotFoundDetectableResult("nuget");
        }

        return new PassedDetectableResult(new FoundInspector(inspector)); //TODO: Inspector should describe itself.
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) {
        File outputDirectory = extractionEnvironment.getOutputDirectory();
        return nugetInspectorExtractor.extract(solutionFiles, outputDirectory, inspector, nugetInspectorOptions);
    }

}
