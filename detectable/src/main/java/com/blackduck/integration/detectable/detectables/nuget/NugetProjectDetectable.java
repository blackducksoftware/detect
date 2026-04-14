package com.blackduck.integration.detectable.detectables.nuget;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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

@DetectableInfo(name = "NuGet Project Native Inspector", language = "C#", forge = "NuGet.org", accuracy = DetectableAccuracyType.HIGH,
    requirementsMarkdown = "File: a project file with one of the following extensions: .csproj, .fsproj, .vbproj, .asaproj, .dcproj, .shproj, .ccproj, " +
        ".sfproj, .njsproj, .vcxproj, .vcproj, .xproj, .pyproj, .hiveproj, .pigproj, .jsproj, .usqlproj, .deployproj, " +
        ".msbuildproj, .sqlproj, .dbproj, .rproj")
public class NugetProjectDetectable extends Detectable {
    static final List<String> SUPPORTED_PROJECT_PATTERNS = Arrays.asList(
        // C#
        "*.csproj",
        // F#
        "*.fsproj",
        // VB
        "*.vbproj",
        // Azure Stream Analytics
        "*.asaproj",
        // Docker Compose
        "*.dcproj",
        // Shared Projects
        "*.shproj",
        // Cloud Computing
        "*.ccproj",
        // Fabric Application
        "*.sfproj",
        // Node.js
        "*.njsproj",
        // VC++
        "*.vcxproj",
        // VC++
        "*.vcproj",
        // .NET Core
        "*.xproj",
        // Python
        "*.pyproj",
        // Hive
        "*.hiveproj",
        // Pig
        "*.pigproj",
        // JavaScript
        "*.jsproj",
        // U-SQL
        "*.usqlproj",
        // Deployment
        "*.deployproj",
        // Common Project System Files
        "*.msbuildproj",
        // SQL
        "*.sqlproj",
        // SQL Project Files
        "*.dbproj",
        // RStudio
        "*.rproj"
    );

    private final FileFinder fileFinder;
    private final NugetInspectorOptions nugetInspectorOptions;
    private final NugetInspectorResolver nugetInspectorResolver;
    private final NugetInspectorExtractor nugetInspectorExtractor;

    private ExecutableTarget inspector;
    private List<File> projectFiles = new ArrayList<>();

    public NugetProjectDetectable(
        DetectableEnvironment detectableEnvironment,
        FileFinder fileFinder,
        NugetInspectorOptions nugetInspectorOptions,
        NugetInspectorResolver nugetInspectorResolver,
        NugetInspectorExtractor nugetInspectorExtractor
    ) {
        super(detectableEnvironment);
        this.fileFinder = fileFinder;
        this.nugetInspectorOptions = nugetInspectorOptions;
        this.nugetInspectorResolver = nugetInspectorResolver;
        this.nugetInspectorExtractor = nugetInspectorExtractor;
    }

    @Override
    public DetectableResult applicable() {
        projectFiles = fileFinder.findFiles(environment.getDirectory(), SUPPORTED_PROJECT_PATTERNS);

        if (projectFiles != null && projectFiles.size() > 0) {
            PassedResultBuilder passedResultBuilder = new PassedResultBuilder();
            projectFiles.forEach(passedResultBuilder::foundFile);
            return passedResultBuilder.build();
        } else {
            return new FilesNotFoundDetectableResult(SUPPORTED_PROJECT_PATTERNS);
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
        return nugetInspectorExtractor.extract(projectFiles, extractionEnvironment.getOutputDirectory(), inspector, nugetInspectorOptions);
    }

}