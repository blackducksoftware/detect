package com.blackduck.integration.detectable.detectables.maven.resolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class MavenDownloader {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final List<JavaRepository> remoteRepositories;
    private final Path downloadDir;

    public MavenDownloader(List<JavaRepository> remoteRepositories, Path downloadDir) {
        this.remoteRepositories = remoteRepositories;
        this.downloadDir = downloadDir;
    }

    public File downloadPom(JavaCoordinates coordinates) {
        String pomPath = String.format("%s/%s/%s/%s-%s.pom",
            coordinates.getGroupId().replace('.', '/'),
            coordinates.getArtifactId(),
            coordinates.getVersion(),
            coordinates.getArtifactId(),
            coordinates.getVersion()
        );

        for (JavaRepository repository : remoteRepositories) {
            try {
                URL url = new URL(repository.getUrl() + pomPath);
                logger.debug("Attempting to download parent POM from: {}", url);

                Path destination = downloadDir.resolve(coordinates.getArtifactId() + "-" + coordinates.getVersion() + ".pom");
                if (Files.exists(destination)) {
                    logger.debug("POM already exists locally: {}", destination);
                    return destination.toFile();
                }

                try (InputStream inputStream = url.openStream();
                     ReadableByteChannel readableByteChannel = Channels.newChannel(inputStream);
                     FileOutputStream fileOutputStream = new FileOutputStream(destination.toFile())) {
                    fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                    logger.debug("Successfully downloaded POM to: {}", destination);
                    return destination.toFile();
                }
            } catch (Exception e) {
                logger.debug("Failed to download from repository {}: {}", repository.getUrl(), e.getMessage());
            }
        }

        logger.error("Could not download POM for coordinates: {}", coordinates);
        return null;
    }
}

