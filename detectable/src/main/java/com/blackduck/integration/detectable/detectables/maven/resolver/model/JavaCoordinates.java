package com.blackduck.integration.detectable.detectables.maven.resolver.model;

/**
 * Java domain model representing Maven artifact coordinates.
 *
 * <p>This is the processed, normalized representation of Maven coordinates used
 * throughout the resolver logic after POM parsing is complete.
 *
 * <p>Contains the standard Maven GAV (GroupId:ArtifactId:Version) coordinates
 * plus optional type (packaging) information.
 */
public class JavaCoordinates {
    private String groupId;
    private String artifactId;
    private String version;
    private String type;

    public JavaCoordinates() {}

    public JavaCoordinates(String groupId, String artifactId, String version, String type) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
    }

    // Getters and Setters
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getArtifactId() { return artifactId; }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}

