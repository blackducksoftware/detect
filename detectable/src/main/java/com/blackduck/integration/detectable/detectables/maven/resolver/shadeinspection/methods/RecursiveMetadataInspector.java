package com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.methods;

// Method 2: Detect shaded dependencies by scanning all embedded pom.properties files inside the JAR.

import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.ShadedDependencyInspector;
import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.model.DiscoveredDependency;

import org.eclipse.aether.artifact.Artifact;
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
 * <p>The detector excludes the host JAR's own coordinates to avoid self-referencing.
 */
public class RecursiveMetadataInspector implements ShadedDependencyInspector {

    private static final Logger logger = LoggerFactory.getLogger(RecursiveMetadataInspector.class);

    private final Artifact hostArtifact;

    /**
     * Initializes the inspector with the host artifact for reliable self-reference exclusion.
     *
     * @param hostArtifact The host artifact whose JAR is being inspected (used to exclude self-reference)
     */
    public RecursiveMetadataInspector(Artifact hostArtifact) {
        this.hostArtifact = hostArtifact;
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
        logger.debug("[Method 2] Scanning all entries for embedded pom.properties files.");
        logger.debug("========================================================================");

        // Step 1: Host JAR coordinates are provided via constructor
        logger.debug("[Method 2] Step 1 - Using provided host artifact coordinates for self-reference exclusion...");

        if (hostArtifact != null) {
            logger.debug("[Method 2] Step 1 - Host JAR identified as: {}:{}:{}",
                    hostArtifact.getGroupId(), hostArtifact.getArtifactId(), hostArtifact.getVersion());
        } else {
            logger.warn("[Method 2] Step 1 - Host artifact not provided. " +
                    "Self-reference exclusion may not work correctly.");
        }

        // Step 1.5: Pre-scan all JAR entries to build a set of class file directory prefixes
        // This is used to detect ghost dependencies (metadata without classes)
        logger.debug("[Method 2] Step 1.5 - Pre-scanning JAR entries for .class file prefixes...");
        // TreeSet gives O(log N) ghost lookups via ceiling() instead of O(N) linear scan
        NavigableSet<String> classPathPrefixes = new TreeSet<String>();
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

        // Step 2: Walk through every entry inside the JAR archive
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
                    if (hostArtifact != null
                            && hostArtifact.getGroupId().equals(groupId)
                            && hostArtifact.getArtifactId().equals(artifactId)) {
                        logger.debug("[Method 2] Step 2 - Skipping host JAR's own coordinates: {}:{}:{}",
                                groupId, artifactId, version);
                        skippedHostCount++;
                        continue;
                    }

                    // Check for ghost dependency: metadata exists but no .class files for this groupId
                    String expectedClassPrefix = groupId.replace('.', '/') + "/";
                    String ceiling = classPathPrefixes.ceiling(expectedClassPrefix);
                    boolean hasClasses = ceiling != null && ceiling.startsWith(expectedClassPrefix);
                    if (!hasClasses) {
                        logger.debug("[Method 2] Step 2 - Skipping ghost dependency (no .class files found "
                                + "for prefix '{}'): {}:{}:{}", expectedClassPrefix, groupId, artifactId, version);
                        skippedGhostCount++;
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
        logger.debug("[Method 2] Step 3 - Ghost entries skipped (metadata without classes): {}", skippedGhostCount);
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

}
