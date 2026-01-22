package com.blackduck.integration.detectable.detectables.maven.resolver.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Represents a {@code <repository>} element in a Maven POM file.
 *
 * <p>Defines remote Maven repositories where dependencies and plugins can be resolved.
 * Includes configuration for releases and snapshots policies.
 */
public class MavenRepositoryXml {
    @JacksonXmlProperty(localName = "id")
    private String id;

    @JacksonXmlProperty(localName = "name")
    private String name;

    @JacksonXmlProperty(localName = "url")
    private String url;

    // Repository policy elements
    @JacksonXmlProperty(localName = "releases")
    private PomXmlRepositoryPolicy releases;

    @JacksonXmlProperty(localName = "snapshots")
    private PomXmlRepositoryPolicy snapshots;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public PomXmlRepositoryPolicy getReleases() { return releases; }
    public void setReleases(PomXmlRepositoryPolicy releases) { this.releases = releases; }

    public PomXmlRepositoryPolicy getSnapshots() { return snapshots; }
    public void setSnapshots(PomXmlRepositoryPolicy snapshots) { this.snapshots = snapshots; }
}

