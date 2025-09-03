package com.blackduck.integration.detectable.detectables.go.gomodbuildless;

import java.io.File;

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

@DetectableInfo(name = "Go Mod Buildless", language = "Golang", forge = "GitHub", accuracy = DetectableAccuracyType.HIGH, requirementsMarkdown = "File: go.mod.")
public class GoModBuildlessDetectable extends Detectable {
    public static final String GO_MOD_FILENAME = "go.mod";

    private final FileFinder fileFinder;
    private final GoModBuildlessExtractor goModBuildlessExtractor;

    private File goModFile;

    public GoModBuildlessDetectable(DetectableEnvironment environment, FileFinder fileFinder, GoModBuildlessExtractor goModBuildlessExtractor) {
        super(environment);
        this.fileFinder = fileFinder;
        this.goModBuildlessExtractor = goModBuildlessExtractor;
    }

    @Override
    public DetectableResult applicable() {
        Requirements requirements = new Requirements(fileFinder, environment);
        goModFile = requirements.file(GO_MOD_FILENAME);
        return requirements.result();
    }

    @Override
    public DetectableResult extractable() {
        return new PassedDetectableResult();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) {
        return goModBuildlessExtractor.extract(goModFile);
    }

}
