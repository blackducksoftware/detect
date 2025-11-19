package com.blackduck.integration.detectable.detectables.maven.resolver;

/**
 * Data model classes for Maven POM parsing - Java port of Go javamodels
 */

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;
import java.util.Map;

// Main POM XML model
@JacksonXmlRootElement(localName = "project")
class PomXml {
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

// POM XML Parent
class PomXmlParent {
    @JacksonXmlProperty(localName = "groupId")
    private String groupId;

    @JacksonXmlProperty(localName = "artifactId")
    private String artifactId;

    @JacksonXmlProperty(localName = "version")
    private String version;

    @JacksonXmlProperty(localName = "relativePath")
    private String relativePath;

    // Getters and Setters
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getArtifactId() { return artifactId; }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getRelativePath() { return relativePath; }
    public void setRelativePath(String relativePath) { this.relativePath = relativePath; }
}

class PomXmlDependencyManagement {
    @JacksonXmlElementWrapper(localName = "dependencies")
    @JacksonXmlProperty(localName = "dependency")
    private List<PomXmlDependency> dependencies;

    public List<PomXmlDependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<PomXmlDependency> dependencies) {
        this.dependencies = dependencies;
    }
}

class PomXmlBuild {
    @JacksonXmlElementWrapper(localName = "plugins")
    @JacksonXmlProperty(localName = "plugin")
    private List<PomXmlPlugin> plugins;

    public List<PomXmlPlugin> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<PomXmlPlugin> plugins) {
        this.plugins = plugins;
    }
}

class PomXmlPlugin {
    @JacksonXmlProperty(localName = "groupId")
    private String groupId;

    @JacksonXmlProperty(localName = "artifactId")
    private String artifactId;

    @JacksonXmlProperty(localName = "version")
    private String version;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}

// Maven Repository XML model
class MavenRepositoryXml {
    @JacksonXmlProperty(localName = "id")
    private String id;

    @JacksonXmlProperty(localName = "name")
    private String name;

    @JacksonXmlProperty(localName = "url")
    private String url;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}

// POM XML Dependency Exclusion
class PomXmlDependencyExclusion {
    @JacksonXmlProperty(localName = "groupId")
    private String groupId;

    @JacksonXmlProperty(localName = "artifactId")
    private String artifactId;

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getArtifactId() { return artifactId; }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }
}

// POM XML Dependency
class PomXmlDependency {
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

// POM XML Coordinates (simplified model for coordinate-only parsing)
class PomXmlCoordinates {
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

// Java Coordinates
class JavaCoordinates {
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

// Java Repository
class JavaRepository {
    private String id;
    private String name;
    private String url;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}

// Java Dependency
class JavaDependency {
    private List<String> includedBy;
    private JavaCoordinates coordinates;
    private String scope;
    private String type;
    private String classifier;
    private String projectDependencyFilePath;
    private List<JavaDependencyExclusion> exclusions;

    public JavaDependency() {}

    public JavaDependency(JavaCoordinates coordinates, String scope, String type, String classifier, List<JavaDependencyExclusion> exclusions) {
        this.coordinates = coordinates;
        this.scope = scope;
        this.type = type;
        this.classifier = classifier;
        this.exclusions = exclusions;
    }

    // Getters and Setters
    public List<String> getIncludedBy() { return includedBy; }
    public void setIncludedBy(List<String> includedBy) { this.includedBy = includedBy; }

    public JavaCoordinates getCoordinates() { return coordinates; }
    public void setCoordinates(JavaCoordinates coordinates) { this.coordinates = coordinates; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getClassifier() { return classifier; }
    public void setClassifier(String classifier) { this.classifier = classifier; }

    public String getProjectDependencyFilePath() { return projectDependencyFilePath; }
    public void setProjectDependencyFilePath(String projectDependencyFilePath) { this.projectDependencyFilePath = projectDependencyFilePath; }

    public List<JavaDependencyExclusion> getExclusions() { return exclusions; }
    public void setExclusions(List<JavaDependencyExclusion> exclusions) { this.exclusions = exclusions; }
}
