package com.blackduck.integration.detectable.detectables.maven.resolver.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Simplified POM model for parsing only coordinate information.
 *
 * <p>This lightweight model is used when only basic project and parent coordinates
 * are needed, avoiding the overhead of parsing the entire POM structure.
 *
 * <p>Includes both project coordinates and parent coordinates for inheritance resolution.
 */
public class PomXmlCoordinates {
    @JacksonXmlProperty(localName = "groupId", namespace = "parent")
    private String parentGroupId;

    @JacksonXmlProperty(localName = "artifactId", namespace = "parent")
    private String parentArtifactId;

    @JacksonXmlProperty(localName = "version", namespace = "parent")
    private String parentVersion;

    @JacksonXmlProperty(localName = "groupId")
    private String groupId;

    @JacksonXmlProperty(localName = "artifactId")
    private String artifactId;

    @JacksonXmlProperty(localName = "version")
    private String version;

    // Getters and Setters
    public String getParentGroupId() { return parentGroupId; }
    public void setParentGroupId(String parentGroupId) { this.parentGroupId = parentGroupId; }

    public String getParentArtifactId() { return parentArtifactId; }
    public void setParentArtifactId(String parentArtifactId) { this.parentArtifactId = parentArtifactId; }

    public String getParentVersion() { return parentVersion; }
    public void setParentVersion(String parentVersion) { this.parentVersion = parentVersion; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getArtifactId() { return artifactId; }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
}

