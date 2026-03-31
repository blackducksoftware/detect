package com.blackduck.integration.detectable.detectables.maven.resolver.proxy;

import com.blackduck.integration.detectable.detectables.maven.resolver.settings.MavenSettingsParseException;
import com.blackduck.integration.detectable.detectables.maven.resolver.settings.MavenSettingsParser;
import com.blackduck.integration.detectable.detectables.maven.resolver.settings.MavenSettingsXml;
import com.blackduck.integration.detectable.detectables.maven.resolver.settings.MavenSettingsXmlProxy;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Service responsible for resolving Maven proxy configuration using a strict precedence hierarchy.
 *
 * <p><strong>Precedence Rules (Highest to Lowest):</strong>
 * <ol>
 *   <li><strong>settings.xml:</strong> If proxy configuration is found in settings.xml
 *       (either custom path or default {@code ~/.m2/settings.xml}), use it and ignore CLI flags completely.
 *       This allows Maven-specific proxy configuration to take precedence.</li>
 *   <li><strong>CLI Flags (Universal Proxy):</strong> If no settings.xml proxy is found, fall back to
 *       the global Black Duck proxy flags (blackduck.proxy.*).</li>
 *   <li><strong>No Proxy:</strong> If neither settings.xml nor CLI flags provide proxy configuration, return null
 *       (no proxy will be used).</li>
 * </ol>
 *
 * <p><strong>Rationale for Precedence:</strong>
 * settings.xml takes priority because it represents Maven-specific configuration that may differ from
 * the general Black Duck proxy (e.g., using Artifactory/Nexus as a caching proxy for Maven artifacts).
 * CLI flags serve as a universal fallback for the common case where the same proxy is used for both
 * Black Duck and Maven traffic.
 *
 * <p>This class encapsulates all proxy resolution logic to keep {@code DetectableOptionFactory} clean
 * and focused on reading configuration values.
 *
 * <p>Thread-safety: This class is stateless and thread-safe.
 */
public class MavenProxyConfigResolver {

    private static final Logger logger = LoggerFactory.getLogger(MavenProxyConfigResolver.class);

    private final MavenSettingsParser settingsParser;

    /**
     * Constructs a resolver with a new settings parser.
     */
    public MavenProxyConfigResolver() {
        this.settingsParser = new MavenSettingsParser();
    }

    /**
     * Resolves proxy configuration based on precedence: settings.xml → CLI flags → none.
     *
     * <p><strong>Priority Order:</strong>
     * <ol>
     *   <li>settings.xml proxy (highest priority - Maven-specific configuration)</li>
     *   <li>CLI flags (universal Black Duck proxy - fallback)</li>
     *   <li>No proxy (if neither source provides configuration)</li>
     * </ol>
     *
     * @param cliProxyHost         proxy host from CLI flag (may be null)
     * @param cliProxyPort         proxy port from CLI flag (0 means not configured)
     * @param cliProxyUsername     proxy username from CLI flag (may be null)
     * @param cliProxyPassword     proxy password from CLI flag (may be null)
     * @param cliProxyIgnoredHosts comma-separated list of hosts to bypass proxy (may be null)
     * @param settingsFilePath     custom settings.xml path (may be null, defaults to ~/.m2/settings.xml)
     * @return proxy configuration, or null if no proxy is configured
     */
    @Nullable
    public MavenProxyConfig resolve(
        @Nullable String cliProxyHost,
        int cliProxyPort,
        @Nullable String cliProxyUsername,
        @Nullable String cliProxyPassword,
        @Nullable List<String> cliProxyIgnoredHosts,
        @Nullable Path settingsFilePath
    ) {
        // PRIORITY 1: settings.xml (Maven-Specific Configuration)
        // Check settings.xml FIRST because it represents Maven-specific proxy configuration
        // that may differ from the general Black Duck proxy
        logger.debug("Checking for Maven proxy configuration in settings.xml...");

        try {
            MavenProxyConfig config;
            if (settingsFilePath != null) {
                // User provided explicit settings.xml path
                logger.info("Parsing proxies from specified settings.xml: {}", settingsFilePath);
                config = settingsParser.parseProxiesFromSettingsFile(settingsFilePath);
            } else {
                // Use default ~/.m2/settings.xml
                config = settingsParser.parseDefaultSettingsForProxy();
            }

            if (config != null) {
                logger.info("Using settings.xml proxy configuration. Ignoring CLI flags.");
                logger.info("settings.xml proxy: host='{}', port={}, hasAuth={}, nonProxyHosts={}",
                    config.getHost(), config.getPort(), config.hasAuthentication(), config.getNonProxyHosts().size());
                return config;
            } else {
                logger.debug("No active proxy found in settings.xml. Checking CLI flags...");
            }
        } catch (MavenSettingsParseException e) {
            // If user explicitly provided a custom path that fails, this is a hard error
            if (settingsFilePath != null) {
                throw new RuntimeException(
                    "Failed to parse proxy configuration from specified settings.xml: " + settingsFilePath,
                    e
                );
            }
            // If default settings.xml fails to parse, just log and continue (not a hard error)
            logger.warn("Failed to parse proxy configuration from default settings.xml: {}", e.getMessage());
            logger.debug("Full exception for settings.xml proxy parsing:", e);
            logger.debug("Falling back to CLI proxy flags...");
        }

        // PRIORITY 2: CLI Flags (Universal Black Duck Proxy - Fallback)
        // If settings.xml has no proxy, fall back to the universal Black Duck proxy configuration
        if (cliProxyHost != null && !cliProxyHost.trim().isEmpty() && cliProxyPort > 0) {
            logger.info("No settings.xml proxy found. Using CLI proxy flags (universal Black Duck proxy).");
            
            List<String> nonProxyHosts = cliProxyIgnoredHosts != null ? cliProxyIgnoredHosts : Collections.emptyList();
            
            MavenProxyConfig config = new MavenProxyConfig(
                cliProxyHost.trim(),
                cliProxyPort,
                cliProxyUsername,
                cliProxyPassword,
                nonProxyHosts
            );

            logger.info("CLI proxy configured: host='{}', port={}, hasAuth={}, nonProxyHosts={}",
                config.getHost(), config.getPort(), config.hasAuthentication(), config.getNonProxyHosts().size());

            return config;
        }

        // PRIORITY 3: No Proxy
        logger.debug("No proxy configuration available (neither settings.xml nor CLI flags)");
        return null;
    }
}

