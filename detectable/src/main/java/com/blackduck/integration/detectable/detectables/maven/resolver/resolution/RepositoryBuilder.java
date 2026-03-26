package com.blackduck.integration.detectable.detectables.maven.resolver.resolution;

import com.blackduck.integration.detectable.detectables.maven.resolver.model.JavaRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds RemoteRepository objects from various sources (external URLs, POM declarations, Maven Central).
 *
 * <p>Handles URL validation, repository ID generation, and aggregation of repositories
 * from multiple sources into a unified collection.
 */
public class RepositoryBuilder {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String MAVEN_CENTRAL_URL = "https://repo.maven.apache.org/maven2/";
    private static final String REPOSITORY_TYPE_DEFAULT = "default";

    /**
     * Builds RemoteRepository objects from external repository URLs.
     *
     * @param externalRepositories List of external repository URLs
     * @return List of RemoteRepository objects (empty if none valid)
     */
    public List<RemoteRepository> buildExternalRepositories(List<String> externalRepositories) {
        List<RemoteRepository> externalRepos = new ArrayList<>();

        if (externalRepositories == null || externalRepositories.isEmpty()) {
            return externalRepos;
        }

        logger.info("Processing {} external repository URL(s)...", externalRepositories.size());

        for (String url : externalRepositories) {
            if (url == null || url.trim().isEmpty()) {
                logger.warn("Skipping empty external repository URL.");
                continue;
            }

            String trimmedUrl = url.trim();

            // Basic URL validation
            if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
                logger.warn("Skipping invalid external repository URL (must start with http:// or https://): {}", trimmedUrl);
                continue;
            }

            try {
                String id = generateRepositoryId(trimmedUrl, "external");
                RemoteRepository.Builder builder = new RemoteRepository.Builder(id, REPOSITORY_TYPE_DEFAULT, trimmedUrl);
                externalRepos.add(builder.build());
                logger.info("Added external repository: {} (id: {})", trimmedUrl, id);
            } catch (Exception e) {
                logger.warn("Failed to construct RemoteRepository for external URL: {}. Error: {}", trimmedUrl, e.getMessage());
            }
        }

        if (!externalRepos.isEmpty()) {
            logger.info("Using {} external repository(ies): {}", externalRepos.size(),
                externalRepos.stream().map(RemoteRepository::getUrl).collect(Collectors.joining(", ")));
        }

        return externalRepos;
    }

    /**
     * Builds RemoteRepository objects from POM-declared repositories.
     *
     * @param pomRepositories List of repositories declared in the POM
     * @return List of RemoteRepository objects (empty if none declared)
     */
    public List<RemoteRepository> buildDeclaredRepositories(List<JavaRepository> pomRepositories) {
        if (pomRepositories == null || pomRepositories.isEmpty()) {
            logger.info("No repositories declared in POM.");
            return new ArrayList<>();
        }

        List<RemoteRepository> declaredRepositories = pomRepositories.stream()
            .map(repo -> {
                String id = repo.getId() == null || repo.getId().trim().isEmpty() ? repo.getUrl() : repo.getId();
                try {
                    RemoteRepository.Builder builder = new RemoteRepository.Builder(id, REPOSITORY_TYPE_DEFAULT, repo.getUrl());
                    if (!repo.isReleasesEnabled()) {
                        logger.info("Repository {} has releases disabled", repo.getUrl());
                    }
                    if (!repo.isSnapshotsEnabled()) {
                        logger.info("Repository {} has snapshots disabled", repo.getUrl());
                    }
                    return builder.build();
                } catch (Exception e) {
                    logger.info("Failed to construct RemoteRepository for {} (id: {}). Falling back to basic builder. Exception: {}",
                        repo.getUrl(), id, e.getMessage());
                    return new RemoteRepository.Builder(id, REPOSITORY_TYPE_DEFAULT, repo.getUrl()).build();
                }
            })
            .collect(Collectors.toList());

        logger.info("Using repositories declared in POM (in order): {}",
            pomRepositories.stream().map(JavaRepository::getUrl).collect(Collectors.joining(", ")));

        return declaredRepositories;
    }

    /**
     * Builds the Maven Central repository.
     *
     * @return RemoteRepository for Maven Central
     */
    public RemoteRepository buildMavenCentral() {
        return new RemoteRepository.Builder("central", REPOSITORY_TYPE_DEFAULT, MAVEN_CENTRAL_URL).build();
    }

    /**
     * Builds a union of repositories: external + declared + Central (if not already present).
     *
     * <p>Priority order:
     * <ol>
     *   <li>External repositories (user-specified, highest priority)</li>
     *   <li>POM-declared repositories</li>
     *   <li>Maven Central (added only if not already present)</li>
     * </ol>
     *
     * @param externalRepos External repositories
     * @param declaredRepos POM-declared repositories
     * @return RepositorySet containing all repository collections
     */
    public RepositorySet buildRepositorySet(List<RemoteRepository> externalRepos, List<RemoteRepository> declaredRepos) {
        RemoteRepository central = buildMavenCentral();

        // Check if Central is present in either external or declared repos
        boolean centralPresent = externalRepos.stream().anyMatch(r -> MAVEN_CENTRAL_URL.equalsIgnoreCase(r.getUrl()))
            || declaredRepos.stream().anyMatch(r -> MAVEN_CENTRAL_URL.equalsIgnoreCase(r.getUrl()));

        // Build union: External → Declared → Central (if not present)
        List<RemoteRepository> unionRepos = new ArrayList<>();
        unionRepos.addAll(externalRepos);
        unionRepos.addAll(declaredRepos);
        if (!centralPresent) {
            unionRepos.add(central);
        }

        return new RepositorySet(externalRepos, declaredRepos, central, unionRepos, centralPresent);
    }

    /**
     * Generates a unique repository ID from a URL.
     *
     * @param url The repository URL
     * @param prefix Prefix for the ID (e.g., "external")
     * @return Generated repository ID
     */
    private String generateRepositoryId(String url, String prefix) {
        String sanitized = url.replaceAll("[^a-zA-Z0-9]", "-");
        int maxLength = Math.min(50, sanitized.length());
        return prefix + "-" + sanitized.substring(0, maxLength);
    }

    /**
     * Value object containing repository collections for dependency resolution.
     */
    public static class RepositorySet {
        private final List<RemoteRepository> external;
        private final List<RemoteRepository> declared;
        private final RemoteRepository central;
        private final List<RemoteRepository> union;
        private final boolean centralPresent;

        public RepositorySet(
            List<RemoteRepository> external,
            List<RemoteRepository> declared,
            RemoteRepository central,
            List<RemoteRepository> union,
            boolean centralPresent
        ) {
            this.external = Collections.unmodifiableList(external);
            this.declared = Collections.unmodifiableList(declared);
            this.central = central;
            this.union = Collections.unmodifiableList(union);
            this.centralPresent = centralPresent;
        }

        public List<RemoteRepository> getExternal() { return external; }
        public List<RemoteRepository> getDeclared() { return declared; }
        public RemoteRepository getCentral() { return central; }
        public List<RemoteRepository> getUnion() { return union; }
        public boolean isCentralPresent() { return centralPresent; }
    }
}
