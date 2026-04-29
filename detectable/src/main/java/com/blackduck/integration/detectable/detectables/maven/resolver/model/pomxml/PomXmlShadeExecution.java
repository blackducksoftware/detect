package com.blackduck.integration.detectable.detectables.maven.resolver.model.pomxml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Represents a single {@code <execution>} element inside the maven-shade-plugin's
 * {@code <executions>} block.
 *
 * <p>Shade plugin configuration is commonly placed inside an execution block:
 * <pre>{@code
 * <executions>
 *     <execution>
 *         <phase>package</phase>
 *         <goals><goal>shade</goal></goals>
 *         <configuration>
 *             <relocations>...</relocations>
 *             <artifactSet>...</artifactSet>
 *         </configuration>
 *     </execution>
 * </executions>
 * }</pre>
 */
public class PomXmlShadeExecution {

    @JacksonXmlProperty(localName = "configuration")
    private PomXmlShadeConfig configuration;

    public PomXmlShadeConfig getConfiguration() {
        return configuration;
    }

    public void setConfiguration(PomXmlShadeConfig configuration) {
        this.configuration = configuration;
    }
}

