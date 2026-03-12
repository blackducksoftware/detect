package com.blackduck.integration.detectable.detectables.maven.resolver;

import java.nio.file.Path;

/**
 * Configuration options for Maven resolver operations.
 *
 * <p>Only user-facing options are exposed here:
 * <ul>
 *   <li>{@code includeShadedDependenciesV2Enabled} — opt-in flag to download JARs and detect shaded dependencies</li>
 *   <li>{@code jarRepositoryPath} — custom .m2 location to check for existing JARs</li>
 * </ul>
 *
 * <p>Internal download tuning (timeouts, threads, retries) is handled by
 * {@link MavenDownloadConstants}.
 */
public class MavenResolverOptions {
    private final boolean includeShadedDependenciesV2Enabled;
    private final Path jarRepositoryPath;

    /**
     * @param includeShadedDependenciesV2Enabled whether to download artifact JARs and detect shaded dependencies
     * @param jarRepositoryPath optional path to a custom local Maven repository (.m2 location).
     *                          The path is resolved automatically by {@link M2RepositoryPathResolver}
     *                          at download time, so users can provide it in any of these forms:
     *                          <ul>
     *                            <li>{@code /custom/path/.m2/repository}</li>
     *                            <li>{@code /custom/path} (if .m2/repository exists inside)</li>
     *                            <li>{@code /custom/path/.m2/repository/org/example/...} (truncated)</li>
     *                          </ul>
     */
    public MavenResolverOptions(boolean includeShadedDependenciesV2Enabled, Path jarRepositoryPath) {
        this.includeShadedDependenciesV2Enabled = includeShadedDependenciesV2Enabled;
        this.jarRepositoryPath = jarRepositoryPath;
    }

    public boolean isIncludeShadedDependenciesV2Enabled() {
        return includeShadedDependenciesV2Enabled;
    }

    /**
     * @return the raw user-provided path to the custom Maven repository location,
     *         or null if not configured. This path will be resolved by
     *         {@link M2RepositoryPathResolver} before use.
     */
    public Path getJarRepositoryPath() {
        return jarRepositoryPath;
    }
}