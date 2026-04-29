package com.blackduck.integration.detectable.detectables.maven.resolver.mavendownload;

import com.blackduck.integration.detectable.detectables.maven.resolver.mirror.MavenMirrorConfig;
import com.blackduck.integration.detectable.detectables.maven.resolver.mirror.MavenMirrorResolver;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Downloads parent POMs and BOM POMs (and shaded-dependency POMs) directly via HTTP.
 *
 * <p>Used during the bootstrap phase, before Aether has built a session, where we still need to
 * fetch a single POM file in order to assemble the effective project model. Aether is not
 * available at this stage, so this class performs raw HTTP GETs.
 *
 * <p>This downloader is <strong>mirror-aware</strong>: every configured {@link JavaRepository}
 * (including the implicit Maven Central and the Sonatype snapshot fallbacks) is resolved against
 * the supplied list of {@link MavenMirrorConfig} via {@link MavenMirrorResolver} before any URL
 * is built. When a mirror matches:
 * <ul>
 *     <li>the request URL is rewritten to the mirror's URL, and</li>
 *     <li>if the mirror has credentials, an {@code Authorization: Basic} header is attached.</li>
 * </ul>
 *
 * <p>Mirror substitution is intentionally applied even to the Sonatype snapshot fallbacks: a
 * mirror with {@code mirrorOf=*} should intercept all repositories, fallbacks included; a mirror
 * scoped to {@code central} (or any specific id) leaves the fallbacks untouched.
 *
 * <p>This class is not thread-safe; instances are intended to be short-lived and used per-POM.
 */
public class MavenDownloader {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final List<JavaRepository> remoteRepositories;
    private final List<MavenMirrorConfig> mirrorConfigs;
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

    /**
     * Sonatype snapshot fallback repositories, modelled as {@link JavaRepository} so they go
     * through the same mirror-resolution path as configured repositories. Their id is set to
     * {@code "sonatype-snapshots"} so a mirror with {@code mirrorOf=*} (or one explicitly
     * targeting that id) will still intercept them.
     */
    private static final List<JavaRepository> SONATYPE_FALLBACK_REPOSITORIES = Arrays.asList(
        sonatypeFallback("https://oss.sonatype.org/content/repositories/snapshots/"),
        sonatypeFallback("https://s01.oss.sonatype.org/content/repositories/snapshots/"),
        sonatypeFallback("https://central.sonatype.com/repository/maven-snapshots/")
    );

    private static JavaRepository sonatypeFallback(String url) {
        JavaRepository repo = new JavaRepository();
        repo.setId("sonatype-snapshots");
        repo.setName("Sonatype Snapshots Fallback");
        repo.setUrl(url);
        repo.setSnapshotsEnabled(true);
        repo.setReleasesEnabled(false);
        return repo;
    }

    /**
     * Constructs a downloader without mirror support. Equivalent to passing an empty mirror list.
     * Retained for callers that have not yet been threaded for mirror configuration.
     */
    public MavenDownloader(List<JavaRepository> remoteRepositories, Path downloadDir) {
        this(remoteRepositories, downloadDir, Collections.emptyList());
    }

    /**
     * Constructs a mirror-aware downloader.
     *
     * @param remoteRepositories repositories declared in the POM being processed; may be null
     * @param downloadDir        directory where downloaded POMs will be written
     * @param mirrorConfigs      mirror configurations to apply; may be null or empty for none
     */
    public MavenDownloader(List<JavaRepository> remoteRepositories, Path downloadDir, @Nullable List<MavenMirrorConfig> mirrorConfigs) {
        if (remoteRepositories != null) {
            this.remoteRepositories = new ArrayList<>(remoteRepositories);
        } else {
            this.remoteRepositories = new ArrayList<>();
        }
        this.downloadDir = downloadDir;
        this.mirrorConfigs = (mirrorConfigs != null) ? new ArrayList<>(mirrorConfigs) : Collections.emptyList();

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
        EffectiveRepository effective = resolveEffectiveRepository(repository);
        try {
            if (ctx.isSnapshot) {
                File snapshotResult = trySnapshotDownload(effective, ctx);
                if (snapshotResult != null) {
                    return snapshotResult;
                }
            }
            return tryDirectDownload(effective, ctx);
        } catch (Exception e) {
            logger.debug("Failed to mavendownload from repository {} (effective {}) for coordinates {}:{}:{} - exception: {}",
                repository.getUrl(), effective.baseUrl, ctx.groupId, ctx.artifactId, ctx.version, e.toString());
            logger.debug("Download exception stacktrace for repository {}: ", repository.getUrl(), e);
        }
        return null;
    }

    private File trySnapshotDownload(EffectiveRepository effective, DownloadContext ctx) {
        String metadataUrl = effective.baseUrl + ctx.versionDir + MAVEN_METADATA_XML;

        File result = trySnapshotVersionMetadataDownload(effective, metadataUrl, ctx);
        if (result != null) {
            return result;
        }

        return tryTimestampBuildMetadataDownload(effective, metadataUrl, ctx);
    }

    private File trySnapshotVersionMetadataDownload(EffectiveRepository effective, String metadataUrl, DownloadContext ctx) {
        try (InputStream metaIn = openStream(new URL(metadataUrl), effective)) {
            logger.debug("Found {} at {}", MAVEN_METADATA_XML, metadataUrl);
            String timestamped = parseSnapshotVersionFromMetadata(metaIn, ctx.artifactId);
            if (timestamped != null) {
                return downloadTimestampedPom(effective, ctx, timestamped, "timestamped snapshot");
            }
            logger.debug("No snapshotVersion entry found in metadata for POM; will attempt timestamp+buildNumber method if present.");
        } catch (Exception metaEx) {
            logger.debug("No {} found at {} or failed to read: {}", MAVEN_METADATA_XML, metadataUrl, metaEx.getMessage());
        }
        return null;
    }

    private File tryTimestampBuildMetadataDownload(EffectiveRepository effective, String metadataUrl, DownloadContext ctx) {
        try (InputStream metaIn = openStream(new URL(metadataUrl), effective)) {
            String ts = parseTimestampBuildFromMetadata(metaIn);
            if (ts != null) {
                return downloadTimestampedPom(effective, ctx, ts, "constructed timestamped");
            }
        } catch (Exception e) {
            logger.debug("Failed to re-read metadata for timestamp+buildNumber method: {}", e.getMessage());
        }
        return null;
    }

    private File downloadTimestampedPom(EffectiveRepository effective, DownloadContext ctx, String timestamped, String description) {
        try {
            String timestampedPomPath = ctx.versionDir + ctx.artifactId + "-" + timestamped + ".pom";
            URL pomUrl = new URL(effective.baseUrl + timestampedPomPath);
            logger.info("Attempting to mavendownload {} POM from {}", description, pomUrl);
            if (downloadUrlToPath(pomUrl, ctx.newDestination, effective)) {
                logger.info("Successfully downloaded {} POM to: {}", description, ctx.newDestination);
                return ctx.newDestination.toFile();
            }
            logger.debug("{} POM at {} not found or failed to mavendownload", description, pomUrl);
        } catch (Exception e) {
            logger.debug("Failed to mavendownload {} POM: {}", description, e.getMessage());
        }
        return null;
    }

    private File tryDirectDownload(EffectiveRepository effective, DownloadContext ctx) {
        try {
            URL url = new URL(effective.baseUrl + ctx.pomPath);
            logger.debug("Attempting to mavendownload parent POM from: {} (repo id={}, repo name={}{})",
                url, effective.origin.getId(), effective.origin.getName(),
                effective.isMirrored() ? ", via mirror" : "");
            if (downloadUrlToPath(url, ctx.newDestination, effective)) {
                logger.debug("Successfully downloaded POM to: {} (from repo: {})", ctx.newDestination, effective.describe());
                return ctx.newDestination.toFile();
            }
        } catch (Exception e) {
            logger.debug("Direct mavendownload failed: {}", e.getMessage());
        }
        return null;
    }

    private File trySonatypeFallbacks(DownloadContext ctx) {
        // Resolve and dedupe by effective base URL — a wildcard mirror would otherwise
        // collapse all fallbacks into the same effective URL and we'd retry it pointlessly.
        Set<String> attempted = new LinkedHashSet<>();
        for (JavaRepository fallback : SONATYPE_FALLBACK_REPOSITORIES) {
            EffectiveRepository effective = resolveEffectiveRepository(fallback);
            if (!attempted.add(effective.baseUrl)) {
                continue;
            }
            File result = trySonatypeFallback(effective, ctx);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private File trySonatypeFallback(EffectiveRepository effective, DownloadContext ctx) {
        try {
            File metadataResult = tryFallbackMetadataDownload(effective, ctx);
            if (metadataResult != null) {
                return metadataResult;
            }
            return tryFallbackDirectDownload(effective, ctx);
        } catch (Exception e) {
            logger.debug("Fallback attempt failed for base {}: {}", effective.baseUrl, e.getMessage());
        }
        return null;
    }

    private File tryFallbackMetadataDownload(EffectiveRepository effective, DownloadContext ctx) {
        String metadataUrl = effective.baseUrl + ctx.versionDir + MAVEN_METADATA_XML;
        try (InputStream metaIn = openStream(new URL(metadataUrl), effective)) {
            logger.debug("Found {} at {} (fallback)", MAVEN_METADATA_XML, metadataUrl);
            String timestamped = parseSnapshotVersionFromMetadata(metaIn, ctx.artifactId);
            if (timestamped != null) {
                String timestampedPomPath = ctx.versionDir + ctx.artifactId + "-" + timestamped + ".pom";
                URL pomUrl = new URL(effective.baseUrl + timestampedPomPath);
                logger.info("Attempting to mavendownload timestamped snapshot POM from fallback {}", pomUrl);
                if (downloadUrlToPath(pomUrl, ctx.newDestination, effective)) {
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

    private File tryFallbackDirectDownload(EffectiveRepository effective, DownloadContext ctx) {
        try {
            URL url = new URL(effective.baseUrl + ctx.pomPath);
            logger.debug("Attempting fallback mavendownload from {}", url);
            if (downloadUrlToPath(url, ctx.newDestination, effective)) {
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

    /**
     * Resolves a {@link JavaRepository} into an {@link EffectiveRepository} by consulting the
     * configured mirrors. If a mirror matches the repository's id, its URL and credentials are
     * substituted; otherwise the repository's own URL is used.
     */
    private EffectiveRepository resolveEffectiveRepository(JavaRepository repository) {
        MavenMirrorConfig mirror = MavenMirrorResolver.findMatchingMirror(mirrorConfigs, repository.getId());
        if (mirror != null) {
            logger.debug("Mirror '{}' redirects repository '{}' ({}) to {}",
                mirror.getId(), repository.getId(), repository.getUrl(), mirror.getUrl());
            return new EffectiveRepository(normalizeBaseUrl(mirror.getUrl()), mirror.getUsername(), mirror.getPassword(), mirror.getId(), repository);
        }
        return new EffectiveRepository(normalizeBaseUrl(repository.getUrl()), null, null, null, repository);
    }

    private void logDownloadFailure(DownloadContext ctx) {
        String triedRepos = remoteRepositories.stream()
            .map(repo -> resolveEffectiveRepository(repo).describe())
            .collect(Collectors.joining(", "));

        logger.error("Could not mavendownload POM for coordinates: {}:{}:{}; tried repositories: {}; downloadDir: {}",
            ctx.groupId, ctx.artifactId, ctx.version, triedRepos, downloadDir.toAbsolutePath());
    }

    /**
     * Opens an input stream to the given URL, attaching an {@code Authorization: Basic} header
     * when the effective repository carries credentials. Centralizing this means every code path
     * that performs a raw HTTP read gets authentication for free.
     */
    private InputStream openStream(URL url, EffectiveRepository effective) throws IOException {
        URLConnection connection = url.openConnection();
        applyBasicAuth(connection, effective);
        return connection.getInputStream();
    }

    private boolean downloadUrlToPath(URL url, Path destination, EffectiveRepository effective) {
        try {
            URLConnection connection = url.openConnection();
            applyBasicAuth(connection, effective);
            try (InputStream inputStream = connection.getInputStream();
                 ReadableByteChannel readableByteChannel = Channels.newChannel(inputStream);
                 FileOutputStream fileOutputStream = new FileOutputStream(destination.toFile())) {
                fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                return true;
            }
        } catch (Exception e) {
            logger.debug("Failed to mavendownload URL {}: {}", url, e.getMessage());
            return false;
        }
    }

    private void applyBasicAuth(URLConnection connection, EffectiveRepository effective) {
        if (!effective.hasCredentials()) {
            return;
        }
        String token = effective.username + ":" + effective.password;
        String encoded = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
        connection.setRequestProperty("Authorization", "Basic " + encoded);
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

    /**
     * The repository URL (and optional credentials) actually used for HTTP requests, after mirror
     * substitution has been applied. Carries a back-reference to the originating
     * {@link JavaRepository} for diagnostic logging.
     */
    private static final class EffectiveRepository {
        final String baseUrl;
        @Nullable final String username;
        @Nullable final String password;
        @Nullable final String mirrorId; // null when no mirror was applied
        final JavaRepository origin;

        EffectiveRepository(String baseUrl, @Nullable String username, @Nullable String password,
                            @Nullable String mirrorId, JavaRepository origin) {
            this.baseUrl = baseUrl;
            this.username = username;
            this.password = password;
            this.mirrorId = mirrorId;
            this.origin = origin;
        }

        boolean hasCredentials() {
            return username != null && !username.isEmpty()
                && password != null && !password.isEmpty();
        }

        boolean isMirrored() {
            return mirrorId != null;
        }

        String describe() {
            if (isMirrored()) {
                return origin.getUrl() + " [via mirror '" + mirrorId + "' -> " + baseUrl + "]";
            }
            return origin.getUrl();
        }
    }
}

