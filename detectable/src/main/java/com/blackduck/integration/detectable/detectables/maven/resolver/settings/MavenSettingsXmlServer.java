package com.blackduck.integration.detectable.detectables.maven.resolver.settings;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Jackson XML model for a {@code <server>} element in Maven's settings.xml.
 *
 * <p>Server elements provide authentication credentials for repositories and mirrors.
 * The {@code id} field is used to match the server with a corresponding mirror or
 * repository definition.
 *
 * <p>Example XML:
 * <pre>{@code
 * <server>
 *   <id>company-mirror</id>
 *   <username>deploy-user</username>
 *   <password>secret-password</password>
 * </server>
 * }</pre>
 *
 * <p><strong>Note:</strong> This implementation does not support encrypted passwords
 * (Maven's settings-security.xml). Passwords must be provided in plain text.
 */
public class MavenSettingsXmlServer {

    @JacksonXmlProperty(localName = "id")
    private String id;

    @JacksonXmlProperty(localName = "username")
    private String username;

    @JacksonXmlProperty(localName = "password")
    private String password;


    /**
     * Returns the unique identifier for this server.
     * This ID is used to match with mirror or repository definitions.
     *
     * @return server ID, may be null if not specified
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the server ID.
     *
     * @param id server identifier
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the authentication username.
     *
     * @return username, may be null if not specified
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the authentication username.
     *
     * @param username authentication username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the authentication password.
     *
     * @return password, may be null if not specified
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the authentication password.
     *
     * @param password authentication password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Checks if this server has valid credentials.
     *
     * @return true if both username and password are non-null and non-empty
     */
    public boolean hasCredentials() {
        return username != null && !username.trim().isEmpty()
            && password != null && !password.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "MavenSettingsXmlServer{" +
            "id='" + id + '\'' +
            ", hasCredentials=" + hasCredentials() +
            '}';
    }
}

