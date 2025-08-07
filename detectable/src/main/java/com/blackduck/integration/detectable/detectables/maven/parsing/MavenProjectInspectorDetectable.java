package com.blackduck.integration.detectable.detectables.maven.parsing;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.Requirements;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.detectable.inspector.ProjectInspectorResolver;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectables.projectinspector.ProjectInspectorExtractor;
import com.blackduck.integration.detectable.detectables.projectinspector.ProjectInspectorOptions;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;

@DetectableInfo(name = "Maven Project Inspector", language = "various", forge = "Maven Central", accuracy = DetectableAccuracyType.LOW, requirementsMarkdown = "File: pom.xml.")
public class MavenProjectInspectorDetectable extends Detectable {
    private static final String POM_XML_FILENAME = "pom.xml";
    private static final String INCLUDE_SHADED_DEPENDENCIES = "include_shaded_dependencies";
    private final FileFinder fileFinder;
    private final ProjectInspectorResolver projectInspectorResolver;
    private final ProjectInspectorExtractor projectInspectorExtractor;
    private final ProjectInspectorOptions projectInspectorOptions; // TODO: Options don't belong here
    private boolean includeShadedDependencies = false;


    private ExecutableTarget inspector;

    public MavenProjectInspectorDetectable(
        DetectableEnvironment detectableEnvironment,
        FileFinder fileFinder,
        ProjectInspectorResolver projectInspectorResolver,
        ProjectInspectorExtractor projectInspectorExtractor,
        ProjectInspectorOptions projectInspectorOptions
    ) {
        super(detectableEnvironment);
        this.fileFinder = fileFinder;
        this.projectInspectorResolver = projectInspectorResolver;
        this.projectInspectorExtractor = projectInspectorExtractor;
        this.projectInspectorOptions = projectInspectorOptions;
    }

    @Override
    public DetectableResult applicable() {
        Requirements requirements = new Requirements(fileFinder, environment);
        requirements.file(POM_XML_FILENAME);
        return requirements.result();
    }

    @Override
    public DetectableResult extractable() throws DetectableException {
        Requirements requirements = new Requirements(fileFinder, environment);
        inspector = requirements.executable(projectInspectorResolver::resolveProjectInspector, "Project Inspector");
        return requirements.result();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) throws ExecutableFailedException, IOException {
        return projectInspectorExtractor.extract(
            projectInspectorOptions,
            includeShadedDependencies ? Collections.singletonList(INCLUDE_SHADED_DEPENDENCIES) : Collections.emptyList(),
            environment.getDirectory(),
            extractionEnvironment.getOutputDirectory(),
            inspector
        );
    }

    public void setIncludeShadedDependencies(boolean includeShadedDependencies) {
        this.includeShadedDependencies = includeShadedDependencies;
    }

    public Map<String, Set<String>> getShadedDependencies() {
        return projectInspectorExtractor.getShadedDependencies();
    }

}