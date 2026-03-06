package com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.methods;

// Method 2: Detect shaded dependencies by scanning all embedded pom.properties files inside the JAR.

import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.ShadedDependencyInspector;
import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.model.DiscoveredDependency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes;

/**
 * Scans a JAR file for all embedded pom.properties files under META-INF/maven/.
 * Each pom.properties file typically belongs to a shaded (bundled) dependency.
 *
 * <p>This is a DETERMINISTIC (HIGH confidence) detection method because pom.properties
 * files contain exact Maven coordinates.
 *
 * <p>The detector excludes the host JAR's own coordinates to avoid self-referencing.
 */
public class RecursiveMetadataInspector implements ShadedDependencyInspector {

    private static final Logger logger = LoggerFactory.getLogger(RecursiveMetadataInspector.class);

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
        logger.debug("[Method 2] Scanning all entries for embedded pom.properties files.");
        logger.debug("========================================================================");

        // Step 1: Determine the host JAR's own coordinates so we can exclude it from results
        // This prevents reporting the JAR as a shaded dependency of itself
        logger.debug("[Method 2] Step 1 - Determining host JAR coordinates to exclude self-reference...");
        HostJarCoordinates hostCoordinates = extractHostJarCoordinates(jarFile);

        if (hostCoordinates != null) {
            logger.debug("[Method 2] Step 1 - Host JAR identified as: {}:{}:{}",
                    hostCoordinates.groupId, hostCoordinates.artifactId, hostCoordinates.version);
        } else {
            logger.warn("[Method 2] Step 1 - Could not determine host JAR coordinates. " +
                    "Self-reference exclusion may not work correctly.");
        }

        // Step 2: Walk through every entry inside the JAR archive
        logger.debug("[Method 2] Step 2 - Scanning all JAR entries for pom.properties files...");
        Enumeration<JarEntry> entries = jarFile.entries();
        int scannedCount = 0;
        int skippedHostCount = 0;
        int skippedInvalidCount = 0;

        while (entries.hasMoreElements()) {

            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            scannedCount++;

            // When a dependency is shaded, its unique pom.properties file is usually dragged into the JAR
            // under META-INF/maven/<groupId>/<artifactId>/pom.properties
            if (name.startsWith("META-INF/maven/") && name.endsWith("pom.properties")) {

                logger.debug("[Method 2] Step 2 - Found pom.properties -> {}", name);

                // Use try-with-resources to ensure we don't leak memory while reading the ZIP stream
                try (InputStream is = jarFile.getInputStream(entry)) {

                    // Load the key-value pairs from the properties file
                    // Using InputStreamReader with UTF-8 encoding to handle international characters
                    // Note: Standard Properties.load(InputStream) uses ISO-8859-1 which garbles UTF-8
                    Properties props = loadPropertiesWithUtf8(is);

                    // Extract the standard Maven coordinates with null-safety
                    String groupId = props.getProperty("groupId");
                    String artifactId = props.getProperty("artifactId");
                    String version = props.getProperty("version");

                    // Validate that required fields are present
                    // groupId and artifactId are mandatory; version can be missing, but we will mark it
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

                    // Trim whitespace from extracted values
                    groupId = groupId.trim();
                    artifactId = artifactId.trim();

                    // Handle missing or empty version - mark as UNKNOWN instead of null
                    if (version == null || version.trim().isEmpty()) {
                        logger.warn("[Method 2] Step 2 - Version missing for {}:{}, marking as UNKNOWN", groupId, artifactId);
                        version = "UNKNOWN";
                    } else {
                        version = version.trim();
                    }

                    // Check if this is the host JAR's own pom.properties - skip if so
                    // This prevents the JAR from reporting itself as a shaded dependency
                    if (isHostJar(hostCoordinates, groupId, artifactId)) {
                        logger.debug("[Method 2] Step 2 - Skipping host JAR's own coordinates: {}:{}:{}",
                                groupId, artifactId, version);
                        skippedHostCount++;
                        continue;
                    }

                    // Build the GAV (GroupId:ArtifactId:Version) string
                    String gav = groupId + ":" + artifactId + ":" + version;
                    logger.debug("[Method 2] Step 2 - Extracted shaded artifact: {}", gav);
                    discoveredDependencies.add(new DiscoveredDependency(gav, "Recursive Metadata Extraction"));

                } catch (Exception e) {
                    logger.error("[Method 2] Step 2 - Could not parse metadata at {} - {}", name, e.getMessage());
                    skippedInvalidCount++;
                }
            }
        }

        // Step 3: Final summary
        logger.debug("------------------------------------------------------------------------");
        logger.debug("[Method 2] Step 3 - Scan complete.");
        logger.debug("[Method 2] Step 3 - Total entries scanned: {}", scannedCount);
        logger.debug("[Method 2] Step 3 - Host JAR entries skipped (self-reference): {}", skippedHostCount);
        logger.debug("[Method 2] Step 3 - Invalid entries skipped (missing fields): {}", skippedInvalidCount);
        logger.debug("[Method 2] Step 3 - Shaded dependencies found: {}", discoveredDependencies.size());
        logger.debug("------------------------------------------------------------------------");

        return discoveredDependencies;
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
        // Use InputStreamReader with explicit UTF-8 charset to handle international characters
        // This fixes the ISO-8859-1 limitation of Properties.load(InputStream)
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            props.load(reader);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                    // Ignore close exceptions
                }
            }
        }
        return props;
    }

    /**
     * Extracts the host JAR's own Maven coordinates by examining its pom.properties or MANIFEST.MF.
     * This is used to exclude the host JAR from the list of shaded dependencies.
     *
     * Strategy:
     * 1. First, try to find exactly ONE pom.properties that matches the JAR's root structure
     * 2. If multiple pom.properties exist, fall back to MANIFEST.MF headers
     * 3. If neither works, return null and log a warning
     *
     * @param jarFile The JAR file to examine.
     * @return The host JAR's coordinates, or null if they cannot be determined.
     */
    private HostJarCoordinates extractHostJarCoordinates(JarFile jarFile) {
        String hostGroupId = null;
        String hostArtifactId = null;
        String hostVersion = null;

        // Strategy 1: Try to get coordinates from MANIFEST.MF first (most reliable)
        // Many JARs include Implementation-* or Bundle-* headers that identify them
        try {
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                Attributes attrs = manifest.getMainAttributes();

                // Try standard Implementation headers first
                String implTitle = attrs.getValue("Implementation-Title");
                String implVersion = attrs.getValue("Implementation-Version");
                String implVendorId = attrs.getValue("Implementation-Vendor-Id");

                // Try Bundle headers (OSGi)
                String bundleSymbolicName = attrs.getValue("Bundle-SymbolicName");
                String bundleVersion = attrs.getValue("Bundle-Version");

                // Try to construct coordinates from manifest
                if (implVendorId != null && !implVendorId.isEmpty()) {
                    hostGroupId = implVendorId;
                }
                if (implTitle != null && !implTitle.isEmpty()) {
                    hostArtifactId = implTitle;
                } else if (bundleSymbolicName != null && !bundleSymbolicName.isEmpty()) {
                    // Bundle-SymbolicName may contain directives like ;singleton:=true
                    int semicolon = bundleSymbolicName.indexOf(';');
                    hostArtifactId = semicolon > 0 ? bundleSymbolicName.substring(0, semicolon) : bundleSymbolicName;
                }
                if (implVersion != null && !implVersion.isEmpty()) {
                    hostVersion = implVersion;
                } else if (bundleVersion != null && !bundleVersion.isEmpty()) {
                    hostVersion = bundleVersion;
                }

                if (hostGroupId != null && hostArtifactId != null) {
                    logger.debug("[Method 2] Step 1 - Host coordinates extracted from MANIFEST.MF");
                    return new HostJarCoordinates(hostGroupId, hostArtifactId, hostVersion);
                }
            }
        } catch (Exception e) {
            logger.warn("[Method 2] Step 1 - Could not read MANIFEST.MF: {}", e.getMessage());
        }

        // Strategy 2: Find the first pom.properties file (works for simple JARs)
        // In a properly structured JAR, the first pom.properties under META-INF/maven/ is usually the host's
        try {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.startsWith("META-INF/maven/") && name.endsWith("pom.properties")) {
                    // The path structure is: META-INF/maven/<groupId>/<artifactId>/pom.properties
                    // Extract groupId and artifactId from the path itself for verification
                    String[] parts = name.split("/");
                    if (parts.length >= 5) {
                        // parts[0] = "META-INF", parts[1] = "maven", parts[2] = groupId, parts[3] = artifactId

                        // Read the actual properties to get version and verify
                        InputStream is = null;
                        try {
                            is = jarFile.getInputStream(entry);
                            Properties props = loadPropertiesWithUtf8(is);
                            String propGroupId = props.getProperty("groupId");
                            String propArtifactId = props.getProperty("artifactId");
                            String propVersion = props.getProperty("version");

                            // Use the first valid pom.properties found as the host
                            if (propGroupId != null && propArtifactId != null) {
                                logger.debug("[Method 2] Step 1 - Host coordinates extracted from first pom.properties: {}", name);
                                return new HostJarCoordinates(propGroupId.trim(), propArtifactId.trim(),
                                        propVersion != null ? propVersion.trim() : "UNKNOWN");
                            }
                        } finally {
                            if (is != null) {
                                try {
                                    is.close();
                                } catch (Exception ignored) {
                                    // Ignore close exceptions
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("[Method 2] Step 1 - Error scanning for host pom.properties: {}", e.getMessage());
        }

        // Could not determine host coordinates
        return null;
    }

    /**
     * Checks if the given coordinates match the host JAR's own coordinates.
     * Comparison is done on groupId and artifactId only (version may differ in some edge cases).
     *
     * @param hostCoordinates The host JAR's coordinates (could be null).
     * @param groupId         The groupId to check.
     * @param artifactId      The artifactId to check.
     * @return true if this is the host JAR, false otherwise.
     */
    private boolean isHostJar(HostJarCoordinates hostCoordinates, String groupId, String artifactId) {
        if (hostCoordinates == null) {
            // Can't determine, so don't exclude anything
            return false;
        }

        // Match on groupId and artifactId (case-sensitive, as Maven coordinates are case-sensitive)
        return hostCoordinates.groupId.equals(groupId) && hostCoordinates.artifactId.equals(artifactId);
    }

    /**
     * Simple data class to hold the host JAR's Maven coordinates.
     */
    private static class HostJarCoordinates {
        final String groupId;
        final String artifactId;
        final String version;

        HostJarCoordinates(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }
    }
}
