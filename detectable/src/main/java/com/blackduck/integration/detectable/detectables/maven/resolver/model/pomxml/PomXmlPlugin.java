package com.blackduck.integration.detectable.detectables.maven.resolver.model.pomxml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

/**
 * Represents a {@code <plugin>} element within the {@code <build><plugins>} section of a Maven POM.
 *
 * <p>Captures plugin coordinates plus the {@code <configuration>} and {@code <executions>} blocks
 * needed to read maven-shade-plugin relocation and exclusion settings.
 */
public class PomXmlPlugin {
    @JacksonXmlProperty(localName = "groupId")
    private String groupId;

    @JacksonXmlProperty(localName = "artifactId")
    private String artifactId;

    @JacksonXmlProperty(localName = "version")
    private String version;

    /** Plugin-level configuration (Style A — less common for shade plugin). */
    @JacksonXmlProperty(localName = "configuration")
    private PomXmlShadeConfig configuration;

    /** Execution-level configuration (Style B — most common for shade plugin). */
    @JacksonXmlElementWrapper(localName = "executions")
    @JacksonXmlProperty(localName = "execution")
    private List<PomXmlShadeExecution> executions;

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getArtifactId() { return artifactId; }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public PomXmlShadeConfig getConfiguration() { return configuration; }
    public void setConfiguration(PomXmlShadeConfig configuration) { this.configuration = configuration; }

    public List<PomXmlShadeExecution> getExecutions() { return executions; }
    public void setExecutions(List<PomXmlShadeExecution> executions) { this.executions = executions; }
}
