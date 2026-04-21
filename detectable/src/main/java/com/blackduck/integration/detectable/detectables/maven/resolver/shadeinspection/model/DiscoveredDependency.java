package com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.model;


import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a discovered shaded dependency within a JAR file.
 *
 * This model is flexible to support different types of discovery:
 *   - GAV coordinates (groupId:artifactId:version) from Maven metadata
 *   - Package names from OSGi manifests
 *   - Fully qualified class names from SPI descriptors
 *   - Pseudo-GAV derived from package/class names
 *
 * Each discovery includes:
 *   - An identifier (GAV, package name, or FQCN)
 *   - The detection method that found it
 *   - A confidence level (HIGH for deterministic, MEDIUM/LOW for heuristic)
 *   - Optional additional metadata
 *
 * Confidence Classification:
 *   - DETERMINISTIC (HIGH): 100% certain - found via exact metadata (pom.properties, POM delta)
 *   - HEURISTIC (MEDIUM): ~70-90% certain - inferred from related metadata (OSGi headers)
 *   - HEURISTIC (LOW): ~30-60% certain - weak indicators (SPI class names, patterns)
 */
public class DiscoveredDependency {

    /**
     * Confidence level indicating how reliable the detection is.
     *
     * HIGH = Deterministic (100% certain) - based on exact Maven metadata
     * MEDIUM = Heuristic (~70-90% certain) - based on strong indirect indicators
     * LOW = Heuristic (~30-60% certain) - based on weak indicators, needs verification
     */
    public enum Confidence {
        /** Deterministic detection - 100% reliability (e.g., exact POM delta, pom.properties) */
        HIGH,
        /** Heuristic detection - ~70-90% reliability (e.g., OSGi packages with version info) */
        MEDIUM,
        /** Low confidence - ~30-60% reliability, requires manual verification */
        LOW
    }

    /**
     * Type of identifier stored in this dependency.
     */
    public enum IdentifierType {
        /** Standard Maven GAV coordinate (groupId:artifactId:version) */
        GAV,
        /** Java package name (e.g., org.apache.commons.lang3) */
        PACKAGE,
        /** Fully qualified class name (e.g., com.mysql.cj.jdbc.Driver) */
        FQCN,
        /** Pseudo-GAV derived from package/class structure */
        PSEUDO_GAV
    }

    // Primary identifier for the dependency (GAV, package name, or FQCN)
    private final String identifier;

    // The detection method that discovered this dependency
    private final String detectionSource;

    // Confidence level of the detection
    private final Confidence confidence;

    // Type of the identifier
    private final IdentifierType identifierType;

    // Optional additional metadata (e.g., original FQCN when pseudo-GAV is used)
    // Java 8 compatible: explicit type parameter
    private final Map<String, String> metadata;

    /**
     * Creates a new DiscoveredDependency with default confidence based on detection source.
     * This constructor maintains backward compatibility with existing code.
     *
     * @param identifier      The dependency identifier (GAV, package, or FQCN).
     * @param detectionSource The method/detector that found this dependency.
     */
    public DiscoveredDependency(String identifier, String detectionSource) {
        this(identifier, detectionSource, inferConfidence(detectionSource), inferIdentifierType(detectionSource));
    }

    /**
     * Creates a new DiscoveredDependency with explicit confidence level.
     *
     * @param identifier      The dependency identifier (GAV, package, or FQCN).
     * @param detectionSource The method/detector that found this dependency.
     * @param confidence      The confidence level of this detection.
     * @param identifierType  The type of identifier.
     */
    public DiscoveredDependency(String identifier, String detectionSource, Confidence confidence, IdentifierType identifierType) {
        this.identifier = identifier;
        this.detectionSource = detectionSource;
        this.confidence = confidence;
        this.identifierType = identifierType;
        this.metadata = new HashMap<String, String>();
    }

    /**
     * Infers confidence level based on the detection source.
     *
     * Classification criteria:
     *
     * DETERMINISTIC (HIGH - 100% certain):
     *   - Delta Analysis (Exact): Direct comparison of POM files - mathematically certain
     *   - Recursive Metadata Extraction: Direct reading of pom.properties - exact match
     *
     * HEURISTIC (MEDIUM - ~70-90% certain):
     *   - Original POM (Needs external Delta): Has POM but no reduced version for comparison
     *   - OSGi Export-Package: Package is bundled, likely from a shaded dependency
     *
     * HEURISTIC (LOW - ~30-60% certain):
     *   - OSGi Import-Package: Could be runtime dependency, not necessarily shaded
     *   - SPI Descriptor: Class name pattern matching, may produce false positives
     *
     * @param detectionSource The detection source string.
     * @return Inferred confidence level.
     */
    private static Confidence inferConfidence(String detectionSource) {
        if (detectionSource == null) {
            return Confidence.LOW;
        }

        String source = detectionSource.toLowerCase();

        // DETERMINISTIC (HIGH - 100% certain)
        // These methods read exact Maven metadata and are mathematically certain
        if (source.contains("delta analysis (exact)")) {
            // Direct POM delta - found in original POM but not in reduced POM
            // This is the gold standard - 100% certain
            return Confidence.HIGH;
        }
        if (source.contains("recursive metadata extraction") ||
                source.contains("recursive metadata")) {
            // Direct reading of pom.properties embedded in the JAR
            // Each pom.properties file is definitive proof of a bundled artifact
            return Confidence.HIGH;
        }

        // HEURISTIC (MEDIUM - ~70-90% certain)
        // These methods provide strong indirect evidence
        if (source.contains("original pom")) {
            // Has POM metadata but couldn't do delta comparison
            // Still fairly reliable as it's reading Maven coordinates
            return Confidence.MEDIUM;
        }
        if (source.contains("osgi export-package")) {
            // Exported packages are bundled in the JAR
            // High correlation with shaded dependencies
            return Confidence.MEDIUM;
        }

        // HEURISTIC (LOW - ~30-60% certain)
        // These methods use pattern matching and may have false positives
        if (source.contains("osgi import-package")) {
            // Imported packages could be runtime dependencies, not shaded
            // Lower confidence than exports
            return Confidence.LOW;
        }
        if (source.contains("spi descriptor") || source.contains("spi")) {
            // SPI files contain class names, converted to pseudo-GAV
            // This is pattern-based inference, not direct metadata
            return Confidence.LOW;
        }

        // Default to LOW for any unknown sources
        return Confidence.LOW;
    }

    /**
     * Infers identifier type based on the detection source.
     *
     * @param detectionSource The detection source string.
     * @return Inferred identifier type.
     */
    private static IdentifierType inferIdentifierType(String detectionSource) {
        if (detectionSource == null) {
            return IdentifierType.GAV;
        }

        String source = detectionSource.toLowerCase();

        // OSGi detection produces package-based pseudo-GAV
        if (source.contains("osgi")) {
            return IdentifierType.PSEUDO_GAV;
        }

        // SPI detection produces FQCN-based pseudo-GAV
        if (source.contains("spi") || source.contains("fqcn")) {
            return IdentifierType.PSEUDO_GAV;
        }

        // Default to GAV for Maven-based detection
        return IdentifierType.GAV;
    }

    /**
     * Gets the primary identifier (GAV, package name, or FQCN).
     *
     * @return The dependency identifier.
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Gets the detection source/method that found this dependency.
     *
     * @return The detection source description.
     */
    public String getDetectionSource() {
        return detectionSource;
    }

    /**
     * Gets the confidence level of this detection.
     *
     * @return The confidence level.
     */
    public Confidence getConfidence() {
        return confidence;
    }

    /**
     * Gets the type of identifier stored.
     *
     * @return The identifier type.
     */
    public IdentifierType getIdentifierType() {
        return identifierType;
    }

    /**
     * Gets additional metadata associated with this dependency.
     *
     * @return Map of metadata key-value pairs.
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * Adds metadata to this dependency.
     *
     * @param key   The metadata key.
     * @param value The metadata value.
     * @return This instance for method chaining.
     */
    public DiscoveredDependency addMetadata(String key, String value) {
        this.metadata.put(key, value);
        return this;
    }

    /**
     * Parses the identifier as GAV components if it's in GAV format.
     * Returns null values for components that can't be parsed.
     *
     * @return Array of [groupId, artifactId, version], with nulls for missing parts.
     */
    public String[] parseAsGav() {
        if (identifier == null || identifier.isEmpty()) {
            return new String[]{null, null, null};
        }

        String[] parts = identifier.split(":");
        String groupId = parts.length > 0 ? parts[0] : null;
        String artifactId = parts.length > 1 ? parts[1] : null;
        String version = parts.length > 2 ? parts[2] : null;

        return new String[]{groupId, artifactId, version};
    }

    /**
     * Returns whether this detection is deterministic (100% certain).
     *
     * @return true if confidence is HIGH (deterministic), false if heuristic.
     */
    public boolean isDeterministic() {
        return confidence == Confidence.HIGH;
    }

    /**
     * Returns a human-readable confidence description.
     *
     * @return Description like "DETERMINISTIC (100%)" or "HEURISTIC (70-90%)"
     */
    public String getConfidenceDescription() {
        switch (confidence) {
            case HIGH:
                return "DETERMINISTIC (100%)";
            case MEDIUM:
                return "HEURISTIC (70-90%)";
            case LOW:
                return "HEURISTIC (30-60%)";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * Checks equality based on the identifier only.
     * Two dependencies with the same identifier are considered equal,
     * regardless of detection source. This enables proper deduplication.
     *
     * @param obj The object to compare.
     * @return true if identifiers are equal.
     */
    @Override
    public boolean equals(Object obj) {
        // Same reference check
        if (this == obj) {
            return true;
        }

        // Null and type check
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        // Compare by identifier only - this enables deduplication across detectors
        DiscoveredDependency other = (DiscoveredDependency) obj;
        return Objects.equals(identifier, other.identifier);
    }

    /**
     * Generates hash code based on the identifier only.
     * This ensures consistency with equals() for use in HashSet/HashMap.
     *
     * @return Hash code of the identifier.
     */
    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }

    /**
     * Returns a human-readable string representation of this dependency.
     * Format: identifier [source] (confidence description)
     *
     * Example outputs:
     *   org.apache.commons:commons-lang3:3.12.0 [Delta Analysis (Exact)] (DETERMINISTIC 100%)
     *   org.slf4j:api:1.7.32 [OSGi Export-Package] (HEURISTIC 70-90%)
     *
     * @return Formatted string representation.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Primary identifier
        sb.append(identifier != null ? identifier : "UNKNOWN");

        // Detection source in brackets
        sb.append(" [");
        sb.append(detectionSource != null ? detectionSource : "Unknown Source");
        sb.append("]");

        // Confidence level with percentage hint
        sb.append(" (");
        sb.append(getConfidenceDescription());
        sb.append(")");

        return sb.toString();
    }

    /**
     * Returns a compact string representation with just the identifier.
     * Useful for logging or when source/confidence is not needed.
     *
     * @return Just the identifier string.
     */
    public String toCompactString() {
        return identifier != null ? identifier : "UNKNOWN";
    }

    /**
     * Returns a detailed string representation including all metadata.
     * Useful for debugging or detailed reports.
     *
     * @return Detailed string with all fields.
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DiscoveredDependency {\n");
        sb.append("  identifier: ").append(identifier).append("\n");
        sb.append("  type: ").append(identifierType).append("\n");
        sb.append("  source: ").append(detectionSource).append("\n");
        sb.append("  confidence: ").append(getConfidenceDescription()).append("\n");
        sb.append("  deterministic: ").append(isDeterministic()).append("\n");

        if (!metadata.isEmpty()) {
            sb.append("  metadata: {\n");
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                sb.append("    ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            sb.append("  }\n");
        }

        sb.append("}");
        return sb.toString();
    }
}
