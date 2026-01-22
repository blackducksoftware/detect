package com.blackduck.integration.detectable.detectables.maven.resolver.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

/**
 * Represents a {@code <dependency>} element in a Maven POM file.
 *
 * <p>Contains all information about a Maven dependency including:
 * <ul>
 *   <li>Coordinates (groupId, artifactId, version)</li>
 *   <li>Scope (compile, test, provided, runtime, system)</li>
 *   <li>Type and classifier</li>
 *   <li>System path for system-scoped dependencies</li>
 *   <li>Optional flag</li>
 *   <li>Exclusions list</li>
 * </ul>
 */
public class PomXmlDependency {
    @JacksonXmlProperty(localName = "groupId")
    private String groupId;

    @JacksonXmlProperty(localName = "artifactId")
    private String artifactId;

    @JacksonXmlProperty(localName = "version")
    private String version;

    @JacksonXmlProperty(localName = "scope")
    private String scope;

    @JacksonXmlProperty(localName = "classifier")
    private String classifier;

    @JacksonXmlProperty(localName = "type")
    private String type;

    @JacksonXmlProperty(localName = "systemPath")
    private String filePath;

    @JacksonXmlProperty(localName = "optional")
    private String optional;

    @JacksonXmlElementWrapper(localName = "exclusions")
    @JacksonXmlProperty(localName = "exclusion")
    private List<PomXmlDependencyExclusion> exclusions;

    // Getters and Setters
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getArtifactId() { return artifactId; }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getClassifier() { return classifier; }
    public void setClassifier(String classifier) { this.classifier = classifier; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getOptional() { return optional; }
    public void setOptional(String optional) { this.optional = optional; }

    public List<PomXmlDependencyExclusion> getExclusions() { return exclusions; }
    public void setExclusions(List<PomXmlDependencyExclusion> exclusions) { this.exclusions = exclusions; }
}

