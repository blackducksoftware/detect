package com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.methods;

// Method 2: Detect shaded dependencies by scanning all embedded pom.properties files inside the JAR.

import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.ShadedDependencyInspector;
import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.model.DiscoveredDependency;
import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.model.ShadePluginConfig;

import org.eclipse.aether.artifact.Artifact;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.NavigableSet;
import java.util.Properties;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Scans a JAR file for all embedded pom.properties files under META-INF/maven/.
 * Each pom.properties file typically belongs to a shaded (bundled) dependency.
 *
 * <p>This is a DETERMINISTIC (HIGH confidence) detection method because pom.properties
 * files contain exact Maven coordinates.
 *
 * <h2>Ghost Filter (relocation-aware)</h2>
 * <p>A dep found via pom.properties is a ghost only if it is explicitly listed in the
 * shade plugin's {@code <artifactSet><excludes>}. This replaces the old class-path prefix
 * heuristic which incorrectly dropped relocated deps because their classes live at the
 * relocated path, not the original groupId path.
 *
 * <p>If no {@link ShadePluginConfig} is available, falls back to the legacy class-path
 * prefix heuristic.
 */
public class RecursiveMetadataInspector implements ShadedDependencyInspector {

    private static final Logger logger = LoggerFactory.getLogger(RecursiveMetadataInspector.class);

    private final Artifact hostArtifact;

    /**
     * Parsed shade plugin configuration for the host artifact's JAR.
     * May be null — in that case the legacy class-path heuristic is used as fallback.
     */
    @Nullable
    private final ShadePluginConfig shadePluginConfig;

    /**
     * Initializes the inspector with the host artifact and parsed shade configuration.
     *
     * @param hostArtifact     The host artifact whose JAR is being inspected (used to exclude self-reference)
     * @param shadePluginConfig Parsed shade config (excludes + relocations); may be null → fallback to legacy heuristic
     */
    public RecursiveMetadataInspector(Artifact hostArtifact, @Nullable ShadePluginConfig shadePluginConfig) {
        this.hostArtifact = hostArtifact;
        this.shadePluginConfig = shadePluginConfig;
    }

    /**
     * Iterates over every entry in the JAR, looking for pom.properties files.
     * Extracts groupId, artifactId, and version from each one found.
     * Excludes the host JAR's own coordinates to avoid self-referencing.
     *
     * @param jarFile The JAR file to scan.
     * @return List of discovered shaded dependencies.
     */
    @Override
    public List<DiscoveredDependency> detectShadedDependencies(JarFile jarFile) {

        List<DiscoveredDependency> discoveredDependencies = new ArrayList<DiscoveredDependency>();

        logger.debug("========================================================================");
        logger.debug("[Method 2] Starting Recursive Metadata Detection...");
        logger.debug("[Method 2] ShadePluginConfig available: {}", shadePluginConfig != null);
        logger.debug("========================================================================");

        if (hostArtifact != null) {
            logger.debug("[Method 2] Step 1 - Host JAR identified as: {}:{}:{}",
                    hostArtifact.getGroupId(), hostArtifact.getArtifactId(), hostArtifact.getVersion());
        } else {
            logger.warn("[Method 2] Step 1 - Host artifact not provided. Self-reference exclusion may not work correctly.");
        }

        // Legacy fallback: pre-scan .class file path prefixes only if we have no shade config.
        NavigableSet<String> classPathPrefixes = null;
        if (shadePluginConfig == null) {
            logger.debug("[Method 2] Step 1.5 - No ShadePluginConfig — falling back to legacy class-path ghost filter.");
            classPathPrefixes = new TreeSet<String>();
            Enumeration<JarEntry> prePassEntries = jarFile.entries();
            while (prePassEntries.hasMoreElements()) {
                String entryName = prePassEntries.nextElement().getName();
                if (entryName.endsWith(".class")) {
                    int lastSlash = entryName.lastIndexOf('/');
                    if (lastSlash > 0) {
                        classPathPrefixes.add(entryName.substring(0, lastSlash + 1));
                    }
                }
            }
            logger.debug("[Method 2] Step 1.5 - Found {} unique class path prefixes in JAR.", classPathPrefixes.size());
        }

        // Walk through every entry inside the JAR archive looking for pom.properties files
        logger.debug("[Method 2] Step 2 - Scanning all JAR entries for pom.properties files...");
        Enumeration<JarEntry> entries = jarFile.entries();
        int scannedCount = 0;
        int skippedHostCount = 0;
        int skippedInvalidCount = 0;
        int skippedGhostCount = 0;

        while (entries.hasMoreElements()) {

            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            scannedCount++;

            if (name.startsWith("META-INF/maven/") && name.endsWith("pom.properties")) {

                logger.debug("[Method 2] Step 2 - Found pom.properties -> {}", name);

                try (InputStream is = jarFile.getInputStream(entry)) {

                    Properties props = loadPropertiesWithUtf8(is);

                    String groupId = props.getProperty("groupId");
                    String artifactId = props.getProperty("artifactId");
                    String version = props.getProperty("version");

                    if (groupId == null || groupId.trim().isEmpty()) {
                        logger.warn("[Method 2] Step 2 - Skipping entry (missing groupId): {}", name);
                        skippedInvalidCount++;
                        continue;
                    }

                    if (artifactId == null || artifactId.trim().isEmpty()) {
                        logger.warn("[Method 2] Step 2 - Skipping entry (missing artifactId): {}", name);
                        skippedInvalidCount++;
                        continue;
                    }

                    groupId = groupId.trim();
                    artifactId = artifactId.trim();

                    if (version == null || version.trim().isEmpty()) {
                        logger.warn("[Method 2] Step 2 - Version missing for {}:{}, marking as UNKNOWN", groupId, artifactId);
                        version = "UNKNOWN";
                    } else {
                        version = version.trim();
                    }

                    // Skip the host JAR's own pom.properties — prevents self-reporting
                    if (hostArtifact != null
                            && hostArtifact.getGroupId().equals(groupId)
                            && hostArtifact.getArtifactId().equals(artifactId)) {
                        logger.debug("[Method 2] Step 2 - Skipping host JAR's own coordinates: {}:{}:{}", groupId, artifactId, version);
                        skippedHostCount++;
                        continue;
                    }

                    // Ghost filter: only drop the dep if it was explicitly excluded from the bundle
                    if (isGhost(groupId, artifactId, version, classPathPrefixes)) {
                        skippedGhostCount++;
                        continue;
                    }

                    String gav = groupId + ":" + artifactId + ":" + version;
                    logger.debug("[Method 2] Step 2 - Extracted shaded artifact: {}", gav);
                    discoveredDependencies.add(new DiscoveredDependency(gav, "Recursive Metadata Extraction"));

                } catch (Exception e) {
                    logger.error("[Method 2] Step 2 - Could not parse metadata at {} - {}", name, e.getMessage());
                    skippedInvalidCount++;
                }
            }
        }

        logger.debug("------------------------------------------------------------------------");
        logger.debug("[Method 2] Step 3 - Scan complete.");
        logger.debug("[Method 2] Step 3 - Total entries scanned: {}", scannedCount);
        logger.debug("[Method 2] Step 3 - Host JAR entries skipped (self-reference): {}", skippedHostCount);
        logger.debug("[Method 2] Step 3 - Invalid entries skipped (missing fields): {}", skippedInvalidCount);
        logger.debug("[Method 2] Step 3 - Ghost entries skipped: {}", skippedGhostCount);
        logger.debug("[Method 2] Step 3 - Shaded dependencies found: {}", discoveredDependencies.size());
        logger.debug("------------------------------------------------------------------------");

        return discoveredDependencies;
    }

    /**
     * Determines whether a pom.properties candidate is a ghost (not actually bundled).
     *
     * <p><strong>Primary path (ShadePluginConfig available):</strong>
     * A dep is a ghost if and only if it is explicitly listed in {@code <artifactSet><excludes>}.
     * Relocated deps are NOT in the excludes list, so they correctly pass through.
     *
     * <p><strong>Fallback path (ShadePluginConfig null):</strong>
     * Legacy class-path prefix heuristic — incorrectly drops relocated deps (known limitation).
     */
    private boolean isGhost(
            String groupId,
            String artifactId,
            String version,
            @Nullable NavigableSet<String> classPathPrefixesFallback
    ) {
        if (shadePluginConfig != null) {
            if (shadePluginConfig.isExcluded(groupId, artifactId)) {
                logger.debug("[Method 2] Ghost (explicitly excluded in shade config): {}:{}:{}", groupId, artifactId, version);
                return true;
            }
            if (shadePluginConfig.isRelocated(groupId)) {
                logger.debug("[Method 2] Dep was relocated, confirming as shaded: {}:{}:{}", groupId, artifactId, version);
            }
            return false;
        }

        // Fallback: legacy class-path heuristic
        if (classPathPrefixesFallback != null) {
            String expectedClassPrefix = groupId.replace('.', '/') + "/";
            String ceiling = classPathPrefixesFallback.ceiling(expectedClassPrefix);
            boolean hasClasses = ceiling != null && ceiling.startsWith(expectedClassPrefix);
            if (!hasClasses) {
                logger.debug("[Method 2] Ghost (legacy fallback — no .class files for '{}'): {}:{}:{}",
                        expectedClassPrefix, groupId, artifactId, version);
                return true;
            }
        }

        return false;
    }

    /**
     * Loads properties from an InputStream using UTF-8 encoding.
     * Standard Properties.load(InputStream) uses ISO-8859-1 which cannot handle UTF-8 characters.
     * This method wraps the stream with a UTF-8 reader to properly handle international characters.
     *
     * @param is The input stream to read properties from.
     * @return A Properties object with correctly decoded values.
     * @throws Exception If reading the stream fails.
     */
    private Properties loadPropertiesWithUtf8(InputStream is) throws Exception {
        Properties props = new Properties();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            props.load(reader);
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (Exception ignored) {}
            }
        }
        return props;
    }
}
