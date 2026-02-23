package com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload;

import org.eclipse.aether.artifact.Artifact;

import java.util.Objects;

/**
 * Immutable representation of Maven artifact coordinates including classifier support.
 * Single Responsibility: Encapsulate artifact identity with classifier awareness.
 */
public class ArtifactCoordinate {

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String classifier;
    private final String extension;

    /**
     * Creates an artifact coordinate from an Aether artifact.
     */
    public static ArtifactCoordinate fromAetherArtifact(Artifact artifact) {
        if (artifact == null) {
            throw new IllegalArgumentException("Artifact cannot be null");
        }

        return new ArtifactCoordinate(
            artifact.getGroupId(),
            artifact.getArtifactId(),
            artifact.getVersion(),
            artifact.getClassifier(),
            artifact.getExtension()
        );
    }

    /**
     * Creates an artifact coordinate.
     *
     * @param groupId The group ID (required)
     * @param artifactId The artifact ID (required)
     * @param version The version (required)
     * @param classifier The classifier (optional, can be null or empty)
     * @param extension The file extension (optional, defaults to "jar")
     */
    public ArtifactCoordinate(String groupId, String artifactId, String version,
                             String classifier, String extension) {
        this.groupId = validateRequired(groupId, "groupId");
        this.artifactId = validateRequired(artifactId, "artifactId");
        this.version = validateRequired(version, "version");
        this.classifier = normalizeClassifier(classifier);
        this.extension = normalizeExtension(extension);

        // Security: validate classifier doesn't contain path separators
        if (this.classifier != null && containsPathSeparator(this.classifier)) {
            throw new IllegalArgumentException(
                "Classifier contains illegal characters: " + this.classifier
            );
        }
    }

    private String validateRequired(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be null or empty");
        }
        return value.trim();
    }

    private String normalizeClassifier(String classifier) {
        if (classifier == null || classifier.trim().isEmpty()) {
            return null;
        }
        return classifier.trim();
    }

    private String normalizeExtension(String extension) {
        if (extension == null || extension.trim().isEmpty()) {
            return "jar";
        }
        return extension.trim();
    }

    private boolean containsPathSeparator(String value) {
        return value.contains("/") || value.contains("\\") ||
               value.contains("..") || value.contains(":") ||
               value.contains("*") || value.contains("?");
    }

    /**
     * Returns the Maven coordinates string (GAV or GAVC).
     *
     * @return Formatted string like "group:artifact:version" or "group:artifact:version:classifier"
     */
    public String toCoordinateString() {
        StringBuilder sb = new StringBuilder();
        sb.append(groupId).append(":");
        sb.append(artifactId).append(":");
        sb.append(version);

        if (classifier != null) {
            sb.append(":").append(classifier);
        }

        return sb.toString();
    }

    /**
     * Returns the artifact filename.
     *
     * @return Filename like "artifact-version.jar" or "artifact-version-classifier.jar"
     */
    public String toFileName() {
        StringBuilder sb = new StringBuilder();
        sb.append(artifactId);
        sb.append("-");
        sb.append(version);

        if (classifier != null) {
            sb.append("-");
            sb.append(classifier);
        }

        sb.append(".");
        sb.append(extension);

        return sb.toString();
    }

    /**
     * Returns the Maven repository path for this artifact.
     *
     * @return Path like "com/example/artifact/1.0/artifact-1.0.jar"
     */
    public String toRepositoryPath() {
        String groupPath = groupId.replace('.', '/');
        return groupPath + "/" + artifactId + "/" + version + "/" + toFileName();
    }

    /**
     * Checks if this coordinate matches another, considering classifier.
     *
     * @param other The other coordinate to compare
     * @return true if all components match exactly
     */
    public boolean matches(ArtifactCoordinate other) {
        if (other == null) {
            return false;
        }

        return Objects.equals(groupId, other.groupId) &&
               Objects.equals(artifactId, other.artifactId) &&
               Objects.equals(version, other.version) &&
               Objects.equals(classifier, other.classifier) &&
               Objects.equals(extension, other.extension);
    }

    /**
     * Checks if this coordinate matches the base coordinates (ignoring classifier).
     *
     * @param other The other coordinate to compare
     * @return true if GAV matches (classifier ignored)
     */
    public boolean matchesBase(ArtifactCoordinate other) {
        if (other == null) {
            return false;
        }

        return Objects.equals(groupId, other.groupId) &&
               Objects.equals(artifactId, other.artifactId) &&
               Objects.equals(version, other.version);
    }

    // Getters
    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getClassifier() {
        return classifier;
    }

    public String getExtension() {
        return extension;
    }

    public boolean hasClassifier() {
        return classifier != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ArtifactCoordinate that = (ArtifactCoordinate) o;
        return matches(that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version, classifier, extension);
    }

    @Override
    public String toString() {
        return toCoordinateString();
    }
}