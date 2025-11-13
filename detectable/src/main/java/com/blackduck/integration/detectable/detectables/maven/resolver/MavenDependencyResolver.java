package com.blackduck.integration.detectable.detectables.maven.resolver;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MavenDependencyResolver {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final RepositorySystem repositorySystem;

    public MavenDependencyResolver() {
        this.repositorySystem = new RepositorySystemSupplier() {
            @Override
            protected Map<String, org.eclipse.aether.spi.connector.transport.TransporterFactory> createTransporterFactories() {
                Map<String, org.eclipse.aether.spi.connector.transport.TransporterFactory> factories = super.createTransporterFactories();
                // This is intentionally left blank as the JdkTransporterFactory is not available in this context.
                // The default transporters will be used.
                return factories;
            }
        }.get();
    }

    public CollectResult resolveDependencies(File pomFile, MavenProject mavenProject, File localRepoDir) throws DependencyCollectionException, DependencyResolutionException {
        RepositorySystemSession session = newSession(localRepoDir);

        List<Dependency> dependencies = mavenProject.getDependencies().stream()
                .map(dep -> new Dependency(new org.eclipse.aether.artifact.DefaultArtifact(dep.getCoordinates().getGroupId(), dep.getCoordinates().getArtifactId(), "jar", dep.getCoordinates().getVersion()), dep.getScope()))
                .collect(Collectors.toList());

        List<RemoteRepository> remoteRepositories = mavenProject.getRepositories().stream()
                .map(repo -> new RemoteRepository.Builder(repo.getId(), "default", repo.getUrl()).build())
                .collect(Collectors.toList());

        // Fallback to Maven Central if no repositories are defined
        if (remoteRepositories.isEmpty()) {
            remoteRepositories.add(new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build());
        }

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setDependencies(dependencies);
        collectRequest.setRepositories(remoteRepositories);

        logger.info("Resolving dependency tree for: {}", pomFile.getAbsolutePath());
        CollectResult collectResult = repositorySystem.collectDependencies(session, collectRequest);

        DependencyRequest dependencyRequest = new DependencyRequest(collectResult.getRoot(), null);
        repositorySystem.resolveDependencies(session, dependencyRequest);

        return collectResult;
    }

    private RepositorySystemSession newSession(File localRepoDir) {
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();
        LocalRepository localRepo = new LocalRepository(localRepoDir.getAbsolutePath());
        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepo));
        return session;
    }
}
