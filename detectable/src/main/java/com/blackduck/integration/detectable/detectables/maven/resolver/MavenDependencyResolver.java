package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.blackduck.integration.detectable.detectables.maven.resolver.mirror.MavenMirrorConfig;
import com.blackduck.integration.detectable.detectables.maven.resolver.mirror.MavenMirrorConfigurator;
import com.blackduck.integration.detectable.detectables.maven.resolver.mirror.MavenProxyConfig;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession.SessionBuilder;
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
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MavenDependencyResolver is responsible for turning a resolved {@link MavenProject} model into
 * an Aether dependency collection request and executing the dependency collection to produce
 * a resolved dependency graph ({@link CollectResult}).
 *
 * Key responsibilities:
 * - Map project dependencies and dependency management entries from {@link JavaDependency}
 *   into Aether {@link org.eclipse.aether.artifact.Artifact} and {@link Dependency} objects,
 *   applying managed versions (BOM/dependencyManagement) when a declared version is missing.
 * - Convert exclusion rules into Aether {@link Exclusion} instances.
 * - Build a RepositorySystem and RepositorySystemSession (with optional test-scope support)
 *   for use by Aether, including local repository configuration.
 * - Attempt dependency collection using a fallback strategy: a union of declared repositories
 *   (plus Maven Central if missing), declared repositories only, and finally Maven Central only.
 * - Provide diagnostic logging for repository health and collection attempts.
 *
 * The resolver prefers explicit, resolved versions. If a dependency's version cannot be
 * determined (null, empty, or containing unresolved property expressions like "${...}"),
 * that dependency will be skipped during collection to avoid Aether failures.
 *
 * Thread-safety: instances create and hold a RepositorySystem but do not maintain mutable
 * per-request state; however, RepositorySystem implementations may not be fully thread-safe
 * depending on their configuration, so callers should treat the resolver as not guaranteed
 * thread-safe unless the underlying components are known to be.
 *
 * Usage example:
 *   MavenDependencyResolver resolver = new MavenDependencyResolver();
 *   CollectResult result = resolver.resolveDependencies(pomFile, mavenProject, localRepoDir, "compile");
 */
public class MavenDependencyResolver {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String MAVEN_CENTRAL_URL = "https://repo.maven.apache.org/maven2/";
    private static final String REPOSITORY_TYPE_DEFAULT = "default";

    private final RepositorySystem repositorySystem;

    // Proxy configurator — handles all forward-proxy configuration.
    // Null if no proxy is configured.
    @Nullable
    private final MavenProxyConfigurator proxyConfigurator;

    // Mirror configurator — handles corporate mirror (repository manager) configuration.
    // Null if no mirrors are configured.
    @Nullable
    private final MavenMirrorConfigurator mirrorConfigurator;

    /**
     * No-arg constructor: no proxy or mirrors configured.
     * Kept for backward compatibility with existing callers (e.g. module processor).
     */
    public MavenDependencyResolver() {
        this(null, Collections.emptyList());
    }

    /**
     * Constructs a resolver with forward-proxy support (no mirrors).
     *
     * <p>This constructor is provided for backward compatibility.
     *
     * @param proxyHost         Proxy hostname or IP — plain value, <strong>no</strong> {@code http://} or {@code https://} prefix.
     * @param proxyPort         Proxy port (0 means no proxy).
     * @param proxyUsername     Optional proxy-auth username (may be null).
     * @param proxyPassword     Optional proxy-auth password (may be null).
     * @param proxyIgnoredHosts Host patterns that should bypass the proxy (may be empty, never null).
     */
    public MavenDependencyResolver(
        @Nullable String proxyHost,
        int proxyPort,
        @Nullable String proxyUsername,
        @Nullable String proxyPassword,
        List<String> proxyIgnoredHosts
    ) {
        // Convert individual proxy fields to MavenProxyConfig
        MavenProxyConfig proxyConfig = null;
        if (proxyHost != null && !proxyHost.trim().isEmpty() && proxyPort > 0) {
            proxyConfig = new MavenProxyConfig(
                proxyHost,
                proxyPort,
                proxyUsername,
                proxyPassword,
                proxyIgnoredHosts != null ? proxyIgnoredHosts : Collections.emptyList()
            );
        }
        
        // Delegate to main constructor
        this.proxyConfigurator = proxyConfig != null ? new MavenProxyConfigurator(proxyConfig) : null;
        this.mirrorConfigurator = null;
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

    /**
     * Constructs a resolver with forward-proxy and corporate mirror support.
     *
     * @param proxyConfig          Proxy configuration (may be null if no proxy is configured).
     * @param mirrorConfigurations List of mirror configurations for corporate repository managers.
     */
    public MavenDependencyResolver(
        @Nullable MavenProxyConfig proxyConfig,
        List<MavenMirrorConfig> mirrorConfigurations
    ) {
        // Create MavenProxyConfigurator only if proxy config is provided
        if (proxyConfig != null) {
            this.proxyConfigurator = new MavenProxyConfigurator(proxyConfig);
        } else {
            this.proxyConfigurator = null;
        }

        // Create MavenMirrorConfigurator only if mirrors are configured
        if (mirrorConfigurations != null && !mirrorConfigurations.isEmpty()) {
            this.mirrorConfigurator = new MavenMirrorConfigurator(mirrorConfigurations);
            logger.info("Mirror configurator initialized with {} mirror(s)", mirrorConfigurations.size());
        } else {
            this.mirrorConfigurator = null;
        }

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
        return resolveDependencies(pomFile, mavenProject, localRepoDir, rootScope, Collections.emptyList());
    }

    /**
     * Resolves dependencies for a Maven project with support for external repositories.
     *
     * @param pomFile The POM file being resolved
     * @param mavenProject The parsed Maven project model
     * @param localRepoDir The local repository directory for caching artifacts
     * @param rootScope The root scope for resolution ("compile" or "test")
     * @param externalRepositories List of external repository URLs to use alongside POM-declared repos
     * @return CollectResult containing the resolved dependency tree
     * @throws DependencyCollectionException if dependency collection fails
     */
    public CollectResult resolveDependencies(File pomFile, MavenProject mavenProject, File localRepoDir, String rootScope, List<String> externalRepositories) throws DependencyCollectionException {
        try {
            // Perform the actual dependency resolution
            return doResolveDependencies(pomFile, mavenProject, localRepoDir, rootScope, externalRepositories);
        } finally {
            // CRITICAL: Always restore original system proxy properties after resolution completes
            // This ensures Maven's proxy settings don't leak to other parts of the application
            // This runs whether resolution succeeds, fails, or throws an exception
            if (proxyConfigurator != null) {
                proxyConfigurator.restoreOriginalProxyProperties();
            }
        }
    }

    /**
     * Internal method that performs the actual dependency resolution.
     * Separated from the public method to allow proper try-finally cleanup.
     */
    private CollectResult doResolveDependencies(File pomFile, MavenProject mavenProject, File localRepoDir, String rootScope, List<String> externalRepositories) throws DependencyCollectionException {
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

                logger.info("Mapping dependency to Aether Artifact: {}:{}:{}:{}:{} (scope={})", groupId, artifactId, classifier == null ? "" : classifier, type, effectiveVersion, dep.getScope());

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

        // STEP 1: Convert external repository URLs to RemoteRepository objects
        List<RemoteRepository> externalRepos = new ArrayList<>();
        if (externalRepositories != null && !externalRepositories.isEmpty()) {
            logger.info("Processing {} external repository URL(s)...", externalRepositories.size());
            for (String url : externalRepositories) {
                if (url == null || url.trim().isEmpty()) {
                    logger.warn("Skipping empty external repository URL.");
                    continue;
                }
                String trimmedUrl = url.trim();
                // Basic URL validation
                if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
                    logger.warn("Skipping invalid external repository URL (must start with http:// or https://): {}", trimmedUrl);
                    continue;
                }
                try {
                    // Generate a unique ID based on the URL
                    String id = "external-" + trimmedUrl.replaceAll("[^a-zA-Z0-9]", "-").substring(0, Math.min(50, trimmedUrl.length()));
                    RemoteRepository.Builder builder = new RemoteRepository.Builder(id, REPOSITORY_TYPE_DEFAULT, trimmedUrl);
                    externalRepos.add(builder.build());
                    logger.info("Added external repository: {} (id: {})", trimmedUrl, id);
                } catch (Exception e) {
                    logger.warn("Failed to construct RemoteRepository for external URL: {}. Error: {}", trimmedUrl, e.getMessage());
                }
            }
            if (!externalRepos.isEmpty()) {
                logger.info("Using {} external repository(ies): {}", externalRepos.size(),
                    externalRepos.stream().map(RemoteRepository::getUrl).collect(Collectors.joining(", ")));
            }
        }

        // STEP 2: Map project-declared repositories into Aether RemoteRepository objects
        List<RemoteRepository> declaredRepositories;
        if (mavenProject.getRepositories() != null && !mavenProject.getRepositories().isEmpty()) {
            declaredRepositories = mavenProject.getRepositories().stream()
                .map(repo -> {
                    String id = repo.getId() == null || repo.getId().trim().isEmpty() ? repo.getUrl() : repo.getId();
                    try {
                        RemoteRepository.Builder builder = new RemoteRepository.Builder(id, REPOSITORY_TYPE_DEFAULT, repo.getUrl());
                        if (!repo.isReleasesEnabled()) {
                            logger.info("Repository {} has releases disabled", repo.getUrl());
                        }
                        if (!repo.isSnapshotsEnabled()) {
                            logger.info("Repository {} has snapshots disabled", repo.getUrl());
                        }
                        return builder.build();
                    } catch (Exception e) {
                        logger.info("Failed to construct RemoteRepository for {} (id: {}). Falling back to basic builder. Exception: {}", repo.getUrl(), id, e.getMessage());
                        return new RemoteRepository.Builder(id, REPOSITORY_TYPE_DEFAULT, repo.getUrl()).build();
                    }
                })
                .collect(Collectors.toList());

            logger.info("Using repositories declared in POM (in order): {}", mavenProject.getRepositories().stream().map(r -> r.getUrl()).collect(Collectors.joining(", ")));
        } else {
            declaredRepositories = new ArrayList<>();
            logger.info("No repositories declared in POM.");
        }

        RemoteRepository central = new RemoteRepository.Builder("central", REPOSITORY_TYPE_DEFAULT, MAVEN_CENTRAL_URL).build();
        // Check if Central is present in either external or declared repos
        boolean centralPresent = externalRepos.stream().anyMatch(r -> MAVEN_CENTRAL_URL.equalsIgnoreCase(r.getUrl()))
            || declaredRepositories.stream().anyMatch(r -> MAVEN_CENTRAL_URL.equalsIgnoreCase(r.getUrl()));

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(new DefaultArtifact(
            mavenProject.getCoordinates().getGroupId(),
            mavenProject.getCoordinates().getArtifactId(),
            "pom",
            mavenProject.getCoordinates().getVersion()
        ), rootScope));

        // If the project has no direct dependencies but has managed dependencies, treat managed as direct for resolution
//        if (dependencies.isEmpty() && !managedDependencies.isEmpty()) {
//            logger.info("Project has no direct dependencies but does have managed dependencies. Treating managed dependencies as direct for resolution.");
//            collectRequest.setDependencies(managedDependencies);
//        } else {
            collectRequest.setDependencies(dependencies);
            collectRequest.setManagedDependencies(managedDependencies);
//        }

        // Inclusive retry strategy
        // Repository order: External repos (user-specified) → POM-declared repos → Central (if not already present)
        List<RemoteRepository> unionRepos = new ArrayList<>();
        // Add external repositories first (highest priority - user intent)
        unionRepos.addAll(externalRepos);
        // Add POM-declared repositories
        unionRepos.addAll(declaredRepositories);
        // Add Central if not already present
        if (!centralPresent) {
            unionRepos.add(central);
        }

        // Attempt 1: union of external + declared repos + Central (if not present)
        try {
            collectRequest.setRepositories(unionRepos);
            logger.info("Attempting dependency collection with UNION repos: {}", unionRepos.stream().map(r -> r.getUrl()).collect(Collectors.joining(", ")));
            CollectResult result = repositorySystem.collectDependencies(session, collectRequest);
            logger.info("Dependency tree collected for: {} using union repositories", pomFile.getAbsolutePath());
            logger.info("Collectec root children: {}", result.getRoot().getChildren().stream().map(c -> c.getDependency().getArtifact().toString()).collect(Collectors.joining(", ")));
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
        SessionBuilderSupplier sessionBuilderSupplier = new BaseSessionBuilderSupplier(repositorySystem);
        SessionBuilder builder = sessionBuilderSupplier
            .get()
            .withLocalRepositoryBaseDirectories(localRepoDir.toPath())
                .setConfigProperty("aether.remoteRepositoryFilter.prefixes", "false")
                .setIgnoreArtifactDescriptorRepositories(true);

        // Configure proxy if available
        if (proxyConfigurator != null) {
            proxyConfigurator.configureProxy(builder);
        }

        // Configure mirrors if available
        if (mirrorConfigurator != null) {
            mirrorConfigurator.configureMirrors(builder);
        }

        return builder.build();
    }

    // New helper to create a session with optional TEST scope enabled via TestSessionBuilderSupplier
    private RepositorySystemSession newSession(File localRepoDir, boolean includeTestScope) {
        if (includeTestScope) {
            TestSessionBuilderSupplier testSupplier = new TestSessionBuilderSupplier(repositorySystem);
            SessionBuilder builder = testSupplier
                .get()
                .withLocalRepositoryBaseDirectories(localRepoDir.toPath())
                    .setConfigProperty("aether.remoteRepositoryFilter.prefixes", "false")
                    .setIgnoreArtifactDescriptorRepositories(true);

            // Configure proxy if available
            if (proxyConfigurator != null) {
                proxyConfigurator.configureProxy(builder);
            }

            // Configure mirrors if available
            if (mirrorConfigurator != null) {
                mirrorConfigurator.configureMirrors(builder);
            }

            return builder.build();
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
