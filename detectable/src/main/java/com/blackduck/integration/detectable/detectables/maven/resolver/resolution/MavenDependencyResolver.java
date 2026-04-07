package com.blackduck.integration.detectable.detectables.maven.resolver.resolution;

import com.blackduck.integration.detectable.detectables.maven.resolver.mirror.MavenMirrorConfig;
import com.blackduck.integration.detectable.detectables.maven.resolver.mirror.MavenMirrorConfigurator;
import com.blackduck.integration.detectable.detectables.maven.resolver.pom.MavenProject;
import com.blackduck.integration.detectable.detectables.maven.resolver.proxy.MavenProxyConfig;
import com.blackduck.integration.detectable.detectables.maven.resolver.proxy.MavenProxyConfigurator;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
 * - Map project dependencies and dependency management entries from JavaDependency
 *   into Aether {@link org.eclipse.aether.artifact.Artifact} and {@link Dependency} objects,
 *   applying managed versions (BOM/dependencyManagement) when a declared version is missing.
 * - Convert exclusion rules into Aether {@link org.eclipse.aether.graph.Exclusion} instances.
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

    /**
     * Resolves dependencies for a Maven project with default scope (compile).
     *
     * @param pomFile The POM file being resolved
     * @param mavenProject The parsed Maven project model
     * @param localRepoDir The local repository directory for caching artifacts
     * @param rootScope The root scope for resolution ("compile" or "test")
     * @return CollectResult containing the resolved dependency tree
     * @throws DependencyCollectionException if dependency collection fails
     */
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
     *
     * <p>This method has been refactored into smaller, focused components to improve
     * maintainability and reduce cognitive complexity. The orchestration follows these steps:
     * <ol>
     *   <li>Create session with appropriate scope configuration</li>
     *   <li>Map dependencies using version resolution and exclusion handling</li>
     *   <li>Build repository collections from various sources</li>
     *   <li>Create collect request with dependencies and managed dependencies</li>
     *   <li>Execute 3-tier collection strategy (union → declared → central)</li>
     * </ol>
     */
    private CollectResult doResolveDependencies(
        File pomFile,
        MavenProject mavenProject,
        File localRepoDir,
        String rootScope,
        List<String> externalRepositories
    ) throws DependencyCollectionException {

        // Step 1: Create session with appropriate scope configuration
        boolean includeTestScope = "test".equalsIgnoreCase(rootScope);
        SessionFactory sessionFactory = new SessionFactory(repositorySystem, proxyConfigurator, mirrorConfigurator);
        RepositorySystemSession session = sessionFactory.createSession(localRepoDir, includeTestScope);

        // Step 2: Map dependencies (applying managed versions when needed)
        DependencyMapper mapper = new DependencyMapper(mavenProject.getDependencyManagement());
        List<Dependency> dependencies = mapper.mapDependencies(mavenProject.getDependencies());
        List<Dependency> managedDependencies = mapper.mapManagedDependencies(mavenProject.getDependencyManagement());

        // Step 3: Build repository collections from external URLs, POM declarations, and Maven Central
        RepositoryBuilder repoBuilder = new RepositoryBuilder();
        List<RemoteRepository> externalRepos = repoBuilder.buildExternalRepositories(externalRepositories);
        List<RemoteRepository> declaredRepos = repoBuilder.buildDeclaredRepositories(mavenProject.getRepositories());
        RepositoryBuilder.RepositorySet repositories = repoBuilder.buildRepositorySet(externalRepos, declaredRepos);

        // Step 4: Create collect request with root artifact, dependencies, and managed dependencies
        CollectRequest request = createCollectRequest(mavenProject, rootScope, dependencies, managedDependencies);

        // Step 5: Execute 3-tier collection strategy (union → declared → central)
        CollectionStrategy strategy = new CollectionStrategy(repositorySystem);
        return strategy.executeCollection(request, session, repositories, pomFile);
    }

    /**
     * Creates a CollectRequest from the Maven project and mapped dependencies.
     *
     * @param project The Maven project
     * @param scope The root scope ("compile" or "test")
     * @param dependencies Mapped Aether dependencies
     * @param managedDependencies Mapped Aether managed dependencies
     * @return Configured CollectRequest
     */
    private CollectRequest createCollectRequest(
        MavenProject project,
        String scope,
        List<Dependency> dependencies,
        List<Dependency> managedDependencies
    ) {
        CollectRequest request = new CollectRequest();

        // Set root artifact (the project itself as a POM)
        request.setRoot(new Dependency(
            new DefaultArtifact(
                project.getCoordinates().getGroupId(),
                project.getCoordinates().getArtifactId(),
                "pom",
                project.getCoordinates().getVersion()
            ),
            scope
        ));

        // Set dependencies and managed dependencies
        request.setDependencies(dependencies);
        request.setManagedDependencies(managedDependencies);

        return request;
    }
}
