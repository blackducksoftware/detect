package com.blackduck.integration.detectable.detectables.maven.resolver.model.pomxml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Represents a single {@code <relocation>} entry inside the maven-shade-plugin's
 * {@code <configuration><relocations>} block.
 *
 * <p>Example POM fragment:
 * <pre>{@code
 * <relocation>
 *     <pattern>com.google.guava</pattern>
 *     <shadedPattern>com.mylib.shaded.guava</shadedPattern>
 * </relocation>
 * }</pre>
 *
 * <p>{@code pattern} is the original Java package prefix that was relocated.
 * {@code shadedPattern} is where those classes were moved to inside the fat JAR.
 */
public class PomXmlShadeRelocation {

    @JacksonXmlProperty(localName = "pattern")
    private String pattern;

    @JacksonXmlProperty(localName = "shadedPattern")
    private String shadedPattern;

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getShadedPattern() {
        return shadedPattern;
    }

    public void setShadedPattern(String shadedPattern) {
        this.shadedPattern = shadedPattern;
    }
}

