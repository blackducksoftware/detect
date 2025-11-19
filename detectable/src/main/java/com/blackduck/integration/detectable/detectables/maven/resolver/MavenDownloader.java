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
import java.util.ArrayList;
import java.util.List;

public class MavenDownloader {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final List<JavaRepository> remoteRepositories;
    private final Path downloadDir;

    private static final JavaRepository MAVEN_CENTRAL;
    static {
        MAVEN_CENTRAL = new JavaRepository();
        MAVEN_CENTRAL.setId("central");
        MAVEN_CENTRAL.setName("Maven Central");
        MAVEN_CENTRAL.setUrl("https://repo.maven.apache.org/maven2/");
    }

    public MavenDownloader(List<JavaRepository> remoteRepositories, Path downloadDir) {
        this.remoteRepositories = new ArrayList<>(remoteRepositories != null ? remoteRepositories : List.of());
        this.downloadDir = downloadDir;

        boolean centralRepoMissing = this.remoteRepositories.stream()
            .noneMatch(repo -> MAVEN_CENTRAL.getUrl().equalsIgnoreCase(repo.getUrl()));

        if (centralRepoMissing) {
            logger.debug("Maven Central repository not found in list. Adding it for POM download.");
            this.remoteRepositories.add(MAVEN_CENTRAL);
        }
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
