package com.blackduck.integration.detectable.detectables.maven.resolver.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

/**
 * Main POM XML model representing the root {@code <project>} element.
 *
 * <p>This class is used by Jackson XML to deserialize Maven POM files into
 * a structured Java object for processing.
 *
 * <p>Maps to the standard Maven POM structure with support for:
 * <ul>
 *   <li>Project coordinates (groupId, artifactId, version)</li>
 *   <li>Parent POM references</li>
 *   <li>Dependencies and dependency management</li>
 *   <li>Repositories</li>
 *   <li>Multi-module configuration</li>
 *   <li>Build configuration</li>
 * </ul>
 */
@JacksonXmlRootElement(localName = "project")
public class PomXml {
    @JacksonXmlProperty(localName = "groupId")
    private String groupId;

    @JacksonXmlProperty(localName = "artifactId")
    private String artifactId;

    @JacksonXmlProperty(localName = "version")
    private String version;

    @JacksonXmlProperty(localName = "packaging")
    private String packaging;

    @JacksonXmlProperty(localName = "parent")
    private PomXmlParent parent;

    @JacksonXmlElementWrapper(localName = "repositories")
    @JacksonXmlProperty(localName = "repository")
    private List<MavenRepositoryXml> repositories;

    @JacksonXmlElementWrapper(localName = "dependencies")
    @JacksonXmlProperty(localName = "dependency")
    private List<PomXmlDependency> dependencies;

    @JacksonXmlProperty(localName = "dependencyManagement")
    private PomXmlDependencyManagement dependencyManagement;

    @JacksonXmlElementWrapper(localName = "modules")
    @JacksonXmlProperty(localName = "module")
    private List<String> modules;

    @JacksonXmlProperty(localName = "build")
    private PomXmlBuild build;

    // Getters and Setters
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getArtifactId() { return artifactId; }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getPackaging() { return packaging; }
    public void setPackaging(String packaging) { this.packaging = packaging; }

    public PomXmlParent getParent() { return parent; }
    public void setParent(PomXmlParent parent) { this.parent = parent; }

    public List<MavenRepositoryXml> getRepositories() { return repositories; }
    public void setRepositories(List<MavenRepositoryXml> repositories) { this.repositories = repositories; }

    public List<PomXmlDependency> getDependencies() { return dependencies; }
    public void setDependencies(List<PomXmlDependency> dependencies) { this.dependencies = dependencies; }

    public PomXmlDependencyManagement getDependencyManagement() { return dependencyManagement; }
    public void setDependencyManagement(PomXmlDependencyManagement dependencyManagement) { this.dependencyManagement = dependencyManagement; }

    public List<String> getModules() { return modules; }
    public void setModules(List<String> modules) { this.modules = modules; }

    public PomXmlBuild getBuild() { return build; }
    public void setBuild(PomXmlBuild build) { this.build = build; }
}

