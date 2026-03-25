package com.blackduck.integration.detectable.detectables.maven.resolver;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.detectable.detectables.maven.resolver.mirror.MavenMirrorConfig;
import com.blackduck.integration.detectable.detectables.maven.resolver.mirror.MavenProxyConfig;

/**
 * Configuration options for the Maven Resolver Detectable.
 *
 * <p>Contains settings that can be configured via Detect properties to customize
 * Maven dependency resolution behavior, including external repositories, forward
 * proxy configuration, and corporate mirror (repository manager) support.
 *
 * <p><strong>Proxy configuration:</strong> The proxy details can be sourced from either:
 * <ul>
 *   <li>Global Detect proxy properties ({@code blackduck.proxy.host}, {@code blackduck.proxy.port}, etc.)</li>
 *   <li>Maven's settings.xml file ({@code <proxies>} section)</li>
 * </ul>
 * CLI properties take precedence over settings.xml.
 *
 * <p><strong>Mirror configuration:</strong> Mirror settings allow routing Maven artifact
 * requests through a corporate repository manager (e.g., Nexus, Artifactory). Mirrors
 * can be configured via CLI flags or parsed from a settings.xml file. CLI configuration
 * takes precedence over settings.xml.
 */
public class MavenResolverOptions {
    private final List<String> externalRepositories;

    // Proxy configuration — either from CLI flags or settings.xml
    @Nullable
    private final MavenProxyConfig proxyConfig;

    // Mirror configurations — either from CLI or parsed from settings.xml.
    // These allow routing Maven requests through a corporate repository manager.
    private final List<MavenMirrorConfig> mirrorConfigurations;

    /**
     * Constructs MavenResolverOptions with external repositories, proxy, and mirror configuration.
     *
     * @param externalRepositories  List of external Maven repository URLs to use during resolution.
     *                              These are used alongside repositories declared in pom.xml.
     * @param proxyConfig           Proxy configuration (may be null if no proxy is configured).
     * @param mirrorConfigurations  List of mirror configurations for corporate repository managers.
     *                              May be empty if no mirrors are configured.
     */
    public MavenResolverOptions(
        List<String> externalRepositories,
        @Nullable MavenProxyConfig proxyConfig,
        List<MavenMirrorConfig> mirrorConfigurations
    ) {
        this.externalRepositories = externalRepositories != null ? externalRepositories : Collections.emptyList();
        this.proxyConfig = proxyConfig;
        this.mirrorConfigurations = mirrorConfigurations != null ? mirrorConfigurations : Collections.emptyList();
    }

    /**
     * Constructs MavenResolverOptions with external repositories and individual proxy fields (backward compatibility).
     *
     * <p>This constructor is provided for backward compatibility with existing callers.
     * New code should use the constructor that accepts {@link MavenProxyConfig}.
     *
     * @param externalRepositories List of external Maven repository URLs to use during resolution.
     * @param proxyHost            Proxy hostname or IP (no scheme). May be null if no proxy is configured.
     * @param proxyPort            Proxy port number (0 if not configured).
     * @param proxyUsername        Proxy authentication username. May be null.
     * @param proxyPassword        Proxy authentication password. May be null.
     * @param proxyIgnoredHosts    Host patterns that should bypass the proxy. Never null.
     * @param mirrorConfigurations  List of mirror configurations for corporate repository managers.
     */
    public MavenResolverOptions(
        List<String> externalRepositories,
        @Nullable String proxyHost,
        int proxyPort,
        @Nullable String proxyUsername,
        @Nullable String proxyPassword,
        List<String> proxyIgnoredHosts,
        List<MavenMirrorConfig> mirrorConfigurations
    ) {
        this.externalRepositories = externalRepositories != null ? externalRepositories : Collections.emptyList();
        
        // Convert individual proxy fields to MavenProxyConfig if proxy is configured
        if (proxyHost != null && !proxyHost.trim().isEmpty() && proxyPort > 0) {
            this.proxyConfig = new MavenProxyConfig(
                proxyHost,
                proxyPort,
                proxyUsername,
                proxyPassword,
                proxyIgnoredHosts != null ? proxyIgnoredHosts : Collections.emptyList()
            );
        } else {
            this.proxyConfig = null;
        }
        
        this.mirrorConfigurations = mirrorConfigurations != null ? mirrorConfigurations : Collections.emptyList();
    }

    /**
     * Constructs MavenResolverOptions with external repositories and proxy configuration (no mirrors).
     *
     * <p>This constructor is provided for backward compatibility with existing callers.
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
        
        // Convert individual proxy fields to MavenProxyConfig if proxy is configured
        if (proxyHost != null && !proxyHost.trim().isEmpty() && proxyPort > 0) {
            this.proxyConfig = new MavenProxyConfig(
                proxyHost,
                proxyPort,
                proxyUsername,
                proxyPassword,
                proxyIgnoredHosts != null ? proxyIgnoredHosts : Collections.emptyList()
            );
        } else {
            this.proxyConfig = null;
        }
        
        this.mirrorConfigurations = Collections.emptyList();
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
     * Returns the proxy configuration.
     *
     * @return proxy configuration, or null if no proxy is configured
     */
    @Nullable
    public MavenProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    /**
     * Checks whether a forward proxy is configured.
     *
     * @return true if proxy configuration is present
     */
    public boolean hasProxyConfiguration() {
        return proxyConfig != null;
    }

    /**
     * Returns the list of mirror configurations for corporate repository managers.
     *
     * @return list of mirror configurations, never null (may be empty)
     */
    public List<MavenMirrorConfig> getMirrorConfigurations() {
        return mirrorConfigurations;
    }

    /**
     * Checks whether any mirror configurations are present.
     *
     * @return true if at least one mirror is configured
     */
    public boolean hasMirrorConfiguration() {
        return mirrorConfigurations != null && !mirrorConfigurations.isEmpty();
    }
}

