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

//    public CollectResult resolveDependencies(File pomFile, MavenProject mavenProject, File localRepoDir) throws DependencyCollectionException {
//        return resolveDependencies(pomFile, mavenProject, localRepoDir, "compile");
//    }

    // New overloaded method that allows specifying the root scope (e.g., "compile" or "test")
    public CollectResult resolveDependencies(File pomFile, MavenProject mavenProject, File localRepoDir, String rootScope) throws DependencyCollectionException {
        boolean includeTestScope = "test".equalsIgnoreCase(rootScope);
        RepositorySystemSession session = newSession(localRepoDir, includeTestScope);
        // Build a map of managed versions from dependencyManagement keyed by group:artifact
        java.util.Map<String, String> managedVersionByGa = mavenProject.getDependencyManagement().stream()
            .collect(Collectors.toMap(
                d -> d.getCoordinates().getGroupId() + ":" + d.getCoordinates().getArtifactId(),
                d -> d.getCoordinates().getVersion(),
                (a, b) -> a
            ));

        // Build Aether Dependency objects from MavenProject dependencies, applying managed versions when declared version is missing/unresolved
        List<Dependency> dependencies = mavenProject.getDependencies().stream()
            .map(dep -> {
                String groupId = dep.getCoordinates().getGroupId();
                String artifactId = dep.getCoordinates().getArtifactId();
                String declaredVersion = dep.getCoordinates().getVersion();
                String type = dep.getType();
                String classifier = dep.getClassifier();

                // Determine effective version: prefer declared if resolved; otherwise try managed (BOM)
                String effectiveVersion = declaredVersion;
                boolean declaredUnresolved = (effectiveVersion == null || effectiveVersion.isEmpty() || effectiveVersion.contains("${"));
                if (declaredUnresolved) {
                    String key = groupId + ":" + artifactId;
                    String mv = managedVersionByGa.get(key);
                    if (mv != null && !mv.isEmpty() && !mv.contains("${")) {
                        effectiveVersion = mv;
                    }
                }

                // If still unresolved, skip this dependency (log done in isVersionResolved previously)
                if (effectiveVersion == null || effectiveVersion.isEmpty() || effectiveVersion.contains("${")) {
                    logger.info("Skipping dependency with unresolved version: {}:{}:{}", groupId, artifactId, declaredVersion);
                    return null;
                }

                if (type == null || type.trim().isEmpty()) {
                    type = "jar"; // fallback
                }

                org.eclipse.aether.artifact.Artifact artifact;
                if (classifier != null && !classifier.trim().isEmpty()) {
                    artifact = new DefaultArtifact(groupId, artifactId, classifier, type, effectiveVersion);
                } else {
                    artifact = new DefaultArtifact(groupId, artifactId, type, effectiveVersion);
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

//                logger.info("Mapping dependency to Aether Artifact: {}:{}:{}:{}:{} (scope={})", groupId, artifactId, classifier == null ? "" : classifier, type, effectiveVersion, dep.getScope());

                if (aetherExclusions.isEmpty()) {
                    return new Dependency(artifact, dep.getScope());
                } else {
                    return new Dependency(artifact, dep.getScope(), false, aetherExclusions);
                }
            })
            .filter(java.util.Objects::nonNull)
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

//                logger.info("Mapping managed dependency to Aether Artifact: {}:{}:{}:{}:{} (scope={})", groupId, artifactId, classifier == null ? "" : classifier, type, version, dep.getScope());

                if (aetherExclusions.isEmpty()) {
                    return new Dependency(artifact, dep.getScope());
                } else {
                    return new Dependency(artifact, dep.getScope(), false, aetherExclusions);
                }
            })
            .collect(Collectors.toList());

        logger.info("------------------------------------------------------------");
        logger.info("Resolving dependency tree for: {} (scope={})", pomFile.getAbsolutePath(), rootScope);

        // Map project-declared repositories into Aether RemoteRepository objects
        List<RemoteRepository> declaredRepositories;
        if (mavenProject.getRepositories() != null && !mavenProject.getRepositories().isEmpty()) {
            declaredRepositories = mavenProject.getRepositories().stream()
                .map(repo -> {
                    String id = repo.getId() == null || repo.getId().trim().isEmpty() ? repo.getUrl() : repo.getId();
                    try {
                        RemoteRepository.Builder builder = new RemoteRepository.Builder(id, "default", repo.getUrl());
                        if (!repo.isReleasesEnabled()) {
                            logger.info("Repository {} has releases disabled", repo.getUrl());
                        }
                        if (!repo.isSnapshotsEnabled()) {
                            logger.info("Repository {} has snapshots disabled", repo.getUrl());
                        }
                        return builder.build();
                    } catch (Exception e) {
                        logger.info("Failed to construct RemoteRepository for {} (id: {}). Falling back to basic builder. Exception: {}", repo.getUrl(), id, e.getMessage());
                        return new RemoteRepository.Builder(id, "default", repo.getUrl()).build();
                    }
                })
                .collect(Collectors.toList());

            logger.info("Using repositories declared in POM (in order): {}", mavenProject.getRepositories().stream().map(r -> r.getUrl()).collect(Collectors.joining(", ")));
        } else {
            declaredRepositories = new ArrayList<>();
            logger.info("No repositories declared in POM.");
        }

        RemoteRepository central = new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build();
        boolean centralPresent = declaredRepositories.stream().anyMatch(r -> "https://repo.maven.apache.org/maven2/".equalsIgnoreCase(r.getUrl()));

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(new DefaultArtifact(
            mavenProject.getCoordinates().getGroupId(),
            mavenProject.getCoordinates().getArtifactId(),
            "pom",
            mavenProject.getCoordinates().getVersion()
        ), rootScope));

        // If the project has no direct dependencies but has managed dependencies, treat managed as direct for resolution
        if (dependencies.isEmpty() && !managedDependencies.isEmpty()) {
            logger.info("Project has no direct dependencies but does have managed dependencies. Treating managed dependencies as direct for resolution.");
            collectRequest.setDependencies(managedDependencies);
        } else {
            collectRequest.setDependencies(dependencies);
            collectRequest.setManagedDependencies(managedDependencies);
        }

        // Inclusive retry strategy
        List<RemoteRepository> unionRepos = new ArrayList<>(declaredRepositories);
        if (!centralPresent) {
            unionRepos.add(central);
        }

        // Attempt 1: union of declared repos + Central (if not present)
        try {
            collectRequest.setRepositories(unionRepos);
            logger.info("Attempting dependency collection with UNION repos: {}", unionRepos.stream().map(r -> r.getUrl()).collect(Collectors.joining(", ")));
            CollectResult result = repositorySystem.collectDependencies(session, collectRequest);
            logger.info("Dependency tree collected for: {} using union repositories", pomFile.getAbsolutePath());
            return result;
        } catch (Exception unionEx) {
            logger.info("Union repositories collection failed: {}", unionEx.getMessage());
            logger.info("Full exception for union attempt: ", unionEx);
        }

        // Optional diagnostic probe: test repos individually, but DO NOT exclude on single-repo failure; just log.
        if (!declaredRepositories.isEmpty()) {
            for (RemoteRepository repo : declaredRepositories) {
                try {
                    CollectRequest singleRepoRequest = new CollectRequest();
                    singleRepoRequest.setRoot(collectRequest.getRoot());
                    singleRepoRequest.setDependencies(collectRequest.getDependencies());
                    singleRepoRequest.setManagedDependencies(collectRequest.getManagedDependencies());
                    singleRepoRequest.setRepositories(Collections.singletonList(repo));
                    logger.info("Diagnostic: testing single repository {}", repo.getUrl());
                    repositorySystem.collectDependencies(session, singleRepoRequest);
                    logger.info("Diagnostic: repository {} can collect in isolation.", repo.getUrl());
                } catch (Exception perRepoEx) {
                    logger.info("Diagnostic: repository {} failed in isolation: {}", repo.getUrl(), perRepoEx.getMessage());
                    logger.info("Diagnostic: full exception for repo {}: ", repo.getUrl(), perRepoEx);
                }
            }
        }

        // Attempt 2: declared repositories only (original order)
        try {
            collectRequest.setRepositories(declaredRepositories);
            logger.info("Attempting dependency collection with DECLARED repos: {}", declaredRepositories.stream().map(r -> r.getUrl()).collect(Collectors.joining(", ")));
            CollectResult result = repositorySystem.collectDependencies(session, collectRequest);
            logger.info("Dependency tree collected for: {} using declared repositories", pomFile.getAbsolutePath());
            return result;
        } catch (Exception declaredEx) {
            logger.info("Declared repositories collection failed: {}", declaredEx.getMessage());
            logger.info("Full exception for declared attempt: ", declaredEx);
        }

        // Attempt 3: Central-only fallback
        try {
            collectRequest.setRepositories(Collections.singletonList(central));
            logger.info("Falling back to Maven Central only.");
            CollectResult result = repositorySystem.collectDependencies(session, collectRequest);
            logger.info("Dependency tree collected for: {} using Maven Central fallback", pomFile.getAbsolutePath());
            return result;
        } catch (Exception centralOnlyEx) {
            logger.info("Maven Central-only collection failed: {}", centralOnlyEx.getMessage());
            logger.info("Full exception for Central-only attempt: ", centralOnlyEx);
            // All attempts failed; propagate as runtime to preserve stack without relying on CollectResult
            throw new RuntimeException("Dependency collection failed after union/declared/central attempts: " + centralOnlyEx.getMessage(), centralOnlyEx);
        }
    }

    private RepositorySystemSession newSession(File localRepoDir) {
        SessionBuilderSupplier sessionBuilderSupplier = new SessionBuilderSupplier(repositorySystem);
        return sessionBuilderSupplier
            .get()
            .withLocalRepositoryBaseDirectories(localRepoDir.toPath()).setConfigProperty("aether.remoteRepositoryFilter.prefixes", "false").setIgnoreArtifactDescriptorRepositories(true)
            .build();
    }

    // New helper to create a session with optional TEST scope enabled via TestSessionBuilderSupplier
    private RepositorySystemSession newSession(File localRepoDir, boolean includeTestScope) {
        if (includeTestScope) {
            TestSessionBuilderSupplier testSupplier = new TestSessionBuilderSupplier(repositorySystem);
            return testSupplier
                .get()
                .withLocalRepositoryBaseDirectories(localRepoDir.toPath()).setConfigProperty("aether.remoteRepositoryFilter.prefixes", "false").setIgnoreArtifactDescriptorRepositories(true)
                .build();
        }
        return newSession(localRepoDir);
    }

    private boolean isVersionResolved(JavaDependency dependency) {
        String version = dependency.getCoordinates().getVersion();
        if (version == null || version.isEmpty() || version.contains("${")) {
            logger.info("Skipping dependency with unresolved version: {}:{}:{}",
                dependency.getCoordinates().getGroupId(),
                dependency.getCoordinates().getArtifactId(),
                version);
            return false;
        }
        return true;
    }
}
