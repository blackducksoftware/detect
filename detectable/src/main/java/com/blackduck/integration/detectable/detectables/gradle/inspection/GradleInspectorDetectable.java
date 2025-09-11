package com.blackduck.integration.detectable.detectables.gradle.inspection;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.detectable.executable.resolver.GradleResolver;
import com.blackduck.integration.detectable.detectable.explanation.Explanation;
import com.blackduck.integration.detectable.detectable.explanation.FoundExecutable;
import com.blackduck.integration.detectable.detectable.explanation.FoundFile;
import com.blackduck.integration.detectable.detectable.explanation.FoundInspector;
import com.blackduck.integration.detectable.detectable.inspector.GradleInspectorResolver;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectable.result.ExecutableNotFoundDetectableResult;
import com.blackduck.integration.detectable.detectable.result.FilesNotFoundDetectableResult;
import com.blackduck.integration.detectable.detectable.result.InspectorNotFoundDetectableResult;
import com.blackduck.integration.detectable.detectable.result.PassedDetectableResult;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;

@DetectableInfo(name = "Gradle Native Inspector", language = "various", forge = "Maven Central", accuracy = DetectableAccuracyType.HIGH, requirementsMarkdown = "File: build.gradle, build.gradle.kts, settings.gradle or settings.gradle.kts. Executable: gradlew or gradle.")
public class GradleInspectorDetectable extends Detectable {
    public static final String BUILD_GRADLE_FILENAME = "build.gradle";
    public static final String KOTLIN_BUILD_GRADLE_FILENAME = "build.gradle.kts";
    public static final String SETTINGS_GRADLE_FILENAME = "settings.gradle";
    public static final String KOTLIN_SETTINGS_GRADLE_FILENAME = "settings.gradle.kts";

    private final FileFinder fileFinder;
    private final GradleResolver gradleResolver;
    private final GradleInspectorResolver gradleInspectorResolver;
    private final GradleInspectorExtractor gradleInspectorExtractor;
    private final GradleInspectorOptions gradleInspectorOptions;

    private ExecutableTarget gradleExe;
    private File gradleInspector;

    public GradleInspectorDetectable(
        DetectableEnvironment environment,
        FileFinder fileFinder,
        GradleResolver gradleResolver,
        GradleInspectorResolver gradleInspectorResolver,
        GradleInspectorExtractor gradleInspectorExtractor,
        GradleInspectorOptions gradleInspectorOptions
    ) {
        super(environment);
        this.fileFinder = fileFinder;
        this.gradleResolver = gradleResolver;
        this.gradleInspectorResolver = gradleInspectorResolver;
        this.gradleInspectorExtractor = gradleInspectorExtractor;
        this.gradleInspectorOptions = gradleInspectorOptions;
    }

    @Override
    public DetectableResult applicable() {
        // Legacy check: Look for build files in current directory
        File buildGradle = fileFinder.findFile(environment.getDirectory(), BUILD_GRADLE_FILENAME);
        if (buildGradle != null) {
            return new PassedDetectableResult(new FoundFile(buildGradle));
        }

        File kotlinBuildGradle = fileFinder.findFile(environment.getDirectory(), KOTLIN_BUILD_GRADLE_FILENAME);
        if (kotlinBuildGradle != null) {
            return new PassedDetectableResult(new FoundFile(kotlinBuildGradle));
        }

        // Modern multi-project check: Look for settings files
        File settingsGradle = fileFinder.findFile(environment.getDirectory(), SETTINGS_GRADLE_FILENAME);
        if (settingsGradle != null) {
            return new PassedDetectableResult(new FoundFile(settingsGradle));
        }

        File kotlinSettingsGradle = fileFinder.findFile(environment.getDirectory(), KOTLIN_SETTINGS_GRADLE_FILENAME);
        if (kotlinSettingsGradle != null) {
            return new PassedDetectableResult(new FoundFile(kotlinSettingsGradle));
        }

        return new FilesNotFoundDetectableResult(BUILD_GRADLE_FILENAME, KOTLIN_BUILD_GRADLE_FILENAME, SETTINGS_GRADLE_FILENAME, KOTLIN_SETTINGS_GRADLE_FILENAME);
    }

    @Override
    public DetectableResult extractable() throws DetectableException {
        List<Explanation> explanations = new ArrayList<>();
        gradleExe = gradleResolver.resolveGradle(environment);
        if (gradleExe == null) {
            return new ExecutableNotFoundDetectableResult("gradle");
        } else {
            explanations.add(new FoundExecutable(gradleExe));
        }

        gradleInspector = gradleInspectorResolver.resolveGradleInspector();
        if (gradleInspector == null) {
            return new InspectorNotFoundDetectableResult("gradle");
        } else {
            explanations.add(new FoundInspector(gradleInspector));
        }

        return new PassedDetectableResult(explanations);
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) throws ExecutableFailedException {
        String gradleCommand = gradleInspectorOptions.getGradleBuildCommand().orElse(null);
        return gradleInspectorExtractor.extract(
            environment.getDirectory(),
            gradleExe,
            gradleCommand,
            gradleInspectorOptions.getproxyInfo(),
            gradleInspector,
            extractionEnvironment.getOutputDirectory(),
            gradleInspectorOptions.getGradleInspectorScriptOptions().isRootOnly()
        );
    }
}
