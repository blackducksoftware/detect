package com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload;

import com.blackduck.integration.detectable.detectables.maven.resolver.exception.ArtifactDownloadException;
import org.eclipse.aether.artifact.Artifact;

import java.nio.file.Path;

/**
 * Downloads artifacts from Maven Central.
 */
public class MavenCentralDownloader implements HttpArtifactDownloader {
    private static final String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2";
    private final RemoteRepositoryDownloader delegate;

    public MavenCentralDownloader() {
        this.delegate = new RemoteRepositoryDownloader(MAVEN_CENTRAL_URL);
    }

    @Override
    public DownloadResult download(Artifact artifact, Path targetPath, DownloadConfiguration configuration)
            throws ArtifactDownloadException {
        return delegate.download(artifact, targetPath, configuration);
    }
}