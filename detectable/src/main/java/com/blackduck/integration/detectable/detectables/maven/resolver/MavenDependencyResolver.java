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
            .filter(this::isVersionResolved)
            .map(dep -> new Dependency(new DefaultArtifact(dep.getCoordinates().getGroupId(), dep.getCoordinates().getArtifactId(), "jar", dep.getCoordinates().getVersion()), dep.getScope()))
            .collect(Collectors.toList());

        List<Dependency> managedDependencies = mavenProject.getDependencyManagement().stream()
            .filter(this::isVersionResolved)
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

        //Why? Because if this project doesn't declare any direct dependencies but it is managing a set of dependencies, what we probably want is to see the dependency tree for everything it's managing.
        if (dependencies.isEmpty() && !managedDependencies.isEmpty()) {
            logger.debug("Project has no direct dependencies but does have managed dependencies. Treating managed dependencies as direct for resolution.");
            collectRequest.setDependencies(managedDependencies);
        } else {
            collectRequest.setDependencies(dependencies);
            collectRequest.setManagedDependencies(managedDependencies);
        }

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

    private boolean isVersionResolved(JavaDependency dependency) {
        String version = dependency.getCoordinates().getVersion();
        if (version == null || version.isEmpty() || version.contains("${")) {
            logger.warn("Skipping dependency with unresolved version: {}:{}:{}",
                dependency.getCoordinates().getGroupId(),
                dependency.getCoordinates().getArtifactId(),
                version);
            return false;
        }
        return true;
    }
}
