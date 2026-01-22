package com.blackduck.integration.detectable.detectables.maven.resolver.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Represents repository policy configuration for releases or snapshots.
 *
 * <p>Used within {@code <releases>} or {@code <snapshots>} elements in a repository
 * definition to control whether that type of artifact should be retrieved from the repository.
 */
public class PomXmlRepositoryPolicy {
    @JacksonXmlProperty(localName = "enabled")
    private String enabled;

    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }
}

