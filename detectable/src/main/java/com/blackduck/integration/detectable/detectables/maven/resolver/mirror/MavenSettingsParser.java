package com.blackduck.integration.detectable.detectables.maven.resolver.mirror;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parser for Maven's settings.xml file to extract mirror configurations.
 *
 * <p>This parser reads a settings.xml file and extracts:
 * <ul>
 *   <li>Mirror definitions from {@code <mirrors>/<mirror>}</li>
 *   <li>Server credentials from {@code <servers>/<server>}</li>
 * </ul>
 *
 * <p>Server credentials are automatically matched to mirrors by their {@code id} field.
 *
 * <p><strong>Error Handling:</strong>
 * <ul>
 *   <li>If the file doesn't exist and was explicitly requested, throws {@link MavenSettingsParseException}</li>
 *   <li>If the file is malformed XML, throws {@link MavenSettingsParseException}</li>
 *   <li>Invalid mirror entries (missing required fields) are logged and skipped</li>
 * </ul>
 *
 * <p>Thread-safety: This class is stateless and thread-safe.
 */
public class MavenSettingsParser {

    private static final Logger logger = LoggerFactory.getLogger(MavenSettingsParser.class);
    private static final String DEFAULT_SETTINGS_PATH = ".m2/settings.xml";

    private final XmlMapper xmlMapper;

    /**
     * Constructs a MavenSettingsParser with a pre-configured Jackson XmlMapper.
     */
    public MavenSettingsParser() {
        this.xmlMapper = new XmlMapper();
        // Ignore unknown properties to be forward-compatible with newer settings.xml formats
        this.xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Parses mirror configurations from the default settings.xml location.
     *
     * <p>The default location is {@code ~/.m2/settings.xml}.
     *
     * <p>If the default file doesn't exist, an empty list is returned (not an error).
     *
     * @return list of mirror configurations, never null
     * @throws MavenSettingsParseException if the file exists but cannot be parsed
     */
    public List<MavenMirrorConfig> parseDefaultSettings() throws MavenSettingsParseException {
        Path defaultPath = Paths.get(System.getProperty("user.home"), DEFAULT_SETTINGS_PATH);
        File settingsFile = defaultPath.toFile();

        if (!settingsFile.exists()) {
            logger.debug("Default settings.xml not found at {}. Proceeding with no mirror configuration.", defaultPath);
            return Collections.emptyList();
        }

        return parseSettingsFile(settingsFile);
    }

    /**
     * Parses mirror configurations from a specified settings.xml file.
     *
     * <p>If the file doesn't exist, throws an exception (user explicitly requested this file).
     *
     * @param settingsFilePath path to the settings.xml file
     * @return list of mirror configurations, never null
     * @throws MavenSettingsParseException if the file doesn't exist or cannot be parsed
     */
    public List<MavenMirrorConfig> parseSettingsFile(Path settingsFilePath) throws MavenSettingsParseException {
        File settingsFile = settingsFilePath.toFile();

        if (!settingsFile.exists()) {
            throw new MavenSettingsParseException(
                "Specified settings.xml file does not exist: " + settingsFilePath.toAbsolutePath()
            );
        }

        return parseSettingsFile(settingsFile);
    }

    /**
     * Internal method that performs the actual parsing.
     *
     * @param settingsFile the settings.xml file to parse
     * @return list of mirror configurations
     * @throws MavenSettingsParseException if parsing fails
     */
    private List<MavenMirrorConfig> parseSettingsFile(File settingsFile) throws MavenSettingsParseException {
        logger.info("Parsing Maven settings.xml from: {}", settingsFile.getAbsolutePath());

        if (!settingsFile.canRead()) {
            throw new MavenSettingsParseException(
                "Cannot read settings.xml file (permission denied): " + settingsFile.getAbsolutePath()
            );
        }

        MavenSettingsXml settings;
        try {
            settings = xmlMapper.readValue(settingsFile, MavenSettingsXml.class);
        } catch (IOException e) {
            throw new MavenSettingsParseException(
                "Failed to parse settings.xml at " + settingsFile.getAbsolutePath() + ": " + e.getMessage(),
                e
            );
        }

        if (settings == null) {
            logger.warn("Parsed settings.xml is null. Returning empty mirror list.");
            return Collections.emptyList();
        }

        List<MavenSettingsXmlMirror> mirrors = settings.getMirrors();
        List<MavenSettingsXmlServer> servers = settings.getServers();

        if (mirrors.isEmpty()) {
            logger.info("No mirrors found in settings.xml at {}", settingsFile.getAbsolutePath());
            return Collections.emptyList();
        }

        // Build a map of server credentials by ID for quick lookup
        Map<String, MavenSettingsXmlServer> serverById = servers.stream()
            .filter(s -> s.getId() != null && !s.getId().trim().isEmpty())
            .collect(Collectors.toMap(
                MavenSettingsXmlServer::getId,
                s -> s,
                (a, b) -> a // Keep first if duplicate IDs
            ));

        // Convert mirrors to MavenMirrorConfig, attaching credentials where available
        List<MavenMirrorConfig> mirrorConfigs = new ArrayList<>();
        for (MavenSettingsXmlMirror mirror : mirrors) {
            if (!mirror.isValid()) {
                logger.warn("Skipping invalid mirror entry (missing id, url, or mirrorOf): {}", mirror);
                continue;
            }

            String id = mirror.getId().trim();
            String url = mirror.getUrl().trim();
            String mirrorOf = mirror.getMirrorOf().trim();

            // Look up matching server credentials
            String username = null;
            String password = null;
            MavenSettingsXmlServer server = serverById.get(id);
            if (server != null && server.hasCredentials()) {
                username = server.getUsername();
                password = server.getPassword();
                logger.debug("Found credentials for mirror '{}' from server configuration", id);
            }

            MavenMirrorConfig config = new MavenMirrorConfig(id, url, mirrorOf, username, password);
            mirrorConfigs.add(config);
            // SECURITY: The toString() method of MavenMirrorConfig redacts credentials.
            // Only hasAuth flag is logged, not actual username/password values.
            logger.debug("Loaded mirror configuration: {}", config);
        }

        logger.info("Loaded {} mirror(s) from settings.xml at {}", mirrorConfigs.size(), settingsFile.getAbsolutePath());
        return mirrorConfigs;
    }

    /**
     * Parses proxy configurations from the default settings.xml location.
     *
     * <p>The default location is {@code ~/.m2/settings.xml}.
     *
     * <p>If the default file doesn't exist, null is returned (not an error).
     *
     * @return proxy configuration if an active proxy is found, null otherwise
     * @throws MavenSettingsParseException if the file exists but cannot be parsed
     */
    @org.jetbrains.annotations.Nullable
    public MavenProxyConfig parseDefaultSettingsForProxy() throws MavenSettingsParseException {
        Path defaultPath = Paths.get(System.getProperty("user.home"), DEFAULT_SETTINGS_PATH);
        File settingsFile = defaultPath.toFile();

        if (!settingsFile.exists()) {
            logger.debug("Default settings.xml not found at {}. No proxy configuration available from settings.", defaultPath);
            return null;
        }

        return parseProxiesFromSettingsFile(settingsFile);
    }

    /**
     * Parses proxy configurations from a specified settings.xml file.
     *
     * <p>If the file doesn't exist, throws an exception (user explicitly requested this file).
     *
     * @param settingsFilePath path to the settings.xml file
     * @return proxy configuration if an active proxy is found, null otherwise
     * @throws MavenSettingsParseException if the file doesn't exist or cannot be parsed
     */
    @org.jetbrains.annotations.Nullable
    public MavenProxyConfig parseProxiesFromSettingsFile(Path settingsFilePath) throws MavenSettingsParseException {
        File settingsFile = settingsFilePath.toFile();

        if (!settingsFile.exists()) {
            throw new MavenSettingsParseException(
                "Specified settings.xml file does not exist: " + settingsFilePath.toAbsolutePath()
            );
        }

        return parseProxiesFromSettingsFile(settingsFile);
    }

    /**
     * Internal method that parses proxy configurations from a settings.xml file.
     *
     * <p>Maven's proxy behavior:
     * <ul>
     *   <li>Only ONE proxy is used (the first active proxy for the matching protocol)</li>
     *   <li>If no protocol is specified in settings, defaults to "http"</li>
     *   <li>We prefer the first active proxy marked as "http" or with no protocol specified</li>
     * </ul>
     *
     * @param settingsFile the settings.xml file to parse
     * @return proxy configuration if an active proxy is found, null otherwise
     * @throws MavenSettingsParseException if parsing fails
     */
    @org.jetbrains.annotations.Nullable
    private MavenProxyConfig parseProxiesFromSettingsFile(File settingsFile) throws MavenSettingsParseException {
        logger.debug("Parsing proxy configuration from Maven settings.xml: {}", settingsFile.getAbsolutePath());

        // Parse the XML file
        MavenSettingsXml settings = parseXmlFile(settingsFile);
        if (settings == null) {
            logger.debug("Parsed settings.xml is null. No proxy configuration available.");
            return null;
        }

        List<MavenSettingsXmlProxy> proxies = settings.getProxies();
        if (proxies.isEmpty()) {
            logger.debug("No proxies found in settings.xml at {}", settingsFile.getAbsolutePath());
            return null;
        }

        // Find the first active HTTP proxy
        MavenSettingsXmlProxy activeProxy = findFirstActiveHttpProxy(proxies);
        if (activeProxy == null) {
            logger.debug("No active HTTP proxy found in settings.xml at {}", settingsFile.getAbsolutePath());
            return null;
        }

        // Convert to MavenProxyConfig
        MavenProxyConfig config = buildProxyConfig(activeProxy);

        // SECURITY: The toString() method of MavenProxyConfig redacts credentials.
        // Only hasAuth flag is logged, not actual username/password values.
        logger.info("Loaded proxy configuration from settings.xml: {}", config);

        return config;
    }

    /**
     * Parses the settings.xml file into a MavenSettingsXml object.
     *
     * @param settingsFile the settings.xml file to parse
     * @return parsed settings object, or null if parsing results in null
     * @throws MavenSettingsParseException if file cannot be read or parsed
     */
    @org.jetbrains.annotations.Nullable
    private MavenSettingsXml parseXmlFile(File settingsFile) throws MavenSettingsParseException {
        if (!settingsFile.canRead()) {
            throw new MavenSettingsParseException(
                "Cannot read settings.xml file (permission denied): " + settingsFile.getAbsolutePath()
            );
        }

        try {
            return xmlMapper.readValue(settingsFile, MavenSettingsXml.class);
        } catch (IOException e) {
            throw new MavenSettingsParseException(
                "Failed to parse settings.xml at " + settingsFile.getAbsolutePath() + ": " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Finds the first active HTTP proxy from a list of proxy configurations.
     *
     * <p>Maven uses the first active proxy that matches the protocol. We look for:
     * <ul>
     *   <li>Valid proxies (have host and port)</li>
     *   <li>Active proxies (active flag is true or null)</li>
     *   <li>HTTP proxies (protocol is "http" or null/empty)</li>
     * </ul>
     *
     * @param proxies list of proxy configurations from settings.xml
     * @return the first matching active HTTP proxy, or null if none found
     */
    @org.jetbrains.annotations.Nullable
    private MavenSettingsXmlProxy findFirstActiveHttpProxy(List<MavenSettingsXmlProxy> proxies) {
        for (MavenSettingsXmlProxy proxy : proxies) {
            if (!proxy.isValid()) {
                logger.debug("Skipping invalid proxy entry (missing host or port): {}", proxy);
                continue;
            }

            if (!proxy.isActive()) {
                logger.debug("Skipping inactive proxy: {}", proxy.getId());
                continue;
            }

            // Accept proxies with protocol "http" or no protocol specified (defaults to http)
            String protocol = proxy.getProtocol();
            if (isHttpProtocol(protocol)) {
                logger.info("Found active HTTP proxy in settings.xml: {} (id: {})", 
                    proxy.getHost() + ":" + proxy.getPort(), proxy.getId());
                return proxy; // Maven uses the first matching active proxy
            } else {
                logger.debug("Skipping proxy with unsupported protocol '{}': {}", protocol, proxy.getId());
            }
        }

        return null; // No active HTTP proxy found
    }

    /**
     * Checks if a protocol string represents HTTP (or defaults to HTTP).
     *
     * @param protocol the protocol string (may be null or empty)
     * @return true if protocol is null, empty, or "http" (case-insensitive)
     */
    private boolean isHttpProtocol(@org.jetbrains.annotations.Nullable String protocol) {
        return protocol == null || protocol.trim().isEmpty() || "http".equalsIgnoreCase(protocol.trim());
    }

    /**
     * Builds a MavenProxyConfig from a MavenSettingsXmlProxy.
     *
     * <p>Converts the nonProxyHosts pipe-delimited string into a list.
     *
     * @param activeProxy the active proxy from settings.xml
     * @return configured MavenProxyConfig object
     */
    private MavenProxyConfig buildProxyConfig(MavenSettingsXmlProxy activeProxy) {
        List<String> nonProxyHostsList = parseNonProxyHosts(activeProxy.getNonProxyHosts());

        return new MavenProxyConfig(
            activeProxy.getHost().trim(),
            activeProxy.getPort(),
            activeProxy.getUsername(),
            activeProxy.getPassword(),
            nonProxyHostsList
        );
    }

    /**
     * Parses a pipe-delimited nonProxyHosts string into a list.
     *
     * <p>Example: "localhost|127.0.0.1|*.company.com" becomes ["localhost", "127.0.0.1", "*.company.com"]
     *
     * @param nonProxyHostsString pipe-delimited string (may be null or empty)
     * @return list of host patterns, empty if input is null/empty
     */
    private List<String> parseNonProxyHosts(@org.jetbrains.annotations.Nullable String nonProxyHostsString) {
        List<String> result = new ArrayList<>();

        if (nonProxyHostsString != null && !nonProxyHostsString.trim().isEmpty()) {
            String[] hosts = nonProxyHostsString.split("\\|");
            for (String host : hosts) {
                String trimmed = host.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
        }

        return result;
    }
}


