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
 * <p><strong>Proxy Configuration Precedence:</strong>
 * <ol>
 *   <li><strong>settings.xml:</strong> Maven-specific proxy from {@code <proxies>} section (highest priority)</li>
 *   <li><strong>CLI Flags:</strong> Universal Black Duck proxy properties (blackduck.proxy.*) - fallback</li>
 *   <li><strong>None:</strong> No proxy if neither source provides configuration</li>
 * </ol>
 *
 * <p>The proxy details can be sourced from either:
 * <ul>
 *   <li>Maven's settings.xml file ({@code <proxies>} section) - takes precedence</li>
 *   <li>Global Detect proxy properties ({@code blackduck.proxy.host}, {@code blackduck.proxy.port}, etc.) - fallback</li>
 * </ul>
 *
 * <p><strong>Mirror Configuration Precedence:</strong>
 * <ol>
 *   <li><strong>CLI Flags:</strong> Mirror properties (detect.maven.buildless.mirror.*) - highest priority</li>
 *   <li><strong>settings.xml:</strong> Mirrors from {@code <mirrors>} section - fallback</li>
 *   <li><strong>None:</strong> No mirrors if neither source provides configuration</li>
 * </ol>
 *
 * <p>Mirror settings allow routing Maven artifact requests through a corporate repository
 * manager (e.g., Nexus, Artifactory). Mirrors can be configured via CLI flags or parsed
 * from a settings.xml file. CLI configuration takes precedence over settings.xml.
 */
public class MavenResolverOptions {
    private final List<String> externalRepositories;

    // Proxy configuration — either from CLI flags or settings.xml
    @Nullable
    private final MavenProxyConfig proxyConfig;

    // Mirror configurations — either from CLI or parsed from settings.xml.
    // These allow routing Maven requests through a corporate repository manager.
    private final List<MavenMirrorConfig> mirrorConfigurations;

    // Configuration for including test-scope dependencies
    private final boolean includeTestScope;

    /**
     * Constructs MavenResolverOptions with external repositories, proxy, and mirror configuration.
     *
     * @param externalRepositories  List of external Maven repository URLs to use during resolution.
     *                              These are used alongside repositories declared in pom.xml.
     * @param proxyConfig           Proxy configuration (may be null if no proxy is configured).
     * @param mirrorConfigurations  List of mirror configurations for corporate repository managers.
     *                              May be empty if no mirrors are configured.
     * @param includeTestScope      Whether to include test-scope dependencies in resolution.
     */
    public MavenResolverOptions(
        List<String> externalRepositories,
        @Nullable MavenProxyConfig proxyConfig,
        List<MavenMirrorConfig> mirrorConfigurations,
        boolean includeTestScope
    ) {
        this.externalRepositories = externalRepositories != null ? externalRepositories : Collections.emptyList();
        this.proxyConfig = proxyConfig;
        this.mirrorConfigurations = mirrorConfigurations != null ? mirrorConfigurations : Collections.emptyList();
        this.includeTestScope = includeTestScope;
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

    /**
     * Returns whether test-scope dependencies should be included in resolution.
     *
     * @return true if test-scope dependencies should be included, false otherwise
     */
    public boolean getIncludeTestScope() {
        return includeTestScope;
    }
}

