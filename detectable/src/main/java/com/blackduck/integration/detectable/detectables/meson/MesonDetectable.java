package com.blackduck.integration.detectable.detectables.meson;

import java.io.File;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.Requirements;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectable.result.FilesNotFoundDetectableResult;
import com.blackduck.integration.detectable.detectable.result.PassedDetectableResult;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;

@DetectableInfo(name = "Meson", language = "C/C++", forge = "conan", accuracy = DetectableAccuracyType.HIGH, requirementsMarkdown = "Files: meson.build, intro-projectinfo.json, intro-dependencies.json")
public class MesonDetectable extends Detectable {
    public static final String MESON_BUILD_FILENAME = "meson.build";
    private static final String INTROSPECT_PROJECT_FILENAME = "intro-projectinfo.json";
    private static final String INTROSPECT_DEPENDENCIES_FILENAME = "intro-dependencies.json";

    private final FileFinder fileFinder;
    private final MesonExtractor mesonExtractor;

    private File projectInfoFile;
    private File dependenciesFile;

    public MesonDetectable(DetectableEnvironment environment, FileFinder fileFinder, MesonExtractor mesonExtractor) {
        super(environment);
        this.fileFinder = fileFinder;
        this.mesonExtractor = mesonExtractor;
    }

    @Override
    public DetectableResult applicable() {
        Requirements requirements = new Requirements(fileFinder, environment);
        requirements.file(MESON_BUILD_FILENAME);
        if (requirements.isAlreadyFailed()) {
            return requirements.result();
        }
        projectInfoFile = fileFinder.findFile(environment.getDirectory(), INTROSPECT_PROJECT_FILENAME, false, 2);
        dependenciesFile = fileFinder.findFile(environment.getDirectory(), INTROSPECT_DEPENDENCIES_FILENAME, false, 2);
        if (projectInfoFile == null || dependenciesFile == null) {
            return new FilesNotFoundDetectableResult(INTROSPECT_PROJECT_FILENAME, INTROSPECT_DEPENDENCIES_FILENAME);
        }
        requirements.explainFile(projectInfoFile);
        requirements.explainFile(dependenciesFile);
        return requirements.result();
    }

    @Override
    public DetectableResult extractable() {
        return new PassedDetectableResult();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) {
        return mesonExtractor.extract(projectInfoFile, dependenciesFile);
    }
}
