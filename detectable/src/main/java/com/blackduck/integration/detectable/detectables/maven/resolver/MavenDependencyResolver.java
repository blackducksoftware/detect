package com.blackduck.integration.detectable.detectables.maven.resolver;

import java.io.File;
import java.util.ArrayList;
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
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
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

        // Build Aether Dependency objects from MavenProject dependencies, preserving type/classifier and mapping exclusions
        List<Dependency> dependencies = mavenProject.getDependencies().stream()
            .filter(this::isVersionResolved)
            .map(dep -> {
                String groupId = dep.getCoordinates().getGroupId();
                String artifactId = dep.getCoordinates().getArtifactId();
                String version = dep.getCoordinates().getVersion();
                String type = dep.getType();
                String classifier = dep.getClassifier();

                if (type == null || type.trim().isEmpty()) {
                    type = "jar"; // fallback
                }

                org.eclipse.aether.artifact.Artifact artifact;
                if (classifier != null && !classifier.trim().isEmpty()) {
                    artifact = new DefaultArtifact(groupId, artifactId, classifier, type, version);
                } else {
                    artifact = new DefaultArtifact(groupId, artifactId, type, version);
                }

                // Map exclusions
                List<Exclusion> aetherExclusions = new ArrayList<>();
                if (dep.getExclusions() != null) {
                    dep.getExclusions().forEach(ex -> {
                        String exclGroup = (ex.getGroupId() == null || ex.getGroupId().trim().isEmpty()) ? "*" : ex.getGroupId();
                        String exclArtifact = (ex.getArtifactId() == null || ex.getArtifactId().trim().isEmpty()) ? "*" : ex.getArtifactId();
                        // Use wildcard for classifier and extension
                        aetherExclusions.add(new Exclusion(exclGroup, exclArtifact, "*", "*"));
                    });
                }

                logger.info("Mapping dependency to Aether Artifact: {}:{}:{}:{}:{} (scope={})", groupId, artifactId, classifier == null ? "" : classifier, type, version, dep.getScope());

                if (aetherExclusions.isEmpty()) {
                    return new Dependency(artifact, dep.getScope());
                } else {
                    // Use constructor that includes exclusions when available
                    return new Dependency(artifact, dep.getScope(), false, aetherExclusions);
                }
            })
            .collect(Collectors.toList());

        List<Dependency> managedDependencies = mavenProject.getDependencyManagement().stream()
            .filter(this::isVersionResolved)
            .map(dep -> {
                String groupId = dep.getCoordinates().getGroupId();
                String artifactId = dep.getCoordinates().getArtifactId();
                String version = dep.getCoordinates().getVersion();
                String type = dep.getType();
                String classifier = dep.getClassifier();

                if (type == null || type.trim().isEmpty()) {
                    type = "jar";
                }

                org.eclipse.aether.artifact.Artifact artifact;
                if (classifier != null && !classifier.trim().isEmpty()) {
                    artifact = new DefaultArtifact(groupId, artifactId, classifier, type, version);
                } else {
                    artifact = new DefaultArtifact(groupId, artifactId, type, version);
                }

                List<Exclusion> aetherExclusions = new ArrayList<>();
                if (dep.getExclusions() != null) {
                    dep.getExclusions().forEach(ex -> {
                        String exclGroup = (ex.getGroupId() == null || ex.getGroupId().trim().isEmpty()) ? "*" : ex.getGroupId();
                        String exclArtifact = (ex.getArtifactId() == null || ex.getArtifactId().trim().isEmpty()) ? "*" : ex.getArtifactId();
                        aetherExclusions.add(new Exclusion(exclGroup, exclArtifact, "*", "*"));
                    });
                }

                logger.info("Mapping managed dependency to Aether Artifact: {}:{}:{}:{}:{} (scope={})", groupId, artifactId, classifier == null ? "" : classifier, type, version, dep.getScope());

                if (aetherExclusions.isEmpty()) {
                    return new Dependency(artifact, dep.getScope());
                } else {
                    return new Dependency(artifact, dep.getScope(), false, aetherExclusions);
                }
            })
            .collect(Collectors.toList());

        logger.info("------------------------------------------------------------");
        logger.info("Resolving dependency tree for: {}", pomFile.getAbsolutePath());

        // Map project-declared repositories into Aether RemoteRepository objects
        List<RemoteRepository> repositories = null;
        if (mavenProject.getRepositories() != null && !mavenProject.getRepositories().isEmpty()) {
            repositories = mavenProject.getRepositories().stream()
                .map(repo -> {
                    String id = repo.getId() == null || repo.getId().trim().isEmpty() ? repo.getUrl() : repo.getId();
                    RemoteRepository.Builder builder = new RemoteRepository.Builder(id, "default", repo.getUrl());
                    // Apply simple release/snapshot enable flags
                    RepositoryPolicy releasePolicy = new RepositoryPolicy(repo.isReleasesEnabled(), null, null);
                    RepositoryPolicy snapshotPolicy = new RepositoryPolicy(repo.isSnapshotsEnabled(), null, null);
                    builder.setReleasePolicy(releasePolicy);
                    builder.setSnapshotPolicy(snapshotPolicy);
                    return builder.build();
                })
                .collect(Collectors.toList());

            logger.info("Using repositories declared in POM (in order): {}", mavenProject.getRepositories().stream().map(r -> r.getUrl()).collect(Collectors.joining(", ")));
        } else {
            RemoteRepository central = new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build();
            repositories = Collections.singletonList(central);
            logger.info("No repositories declared in POM. Falling back to Maven Central: https://repo.maven.apache.org/maven2/");
        }

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
