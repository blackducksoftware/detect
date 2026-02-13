package com.blackduck.integration.detectable.detectables.maven.resolver;

import java.nio.file.Path;

/**
 * Options for configuring the Maven Resolver Detectable.
 *
 * <p>This class bridges configuration settings from Detect properties to the Maven Resolver Detectable,
 * providing options that control the behavior of Maven dependency resolution.
 */
public class MavenResolverOptions {

    private final boolean downloadArtifactJars;
    private final Path jarRepositoryPath;

    /**
     * Constructs Maven Resolver Options.
     *
     * @param downloadArtifactJars If true, enables downloading and caching of artifact JARs in addition to POMs.
     *                            This is an opt-in feature that may increase scan time and disk usage.
     * @param jarRepositoryPath Optional custom path to check for existing JARs. Can be either:
     *                         (1) A repository directory using Maven layout, or
     *                         (2) A direct path to a specific JAR file.
     *                         If null, only ~/.m2/repository is checked. Note: downloads always go to ~/.m2/repository.
     */
    public MavenResolverOptions(boolean downloadArtifactJars, Path jarRepositoryPath) {
        this.downloadArtifactJars = downloadArtifactJars;
        this.jarRepositoryPath = jarRepositoryPath;
    }

    /**
     * Returns whether artifact JAR downloads are enabled.
     *
     * <p>When enabled, the Maven Resolver will:
     * <ul>
     *   <li>Check local Maven repository (.m2) for cached JARs</li>
     *   <li>Download missing JARs from Maven Central</li>
     *   <li>Cache downloaded JARs for subsequent runs</li>
     * </ul>
     *
     * @return true if JAR downloads should be performed, false for POM-only mode
     */
    public boolean isDownloadArtifactJarsEnabled() {
        return downloadArtifactJars;
    }

    /**
     * Returns the custom JAR repository path if configured, or null to use only default (~/.m2/repository).
     *
     * <p>This path is used to CHECK for existing JARs before downloading. It can be either:
     * <ul>
     *   <li>A repository directory using Maven layout (groupId/artifactId/version/artifact-version.jar)</li>
     *   <li>A direct path to a specific JAR file</li>
     * </ul>
     *
     * <p><strong>Important:</strong> All downloads are saved to ~/.m2/repository regardless of this setting.
     *
     * @return Path to custom location for JAR lookup, or null to skip custom lookup
     */
    public Path getJarRepositoryPath() {
        return jarRepositoryPath;
    }
}