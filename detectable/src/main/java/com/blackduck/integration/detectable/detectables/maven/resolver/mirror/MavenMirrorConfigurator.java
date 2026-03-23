package com.blackduck.integration.detectable.detectables.maven.resolver.mirror;

import org.eclipse.aether.RepositorySystemSession.SessionBuilder;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Configures Maven Aether's mirror and authentication selectors for corporate mirror support.
 *
 * <p>This class handles:
 * <ul>
 *   <li>Configuration of Aether's MirrorSelector to redirect repository requests to mirrors</li>
 *   <li>Configuration of Aether's authentication selectors for mirror credentials</li>
 * </ul>
 *
 * <p><strong>How Mirroring Works:</strong>
 * When Aether attempts to access a repository (e.g., Maven Central), the MirrorSelector
 * intercepts the request and checks if a mirror is configured for that repository.
 * If a match is found (based on the {@code mirrorOf} pattern), the request is redirected
 * to the mirror URL instead.
 *
 * <p><strong>mirrorOf Pattern Syntax:</strong>
 * <ul>
 *   <li>{@code *} - matches all repositories</li>
 *   <li>{@code central} - matches only the central repository</li>
 *   <li>{@code *,!repo1} - matches all except repo1</li>
 *   <li>{@code external:*} - matches all external (non-localhost) repositories</li>
 * </ul>
 *
 * <p>Thread-safety: This class is immutable and thread-safe after construction.
 */
public class MavenMirrorConfigurator {

    private static final Logger logger = LoggerFactory.getLogger(MavenMirrorConfigurator.class);

    private final List<MavenMirrorConfig> mirrorConfigs;

    /**
     * Constructs a MavenMirrorConfigurator with the given mirror configurations.
     *
     * @param mirrorConfigs list of mirror configurations to apply (may be empty, never null)
     */
    public MavenMirrorConfigurator(List<MavenMirrorConfig> mirrorConfigs) {
        this.mirrorConfigs = mirrorConfigs != null ? mirrorConfigs : Collections.emptyList();
    }

    /**
     * Configures mirror and authentication selectors on the given Aether session builder.
     *
     * <p>This method performs the following steps:
     * <ol>
     *   <li>Creates a {@link DefaultMirrorSelector} and registers all configured mirrors</li>
     *   <li>Creates a {@link DefaultAuthenticationSelector} and registers credentials for mirrors that have them</li>
     *   <li>Attaches both selectors to the session builder</li>
     * </ol>
     *
     * <p>If no mirrors are configured, this method does nothing.
     *
     * @param builder the session builder to configure
     */
    public void configureMirrors(SessionBuilder builder) {
        if (mirrorConfigs.isEmpty()) {
            logger.debug("No mirror configurations to apply.");
            return;
        }

        try {
            DefaultMirrorSelector mirrorSelector = new DefaultMirrorSelector();
            DefaultAuthenticationSelector authSelector = new DefaultAuthenticationSelector();

            for (MavenMirrorConfig config : mirrorConfigs) {
                // Add mirror to selector
                // Parameters: id, url, type, repositoryManager, blocked, mirrorOf, mirrorOfLayouts
                // The 'blocked' parameter (5th) indicates if mirror is blocked - we set to false
                mirrorSelector.add(
                    config.getId(),
                    config.getUrl(),
                    "default",           // type
                    false,               // repositoryManager flag
                    false,               // blocked flag
                    config.getMirrorOf(),
                    null                 // mirrorOfLayouts (null = all layouts)
                );

                logger.info("Registered mirror: id='{}', url='{}', mirrorOf='{}'",
                    config.getId(), config.getUrl(), config.getMirrorOf());

                // Add authentication if credentials are present
                if (config.hasAuthentication()) {
                    Authentication auth = new AuthenticationBuilder()
                        .addUsername(config.getUsername())
                        .addPassword(config.getPassword())
                        .build();

                    // Authentication is keyed by the MIRROR URL, not the mirror ID
                    // When Aether resolves a mirrored repository, it uses the mirror's URL
                    // to look up authentication credentials
                    authSelector.add(config.getUrl(), auth);

                    logger.info("Registered authentication for mirror '{}' (URL: {})",
                        config.getId(), config.getUrl());
                }
            }

            // Attach selectors to the session
            builder.setMirrorSelector(mirrorSelector);
            builder.setAuthenticationSelector(authSelector);

            logger.info("Mirror configuration applied: {} mirror(s) registered", mirrorConfigs.size());

        } catch (Exception e) {
            // Graceful degradation: log the error and continue without mirrors
            logger.warn("Failed to configure mirrors for Maven resolver. Continuing without mirror support. Error: {}",
                e.getMessage());
            logger.debug("Mirror configuration exception details:", e);
        }
    }

    /**
     * Checks if any mirrors are configured.
     *
     * @return true if at least one mirror is configured
     */
    public boolean hasMirrors() {
        return !mirrorConfigs.isEmpty();
    }

    /**
     * Returns the number of configured mirrors.
     *
     * @return mirror count
     */
    public int getMirrorCount() {
        return mirrorConfigs.size();
    }
}

