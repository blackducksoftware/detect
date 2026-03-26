package com.blackduck.integration.detectable.detectables.maven.resolver.resolution;

import com.blackduck.integration.detectable.detectables.maven.resolver.BaseSessionBuilderSupplier;
import com.blackduck.integration.detectable.detectables.maven.resolver.TestSessionBuilderSupplier;
import com.blackduck.integration.detectable.detectables.maven.resolver.mirror.MavenMirrorConfigurator;
import com.blackduck.integration.detectable.detectables.maven.resolver.MavenProxyConfigurator;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession.SessionBuilder;
import org.eclipse.aether.supplier.SessionBuilderSupplier;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Factory for creating RepositorySystemSession instances with appropriate configuration.
 *
 * <p>Centralizes session creation logic to avoid duplication and ensure consistent
 * proxy and mirror configuration across all sessions.
 *
 * <p>Supports two types of sessions:
 * <ul>
 *   <li>Base session (compile scope only)</li>
 *   <li>Test session (includes test scope dependencies)</li>
 * </ul>
 */
public class SessionFactory {
    private final RepositorySystem repositorySystem;

    @Nullable
    private final MavenProxyConfigurator proxyConfigurator;

    @Nullable
    private final MavenMirrorConfigurator mirrorConfigurator;

    /**
     * Constructs a SessionFactory.
     *
     * @param repositorySystem The Aether repository system
     * @param proxyConfigurator Optional proxy configurator (may be null)
     * @param mirrorConfigurator Optional mirror configurator (may be null)
     */
    public SessionFactory(
        RepositorySystem repositorySystem,
        @Nullable MavenProxyConfigurator proxyConfigurator,
        @Nullable MavenMirrorConfigurator mirrorConfigurator
    ) {
        this.repositorySystem = repositorySystem;
        this.proxyConfigurator = proxyConfigurator;
        this.mirrorConfigurator = mirrorConfigurator;
    }

    /**
     * Creates a repository system session with appropriate scope configuration.
     *
     * @param localRepoDir The local repository directory for caching artifacts
     * @param includeTestScope Whether to include test-scope dependencies
     * @return Configured RepositorySystemSession
     */
    public RepositorySystemSession createSession(File localRepoDir, boolean includeTestScope) {
        if (includeTestScope) {
            return createTestSession(localRepoDir);
        }
        return createBaseSession(localRepoDir);
    }

    /**
     * Creates a base session (compile scope only).
     *
     * @param localRepoDir The local repository directory
     * @return Configured session for compile scope
     */
    private RepositorySystemSession createBaseSession(File localRepoDir) {
        SessionBuilderSupplier sessionBuilderSupplier = new BaseSessionBuilderSupplier(repositorySystem);
        SessionBuilder builder = sessionBuilderSupplier
            .get()
            .withLocalRepositoryBaseDirectories(localRepoDir.toPath())
            .setConfigProperty("aether.remoteRepositoryFilter.prefixes", "false")
            .setIgnoreArtifactDescriptorRepositories(true);

        configureProxyAndMirrors(builder);

        return builder.build();
    }

    /**
     * Creates a test session (includes test scope dependencies).
     *
     * @param localRepoDir The local repository directory
     * @return Configured session including test scope
     */
    private RepositorySystemSession createTestSession(File localRepoDir) {
        TestSessionBuilderSupplier testSupplier = new TestSessionBuilderSupplier(repositorySystem);
        SessionBuilder builder = testSupplier
            .get()
            .withLocalRepositoryBaseDirectories(localRepoDir.toPath())
            .setConfigProperty("aether.remoteRepositoryFilter.prefixes", "false")
            .setIgnoreArtifactDescriptorRepositories(true);

        configureProxyAndMirrors(builder);

        return builder.build();
    }

    /**
     * Configures proxy and mirror settings on the session builder.
     *
     * @param builder The session builder to configure
     */
    private void configureProxyAndMirrors(SessionBuilder builder) {
        // Configure proxy if available
        if (proxyConfigurator != null) {
            proxyConfigurator.configureProxy(builder);
        }

        // Configure mirrors if available
        if (mirrorConfigurator != null) {
            mirrorConfigurator.configureMirrors(builder);
        }
    }
}
