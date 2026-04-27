package com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload;

import com.blackduck.integration.detectable.detectables.maven.resolver.exception.ArtifactDownloadException;
import org.eclipse.aether.artifact.Artifact;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * Fallback downloader used as the last-resort tier in JAR resolution.
 *
 * <p>By default, delegates to Maven Central. When a mirror is configured
 * (e.g., a corporate Nexus/Artifactory instance matching {@code central}
 * or {@code *}), the mirror URL and optional credentials are used instead.
 */
public class FallbackRepositoryDownloader implements HttpArtifactDownloader {
    private static final String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2";
    private final RemoteRepositoryDownloader delegate;

    /**
     * Creates a fallback downloader pointing to Maven Central (no mirror).
     */
    public FallbackRepositoryDownloader() {
        this.delegate = new RemoteRepositoryDownloader(MAVEN_CENTRAL_URL);
    }

    /**
     * Creates a fallback downloader pointing to a mirror URL with optional authentication.
     *
     * @param mirrorUrl The mirror repository URL to use instead of Maven Central
     * @param username  Optional authentication username (may be null)
     * @param password  Optional authentication password (may be null)
     */
    public FallbackRepositoryDownloader(String mirrorUrl, @Nullable String username, @Nullable String password) {
        this.delegate = new RemoteRepositoryDownloader(mirrorUrl, username, password);
    }

    @Override
    public DownloadResult download(Artifact artifact, Path targetPath, DownloadConfiguration configuration)
            throws ArtifactDownloadException {
        return delegate.download(artifact, targetPath, configuration);
    }
}
