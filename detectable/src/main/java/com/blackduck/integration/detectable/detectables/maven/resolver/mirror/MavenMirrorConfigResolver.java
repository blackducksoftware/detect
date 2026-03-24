package com.blackduck.integration.detectable.detectables.maven.resolver.mirror;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Resolves the active Maven mirror configurations using a strict precedence hierarchy.
 *
 * <p>This class owns all mirror resolution logic, keeping it co-located with the mirror
 * domain rather than scattered across the broader Detect configuration layer.
 *
 * <p><strong>Precedence order (highest to lowest):</strong>
 * <ol>
 *   <li><strong>CLI flags</strong> — if a mirror URL is provided via command-line, it is used
 *       unconditionally and the settings file is never consulted.</li>
 *   <li><strong>settings.xml</strong> — if no CLI mirror is present, mirrors are parsed from
 *       the settings file (explicit path or the default {@code ~/.m2/settings.xml}).</li>
 *   <li><strong>No mirrors</strong> — if neither source yields any mirrors, an empty list is
 *       returned and resolution proceeds without mirror support.</li>
 * </ol>
 *
 * <p>Thread-safety: This class is stateless and thread-safe.
 */
public class MavenMirrorConfigResolver {

    private static final Logger logger = LoggerFactory.getLogger(MavenMirrorConfigResolver.class);

    private final MavenSettingsParser settingsParser;

    /**
     * Constructs a resolver using the default {@link MavenSettingsParser}.
     */
    public MavenMirrorConfigResolver() {
        this(new MavenSettingsParser());
    }

    /**
     * Constructs a resolver with a custom {@link MavenSettingsParser}.
     * Provided primarily for unit testing.
     *
     * @param settingsParser the parser to use for reading settings.xml files
     */
    public MavenMirrorConfigResolver(MavenSettingsParser settingsParser) {
        this.settingsParser = settingsParser;
    }

    /**
     * Resolves mirror configurations from the given raw inputs.
     *
     * <p>Callers (e.g. the Detect configuration layer) are responsible only for reading the raw
     * property values and passing them here. All precedence logic, defaulting, and error handling
     * live in this method.
     *
     * @param cliMirrorUrl      Mirror URL from CLI flag — null if not provided.
     * @param cliMirrorOf       Repository matching pattern from CLI flag — null if not provided.
     *                          Defaults to {@code *} when the URL is set but this is absent.
     * @param cliUsername       Optional mirror authentication username from CLI flag.
     * @param cliPassword       Optional mirror authentication password from CLI flag.
     * @param settingsFilePath  Explicit path to settings.xml — null means use the default
     *                          {@code ~/.m2/settings.xml} location.
     * @return resolved list of mirror configurations, never null, may be empty
     */
    public List<MavenMirrorConfig> resolve(
        @Nullable String cliMirrorUrl,
        @Nullable String cliMirrorOf,
        @Nullable String cliUsername,
        @Nullable String cliPassword,
        @Nullable Path settingsFilePath
    ) {
        // Priority 1: CLI Override
        if (cliMirrorUrl != null && !cliMirrorUrl.trim().isEmpty()) {
            return resolveFromCli(cliMirrorUrl, cliMirrorOf, cliUsername, cliPassword);
        }

        // Priority 2: settings.xml Fallback
        return resolveFromSettingsFile(settingsFilePath);
    }

    // --- private helpers ---------------------------------------------------

    private List<MavenMirrorConfig> resolveFromCli(
        String mirrorUrl,
        @Nullable String mirrorOf,
        @Nullable String username,
        @Nullable String password
    ) {
        logger.info("Using CLI mirror configuration. Ignoring settings.xml mirrors.");

        // If mirror.url is provided but mirror.of is blank, default mirror.of to * (intercept everything)
        // to prevent unexpected bypasses.
        if (mirrorOf == null || mirrorOf.trim().isEmpty()) {
            mirrorOf = "*";
            logger.info("No mirrorOf pattern specified. Defaulting to '*' (intercept all repositories).");
        }

        String trimmedUrl      = mirrorUrl.trim();
        String trimmedMirrorOf = mirrorOf.trim();

        MavenMirrorConfig cliMirror = new MavenMirrorConfig(
            "detect-cli-mirror",
            trimmedUrl,
            trimmedMirrorOf,
            username,
            password
        );

        logger.info("CLI mirror configured: url='{}', mirrorOf='{}', hasAuth={}",
            trimmedUrl, trimmedMirrorOf, (username != null && password != null));

        return Collections.singletonList(cliMirror);
    }

    private List<MavenMirrorConfig> resolveFromSettingsFile(@Nullable Path settingsFilePath) {
        try {
            List<MavenMirrorConfig> mirrorConfigs;

            if (settingsFilePath != null) {
                // Explicit path provided — fail if file doesn't exist
                logger.info("Parsing mirrors from specified settings.xml: {}", settingsFilePath);
                mirrorConfigs = settingsParser.parseSettingsFile(settingsFilePath);
            } else {
                // Use default path (~/.m2/settings.xml) — don't fail if missing
                mirrorConfigs = settingsParser.parseDefaultSettings();
            }

            if (!mirrorConfigs.isEmpty()) {
                logger.info("Loaded {} mirror(s) from settings.xml", mirrorConfigs.size());
            }

            return mirrorConfigs;

        } catch (MavenSettingsParseException e) {
            // If the settings file fails to parse, propagate as a runtime exception so the
            // extraction fails clearly. Silent fallback would hide a broken configuration.
            logger.error("Failed to parse Maven settings.xml: {}", e.getMessage());
            throw new RuntimeException(
                "Failed to parse Maven settings.xml for mirror configuration: " + e.getMessage(), e
            );
        }
    }
}

