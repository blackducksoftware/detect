package com.blackduck.integration.detectable.detectables.maven.resolver.mirror;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Service responsible for resolving Maven proxy configuration using a strict precedence hierarchy.
 *
 * <p><strong>Precedence Rules (Highest to Lowest):</strong>
 * <ol>
 *   <li><strong>CLI Flags:</strong> If proxy host and port are provided via CLI, use those settings
 *       and ignore settings.xml completely.</li>
 *   <li><strong>settings.xml:</strong> If no CLI flags are provided, attempt to read proxy configuration
 *       from settings.xml (either custom path or default {@code ~/.m2/settings.xml}).</li>
 *   <li><strong>No Proxy:</strong> If neither CLI nor settings.xml provide proxy configuration, return null
 *       (no proxy will be used).</li>
 * </ol>
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
     * Resolves proxy configuration based on precedence: CLI flags → settings.xml → none.
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
        // PRIORITY 1: CLI Override
        // If CLI provides proxy host and a valid port, construct config from CLI flags and skip settings.xml
        if (cliProxyHost != null && !cliProxyHost.trim().isEmpty() && cliProxyPort > 0) {
            logger.info("Using CLI proxy configuration. Ignoring settings.xml proxy.");
            
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

        // PRIORITY 2: XML Fallback (settings.xml)
        // If no CLI proxy is configured, try to load from settings.xml
        logger.debug("No CLI proxy configuration found. Attempting to load from settings.xml...");

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
                logger.info("Using proxy configuration from settings.xml");
                return config;
            } else {
                logger.debug("No active proxy found in settings.xml");
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
        }

        // PRIORITY 3: No Proxy
        logger.debug("No proxy configuration available (neither CLI nor settings.xml)");
        return null;
    }
}

