package com.blackduck.integration.detectable.detectables.maven.resolver;

import java.io.File;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.executable.resolver.MavenResolver;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectable.result.FileNotFoundDetectableResult;
import com.blackduck.integration.detectable.detectable.result.PassedDetectableResult;
import com.blackduck.integration.detectable.detectables.maven.cli.MavenCliExtractor;
import com.blackduck.integration.detectable.detectables.maven.cli.MavenCliExtractorOptions;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;

@DetectableInfo(
        name = "Maven Resolver",
        language = "Java",
        forge = "Maven Central",
        accuracy = DetectableAccuracyType.HIGH,
        requirementsMarkdown = "File: pom.xml"
)
public class MavenResolverDetectable extends Detectable {

    private static final String POM_XML_FILENAME = "pom.xml";

    private final FileFinder fileFinder;
    private final MavenResolver mavenResolver;
    private final MavenCliExtractor mavenCliExtractor;
    private final MavenCliExtractorOptions mavenCliExtractorOptions;
    private final Detectable projectInspector;

    private File pomFile;

    public MavenResolverDetectable(
            DetectableEnvironment environment,
            FileFinder fileFinder,
            MavenResolver mavenResolver,
            MavenCliExtractor mavenCliExtractor,
            MavenCliExtractorOptions mavenCliExtractorOptions,
            Detectable projectInspector
    ) {
        super(environment);
        this.fileFinder = fileFinder;
        this.mavenResolver = mavenResolver;
        this.mavenCliExtractor = mavenCliExtractor;
        this.mavenCliExtractorOptions = mavenCliExtractorOptions;
        this.projectInspector = projectInspector;
    }

    @Override
    public DetectableResult applicable() {
        pomFile = fileFinder.findFile(environment.getDirectory(), POM_XML_FILENAME);
        if (pomFile == null) {
            return new FileNotFoundDetectableResult(POM_XML_FILENAME);
        }
        return new PassedDetectableResult();
    }

    @Override
    public DetectableResult extractable() {
        // For now, we are keeping this simple. We can add more checks later,
        // for example, to ensure network connectivity or resolver-specific prerequisites.
        return new PassedDetectableResult();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) {
        // This is the placeholder for the future Maven Resolver logic.
        // For now, it returns a success with no results, allowing the wiring to be tested.
        return new Extraction.Builder().success().build();
    }
}
