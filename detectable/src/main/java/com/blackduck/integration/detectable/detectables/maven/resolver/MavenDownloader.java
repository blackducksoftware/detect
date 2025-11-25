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
import java.util.stream.Collectors;

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
        MAVEN_CENTRAL.setSnapshotsEnabled(false);
        MAVEN_CENTRAL.setReleasesEnabled(true);
    }

    // package-private constructor to avoid exposing package-private JavaRepository in a public API
    MavenDownloader(List<JavaRepository> remoteRepositories, Path downloadDir) {
        if (remoteRepositories != null) {
            this.remoteRepositories = new ArrayList<>(remoteRepositories);
        } else {
            this.remoteRepositories = new ArrayList<>();
        }
        this.downloadDir = downloadDir;

        boolean centralRepoMissing = this.remoteRepositories.stream()
            .noneMatch(repo -> MAVEN_CENTRAL.getUrl().equalsIgnoreCase(repo.getUrl()));

        if (centralRepoMissing) {
            logger.debug("Maven Central repository not found in list. Adding it for POM download.");
            this.remoteRepositories.add(MAVEN_CENTRAL);
        }
    }

    // package-private to avoid exposing package-private JavaCoordinates in a public API
    File downloadPom(JavaCoordinates coordinates) {
        // Log context for easier debugging
        logger.info("Attempting to download POM for coordinates: {}:{}:{} into download dir: {}",
            coordinates.getGroupId(), coordinates.getArtifactId(), coordinates.getVersion(), downloadDir.toAbsolutePath());

        boolean isSnapshot = coordinates.getVersion() != null && coordinates.getVersion().toUpperCase().contains("SNAPSHOT");

        String pomPath = String.format("%s/%s/%s/%s-%s.pom",
            coordinates.getGroupId().replace('.', '/'),
            coordinates.getArtifactId(),
            coordinates.getVersion(),
            coordinates.getArtifactId(),
            coordinates.getVersion()
        );

        // Ensure download directory exists
        try {
            Files.createDirectories(downloadDir);
        } catch (Exception e) {
            logger.warn("Failed to create download directory {}: {}", downloadDir, e.getMessage());
        }

        for (JavaRepository repository : remoteRepositories) {
            // Respect repository policy for snapshots/releases
            if (isSnapshot && !repository.isSnapshotsEnabled()) {
                logger.debug("Skipping repository {} for SNAPSHOT coordinates {} because snapshots are disabled on that repository.", repository.getUrl(), coordinates.getVersion());
                continue;
            }
            if (!isSnapshot && !repository.isReleasesEnabled()) {
                logger.debug("Skipping repository {} for release coordinates {} because releases are disabled on that repository.", repository.getUrl(), coordinates.getVersion());
                continue;
            }

            try {
                URL url = new URL(repository.getUrl() + pomPath);
                logger.debug("Attempting to download parent POM from: {} (repo id={}, repo name={})", url, repository.getId(), repository.getName());

                Path destination = downloadDir.resolve(coordinates.getArtifactId() + "-" + coordinates.getVersion() + ".pom");
                if (Files.exists(destination)) {
                    logger.debug("POM already exists locally: {}", destination);
                    return destination.toFile();
                }

                try (InputStream inputStream = url.openStream();
                     ReadableByteChannel readableByteChannel = Channels.newChannel(inputStream);
                     FileOutputStream fileOutputStream = new FileOutputStream(destination.toFile())) {
                    fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                    logger.debug("Successfully downloaded POM to: {} (from repo: {})", destination, repository.getUrl());
                    return destination.toFile();
                }
            } catch (Exception e) {
                // SLF4J treats a Throwable as the last parameter specially (it is not used for substitution),
                // so include the Throwable message explicitly and then log the stack trace separately.
                logger.debug("Failed to download from repository {} for coordinates {}:{}:{} - exception: {}",
                    repository.getUrl(), coordinates.getGroupId(), coordinates.getArtifactId(), coordinates.getVersion(), e.toString());
                logger.debug("Download exception stacktrace for repository {}: ", repository.getUrl(), e);
            }
        }

        // Build a readable list of repository URLs for the error message
        String triedRepos = remoteRepositories.stream()
            .map(JavaRepository::getUrl)
            .collect(Collectors.joining(", "));

        logger.error("Could not download POM for coordinates: {}:{}:{}; tried repositories: {}; downloadDir: {}",
            coordinates.getGroupId(), coordinates.getArtifactId(), coordinates.getVersion(), triedRepos, downloadDir.toAbsolutePath());
        return null;
    }
}
