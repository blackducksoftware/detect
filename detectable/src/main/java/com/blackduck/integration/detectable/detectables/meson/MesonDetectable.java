package com.blackduck.integration.detectable.detectables.meson;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.Requirements;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectable.result.PassedDetectableResult;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;

@DetectableInfo(name = "meson", language = "C/C++", forge = "N/A", accuracy = DetectableAccuracyType.HIGH, requirementsMarkdown = "File: meson.build")
public class MesonDetectable extends Detectable {
    public static final String detectable_file = "meson.build";
    public static final String introspect_project = "intro-projectinfo.json";
    public static final String introspect_dependencies = "intro-dependencies.json";
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
        return requirements.result();
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
