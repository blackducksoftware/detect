package com.blackduck.integration.detectable.detectables.ivy;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.Requirements;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.resolver.AntResolver;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectable.result.IvyDependencyTreeNotFoundDetectableResult;
import com.blackduck.integration.detectable.detectable.result.PassedDetectableResult;
import com.blackduck.integration.detectable.detectables.ivy.parse.IvyDependencyTreeTargetParser;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;

@DetectableInfo(
    name = "Ivy CLI",
    language = "various",
    forge = "Maven Central",
    accuracy = DetectableAccuracyType.HIGH,
    requirementsMarkdown = "Files: ivy.xml, build.xml with ivy:dependencytree task. Executable: ant. Ant 1.6.0+ and Ivy 2.4.0+."
)
public class IvyCliDetectable extends Detectable {
    private static final String IVY_XML_FILENAME = "ivy.xml";
    private static final String BUILD_XML_FILENAME = "build.xml";

    private final FileFinder fileFinder;
    private final AntResolver antResolver;
    private final IvyCliExtractor ivyCliExtractor;
    private final IvyDependencyTreeTargetParser dependencyTreeTargetParser;

    private File ivyXmlFile;
    @Nullable
    private File buildXml;
    private ExecutableTarget antExe;
    private String targetName;

    public IvyCliDetectable(
        DetectableEnvironment environment,
        FileFinder fileFinder,
        AntResolver antResolver,
        IvyCliExtractor ivyCliExtractor,
        IvyDependencyTreeTargetParser dependencyTreeTargetParser
    ) {
        super(environment);
        this.fileFinder = fileFinder;
        this.antResolver = antResolver;
        this.ivyCliExtractor = ivyCliExtractor;
        this.dependencyTreeTargetParser = dependencyTreeTargetParser;
    }

    @Override
    public DetectableResult applicable() {
        Requirements requirements = new Requirements(fileFinder, environment);
        ivyXmlFile = requirements.file(IVY_XML_FILENAME);
        buildXml = requirements.file(BUILD_XML_FILENAME);
        return requirements.result();
    }

    @Override
    public DetectableResult extractable() throws DetectableException {
        Requirements requirements = new Requirements(fileFinder, environment);
        antExe = requirements.executable(antResolver::resolveAnt, "ant");
        DetectableResult result = requirements.result();

        // If ant executable not found, return early
        if (!result.getPassed()) {
            return result;
        }

        // Check if build.xml contains ivy:dependencytree task
        try {
            Optional<String> targetWithDependencyTree = dependencyTreeTargetParser.parseTargetWithDependencyTree(buildXml);
            if (targetWithDependencyTree.isPresent()) {
                targetName = targetWithDependencyTree.get();
                return new PassedDetectableResult();
            } else {
                return new IvyDependencyTreeNotFoundDetectableResult();
            }
        } catch (IOException e) {
            throw new DetectableException("Failed to parse build.xml for ivy:dependencytree task: " + e.getMessage(), e);
        }
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) throws IOException {
        return ivyCliExtractor.extract(environment.getDirectory(), antExe, buildXml, targetName);
    }
}