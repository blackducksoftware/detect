package com.blackduck.integration.detectable.detectables.maven.resolver.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

/**
 * Represents the {@code <dependencyManagement>} element in a Maven POM file.
 *
 * <p>Dependency management allows centralizing dependency version and scope information
 * without actually adding dependencies to the project. Child projects can then reference
 * managed dependencies without specifying versions.
 */
public class PomXmlDependencyManagement {
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

