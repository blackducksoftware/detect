package com.blackduck.integration.detectable.detectables.maven.resolver.model.pomxml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

/**
 * Represents the {@code <artifactSet>} element inside the maven-shade-plugin's
 * {@code <configuration>} block.
 *
 * <p>Example POM fragment:
 * <pre>{@code
 * <artifactSet>
 *     <excludes>
 *         <exclude>com.google.guava:guava</exclude>
 *         <exclude>org.apache.hadoop:*</exclude>
 *     </excludes>
 * </artifactSet>
 * }</pre>
 *
 * <p>Each {@code <exclude>} entry is a {@code groupId:artifactId} pattern where
 * {@code *} acts as a wildcard for either coordinate.
 */
public class PomXmlShadeArtifactSet {

    @JacksonXmlElementWrapper(localName = "excludes")
    @JacksonXmlProperty(localName = "exclude")
    private List<String> excludes;

    public List<String> getExcludes() {
        return excludes;
    }

    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }
}

