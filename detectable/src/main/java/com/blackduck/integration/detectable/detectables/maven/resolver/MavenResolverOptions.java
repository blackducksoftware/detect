package com.blackduck.integration.detectable.detectables.maven.resolver;

import java.util.Collections;
import java.util.List;

/**
 * Configuration options for the Maven Resolver Detectable.
 *
 * <p>Contains settings that can be configured via Detect properties to customize
 * Maven dependency resolution behavior.
 */
public class MavenResolverOptions {
    private final List<String> externalRepositories;

    /**
     * Constructs MavenResolverOptions with the specified external repositories.
     *
     * @param externalRepositories List of external Maven repository URLs to use during resolution.
     *                             These are used alongside repositories declared in pom.xml.
     */
    public MavenResolverOptions(List<String> externalRepositories) {
        this.externalRepositories = externalRepositories != null ? externalRepositories : Collections.emptyList();
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
}

