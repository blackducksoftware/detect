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
    private static final Logger logger = LoggerFactory.getLogger(IntermediateStepExecuteShowRepoHeuristic.class);

    private final BazelCommandExecutor bazel;

    public IntermediateStepExecuteShowRepoHeuristic(BazelCommandExecutor bazel) {
        this.bazel = bazel;
    }

    @Override
    public List<String> process(List<String> input) throws DetectableException {
        List<String> out = new ArrayList<>();
        if (input == null || input.isEmpty()) return out;

        int successes = 0, failures = 0;
        for (String raw : input) {
            if (raw == null) continue;
            String token = raw.trim();
            if (token.isEmpty()) continue;

            boolean sawCanonical = token.startsWith("@@");
            String bare = stripAt(token);
            boolean synthetic = looksSynthetic(bare);

            boolean success = false;
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

    private String stripAt(String token) {
        String t = token;
        while (t.startsWith("@")) t = t.substring(1);
        return t;
    }

    private boolean looksSynthetic(String name) {
        // Treat '+' (Bazel 8) and '~' (Bazel 7) as synthetic markers
        return name.contains("+") || name.contains("~");
    }

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

