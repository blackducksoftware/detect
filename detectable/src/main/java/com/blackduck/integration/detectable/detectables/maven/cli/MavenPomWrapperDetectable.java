package com.blackduck.integration.detectable.detectables.maven.cli;

import java.io.File;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.detectable.executable.resolver.MavenResolver;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectable.result.ExecutableNotFoundDetectableResult;
import com.blackduck.integration.detectable.detectable.result.FileNotFoundDetectableResult;
import com.blackduck.integration.detectable.detectable.result.PassedDetectableResult;
import com.blackduck.integration.detectable.detectables.maven.parsing.MavenProjectInspectorDetectable;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;

@DetectableInfo(name = "Maven Wrapper CLI", language = "various", forge = "Maven Central", accuracy = DetectableAccuracyType.HIGH, requirementsMarkdown = "File: pom.groovy. Executable: mvnw or mvn.")
public class MavenPomWrapperDetectable extends Detectable {
    public static final String POM_WRAPPER_FILENAME = "pom.groovy";

    private final FileFinder fileFinder;
    private final MavenResolver mavenResolver;
    private final MavenCliExtractor mavenCliExtractor;
    private final MavenCliExtractorOptions mavenCliExtractorOptions;
    private final MavenProjectInspectorDetectable mavenProjectInspectorDetectable;

    private ExecutableTarget mavenExe;

    public MavenPomWrapperDetectable(
        DetectableEnvironment environment,
        FileFinder fileFinder,
        MavenResolver mavenResolver,
        MavenCliExtractor mavenCliExtractor,
        MavenCliExtractorOptions mavenCliExtractorOptions,
        MavenProjectInspectorDetectable mavenProjectInspectorDetectable
    ) {
        super(environment);
        this.fileFinder = fileFinder;
        this.mavenResolver = mavenResolver;
        this.mavenCliExtractor = mavenCliExtractor;
        this.mavenCliExtractorOptions = mavenCliExtractorOptions; //TODO: Should this be wrapped in a detectable options? - jp
        this.mavenProjectInspectorDetectable = mavenProjectInspectorDetectable;
    }

    @Override
    public DetectableResult applicable() {
        File pom = fileFinder.findFile(environment.getDirectory(), POM_WRAPPER_FILENAME);

        if (pom == null) {
            return new FileNotFoundDetectableResult(POM_WRAPPER_FILENAME);
        }

        return new PassedDetectableResult();
    }

    @Override
    public DetectableResult extractable() throws DetectableException {
        mavenExe = mavenResolver.resolveMaven(environment);

        if (mavenExe == null) {
            return new ExecutableNotFoundDetectableResult("mvn");
        }

        return new PassedDetectableResult();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) throws ExecutableFailedException {
        return mavenCliExtractor.extract(environment.getDirectory(), mavenExe, mavenCliExtractorOptions, mavenProjectInspectorDetectable, extractionEnvironment, null);
    }

}
