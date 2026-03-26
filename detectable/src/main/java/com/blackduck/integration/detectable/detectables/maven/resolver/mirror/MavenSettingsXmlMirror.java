package com.blackduck.integration.detectable.detectables.maven.resolver.mirror;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Jackson XML model for a {@code <mirror>} element in Maven's settings.xml.
 *
 * <p>Example XML:
 * <pre>{@code
 * <mirror>
 *   <id>company-mirror</id>
 *   <url>https://nexus.company.com/maven2</url>
 *   <mirrorOf>*</mirrorOf>
 * </mirror>
 * }</pre>
 *
 * <p>The {@code mirrorOf} field follows Maven's mirror matching syntax:
 * <ul>
 *   <li>{@code *} - matches all repositories</li>
 *   <li>{@code central} - matches only the central repository</li>
 *   <li>{@code *,!repo1} - matches all except repo1</li>
 *   <li>{@code external:*} - matches all external repositories</li>
 * </ul>
 */
public class MavenSettingsXmlMirror {

    @JacksonXmlProperty(localName = "id")
    private String id;

    @JacksonXmlProperty(localName = "url")
    private String url;

    @JacksonXmlProperty(localName = "mirrorOf")
    private String mirrorOf;

    @JacksonXmlProperty(localName = "name")
    private String name;

    /**
     * Returns the unique identifier for this mirror.
     * This ID is used to match with server credentials.
     *
     * @return mirror ID, may be null if not specified
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the mirror ID.
     *
     * @param id mirror identifier
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the URL of the mirror repository.
     *
     * @return mirror URL, may be null if not specified
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the mirror URL.
     *
     * @param url mirror repository URL
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Returns the pattern specifying which repositories this mirror intercepts.
     *
     * @return mirrorOf pattern, may be null if not specified
     */
    public String getMirrorOf() {
        return mirrorOf;
    }

    /**
     * Sets the mirrorOf pattern.
     *
     * @param mirrorOf pattern for repository matching
     */
    public void setMirrorOf(String mirrorOf) {
        this.mirrorOf = mirrorOf;
    }

    /**
     * Returns the human-readable name of this mirror.
     *
     * @return mirror name, may be null
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the mirror name.
     *
     * @param name human-readable name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Validates that this mirror has the minimum required fields.
     *
     * @return true if id, url, and mirrorOf are all non-null and non-empty
     */
    public boolean isValid() {
        return id != null && !id.trim().isEmpty()
            && url != null && !url.trim().isEmpty()
            && mirrorOf != null && !mirrorOf.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "MavenSettingsXmlMirror{" +
            "id='" + id + '\'' +
            ", url='" + url + '\'' +
            ", mirrorOf='" + mirrorOf + '\'' +
            '}';
    }
}

