package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.blackduck.integration.detectable.detectables.maven.resolver.model.*;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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

        // New destination layout includes groupId directories to avoid collisions
        Path newDestination = downloadDir.resolve(coordinates.getGroupId().replace('.', '/')).resolve(coordinates.getArtifactId()).resolve(coordinates.getVersion()).resolve(coordinates.getArtifactId() + "-" + coordinates.getVersion() + ".pom");
        // Backwards-compatible old filename
        Path oldDestination = downloadDir.resolve(coordinates.getArtifactId() + "-" + coordinates.getVersion() + ".pom");

        // If either already exists, return it (prefer new layout)
        try {
            if (Files.exists(newDestination)) {
                logger.debug("Found previously downloaded POM at new location: {}", newDestination);
                return newDestination.toFile();
            }
            if (Files.exists(oldDestination)) {
                logger.debug("Found previously downloaded POM at old location: {}", oldDestination);
                return oldDestination.toFile();
            }
        } catch (Exception e) {
            logger.debug("Error while checking existing download paths: {}", e.getMessage());
        }

        // Ensure parent directories exist for newDestination
        try {
            Files.createDirectories(newDestination.getParent());
        } catch (Exception ignored) {
        }

        // Try each configured repository first
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

            // Attempt metadata-aware download if snapshot
            try {
                String base = repository.getUrl();
                if (!base.endsWith("/")) base = base + "/";
                String versionDir = coordinates.getGroupId().replace('.', '/') + "/" + coordinates.getArtifactId() + "/" + coordinates.getVersion() + "/";

                if (isSnapshot) {
                    String metadataUrl = base + versionDir + "maven-metadata.xml";
                    try (InputStream metaIn = new URL(metadataUrl).openStream()) {
                        logger.debug("Found maven-metadata.xml at {}", metadataUrl);
                        String timestamped = parseSnapshotVersionFromMetadata(metaIn, coordinates.getArtifactId());
                        if (timestamped != null) {
                            String timestampedPomPath = versionDir + coordinates.getArtifactId() + "-" + timestamped + ".pom";
                            URL pomUrl = new URL(base + timestampedPomPath);
                            logger.info("Attempting to download timestamped snapshot POM from {}", pomUrl);
                            if (downloadUrlToPath(pomUrl, newDestination)) {
                                logger.info("Successfully downloaded timestamped snapshot POM to: {}", newDestination);
                                return newDestination.toFile();
                            } else {
                                logger.debug("Timestamped POM at {} not found or failed to download", pomUrl);
                            }
                        } else {
                            logger.debug("No snapshotVersion entry found in metadata for POM; will attempt timestamp+buildNumber method if present.");
                            // Try alternate snapshot assembly using <snapshot><timestamp> and <buildNumber>
                            try (InputStream metaIn2 = new URL(metadataUrl).openStream()) {
                                String ts = parseTimestampBuildFromMetadata(metaIn2);
                                if (ts != null) {
                                    String timestampedPomPath = versionDir + coordinates.getArtifactId() + "-" + ts + ".pom";
                                    URL pomUrl = new URL(base + timestampedPomPath);
                                    logger.info("Attempting to download constructed timestamped snapshot POM from {}", pomUrl);
                                    if (downloadUrlToPath(pomUrl, newDestination)) {
                                        logger.info("Successfully downloaded constructed timestamped POM to: {}", newDestination);
                                        return newDestination.toFile();
                                    } else {
                                        logger.debug("Constructed timestamped POM at {} not found or failed to download", pomUrl);
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("Failed to re-read metadata for timestamp+buildNumber method: {}", e.getMessage());
                            }
                        }
                    } catch (Exception metaEx) {
                        logger.debug("No maven-metadata.xml found at {} or failed to read: {}", base + versionDir + "maven-metadata.xml", metaEx.getMessage());
                        // fall-through to try plain POM path
                    }
                }

                // Default attempt (non-snapshot or fallback)
                URL url = new URL(repository.getUrl() + (repository.getUrl().endsWith("/") ? "" : "/") + pomPath);
                logger.debug("Attempting to download parent POM from: {} (repo id={}, repo name={})", url, repository.getId(), repository.getName());
                if (downloadUrlToPath(url, newDestination)) {
                    logger.debug("Successfully downloaded POM to: {} (from repo: {})", newDestination, repository.getUrl());
                    return newDestination.toFile();
                }
            } catch (Exception e) {
                logger.debug("Failed to download from repository {} for coordinates {}:{}:{} - exception: {}",
                    repository.getUrl(), coordinates.getGroupId(), coordinates.getArtifactId(), coordinates.getVersion(), e.toString());
                logger.debug("Download exception stacktrace for repository {}: ", repository.getUrl(), e);
            }
        }

        // If initial set of repositories failed for SNAPSHOT, try common Sonatype snapshot endpoints as fallback
        if (isSnapshot) {
            List<String> sonatypeFallbacks = Arrays.asList(
                "https://oss.sonatype.org/content/repositories/snapshots/",
                "https://s01.oss.sonatype.org/content/repositories/snapshots/",
                "https://central.sonatype.com/repository/maven-snapshots/"
            );

            for (String base : sonatypeFallbacks) {
                try {
                    if (!base.endsWith("/")) base = base + "/";
                    String versionDir = coordinates.getGroupId().replace('.', '/') + "/" + coordinates.getArtifactId() + "/" + coordinates.getVersion() + "/";
                    String metadataUrl = base + versionDir + "maven-metadata.xml";
                    try (InputStream metaIn = new URL(metadataUrl).openStream()) {
                        logger.debug("Found maven-metadata.xml at {} (fallback)", metadataUrl);
                        String timestamped = parseSnapshotVersionFromMetadata(metaIn, coordinates.getArtifactId());
                        if (timestamped != null) {
                            String timestampedPomPath = versionDir + coordinates.getArtifactId() + "-" + timestamped + ".pom";
                            URL pomUrl = new URL(base + timestampedPomPath);
                            logger.info("Attempting to download timestamped snapshot POM from fallback {}", pomUrl);
                            if (downloadUrlToPath(pomUrl, newDestination)) {
                                logger.info("Successfully downloaded timestamped snapshot POM from fallback to: {}", newDestination);
                                return newDestination.toFile();
                            }
                        } else {
                            logger.debug("No snapshotVersion entry found in fallback metadata at {}", metadataUrl);
                        }
                    } catch (Exception metaEx) {
                        logger.debug("No metadata at fallback {}: {}", metadataUrl, metaEx.getMessage());
                    }

                    // Try plain path fallback
                    String plainPomUrl = base + coordinates.getGroupId().replace('.', '/') + "/" + coordinates.getArtifactId() + "/" + coordinates.getVersion() + "/" + coordinates.getArtifactId() + "-" + coordinates.getVersion() + ".pom";
                    URL url = new URL(plainPomUrl);
                    logger.debug("Attempting fallback download from {}", url);
                    if (downloadUrlToPath(url, newDestination)) {
                        logger.info("Successfully downloaded POM from fallback to: {}", newDestination);
                        return newDestination.toFile();
                    }
                } catch (Exception e) {
                    logger.debug("Fallback attempt failed for base {}: {}", base, e.getMessage());
                }
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

    private boolean downloadUrlToPath(URL url, Path destination) {
        try (InputStream inputStream = url.openStream();
             ReadableByteChannel readableByteChannel = Channels.newChannel(inputStream);
             FileOutputStream fileOutputStream = new FileOutputStream(destination.toFile())) {
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            return true;
        } catch (Exception e) {
            logger.debug("Failed to download URL {}: {}", url, e.getMessage());
            return false;
        }
    }

    private String parseSnapshotVersionFromMetadata(InputStream metaIn, String artifactId) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(metaIn);
            doc.getDocumentElement().normalize();
            // Look for <snapshotVersions><snapshotVersion><extension>pom</extension><value>...</value></snapshotVersion></snapshotVersions>
            NodeList svList = doc.getElementsByTagName("snapshotVersion");
            for (int i = 0; i < svList.getLength(); i++) {
                Element sv = (Element) svList.item(i);
                String extension = getChildText(sv, "extension");
                String classifier = getChildText(sv, "classifier");
                String value = getChildText(sv, "value");
                if (extension != null && extension.equals("pom") && (classifier == null || classifier.isEmpty())) {
                    return value;
                }
            }
            return null;
        } catch (Exception e) {
            logger.debug("Failed to parse snapshotVersions from metadata: {}", e.getMessage());
            return null;
        }
    }

    private String parseTimestampBuildFromMetadata(InputStream metaIn) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(metaIn);
            doc.getDocumentElement().normalize();
            Element snapshot = (Element) doc.getElementsByTagName("snapshot").item(0);
            if (snapshot != null) {
                String timestamp = getChildText(snapshot, "timestamp");
                String buildNumber = getChildText(snapshot, "buildNumber");
                if (timestamp != null && buildNumber != null) {
                    return timestamp + "-" + buildNumber;
                }
            }
            return null;
        } catch (Exception e) {
            logger.debug("Failed to parse timestamp/buildNumber from metadata: {}", e.getMessage());
            return null;
        }
    }

    private String getChildText(Element parent, String childName) {
        if (parent == null) return null;
        NodeList nl = parent.getElementsByTagName(childName);
        if (nl == null || nl.getLength() == 0) return null;
        Element el = (Element) nl.item(0);
        if (el == null) return null;
        if (el.getFirstChild() == null) return null;
        return el.getFirstChild().getNodeValue();
    }
}
