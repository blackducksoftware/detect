package com.blackduck.integration.detectable.detectables.bazel.pipeline.step;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detectable.detectables.bazel.query.BazelQueryBuilder;

/**
 * Shared execution helpers for {@code bazel mod show_repo}: batched calls, output-block splitting,
 * and an ordered per-candidate fallback. Callers supply their own candidate arguments.
 */
public class ShowRepoExecutor {
    private static final Logger logger = LoggerFactory.getLogger(ShowRepoExecutor.class);

    // Repo blocks in batched show_repo output start with "## @@<name>:" or "## @<name>:".
    public static final String BLOCK_HEADER_PREFIX = "## @";
    private static final String BLOCK_SPLIT_REGEX = "(?=" + BLOCK_HEADER_PREFIX + ")";

    private final BazelCommandExecutor bazel;

    public ShowRepoExecutor(BazelCommandExecutor bazel) {
        this.bazel = bazel;
    }

    /**
     * Runs a single batched {@code bazel mod show_repo <repoArgs...>}, returning stdout if non-empty.
     */
    public Optional<String> runBatch(List<String> repoArgs) {
        if (repoArgs == null || repoArgs.isEmpty()) {
            return Optional.empty();
        }
        try {
            List<String> cmd = BazelQueryBuilder.mod().showRepoRawBatch(repoArgs).build();
            // stdout is authoritative even on a non-zero exit code (a broken unrelated extension can
            // poison it, e.g. bazel_jar_jar+ on Bazel 9).
            Optional<String> out = bazel.executeModCommandToString(cmd);
            if (out.isPresent() && !out.get().trim().isEmpty()) {
                return out;
            }
        } catch (Exception e) {
            logger.debug("Batched show_repo failed with exception: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Splits combined batched output into trimmed, non-empty repo blocks on the {@code ## @} boundary.
     */
    public List<String> splitIntoBlocks(String combinedOutput) {
        List<String> blocks = new ArrayList<>();
        if (combinedOutput == null) {
            return blocks;
        }
        for (String part : combinedOutput.split(BLOCK_SPLIT_REGEX)) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                blocks.add(trimmed);
            }
        }
        return blocks;
    }

    /**
     * Tries each candidate in order and returns the first non-empty {@code show_repo} output.
     * A candidate that throws or returns empty is skipped (handles the Bazel 7.x NPE on some
     * canonical forms).
     */
    public Optional<String> runFirstSuccessful(List<String> candidateArgs) {
        if (candidateArgs == null) {
            return Optional.empty();
        }
        for (String candidate : candidateArgs) {
            try {
                List<String> cmd = BazelQueryBuilder.mod().showRepoRaw(candidate).build();
                Optional<String> out = bazel.executeModCommandToString(cmd);
                if (out.isPresent() && !out.get().trim().isEmpty()) {
                    logger.debug("show_repo resolved using candidate '{}'", candidate);
                    return out;
                }
                logger.debug("show_repo candidate '{}' returned no output; trying next", candidate);
            } catch (Exception e) {
                logger.debug("show_repo candidate '{}' failed: {} — trying next", candidate, e.getMessage());
            }
        }
        return Optional.empty();
    }
}


