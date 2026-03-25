package com.blackduck.integration.detectable.detectables.maven.resolver.mirror;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * POJO representing a single {@code <proxy>} element from Maven's settings.xml.
 *
 * <p>Maps to XML structure:
 * <pre>{@code
 * <proxy>
 *   <id>my-proxy</id>
 *   <active>true</active>
 *   <protocol>http</protocol>
 *   <host>proxy.company.com</host>
 *   <port>8080</port>
 *   <username>proxyuser</username>
 *   <password>proxypass</password>
 *   <nonProxyHosts>localhost|127.0.0.1</nonProxyHosts>
 * </proxy>
 * }</pre>
 *
 * <p>Used by Jackson XML for deserialization. Not part of the public API.
 */
public class MavenSettingsXmlProxy {

    @JacksonXmlProperty(localName = "id")
    private String id;

    @JacksonXmlProperty(localName = "active")
    private Boolean active;

    @JacksonXmlProperty(localName = "protocol")
    private String protocol;

    @JacksonXmlProperty(localName = "host")
    private String host;

    @JacksonXmlProperty(localName = "port")
    private Integer port;

    @JacksonXmlProperty(localName = "username")
    private String username;

    @JacksonXmlProperty(localName = "password")
    private String password;

    @JacksonXmlProperty(localName = "nonProxyHosts")
    private String nonProxyHosts;

    // Default constructor for Jackson
    public MavenSettingsXmlProxy() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNonProxyHosts() {
        return nonProxyHosts;
    }

    public void setNonProxyHosts(String nonProxyHosts) {
        this.nonProxyHosts = nonProxyHosts;
    }

    /**
     * Checks if this proxy entry is valid (has required fields).
     *
     * @return true if host and port are present
     */
    public boolean isValid() {
        return host != null && !host.trim().isEmpty()
            && port != null && port > 0;
    }

    /**
     * Checks if this proxy is active.
     *
     * @return true if active field is true or null (default is active)
     */
    public boolean isActive() {
        return active == null || active;
    }

    @Override
    public String toString() {
        return "MavenSettingsXmlProxy{" +
            "id='" + id + '\'' +
            ", active=" + active +
            ", protocol='" + protocol + '\'' +
            ", host='" + host + '\'' +
            ", port=" + port +
            ", hasCredentials=" + (username != null && password != null) +
            '}';
    }
}

