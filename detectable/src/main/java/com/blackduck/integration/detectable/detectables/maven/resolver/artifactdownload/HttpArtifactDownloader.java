package com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload;

import com.blackduck.integration.detectable.detectables.maven.resolver.exception.ArtifactDownloadException;
import org.eclipse.aether.artifact.Artifact;

import java.nio.file.Path;

/**
 * Interface for downloading Maven artifacts via HTTP.
 * Interface Segregation Principle: Focused interface for HTTP downloads only.
 */

public interface HttpArtifactDownloader {

    /**
     * Downloads an artifact from a Maven repository.
     *
     * @param artifact The artifact to download
     * @param targetPath The path where the artifact should be saved
     * @param configuration Download configuration including timeouts and retry policy
     * @return The result of the download operation
     * @throws ArtifactDownloadException if download fails
     */
    DownloadResult download(Artifact artifact, Path targetPath, DownloadConfiguration configuration)
        throws ArtifactDownloadException;
}