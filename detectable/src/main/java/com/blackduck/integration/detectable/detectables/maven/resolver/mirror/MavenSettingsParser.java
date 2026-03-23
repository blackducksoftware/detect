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
            logger.debug("Loaded mirror configuration: {}", config);
        }

        logger.info("Loaded {} mirror(s) from settings.xml at {}", mirrorConfigs.size(), settingsFile.getAbsolutePath());
        return mirrorConfigs;
    }
}

