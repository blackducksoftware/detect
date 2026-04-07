package com.blackduck.integration.detectable.detectables.maven.resolver.mavendownload;

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

    private static final String MAVEN_METADATA_XML = "maven-metadata.xml";

    private static final JavaRepository MAVEN_CENTRAL;
    static {
        MAVEN_CENTRAL = new JavaRepository();
        MAVEN_CENTRAL.setId("central");
        MAVEN_CENTRAL.setName("Maven Central");
        MAVEN_CENTRAL.setUrl("https://repo.maven.apache.org/maven2/");
        MAVEN_CENTRAL.setSnapshotsEnabled(false);
        MAVEN_CENTRAL.setReleasesEnabled(true);
    }

    private static final List<String> SONATYPE_FALLBACK_REPOS = Arrays.asList(
        "https://oss.sonatype.org/content/repositories/snapshots/",
        "https://s01.oss.sonatype.org/content/repositories/snapshots/",
        "https://central.sonatype.com/repository/maven-snapshots/"
    );

    public MavenDownloader(List<JavaRepository> remoteRepositories, Path downloadDir) {
        if (remoteRepositories != null) {
            this.remoteRepositories = new ArrayList<>(remoteRepositories);
        } else {
            this.remoteRepositories = new ArrayList<>();
        }
        this.downloadDir = downloadDir;

        boolean centralRepoMissing = this.remoteRepositories.stream()
            .noneMatch(repo -> MAVEN_CENTRAL.getUrl().equalsIgnoreCase(repo.getUrl()));

        if (centralRepoMissing) {
            logger.debug("Maven Central repository not found in list. Adding it for POM mavendownload.");
            this.remoteRepositories.add(MAVEN_CENTRAL);
        }
    }

    public File downloadPom(JavaCoordinates coordinates) {
        DownloadContext ctx = new DownloadContext(coordinates, downloadDir);
        logger.info("Attempting to mavendownload POM for coordinates: {}:{}:{} into mavendownload dir: {}",
            ctx.groupId, ctx.artifactId, ctx.version, downloadDir.toAbsolutePath());

        ensureDownloadDirectoryExists();

        File existingFile = findExistingDownload(ctx);
        if (existingFile != null) {
            return existingFile;
        }

        ensureParentDirectoriesExist(ctx.newDestination);

        File result = tryDownloadFromRepositories(ctx);
        if (result != null) {
            return result;
        }

        if (ctx.isSnapshot) {
            result = trySonatypeFallbacks(ctx);
            if (result != null) {
                return result;
            }
        }

        logDownloadFailure(ctx);
        return null;
    }

    private void ensureDownloadDirectoryExists() {
        try {
            Files.createDirectories(downloadDir);
        } catch (Exception e) {
            logger.warn("Failed to create mavendownload directory {}: {}", downloadDir, e.getMessage());
        }
    }

    private File findExistingDownload(DownloadContext ctx) {
        try {
            if (Files.exists(ctx.newDestination)) {
                logger.debug("Found previously downloaded POM at new location: {}", ctx.newDestination);
                return ctx.newDestination.toFile();
            }
            if (Files.exists(ctx.oldDestination)) {
                logger.debug("Found previously downloaded POM at old location: {}", ctx.oldDestination);
                return ctx.oldDestination.toFile();
            }
        } catch (Exception e) {
            logger.debug("Error while checking existing mavendownload paths: {}", e.getMessage());
        }
        return null;
    }

    private void ensureParentDirectoriesExist(Path destination) {
        try {
            Files.createDirectories(destination.getParent());
        } catch (Exception ignored) {
        }
    }

    private File tryDownloadFromRepositories(DownloadContext ctx) {
        for (JavaRepository repository : remoteRepositories) {
            if (!isRepositoryApplicable(repository, ctx.isSnapshot, ctx.version)) {
                continue;
            }
            File result = tryDownloadFromRepository(repository, ctx);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private boolean isRepositoryApplicable(JavaRepository repository, boolean isSnapshot, String version) {
        if (isSnapshot && !repository.isSnapshotsEnabled()) {
            logger.debug("Skipping repository {} for SNAPSHOT coordinates {} because snapshots are disabled on that repository.",
                repository.getUrl(), version);
            return false;
        }
        if (!isSnapshot && !repository.isReleasesEnabled()) {
            logger.debug("Skipping repository {} for release coordinates {} because releases are disabled on that repository.",
                repository.getUrl(), version);
            return false;
        }
        return true;
    }

    private File tryDownloadFromRepository(JavaRepository repository, DownloadContext ctx) {
        try {
            String baseUrl = normalizeBaseUrl(repository.getUrl());

            if (ctx.isSnapshot) {
                File snapshotResult = trySnapshotDownload(baseUrl, ctx);
                if (snapshotResult != null) {
                    return snapshotResult;
                }
            }

            return tryDirectDownload(repository, ctx);
        } catch (Exception e) {
            logger.debug("Failed to mavendownload from repository {} for coordinates {}:{}:{} - exception: {}",
                repository.getUrl(), ctx.groupId, ctx.artifactId, ctx.version, e.toString());
            logger.debug("Download exception stacktrace for repository {}: ", repository.getUrl(), e);
        }
        return null;
    }

    private File trySnapshotDownload(String baseUrl, DownloadContext ctx) {
        String metadataUrl = baseUrl + ctx.versionDir + MAVEN_METADATA_XML;

        File result = trySnapshotVersionMetadataDownload(baseUrl, metadataUrl, ctx);
        if (result != null) {
            return result;
        }

        return tryTimestampBuildMetadataDownload(baseUrl, metadataUrl, ctx);
    }

    private File trySnapshotVersionMetadataDownload(String baseUrl, String metadataUrl, DownloadContext ctx) {
        try (InputStream metaIn = new URL(metadataUrl).openStream()) {
            logger.debug("Found {} at {}", MAVEN_METADATA_XML, metadataUrl);
            String timestamped = parseSnapshotVersionFromMetadata(metaIn, ctx.artifactId);
            if (timestamped != null) {
                return downloadTimestampedPom(baseUrl, ctx, timestamped, "timestamped snapshot");
            }
            logger.debug("No snapshotVersion entry found in metadata for POM; will attempt timestamp+buildNumber method if present.");
        } catch (Exception metaEx) {
            logger.debug("No {} found at {} or failed to read: {}", MAVEN_METADATA_XML, metadataUrl, metaEx.getMessage());
        }
        return null;
    }

    private File tryTimestampBuildMetadataDownload(String baseUrl, String metadataUrl, DownloadContext ctx) {
        try (InputStream metaIn = new URL(metadataUrl).openStream()) {
            String ts = parseTimestampBuildFromMetadata(metaIn);
            if (ts != null) {
                return downloadTimestampedPom(baseUrl, ctx, ts, "constructed timestamped");
            }
        } catch (Exception e) {
            logger.debug("Failed to re-read metadata for timestamp+buildNumber method: {}", e.getMessage());
        }
        return null;
    }

    private File downloadTimestampedPom(String baseUrl, DownloadContext ctx, String timestamped, String description) {
        try {
            String timestampedPomPath = ctx.versionDir + ctx.artifactId + "-" + timestamped + ".pom";
            URL pomUrl = new URL(baseUrl + timestampedPomPath);
            logger.info("Attempting to mavendownload {} POM from {}", description, pomUrl);
            if (downloadUrlToPath(pomUrl, ctx.newDestination)) {
                logger.info("Successfully downloaded {} POM to: {}", description, ctx.newDestination);
                return ctx.newDestination.toFile();
            }
            logger.debug("{} POM at {} not found or failed to mavendownload", description, pomUrl);
        } catch (Exception e) {
            logger.debug("Failed to mavendownload {} POM: {}", description, e.getMessage());
        }
        return null;
    }

    private File tryDirectDownload(JavaRepository repository, DownloadContext ctx) {
        try {
            URL url = new URL(normalizeBaseUrl(repository.getUrl()) + ctx.pomPath);
            logger.debug("Attempting to mavendownload parent POM from: {} (repo id={}, repo name={})",
                url, repository.getId(), repository.getName());
            if (downloadUrlToPath(url, ctx.newDestination)) {
                logger.debug("Successfully downloaded POM to: {} (from repo: {})", ctx.newDestination, repository.getUrl());
                return ctx.newDestination.toFile();
            }
        } catch (Exception e) {
            logger.debug("Direct mavendownload failed: {}", e.getMessage());
        }
        return null;
    }

    private File trySonatypeFallbacks(DownloadContext ctx) {
        for (String base : SONATYPE_FALLBACK_REPOS) {
            File result = trySonatypeFallback(base, ctx);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private File trySonatypeFallback(String baseUrl, DownloadContext ctx) {
        try {
            String normalizedBase = normalizeBaseUrl(baseUrl);

            File metadataResult = tryFallbackMetadataDownload(normalizedBase, ctx);
            if (metadataResult != null) {
                return metadataResult;
            }

            return tryFallbackDirectDownload(normalizedBase, ctx);
        } catch (Exception e) {
            logger.debug("Fallback attempt failed for base {}: {}", baseUrl, e.getMessage());
        }
        return null;
    }

    private File tryFallbackMetadataDownload(String baseUrl, DownloadContext ctx) {
        String metadataUrl = baseUrl + ctx.versionDir + MAVEN_METADATA_XML;
        try (InputStream metaIn = new URL(metadataUrl).openStream()) {
            logger.debug("Found {} at {} (fallback)", MAVEN_METADATA_XML, metadataUrl);
            String timestamped = parseSnapshotVersionFromMetadata(metaIn, ctx.artifactId);
            if (timestamped != null) {
                String timestampedPomPath = ctx.versionDir + ctx.artifactId + "-" + timestamped + ".pom";
                URL pomUrl = new URL(baseUrl + timestampedPomPath);
                logger.info("Attempting to mavendownload timestamped snapshot POM from fallback {}", pomUrl);
                if (downloadUrlToPath(pomUrl, ctx.newDestination)) {
                    logger.info("Successfully downloaded timestamped snapshot POM from fallback to: {}", ctx.newDestination);
                    return ctx.newDestination.toFile();
                }
            } else {
                logger.debug("No snapshotVersion entry found in fallback metadata at {}", metadataUrl);
            }
        } catch (Exception metaEx) {
            logger.debug("No metadata at fallback {}: {}", metadataUrl, metaEx.getMessage());
        }
        return null;
    }

    private File tryFallbackDirectDownload(String baseUrl, DownloadContext ctx) {
        try {
            URL url = new URL(baseUrl + ctx.pomPath);
            logger.debug("Attempting fallback mavendownload from {}", url);
            if (downloadUrlToPath(url, ctx.newDestination)) {
                logger.info("Successfully downloaded POM from fallback to: {}", ctx.newDestination);
                return ctx.newDestination.toFile();
            }
        } catch (Exception e) {
            logger.debug("Fallback direct mavendownload failed: {}", e.getMessage());
        }
        return null;
    }

    private String normalizeBaseUrl(String url) {
        return url.endsWith("/") ? url : url + "/";
    }

    private void logDownloadFailure(DownloadContext ctx) {
        String triedRepos = remoteRepositories.stream()
            .map(JavaRepository::getUrl)
            .collect(Collectors.joining(", "));

        logger.error("Could not mavendownload POM for coordinates: {}:{}:{}; tried repositories: {}; downloadDir: {}",
            ctx.groupId, ctx.artifactId, ctx.version, triedRepos, downloadDir.toAbsolutePath());
    }

    private boolean downloadUrlToPath(URL url, Path destination) {
        try (InputStream inputStream = url.openStream();
             ReadableByteChannel readableByteChannel = Channels.newChannel(inputStream);
             FileOutputStream fileOutputStream = new FileOutputStream(destination.toFile())) {
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            return true;
        } catch (Exception e) {
            logger.debug("Failed to mavendownload URL {}: {}", url, e.getMessage());
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

    /**
     * Context object holding all mavendownload-related state for a single POM mavendownload operation.
     * Reduces parameter passing and variable count in methods.
     */
    private static class DownloadContext {
        final String groupId;
        final String artifactId;
        final String version;
        final boolean isSnapshot;
        final String versionDir;
        final String pomPath;
        final Path newDestination;
        final Path oldDestination;

        DownloadContext(JavaCoordinates coordinates, Path downloadDir) {
            this.groupId = coordinates.getGroupId();
            this.artifactId = coordinates.getArtifactId();
            this.version = coordinates.getVersion();
            this.isSnapshot = version != null && version.toUpperCase().contains("SNAPSHOT");

            String groupPath = groupId.replace('.', '/');
            this.versionDir = groupPath + "/" + artifactId + "/" + version + "/";
            this.pomPath = versionDir + artifactId + "-" + version + ".pom";

            this.newDestination = downloadDir
                .resolve(groupPath)
                .resolve(artifactId)
                .resolve(version)
                .resolve(artifactId + "-" + version + ".pom");
            this.oldDestination = downloadDir.resolve(artifactId + "-" + version + ".pom");
        }
    }
}
