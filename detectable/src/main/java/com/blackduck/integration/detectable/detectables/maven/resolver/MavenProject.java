package com.blackduck.integration.detectable.detectables.maven.resolver;

import java.util.List;

public class MavenProject {
    private final String pomFile;
    private final JavaCoordinates coordinates;
    private final List<JavaRepository> repositories;
    private final List<JavaDependency> dependencies;
    private final List<JavaDependency> dependencyManagement;
    private final List<String> modules;

    public MavenProject(
        String pomFile,
        JavaCoordinates coordinates,
        List<JavaRepository> repositories,
        List<JavaDependency> dependencies,
        List<JavaDependency> dependencyManagement,
        List<String> modules
    ) {
        this.pomFile = pomFile;
        this.coordinates = coordinates;
        this.repositories = repositories;
        this.dependencies = dependencies;
        this.dependencyManagement = dependencyManagement;
        this.modules = modules;
    }

    public String getPomFile() {
        return pomFile;
    }

    public JavaCoordinates getCoordinates() {
        return coordinates;
    }

    public List<JavaRepository> getRepositories() {
        return repositories;
    }

    public List<JavaDependency> getDependencies() {
        return dependencies;
    }

    public List<JavaDependency> getDependencyManagement() {
        return dependencyManagement;
    }

    public List<String> getModules() {
        return modules;
    }
}

