package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.blackduck.integration.detectable.detectables.maven.resolver.model.*;

import java.util.List;
import java.util.Map;

/**
 * Main data classes for the POM parser - Java port of Go pomparser models
 */

/**
 * Contains parent pom information for a parsed pom file.
 */
class ParentPomInfo {
    private JavaCoordinates coordinates;

    // Dependencies is a list containing the lists of dependencies
    // for each pom in the parent chain. Processing is delayed for property resolution.
    // Starts with top level parent.
    private List<List<PomXmlDependency>> dependencies;

    // DependencyManagement is a list containing the lists of dependency management info
    // for each pom in the parent chain. Processing is delayed for property resolution.
    // Starts with top level parent.
    private List<List<PomXmlDependency>> dependencyManagement;

    // ExpectedPath is the location where the parent pom file is expected to be found.
    private String expectedPath;

    // Getters and Setters
    public JavaCoordinates getCoordinates() { return coordinates; }
    public void setCoordinates(JavaCoordinates coordinates) { this.coordinates = coordinates; }

    public List<List<PomXmlDependency>> getDependencies() { return dependencies; }
    public void setDependencies(List<List<PomXmlDependency>> dependencies) { this.dependencies = dependencies; }

    public List<List<PomXmlDependency>> getDependencyManagement() { return dependencyManagement; }
    public void setDependencyManagement(List<List<PomXmlDependency>> dependencyManagement) { this.dependencyManagement = dependencyManagement; }

    public String getExpectedPath() { return expectedPath; }
    public void setExpectedPath(String expectedPath) { this.expectedPath = expectedPath; }
}

/**
 * Represents a maven module that is still being processed into an effective model.
 */
class PartialMavenProject {
    // ParentPomInfo is the information collected from a parent pom
    private ParentPomInfo parentPomInfo;

    // Properties is the list of properties defined by this pom and its parent
    private Map<String, String> properties;

    // Coordinates are the coordinates of this pom
    private JavaCoordinates coordinates;

    // Repositories is the list of repositories available to this pom
    private List<JavaRepository> repositories;

    // DependenciesWithShaded list of dependencies shaded in by this pom file
    // using plain list as these are stored after prop resolution. See: projectbuilder.go
    // This is kept in this model so shaded deps info can be cached.
    private List<JavaDependency> dependenciesWithShaded;

    // DependencyManagementForShaded this is the effective dependency
    // management section used to resolve this pom file shaded dependency list. See: projectbuilder.go
    // This is kept in this model so shaded deps info can be cached.
    private List<JavaDependency> dependencyManagementForShaded;

    // Dependencies is a list containing the dependencies
    // defined by this pom. Processing is delayed for property resolution.
    private List<PomXmlDependency> dependencies;

    // DependencyManagement is a list containing the dependency management info
    // defined by this pom. Processing is delayed for property resolution.
    private List<PomXmlDependency> dependencyManagement;

    // Modules is the list of modules aggregated by this pom
    private List<String> modules;

    // Plugins is the set of artifact ids of the plugins used by the pom.xml file
    private Map<String, Boolean> plugins;

    // Getters and Setters
    public ParentPomInfo getParentPomInfo() { return parentPomInfo; }
    public void setParentPomInfo(ParentPomInfo parentPomInfo) { this.parentPomInfo = parentPomInfo; }

    public Map<String, String> getProperties() { return properties; }
    public void setProperties(Map<String, String> properties) { this.properties = properties; }

    public JavaCoordinates getCoordinates() { return coordinates; }
    public void setCoordinates(JavaCoordinates coordinates) { this.coordinates = coordinates; }

    public List<JavaRepository> getRepositories() { return repositories; }
    public void setRepositories(List<JavaRepository> repositories) { this.repositories = repositories; }

    public List<JavaDependency> getDependenciesWithShaded() { return dependenciesWithShaded; }
    public void setDependenciesWithShaded(List<JavaDependency> dependenciesWithShaded) { this.dependenciesWithShaded = dependenciesWithShaded; }

    public List<JavaDependency> getDependencyManagementForShaded() { return dependencyManagementForShaded; }
    public void setDependencyManagementForShaded(List<JavaDependency> dependencyManagementForShaded) { this.dependencyManagementForShaded = dependencyManagementForShaded; }

    public List<PomXmlDependency> getDependencies() { return dependencies; }
    public void setDependencies(List<PomXmlDependency> dependencies) { this.dependencies = dependencies; }

    public List<PomXmlDependency> getDependencyManagement() { return dependencyManagement; }
    public void setDependencyManagement(List<PomXmlDependency> dependencyManagement) { this.dependencyManagement = dependencyManagement; }

    public List<String> getModules() { return modules; }
    public void setModules(List<String> modules) { this.modules = modules; }

    public Map<String, Boolean> getPlugins() { return plugins; }
    public void setPlugins(Map<String, Boolean> plugins) { this.plugins = plugins; }
}
