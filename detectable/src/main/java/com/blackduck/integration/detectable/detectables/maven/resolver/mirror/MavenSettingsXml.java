package com.blackduck.integration.detectable.detectables.maven.resolver.mirror;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Jackson XML model for parsing Maven's settings.xml file.
 *
 * <p>This class maps to the root {@code <settings>} element and extracts:
 * <ul>
 *   <li>{@code <mirrors>} - List of mirror definitions</li>
 *   <li>{@code <servers>} - List of server credentials (matched by ID)</li>
 *   <li>{@code <proxies>} - List of proxy configurations</li>
 * </ul>
 *
 * <p>Other settings.xml elements (profiles, etc.) are ignored.
 *
 * <p>Example settings.xml structure:
 * <pre>{@code
 * <settings>
 *   <mirrors>
 *     <mirror>
 *       <id>company-mirror</id>
 *       <url>https://nexus.company.com/maven2</url>
 *       <mirrorOf>*</mirrorOf>
 *     </mirror>
 *   </mirrors>
 *   <servers>
 *     <server>
 *       <id>company-mirror</id>
 *       <username>user</username>
 *       <password>pass</password>
 *     </server>
 *   </servers>
 *   <proxies>
 *     <proxy>
 *       <id>my-proxy</id>
 *       <active>true</active>
 *       <protocol>http</protocol>
 *       <host>proxy.company.com</host>
 *       <port>8080</port>
 *       <username>proxyuser</username>
 *       <password>proxypass</password>
 *       <nonProxyHosts>localhost|127.0.0.1</nonProxyHosts>
 *     </proxy>
 *   </proxies>
 * </settings>
 * }</pre>
 */
@JacksonXmlRootElement(localName = "settings")
public class MavenSettingsXml {

    @JacksonXmlElementWrapper(localName = "mirrors")
    @JacksonXmlProperty(localName = "mirror")
    private List<MavenSettingsXmlMirror> mirrors;

    @JacksonXmlElementWrapper(localName = "servers")
    @JacksonXmlProperty(localName = "server")
    private List<MavenSettingsXmlServer> servers;

    @JacksonXmlElementWrapper(localName = "proxies")
    @JacksonXmlProperty(localName = "proxy")
    private List<MavenSettingsXmlProxy> proxies;

    /**
     * Default constructor for Jackson deserialization.
     */
    public MavenSettingsXml() {
        this.mirrors = new ArrayList<>();
        this.servers = new ArrayList<>();
        this.proxies = new ArrayList<>();
    }

    /**
     * Returns the list of mirror definitions from the settings file.
     *
     * @return list of mirrors, never null (may be empty)
     */
    public List<MavenSettingsXmlMirror> getMirrors() {
        return mirrors != null ? mirrors : new ArrayList<>();
    }

    /**
     * Sets the list of mirror definitions.
     *
     * @param mirrors list of mirrors
     */
    public void setMirrors(List<MavenSettingsXmlMirror> mirrors) {
        this.mirrors = mirrors;
    }

    /**
     * Returns the list of server credentials from the settings file.
     *
     * @return list of servers, never null (may be empty)
     */
    public List<MavenSettingsXmlServer> getServers() {
        return servers != null ? servers : new ArrayList<>();
    }

    /**
     * Sets the list of server credentials.
     *
     * @param servers list of servers
     */
    public void setServers(List<MavenSettingsXmlServer> servers) {
        this.servers = servers;
    }

    /**
     * Returns the list of proxy configurations from the settings file.
     *
     * @return list of proxies, never null (may be empty)
     */
    public List<MavenSettingsXmlProxy> getProxies() {
        return proxies != null ? proxies : new ArrayList<>();
    }

    /**
     * Sets the list of proxy configurations.
     *
     * @param proxies list of proxies
     */
    public void setProxies(List<MavenSettingsXmlProxy> proxies) {
        this.proxies = proxies;
    }
}

