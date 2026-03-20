package com.blackduck.integration.detectable.detectables.maven.resolver;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * Configuration options for the Maven Resolver Detectable.
 *
 * <p>Contains settings that can be configured via Detect properties to customize
 * Maven dependency resolution behavior, including external repositories and
 * forward proxy configuration.
 *
 * <p><strong>Proxy configuration:</strong> The proxy details are sourced from the global
 * Detect proxy properties ({@code blackduck.proxy.host}, {@code blackduck.proxy.port},
 * {@code blackduck.proxy.username}, {@code blackduck.proxy.password}).
 *
 * <p><strong>Important:</strong> The proxy host must be a plain hostname or IP address
 * (e.g. {@code proxy.company.com} or {@code 10.0.0.1}). Do <em>not</em> include a
 * scheme/protocol such as {@code http://} or {@code https://}. The Aether proxy
 * layer adds the protocol automatically.
 */
public class MavenResolverOptions {
    private final List<String> externalRepositories;

    // Proxy fields — sourced from the global blackduck.proxy.* properties.
    // proxyHost must be a bare hostname/IP (no http:// or https:// prefix).
    @Nullable
    private final String proxyHost;
    private final int proxyPort;
    @Nullable
    private final String proxyUsername;
    @Nullable
    private final String proxyPassword;

    // Comma-separated list of host patterns that should bypass the proxy,
    // sourced from blackduck.proxy.ignored.hosts.
    private final List<String> proxyIgnoredHosts;

    /**
     * Constructs MavenResolverOptions with external repositories and proxy configuration.
     *
     * @param externalRepositories List of external Maven repository URLs to use during resolution.
     *                             These are used alongside repositories declared in pom.xml.
     * @param proxyHost            Proxy hostname or IP (no scheme). May be null if no proxy is configured.
     * @param proxyPort            Proxy port number (0 if not configured).
     * @param proxyUsername        Proxy authentication username. May be null.
     * @param proxyPassword        Proxy authentication password. May be null.
     * @param proxyIgnoredHosts    Host patterns that should bypass the proxy. Never null.
     */
    public MavenResolverOptions(
        List<String> externalRepositories,
        @Nullable String proxyHost,
        int proxyPort,
        @Nullable String proxyUsername,
        @Nullable String proxyPassword,
        List<String> proxyIgnoredHosts
    ) {
        this.externalRepositories = externalRepositories != null ? externalRepositories : Collections.emptyList();
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUsername = proxyUsername;
        this.proxyPassword = proxyPassword;
        this.proxyIgnoredHosts = proxyIgnoredHosts != null ? proxyIgnoredHosts : Collections.emptyList();
    }

    /**
     * Returns the list of external Maven repository URLs.
     *
     * @return List of repository URLs, never null
     */
    public List<String> getExternalRepositories() {
        return externalRepositories;
    }

    /**
     * Checks if any external repositories are configured.
     *
     * @return true if external repositories are configured, false otherwise
     */
    public boolean hasExternalRepositories() {
        return externalRepositories != null && !externalRepositories.isEmpty();
    }

    /**
     * Returns the proxy hostname or IP address.
     * This is a bare hostname — no {@code http://} or {@code https://} prefix.
     *
     * @return proxy host, or null if no proxy is configured
     */
    @Nullable
    public String getProxyHost() {
        return proxyHost;
    }

    /** @return proxy port number, 0 if not configured */
    public int getProxyPort() {
        return proxyPort;
    }

    /** @return proxy username, or null if not configured */
    @Nullable
    public String getProxyUsername() {
        return proxyUsername;
    }

    /** @return proxy password, or null if not configured */
    @Nullable
    public String getProxyPassword() {
        return proxyPassword;
    }

    /**
     * Returns the list of host patterns that should bypass the proxy.
     *
     * @return list of ignored host patterns, never null
     */
    public List<String> getProxyIgnoredHosts() {
        return proxyIgnoredHosts;
    }

    /**
     * Checks whether a forward proxy is configured.
     * A proxy is considered configured when a non-blank host and a positive port are provided.
     *
     * @return true if proxy host and port are both set
     */
    public boolean hasProxyConfiguration() {
        return proxyHost != null && !proxyHost.trim().isEmpty() && proxyPort > 0;
    }
}

