package com.blackduck.integration.detectable.detectables.meson;

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

@DetectableInfo(name = "Meson", language = "C/C++", forge = "Generic", accuracy = DetectableAccuracyType.HIGH, requirementsMarkdown = "File: meson.build")
public class MesonDetectable extends Detectable {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public static final String detectable_file = "meson.build";
    private static final String introspect_project = "intro-projectinfo.json";
    private static final String introspect_dependencies = "intro-dependencies.json";

    private final FileFinder fileFinder;
    private final MesonExtractor mesonExtractor;

    public MesonDetectable(DetectableEnvironment environment, FileFinder fileFinder, MesonExtractor mesonExtractor) {
        super(environment);
        this.fileFinder = fileFinder;
        this.mesonExtractor = mesonExtractor;
    }

    @Override
    public DetectableResult applicable() {
        Requirements requirements = new Requirements(fileFinder, environment);
        requirements.file(detectable_file);
        File projectFile = fileFinder.findFile(environment.getDirectory(), introspect_project, false, 2);
        logger.debug("found {}",projectFile);
        File dependencyFile = fileFinder.findFile(environment.getDirectory(), introspect_dependencies, false, 2);
        logger.debug("found {}",dependencyFile);
        if (projectFile != null && dependencyFile != null) {
            return requirements.result();
        }
        logger.debug("even though {} is found, also a builddirectory with the introspect files is needed", detectable_file);
        return new FilesNotFoundDetectableResult();
    }

    @Override
    public DetectableResult extractable() {
        return new PassedDetectableResult();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) {
        return mesonExtractor.extract(environment.getDirectory());
    }
}
