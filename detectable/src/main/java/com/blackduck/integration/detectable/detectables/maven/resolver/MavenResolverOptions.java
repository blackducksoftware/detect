package com.blackduck.integration.detectable.detectables.maven.resolver;

import java.nio.file.Path;

/**
 * Configuration options for Maven resolver operations.
 *
 * <p>Only user-facing options are exposed here:
 * <ul>
 *   <li>{@code downloadArtifactJarsEnabled} — opt-in flag to download JARs</li>
 *   <li>{@code jarRepositoryPath} — custom .m2 location to check for existing JARs</li>
 * </ul>
 *
 * <p>Internal download tuning (timeouts, threads, retries) is handled by
 * {@link MavenDownloadConstants}.
 */
public class MavenResolverOptions {
    private final boolean downloadArtifactJarsEnabled;
    private final Path jarRepositoryPath;

    /**
     * @param downloadArtifactJarsEnabled whether to download artifact JARs
     * @param jarRepositoryPath optional path to a custom local Maven repository (.m2 location).
     *                          The path is resolved automatically by {@link M2RepositoryPathResolver}
     *                          at download time, so users can provide it in any of these forms:
     *                          <ul>
     *                            <li>{@code /custom/path/.m2/repository}</li>
     *                            <li>{@code /custom/path} (if .m2/repository exists inside)</li>
     *                            <li>{@code /custom/path/.m2/repository/org/example/...} (truncated)</li>
     *                          </ul>
     */
    public MavenResolverOptions(boolean downloadArtifactJarsEnabled, Path jarRepositoryPath) {
        this.downloadArtifactJarsEnabled = downloadArtifactJarsEnabled;
        this.jarRepositoryPath = jarRepositoryPath;
    }

    public boolean isDownloadArtifactJarsEnabled() {
        return downloadArtifactJarsEnabled;
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