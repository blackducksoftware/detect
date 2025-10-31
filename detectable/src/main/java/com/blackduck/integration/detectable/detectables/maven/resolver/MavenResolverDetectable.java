package com.blackduck.integration.detectable.detectables.maven.resolver;

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
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession.CloseableSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.supplier.SessionBuilderSupplier;
import org.eclipse.aether.transport.jdk.JdkTransporterFactory;
import org.eclipse.aether.util.graph.visitor.DependencyGraphDumper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        try {
            // 1. Create RepositorySystem using the supplier approach.
            RepositorySystem system = new RepositorySystemSupplier() {
                @Override
                protected Map<String, TransporterFactory> createTransporterFactories() {
                    Map<String, TransporterFactory> result = super.createTransporterFactories();
                    result.put(
                            JdkTransporterFactory.NAME,
                            new JdkTransporterFactory(getChecksumExtractor(), getPathProcessor()));
                    return result;
                }
            }.get();

            // 2. Create a session.
            Path localRepoPath = extractionEnvironment.getOutputDirectory().toPath().resolve("local-repo");
            try (CloseableSession session = new SessionBuilderSupplier(system)
                    .get()
                    .withLocalRepositoryBaseDirectories(localRepoPath)
                    .build()) {

                // 3. Define a hardcoded artifact to resolve.
                Artifact artifact = new DefaultArtifact("com.google.guava:guava:31.0.1-jre");

                logger.info("------------------------------------------------------------");
                logger.info("Resolving dependency tree for: {}", artifact);

                RemoteRepository central = new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build();
                List<RemoteRepository> repositories = Collections.singletonList(central);

                // 4. Create a CollectRequest for the artifact.
                CollectRequest collectRequest = new CollectRequest();
                collectRequest.setRoot(new Dependency(artifact, ""));
                collectRequest.setRepositories(repositories);

                // 5. Collect dependencies and print the tree.
                CollectResult collectResult = system.collectDependencies(session, collectRequest);
                File dependencyTreeFile = extractionEnvironment.getOutputDirectory().toPath().resolve("dependency-tree.txt").toFile();
                try (PrintStream printStream = new PrintStream(dependencyTreeFile)) {
                    collectResult.getRoot().accept(new DependencyGraphDumper(printStream::println));
                }
                logger.info("Dependency tree saved to dependency-tree.txt");

            }
        } catch (Exception e) {
            logger.error("Failed to resolve dependencies", e);
            return new Extraction.Builder().exception(e).build();
        }

        return new Extraction.Builder().success().build();
    }
}