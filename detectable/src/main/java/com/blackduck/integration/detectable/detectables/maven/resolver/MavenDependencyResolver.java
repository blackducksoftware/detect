package com.blackduck.integration.detectable.detectables.maven.resolver;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.supplier.SessionBuilderSupplier;
import org.eclipse.aether.transport.jdk.JdkTransporterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenDependencyResolver {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final RepositorySystem repositorySystem;

    public MavenDependencyResolver() {
        this.repositorySystem = new RepositorySystemSupplier() {
            @Override
            protected Map<String, TransporterFactory> createTransporterFactories() {
                Map<String, TransporterFactory> result = super.createTransporterFactories();
                result.put(
                        JdkTransporterFactory.NAME,
                        new JdkTransporterFactory(getChecksumExtractor(), getPathProcessor()));
                return result;
            }
        }.get();
    }

    public CollectResult resolveDependencies(File pomFile, MavenProject mavenProject, File localRepoDir) throws DependencyCollectionException {
        RepositorySystemSession session = newSession(localRepoDir);

        List<Dependency> dependencies = mavenProject.getDependencies().stream()
            .map(dep -> new Dependency(new DefaultArtifact(dep.getCoordinates().getGroupId(), dep.getCoordinates().getArtifactId(), "jar", dep.getCoordinates().getVersion()), dep.getScope()))
            .collect(Collectors.toList());

        logger.info("------------------------------------------------------------");
        logger.info("Resolving dependency tree for: {}", pomFile.getAbsolutePath());

        RemoteRepository central = new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build();
        List<RemoteRepository> repositories = Collections.singletonList(central);

        CollectRequest collectRequest = new CollectRequest();

        collectRequest.setRoot(new Dependency(new DefaultArtifact(
            mavenProject.getCoordinates().getGroupId(),
            mavenProject.getCoordinates().getArtifactId(),
            "pom",
            mavenProject.getCoordinates().getVersion()
        ), "compile"));

        collectRequest.setDependencies(dependencies);
        collectRequest.setRepositories(repositories);

        CollectResult collectResult = repositorySystem.collectDependencies(session, collectRequest);
        logger.info("Dependency tree collected for: {}", pomFile.getAbsolutePath());

        return collectResult;
    }

    private RepositorySystemSession newSession(File localRepoDir) {
        SessionBuilderSupplier sessionBuilderSupplier = new SessionBuilderSupplier(repositorySystem);
        return sessionBuilderSupplier
            .get()
            .withLocalRepositoryBaseDirectories(localRepoDir.toPath())
            .build();
    }
}
