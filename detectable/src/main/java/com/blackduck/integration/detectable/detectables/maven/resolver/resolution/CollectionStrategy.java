package com.blackduck.integration.detectable.detectables.maven.resolver.resolution;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Executes dependency collection using a 3-tier fallback strategy.
 *
 * <p>Strategy attempts (in order):
 * <ol>
 *   <li><strong>Union</strong>: External + Declared + Central (if not already present)</li>
 *   <li><strong>Declared</strong>: POM-declared repositories only</li>
 *   <li><strong>Central</strong>: Maven Central only (last resort fallback)</li>
 * </ol>
 *
 * <p>Between attempts 1 and 2, runs diagnostic probes on individual repositories
 * to identify problematic repositories (for logging purposes only, does not affect
 * the fallback strategy).
 */
public class CollectionStrategy {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final RepositorySystem repositorySystem;

    /**
     * Constructs a CollectionStrategy.
     *
     * @param repositorySystem The Aether repository system
     */
    public CollectionStrategy(RepositorySystem repositorySystem) {
        this.repositorySystem = repositorySystem;
    }

    /**
     * Executes dependency collection using the fallback strategy.
     *
     * @param request The collect request containing dependencies and root artifact
     * @param session The repository system session
     * @param repositories The repository set containing all repository collections
     * @param pomFile The POM file being resolved (for logging)
     * @return CollectResult containing the resolved dependency tree
     * @throws DependencyCollectionException if all attempts fail
     */
    public CollectResult executeCollection(
        CollectRequest request,
        RepositorySystemSession session,
        RepositoryBuilder.RepositorySet repositories,
        File pomFile
    ) throws DependencyCollectionException {

        logger.info("------------------------------------------------------------");
        logger.info("Resolving dependency tree for: {}", pomFile.getAbsolutePath());

        // Attempt 1: Union (external + declared + central if not present)
        CollectResult result = attemptUnionCollection(request, session, repositories, pomFile);
        if (result != null) {
            return result;
        }

        // Diagnostic probe: test declared repos individually (logging only, does not affect strategy)
        runDiagnosticProbe(request, session, repositories.getDeclared());

        // Attempt 2: Declared repositories only
        result = attemptDeclaredCollection(request, session, repositories, pomFile);
        if (result != null) {
            return result;
        }

        // Attempt 3: Central-only fallback
        result = attemptCentralFallback(request, session, repositories, pomFile);
        if (result != null) {
            return result;
        }

        // All attempts failed
        throw new DependencyCollectionException(
            new CollectResult(request),
            "Dependency collection failed after union/declared/central attempts"
        );
    }

    /**
     * Attempt 1: Collection using union of external + declared + central repositories.
     *
     * @return CollectResult if successful, null if failed
     */
    private CollectResult attemptUnionCollection(
        CollectRequest request,
        RepositorySystemSession session,
        RepositoryBuilder.RepositorySet repositories,
        File pomFile
    ) {
        try {
            request.setRepositories(repositories.getUnion());
            logger.info("Attempting dependency collection with UNION repos: {}",
                repositories.getUnion().stream().map(RemoteRepository::getUrl).collect(Collectors.joining(", ")));

            CollectResult result = repositorySystem.collectDependencies(session, request);

            logger.info("Dependency tree collected for: {} using union repositories", pomFile.getAbsolutePath());
            logger.info("Collected root children: {}",
                result.getRoot().getChildren().stream()
                    .map(c -> c.getDependency().getArtifact().toString())
                    .collect(Collectors.joining(", ")));

            return result;
        } catch (Exception unionEx) {
            logger.info("Union repositories collection failed: {}", unionEx.getMessage());
            logger.info("Full exception for union attempt: ", unionEx);
            return null;
        }
    }

    /**
     * Runs diagnostic probes on individual repositories.
     *
     * <p>Tests each declared repository in isolation to identify problematic ones.
     * This is for logging/diagnostic purposes only and does not affect the fallback strategy.
     *
     * @param request The collect request
     * @param session The repository system session
     * @param declaredRepos List of declared repositories to probe
     */
    private void runDiagnosticProbe(
        CollectRequest request,
        RepositorySystemSession session,
        List<RemoteRepository> declaredRepos
    ) {
        if (declaredRepos.isEmpty()) {
            return;
        }

        for (RemoteRepository repo : declaredRepos) {
            try {
                CollectRequest singleRepoRequest = new CollectRequest();
                singleRepoRequest.setRoot(request.getRoot());
                singleRepoRequest.setDependencies(request.getDependencies());
                singleRepoRequest.setManagedDependencies(request.getManagedDependencies());
                singleRepoRequest.setRepositories(Collections.singletonList(repo));

                logger.info("Diagnostic: testing single repository {}", repo.getUrl());
                repositorySystem.collectDependencies(session, singleRepoRequest);
                logger.info("Diagnostic: repository {} can collect in isolation.", repo.getUrl());
            } catch (Exception perRepoEx) {
                logger.info("Diagnostic: repository {} failed in isolation: {}", repo.getUrl(), perRepoEx.getMessage());
                logger.info("Diagnostic: full exception for repo {}: ", repo.getUrl(), perRepoEx);
            }
        }
    }

    /**
     * Attempt 2: Collection using declared repositories only (original POM order).
     *
     * @return CollectResult if successful, null if failed
     */
    private CollectResult attemptDeclaredCollection(
        CollectRequest request,
        RepositorySystemSession session,
        RepositoryBuilder.RepositorySet repositories,
        File pomFile
    ) {
        try {
            request.setRepositories(repositories.getDeclared());
            logger.info("Attempting dependency collection with DECLARED repos: {}",
                repositories.getDeclared().stream().map(RemoteRepository::getUrl).collect(Collectors.joining(", ")));

            CollectResult result = repositorySystem.collectDependencies(session, request);

            logger.info("Dependency tree collected for: {} using declared repositories", pomFile.getAbsolutePath());
            return result;
        } catch (Exception declaredEx) {
            logger.info("Declared repositories collection failed: {}", declaredEx.getMessage());
            logger.info("Full exception for declared attempt: ", declaredEx);
            return null;
        }
    }

    /**
     * Attempt 3: Collection using Maven Central only (last resort fallback).
     *
     * @return CollectResult if successful, null if failed
     */
    private CollectResult attemptCentralFallback(
        CollectRequest request,
        RepositorySystemSession session,
        RepositoryBuilder.RepositorySet repositories,
        File pomFile
    ) {
        try {
            request.setRepositories(Collections.singletonList(repositories.getCentral()));
            logger.info("Falling back to Maven Central only.");

            CollectResult result = repositorySystem.collectDependencies(session, request);

            logger.info("Dependency tree collected for: {} using Maven Central fallback", pomFile.getAbsolutePath());
            return result;
        } catch (Exception centralOnlyEx) {
            logger.info("Maven Central-only collection failed: {}", centralOnlyEx.getMessage());
            logger.info("Full exception for Central-only attempt: ", centralOnlyEx);
            return null;
        }
    }
}
