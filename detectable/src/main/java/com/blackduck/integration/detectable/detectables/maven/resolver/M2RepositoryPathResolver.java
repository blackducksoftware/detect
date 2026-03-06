package com.blackduck.integration.detectable.detectables.maven.resolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves a user-provided path to a valid Maven local repository directory.
 *
 * <p>Resolution strategy:
 * <ol>
 *   <li>Normalize the path (resolve relative paths, expand user home "~").</li>
 *   <li>If the path itself is an existing directory, check whether it looks like
 *       a repository root (has Maven-style content) or an {@code .m2} directory
 *       that contains a {@code repository} subdirectory.</li>
 *   <li>If the path points to an {@code .m2} directory, append {@code repository}.</li>
 *   <li>Return {@code null} if no valid repository directory can be determined.</li>
 * </ol>
 */
public final class M2RepositoryPathResolver {

    private static final Logger logger = LoggerFactory.getLogger(M2RepositoryPathResolver.class);

    private static final String REPOSITORY_SUBDIR = "repository";

    private M2RepositoryPathResolver() {
        // utility class — no instantiation
    }

    /**
     * Resolves the given path to a Maven local repository directory.
     *
     * @param inputPath the user-provided path (may point to {@code .m2} or
     *                  {@code .m2/repository} or any custom repo root)
     * @return the resolved repository {@link Path} if it exists and is a directory,
     *         or {@code null} if resolution fails
     */
    public static Path resolve(Path inputPath) {
        if (inputPath == null) {
            logger.debug("Input path is null; cannot resolve repository path.");
            return null;
        }

        // Normalize: handle "~" prefix and make absolute
        Path normalized = normalizePath(inputPath);
        logger.debug("Normalized repository path: {} -> {}", inputPath, normalized);

        // Case 1: Path already points to an existing directory
        if (Files.isDirectory(normalized)) {
            // If the directory is named "repository", assume it is the repo root
            if (REPOSITORY_SUBDIR.equals(normalized.getFileName().toString())) {
                logger.debug("Path already ends with '{}'; using as repository root.", REPOSITORY_SUBDIR);
                return normalized;
            }

            // Check if it contains a "repository" subdirectory (i.e., it's an .m2 dir)
            Path repoSubdir = normalized.resolve(REPOSITORY_SUBDIR);
            if (Files.isDirectory(repoSubdir)) {
                logger.debug("Found '{}' subdirectory under provided path; using: {}",
                        REPOSITORY_SUBDIR, repoSubdir);
                return repoSubdir;
            }

            // The directory exists but has no "repository" subdir — treat it as the repo root itself
            logger.debug("Directory exists but has no '{}' subdirectory; using as-is: {}",
                    REPOSITORY_SUBDIR, normalized);
            return normalized;
        }

        // Case 2: Path does not exist yet — try appending "repository"
        Path withRepo = normalized.resolve(REPOSITORY_SUBDIR);
        if (Files.isDirectory(withRepo)) {
            logger.debug("Original path does not exist, but '{}' does; using: {}",
                    REPOSITORY_SUBDIR, withRepo);
            return withRepo;
        }

        // Case 3: Neither the path nor path/repository exist
        logger.warn("Resolved path does not exist as a directory: {}", normalized);
        return null;
    }

    /**
     * Normalizes the input path by expanding a leading "~" to the user home
     * directory and converting to an absolute path.
     */
    private static Path normalizePath(Path inputPath) {
        String pathStr = inputPath.toString();

        // Expand leading ~ to user.home
        if (pathStr.startsWith("~")) {
            String userHome = System.getProperty("user.home");
            if (userHome != null) {
                pathStr = userHome + pathStr.substring(1);
                logger.debug("Expanded '~' to user home: {}", pathStr);
            }
        }

        return Paths.get(pathStr).toAbsolutePath().normalize();
    }
}
