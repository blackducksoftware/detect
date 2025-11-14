package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectable.result.FileNotFoundDetectableResult;
import com.blackduck.integration.detectable.detectable.result.PassedDetectableResult;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.util.graph.visitor.DependencyGraphDumper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

@DetectableInfo(
        name = "Maven Resolver",
        language = "Java",
        forge = "Maven Central",
        accuracy = DetectableAccuracyType.HIGH,
        requirementsMarkdown = "File: pom.xml"
)
public class MavenResolverDetectable extends Detectable {

    private static final String POM_XML_FILENAME = "pom.xml";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final FileFinder fileFinder;

    private File pomFile;

    public MavenResolverDetectable(
            DetectableEnvironment environment,
            FileFinder fileFinder
    ) {
        super(environment);
        this.fileFinder = fileFinder;
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
        try {
            // 1. Build the effective POM using the new ProjectBuilder.
            Path downloadDir = extractionEnvironment.getOutputDirectory().toPath().resolve("downloads");
            Files.createDirectories(downloadDir);
            ProjectBuilder projectBuilder = new ProjectBuilder(downloadDir);
            MavenProject mavenProject = projectBuilder.buildProject(pomFile);

            // Suggestion 3: Log the MavenProject details for verification.
            logger.info("Constructed MavenProject model:");
            logger.info("  Coordinates: {}", mavenProject.getCoordinates());
            mavenProject.getDependencies().forEach(dep ->
                logger.info("  Dependency: {}:{}:{} (Scope: {})", dep.getCoordinates().getGroupId(), dep.getCoordinates().getArtifactId(), dep.getCoordinates().getVersion(), dep.getScope())
            );

            // 2. Resolve dependencies using the Aether-based resolver.
            MavenDependencyResolver dependencyResolver = new MavenDependencyResolver();
            Path localRepoPath = extractionEnvironment.getOutputDirectory().toPath().resolve("local-repo");
            CollectResult collectResult = dependencyResolver.resolveDependencies(pomFile, mavenProject, localRepoPath.toFile());

            // 3. Write the dependency tree to a file for inspection.
            File dependencyTreeFile = new File(extractionEnvironment.getOutputDirectory(), "dependency-tree.txt");
            try (PrintStream printStream = new PrintStream(dependencyTreeFile)) {
                collectResult.getRoot().accept(new DependencyGraphDumper(printStream::println));
            }
            logger.info("Dependency tree saved to: {}", dependencyTreeFile.getAbsolutePath());

            // The actual result would be a CodeLocation, but for now, we signal success.
            return new Extraction.Builder().success().build();

        } catch (Exception e) {
            logger.error("Failed to resolve dependencies for pom.xml: {}", e.getMessage());
            return new Extraction.Builder().exception(e).build();
        }
    }
}