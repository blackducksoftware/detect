package com.blackduck.integration.detectable.detectables.bazel.pipeline.step;

import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectables.bazel.query.BazelQueryBuilder;
import com.blackduck.integration.detectable.detectables.bazel.v2.BazelVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Executes `bazel mod show_repo` for input repos using a mapping-free heuristic:
 * - If input looks canonical/synthetic (starts with @@ or contains '+'/'~'): try only @@<bare>
 * - Otherwise: try @<bare> first, then @@<bare> as fallback
 *
 * For Bazel 7.1+, attempts a single batched `bazel mod show_repo @repo1 @repo2 ...` call first.
 * Falls back to per-repo calls if the batch fails or the Bazel version is < 7.1.
 *
 * Input: repo names as strings (with or without leading @/@@), one per line.
 * Output: the full mod show_repo output blocks for repos that succeed.
 */
public class IntermediateStepExecuteShowRepoHeuristic implements IntermediateStep {
    // Logger for this class
    private static final Logger logger = LoggerFactory.getLogger(IntermediateStepExecuteShowRepoHeuristic.class);

    // Bazel command executor dependency
    private final BazelCommandExecutor bazel;
    // Detected Bazel version; null means unknown (treat as < 7.1)
    private final BazelVersion bazelVersion;

    // Extracted string constants for repo prefix markers
    private static final String REPO_PREFIX_SINGLE = "@";
    private static final String REPO_PREFIX_CANONICAL = "@@";

    // Separator between repo blocks in batched show_repo output
    private static final String REPO_BLOCK_SEPARATOR = "## @";

    /**
     * Constructor for IntermediateStepExecuteShowRepoHeuristic (backward-compatible).
     * @param bazel Bazel command executor
     */
    public IntermediateStepExecuteShowRepoHeuristic(BazelCommandExecutor bazel) {
        this(bazel, null);
    }

    /**
     * Constructor for IntermediateStepExecuteShowRepoHeuristic with version-awareness.
     * @param bazel Bazel command executor
     * @param bazelVersion Detected Bazel version; null means unknown (per-repo fallback)
     */
    public IntermediateStepExecuteShowRepoHeuristic(BazelCommandExecutor bazel, BazelVersion bazelVersion) {
        this.bazel = bazel;
        this.bazelVersion = bazelVersion;
    }

    /**
     * Processes a list of repo names, running 'bazel mod show_repo' for each using a heuristic.
     * For Bazel 7.1+, attempts batched execution first.
     * @param input List of repo names (with or without leading @/@@)
     * @return List of successful mod show_repo output blocks
     * @throws DetectableException if Bazel command execution fails
     */
    @Override
    public List<String> process(List<String> input) throws DetectableException {
        if (input == null || input.isEmpty()) return new ArrayList<>();

        // For Bazel 7.1+, try batched show_repo first
        if (bazelVersion != null && bazelVersion.isAtLeast(7, 1)) {
            Optional<List<String>> batchResult = tryBatchedShowRepo(input);
            if (batchResult.isPresent()) {
                return batchResult.get();
            }
            logger.info("Batched show_repo failed; falling back to per-repo calls.");
        }

        return processPerRepo(input);
    }

    /**
     * Attempts a single batched `bazel mod show_repo @repo1 @repo2 ...` call.
     * Returns present with output blocks if successful, or empty if the batch fails.
     */
    private Optional<List<String>> tryBatchedShowRepo(List<String> input) {
        List<String> repoArgs = new ArrayList<>();
        for (String raw : input) {
            if (raw == null) continue;
            String token = raw.trim();
            if (token.isEmpty()) continue;
            // For batched call, use the first candidate (preferred form)
            List<String> candidates = candidateRepoArgs(token);
            if (!candidates.isEmpty()) {
                repoArgs.add(candidates.get(0));
            }
        }

        if (repoArgs.isEmpty()) return Optional.of(new ArrayList<>());

        try {
            logger.debug("Attempting batched show_repo for {} repos (Bazel {})", repoArgs.size(), bazelVersion);
            List<String> modArgs = BazelQueryBuilder.mod()
                .showRepoRawBatch(repoArgs)
                .build();

            // Use executeModCommandToString: a broken module extension (e.g., bazel_jar_jar+ on Bazel 9)
            // causes exit code 2 even when the batch output is fully valid in stdout.
            Optional<String> result = bazel.executeModCommandToString(modArgs);
            if (result.isPresent() && !result.get().trim().isEmpty()) {
                List<String> blocks = splitShowRepoOutput(result.get());
                logger.debug("Batched show_repo succeeded: {} blocks from {} repos", blocks.size(), repoArgs.size());
                return Optional.of(blocks);
            }
        } catch (Exception e) {
            logger.debug("Batched show_repo failed: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Splits the combined output of a batched `bazel mod show_repo` into individual repo blocks.
     * Each block starts with "## @reponame:" header.
     */
    private List<String> splitShowRepoOutput(String combinedOutput) {
        List<String> blocks = new ArrayList<>();
        // Split on the "## @" boundary which separates repo blocks
        String[] parts = combinedOutput.split("(?=" + REPO_BLOCK_SEPARATOR + ")");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                blocks.add(trimmed);
            }
        }
        return blocks;
    }

    /**
     * Original per-repo processing logic.
     */
    private List<String> processPerRepo(List<String> input) {
        List<String> out = new ArrayList<>();

        int successes = 0, failures = 0;
        for (String raw : input) {
            if (raw == null) continue;
            String token = raw.trim();
            if (token.isEmpty()) continue;

            // Generate candidate repo arguments according to heuristic and try them in order
            List<String> candidates = candidateRepoArgs(token);
            boolean success = false;
            for (String candidate : candidates) {
                if (tryShowRepoAddOutput(candidate, out)) {
                    success = true;
                    break;
                }
            }

            if (success) successes++; else failures++;
        }
        logger.info("bzlmod HTTP show_repo (heuristic) summary: successes={}, failures={}", successes, failures);
        return out;
    }

    /**
     * Given a token (may include leading @/@@), return the list of repo arguments to try, in order.
     * Example: "@org/repo" -> ["@org/repo", "@@org/repo"]
     *          "@@org/repo" -> ["@@org/repo"]
     *          "org+synthetic" -> ["@@org+synthetic"]
     */
    private List<String> candidateRepoArgs(String token) {
        boolean sawCanonical = token.startsWith(REPO_PREFIX_CANONICAL);
        String bare = stripAt(token);
        boolean synthetic = looksSynthetic(bare);

        if (sawCanonical || synthetic) {
            return Collections.singletonList(REPO_PREFIX_CANONICAL + bare);
        }
        return Arrays.asList(REPO_PREFIX_SINGLE + bare, REPO_PREFIX_CANONICAL + bare);
    }

    /**
     * Removes all leading '@' characters from a repo token.
     * @param token Repo name (may start with @ or @@)
     * @return Bare repo name without leading @
     */
    private String stripAt(String token) {
        String t = token;
        while (t.startsWith(REPO_PREFIX_SINGLE)) t = t.substring(REPO_PREFIX_SINGLE.length());
        return t;
    }

    /**
     * Determines if a repo name is synthetic (Bazel 8/7 style).
     * @param name Repo name
     * @return true if name contains '+' (Bazel 8) or '~' (Bazel 7)
     */
    private boolean looksSynthetic(String name) {
        // Treat '+' (Bazel 8) and '~' (Bazel 7) as synthetic markers
        return name.contains("+") || name.contains("~");
    }

    /**
     * Runs 'bazel mod show_repo' for the given repo argument and adds output to the result list if successful.
     * @param repoArg Repo argument (with leading @ or @@)
     * @param out Output list to add successful results
     * @return true if the command succeeded and output was added, false otherwise
     */
    private boolean tryShowRepoAddOutput(String repoArg, List<String> out) {
        // Use executeModCommandToString: a broken module extension (e.g., bazel_jar_jar+ on Bazel 9)
        // poisons the exit code to 2 even when show_repo produced a valid repo definition in stdout.
        // stdout being non-empty is the authoritative success signal for mod show_repo.
        Optional<String> res = bazel.executeModCommandToString(Arrays.asList("mod", "show_repo", repoArg));
        if (res.isPresent()) {
            out.add(res.get());
            return true;
        }
        logger.debug("mod show_repo {} produced no usable output", repoArg);
        return false;
    }
}
