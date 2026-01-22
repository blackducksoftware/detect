package com.blackduck.integration.detectable.detectables.maven.resolver.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Represents an {@code <exclusion>} element within a dependency's {@code <exclusions>} list.
 *
 * <p>Exclusions prevent specific transitive dependencies from being included in the
 * dependency tree. Typically used to exclude unwanted or conflicting transitive dependencies.
 */
public class PomXmlDependencyExclusion {
    @JacksonXmlProperty(localName = "groupId")
    private String groupId;

    @JacksonXmlProperty(localName = "artifactId")
    private String artifactId;

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getArtifactId() { return artifactId; }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }
}

