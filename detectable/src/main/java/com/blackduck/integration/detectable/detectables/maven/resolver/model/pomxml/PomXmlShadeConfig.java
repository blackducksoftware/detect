package com.blackduck.integration.detectable.detectables.maven.resolver.model.pomxml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

/**
 * Represents the {@code <configuration>} block of the maven-shade-plugin.
 *
 * <p>This configuration may appear either directly on the {@code <plugin>} element
 * or nested inside an {@code <execution>} element — both are valid Maven POM syntax.
 *
 * <p>Example POM fragment:
 * <pre>{@code
 * <configuration>
 *     <relocations>
 *         <relocation>
 *             <pattern>com.google.guava</pattern>
 *             <shadedPattern>com.mylib.shaded.guava</shadedPattern>
 *         </relocation>
 *     </relocations>
 *     <artifactSet>
 *         <excludes>
 *             <exclude>com.google.guava:guava</exclude>
 *         </excludes>
 *     </artifactSet>
 * </configuration>
 * }</pre>
 */
public class PomXmlShadeConfig {

    @JacksonXmlElementWrapper(localName = "relocations")
    @JacksonXmlProperty(localName = "relocation")
    private List<PomXmlShadeRelocation> relocations;

    @JacksonXmlProperty(localName = "artifactSet")
    private PomXmlShadeArtifactSet artifactSet;

    public List<PomXmlShadeRelocation> getRelocations() {
        return relocations;
    }

    public void setRelocations(List<PomXmlShadeRelocation> relocations) {
        this.relocations = relocations;
    }

    public PomXmlShadeArtifactSet getArtifactSet() {
        return artifactSet;
    }

    public void setArtifactSet(PomXmlShadeArtifactSet artifactSet) {
        this.artifactSet = artifactSet;
    }
}

