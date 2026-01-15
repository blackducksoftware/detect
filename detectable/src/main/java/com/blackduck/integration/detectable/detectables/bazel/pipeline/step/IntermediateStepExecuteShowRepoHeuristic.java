package com.blackduck.integration.detectable.detectables.bazel.pipeline.step;

import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Executes `bazel mod show_repo` for each input repo using a mapping-free heuristic:
 * - If input looks canonical/synthetic (starts with @@ or contains '+'/'~'): try only @@<bare>
 * - Otherwise: try @<bare> first, then @@<bare> as fallback
 *
 * Input: repo names as strings (with or without leading @/@@), one per line.
 * Output: the full mod show_repo output blocks for repos that succeed.
 */
public class IntermediateStepExecuteShowRepoHeuristic implements IntermediateStep {
    // Logger for this class
    private static final Logger logger = LoggerFactory.getLogger(IntermediateStepExecuteShowRepoHeuristic.class);

    // Bazel command executor dependency
    private final BazelCommandExecutor bazel;

    /**
     * Constructor for IntermediateStepExecuteShowRepoHeuristic
     * @param bazel Bazel command executor
     */
    public IntermediateStepExecuteShowRepoHeuristic(BazelCommandExecutor bazel) {
        this.bazel = bazel;
    }

    /**
     * Processes a list of repo names, running 'bazel mod show_repo' for each using a heuristic.
     * @param input List of repo names (with or without leading @/@@)
     * @return List of successful mod show_repo output blocks
     * @throws DetectableException if Bazel command execution fails
     */
    @Override
    public List<String> process(List<String> input) throws DetectableException {
        List<String> out = new ArrayList<>();
        if (input == null || input.isEmpty()) return out;

        int successes = 0, failures = 0;
        for (String raw : input) {
            if (raw == null) continue;
            String token = raw.trim();
            if (token.isEmpty()) continue;

            // Determine if the repo name is canonical (starts with @@) or synthetic (contains +/~)
            boolean sawCanonical = token.startsWith("@@");
            String bare = stripAt(token);
            boolean synthetic = looksSynthetic(bare);

            boolean success;
            if (sawCanonical || synthetic) {
                // Only try canonical form; '@' is invalid for synthetic names
                success = tryShowRepoAddOutput("@@" + bare, out);
            } else {
                // Try apparent then canonical fallback
                success = tryShowRepoAddOutput("@" + bare, out);
                if (!success) {
                    success = tryShowRepoAddOutput("@@" + bare, out);
                }
            }

            if (success) successes++; else failures++;
        }
        logger.info("bzlmod HTTP show_repo (heuristic) summary: successes={}, failures={}", successes, failures);
        return out;
    }

    /**
     * Removes all leading '@' characters from a repo token.
     * @param token Repo name (may start with @ or @@)
     * @return Bare repo name without leading @
     */
    private String stripAt(String token) {
        String t = token;
        while (t.startsWith("@")) t = t.substring(1);
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
        try {
            Optional<String> res = bazel.executeToString(java.util.Arrays.asList("mod", "show_repo", repoArg));
            if (res.isPresent()) {
                out.add(res.get());
                return true;
            }
        } catch (ExecutableFailedException e) {
            logger.debug("mod show_repo {} failed: {}", repoArg, e.getMessage());
        }
        return false;
    }
}
