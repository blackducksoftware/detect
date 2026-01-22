package com.blackduck.integration.detectable.detectables.maven.resolver.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

/**
 * Represents the {@code <build>} element in a Maven POM file.
 *
 * <p>Contains build-related configuration including plugins, resources, and other
 * build settings. Currently only captures plugin information.
 */
public class PomXmlBuild {
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

