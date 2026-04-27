package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload.DownloadConfiguration;
import com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload.FallbackRepositoryDownloader;
import com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload.HttpArtifactDownloader;
import com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload.LocalRepositoryChecker;
import com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload.ParallelDownloadManager;
import com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload.RemoteRepositoryDownloader;
import com.blackduck.integration.detectable.detectables.maven.resolver.mirror.MavenMirrorConfig;
import com.blackduck.integration.detectable.detectables.maven.resolver.mirror.MavenMirrorResolver;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.JavaRepository;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Artifact Downloader that orchestrates JAR downloads using a 3-tier resolution strategy.
 *
 * <p>Tier-1: Local repositories (custom .m2 location, home ~/.m2/repository, download cache)
 * <p>Tier-2: POM-declared remote repositories
 * <p>Tier-3: Maven Central as fallback
 *
 * <p>This class follows the Single Responsibility Principle: it only orchestrates resolution.
 * All actual work is delegated to specialized classes:
 * <ul>
 *   <li>{@link LocalRepositoryChecker} - checks local repositories for existing JARs</li>
 *   <li>{@link RemoteRepositoryDownloader} - downloads from POM-declared repositories</li>
 *   <li>{@link FallbackRepositoryDownloader} - downloads from Maven Central (or mirror)</li>
 * </ul>
 */
public class ArtifactDownloader {

    private static final Logger logger = LoggerFactory.getLogger(ArtifactDownloader.class);

    private static final String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2";
    private static final String MAVEN_CENTRAL_URL_ALT = "https://repo.maven.apache.org/maven2";

    // Delegates
    private final LocalRepositoryChecker localChecker;
    private final FallbackRepositoryDownloader fallbackDownloader;
    private final Map<String, HttpArtifactDownloader> pomRepoDownloaders;
    private final ParallelDownloadManager parallelDownloadManager;

    // Configuration
    private final Path resolvedCustomRepositoryPath;
    private final Path defaultRepositoryPath;
    private final Path homeM2Repository;
    private final List<JavaRepository> pomRepositories;
    private final DownloadConfiguration downloadConfiguration;

    /**
     * Constructs an ArtifactDownloader with full configuration including POM repositories.
     *
     * @param customRepositoryPath Optional custom path to a local Maven repository (.m2 location).
     * @param defaultRepositoryPath Default download cache path (typically Detect output downloads dir)
     * @param pomRepositories List of repositories declared in POM files (Tier-2)
     * @param options User-facing configuration (download flag, custom repo path, mirrors)
     */
    public ArtifactDownloader(Path customRepositoryPath, Path defaultRepositoryPath,
                              List<JavaRepository> pomRepositories, MavenResolverOptions options) {

        logger.info("Initializing ArtifactDownloader...");

        // Initialize delegates
        this.localChecker = new LocalRepositoryChecker();
        this.downloadConfiguration = DownloadConfiguration.createDefault();

        // Resolve custom repository path
        if (customRepositoryPath != null) {
            logger.info("User-provided custom repository path: {}", customRepositoryPath);
            this.resolvedCustomRepositoryPath = M2RepositoryPathResolver.resolve(customRepositoryPath);
            if (this.resolvedCustomRepositoryPath != null) {
                logger.info("Resolved custom repository path to: {}", this.resolvedCustomRepositoryPath);
            } else {
                logger.warn("Could not resolve custom repository path. Custom repository will be skipped.");
            }
        } else {
            this.resolvedCustomRepositoryPath = null;
            logger.debug("No custom repository path configured");
        }

        if (defaultRepositoryPath == null) {
            throw new IllegalArgumentException("Default repository path must not be null");
        }
        this.defaultRepositoryPath = defaultRepositoryPath;

        // Resolve home .m2 repository once
        this.homeM2Repository = localChecker.resolveHomeM2Repository();

        // Deduplicate and filter POM repositories
        this.pomRepositories = deduplicatePomRepositories(pomRepositories);

        // Apply mirror redirects to POM repository downloaders and fallback downloader
        List<MavenMirrorConfig> mirrorConfigs = options != null ? options.getMirrorConfigurations() : Collections.<MavenMirrorConfig>emptyList();

        this.pomRepoDownloaders = new LinkedHashMap<>();
        for (JavaRepository repo : this.pomRepositories) {
            MavenMirrorConfig mirror = MavenMirrorResolver.findMatchingMirror(mirrorConfigs, repo.getId());
            if (mirror != null) {
                logger.info("Mirror '{}' redirects repository '{}' ({}) to {}", mirror.getId(), repo.getId(), repo.getUrl(), mirror.getUrl());
                pomRepoDownloaders.put(repo.getId(), new RemoteRepositoryDownloader(mirror.getUrl(), mirror.getUsername(), mirror.getPassword()));
            } else {
                pomRepoDownloaders.put(repo.getId(), new RemoteRepositoryDownloader(repo.getUrl()));
            }
        }

        MavenMirrorConfig centralMirror = MavenMirrorResolver.findMatchingMirror(mirrorConfigs, "central");
        if (centralMirror != null) {
            logger.info("Mirror '{}' redirects Maven Central fallback to {}", centralMirror.getId(), centralMirror.getUrl());
            this.fallbackDownloader = new FallbackRepositoryDownloader(centralMirror.getUrl(), centralMirror.getUsername(), centralMirror.getPassword());
        } else {
            this.fallbackDownloader = new FallbackRepositoryDownloader();
        }

        logInitializationSummary();

        this.parallelDownloadManager = new ParallelDownloadManager(null, this.localChecker, this.fallbackDownloader);
    }

    /**
     * Constructs an ArtifactDownloader without POM repositories (backward compatibility).
     */
    public ArtifactDownloader(Path customRepositoryPath, Path defaultRepositoryPath,
                              MavenResolverOptions options) {
        this(customRepositoryPath, defaultRepositoryPath, Collections.emptyList(), options);
    }

    /**
     * Downloads JAR files for all dependencies in the list using parallel downloads.
     *
     * @param dependencies the list of dependencies to download
     * @return a map of successfully downloaded artifacts to their local JAR file paths
     */
    public Map<Artifact, Path> downloadArtifacts(List<Dependency> dependencies) {
        Map<Artifact, Path> artifactPathMap = new ConcurrentHashMap<>();

        if (dependencies == null || dependencies.isEmpty()) {
            logger.info("No dependencies to download. Skipping JAR download phase.");
            return artifactPathMap;
        }

        // Pre-filter: skip artifacts with classifiers (e.g., sources, javadoc, test-jar)
        List<Dependency> downloadable = new ArrayList<>();
        int classifierSkippedCount = 0;
        for (Dependency dep : dependencies) {
            String classifier = dep.getArtifact().getClassifier();
            if (classifier != null && !classifier.isEmpty()) {
                logger.info("SKIPPING artifact with classifier '{}': {}", classifier, formatCoords(dep.getArtifact()));
                classifierSkippedCount++;
            } else {
                downloadable.add(dep);
            }
        }

        // Build a GAV -> Artifact lookup from the input dependencies list.
        // This is needed because ParallelDownloadResult returns ArtifactCoordinate (not Artifact),
        // so we need to map back to the original Artifact objects for the return type.
        Map<String, Artifact> gavToArtifact = new HashMap<>();
        for (Dependency dep : downloadable) {
            Artifact a = dep.getArtifact();
            String gavKey = a.getGroupId() + ":" + a.getArtifactId() + ":" + a.getVersion();
            gavToArtifact.put(gavKey, a);
        }

        // Delegate to ParallelDownloadManager with pre-built mirror-aware downloaders
        ParallelDownloadManager.ParallelDownloadResult parallelResult =
            parallelDownloadManager.downloadArtifacts(
                downloadable,
                resolvedCustomRepositoryPath,
                homeM2Repository,
                defaultRepositoryPath,
                pomRepoDownloaders,
                downloadConfiguration
            );

        // Translate ParallelDownloadResult outcomes back into Map<Artifact, Path>
        for (ParallelDownloadManager.DownloadOutcome outcome : parallelResult.getOutcomes()) {
            if ((outcome.getStatus() == ParallelDownloadManager.DownloadOutcome.DownloadStatus.SUCCESS
                    || outcome.getStatus() == ParallelDownloadManager.DownloadOutcome.DownloadStatus.SKIPPED_LOCAL)
                    && outcome.getPath() != null && outcome.getCoordinate() != null) {
                String gavKey = outcome.getCoordinate().getGroupId() + ":"
                    + outcome.getCoordinate().getArtifactId() + ":"
                    + outcome.getCoordinate().getVersion();
                Artifact originalArtifact = gavToArtifact.get(gavKey);
                if (originalArtifact != null) {
                    artifactPathMap.put(originalArtifact, outcome.getPath());
                }
            }
        }

        // Build summary lists from parallel outcomes for the existing logDownloadSummary method
        List<String> failedArtifacts = new ArrayList<>();
        List<String> unavailableArtifacts = new ArrayList<>();
        for (ParallelDownloadManager.DownloadOutcome outcome : parallelResult.getOutcomes()) {
            String coords = outcome.getCoordinate() != null ? outcome.getCoordinate().toString() : "unknown";
            if (outcome.getStatus() == ParallelDownloadManager.DownloadOutcome.DownloadStatus.FAILED) {
                failedArtifacts.add(coords + " - " + (outcome.getErrorMessage() != null ? outcome.getErrorMessage() : "Unknown error"));
            } else if (outcome.getStatus() == ParallelDownloadManager.DownloadOutcome.DownloadStatus.SKIPPED_NOT_FOUND) {
                unavailableArtifacts.add(coords);
            }
        }

        logDownloadSummary(
            dependencies.size(),
            parallelResult.getSuccessCount(),
            parallelResult.getFailureCount(),
            parallelResult.getSkippedCount() + classifierSkippedCount,
            artifactPathMap.size(),
            parallelResult.getTotalTimeMs(),
            unavailableArtifacts,
            failedArtifacts
        );

        return artifactPathMap;
    }

    /**
     * Deduplicates POM repositories, removing Maven Central references (handled as Tier-3).
     */
    private List<JavaRepository> deduplicatePomRepositories(List<JavaRepository> repositories) {
        if (repositories == null || repositories.isEmpty()) {
            return Collections.emptyList();
        }

        List<JavaRepository> filtered = new ArrayList<>();
        int mavenCentralSkipped = 0;

        for (JavaRepository repo : repositories) {
            String url = repo.getUrl();
            if (url == null || url.isEmpty()) {
                continue;
            }

            String normalizedUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;

            if (normalizedUrl.equals(MAVEN_CENTRAL_URL) || normalizedUrl.equals(MAVEN_CENTRAL_URL_ALT)) {
                mavenCentralSkipped++;
                continue;
            }

            filtered.add(repo);
        }

        if (mavenCentralSkipped > 0) {
            logger.debug("Deduplicated {} Maven Central reference(s) from POM repositories", mavenCentralSkipped);
        }

        return filtered;
    }

    private void logInitializationSummary() {
        logger.info(" TIER-1: LOCAL REPOSITORIES ");
        logger.info("  Custom .m2 repository: {}",
            resolvedCustomRepositoryPath != null ? resolvedCustomRepositoryPath : "not configured");
        logger.info("  Home .m2 repository: {}",
            homeM2Repository != null ? homeM2Repository : "not found");
        logger.info("  Download cache: {}", defaultRepositoryPath);

        logger.info(" TIER-2: POM REPOSITORIES ");
        if (pomRepositories.isEmpty()) {
            logger.info("  No POM-declared repositories configured");
        } else {
            logger.info("  {} POM repositories configured:", pomRepositories.size());
            for (int i = 0; i < pomRepositories.size(); i++) {
                JavaRepository repo = pomRepositories.get(i);
                logger.info("    {}. {} ({})", i + 1, repo.getId(), repo.getUrl());
            }
        }

        logger.info(" TIER-3: MAVEN CENTRAL ");
        logger.info("  Maven Central: Always available as fallback");

        logger.info(" DOWNLOAD CONFIGURATION ");
        logger.info("  {}", downloadConfiguration);
    }

    private void logDownloadSummary(int total, int success, int failures, int skipped,
                                    int mapSize, long elapsedMs,
                                    List<String> unavailable, List<String> failed) {
        logger.info("JAR DOWNLOAD PHASE SUMMARY");
        logger.info("========================================");
        logger.info("Total processed: {}", total);
        logger.info("  Successfully downloaded: {}", success);
        logger.info("  Not available (skipped): {}", skipped);
        logger.info("  Failed with errors: {}", failures);
        logger.info("  Artifact-to-path map entries: {}", mapSize);
        logger.info("Time elapsed: {} ms", elapsedMs);

        if (!unavailable.isEmpty()) {
            logger.info("Artifacts not available in repositories ({}):", unavailable.size());
            unavailable.forEach(a -> logger.info("  - {}", a));
        }

        if (!failed.isEmpty()) {
            logger.error("Failed artifacts requiring attention ({}):", failed.size());
            failed.forEach(a -> logger.error("  - {}", a));
        }

        if (failures > 0) {
            logger.error("JAR DOWNLOAD PHASE COMPLETED WITH ERRORS: {} artifacts failed", failures);
        } else if (skipped > 0) {
            logger.info("JAR DOWNLOAD PHASE COMPLETED: {} artifacts unavailable but no errors", skipped);
        } else {
            logger.info("JAR DOWNLOAD PHASE COMPLETED SUCCESSFULLY: All {} artifacts downloaded", success);
        }
    }


    private String formatCoords(Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
    }
}

