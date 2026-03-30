package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.blackduck.integration.detectable.detectables.maven.resolver.model.*;

import java.util.List;

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
