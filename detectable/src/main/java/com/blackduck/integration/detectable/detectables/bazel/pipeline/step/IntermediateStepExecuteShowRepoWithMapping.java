package com.blackduck.integration.detectable.detectables.bazel.pipeline.step;

import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Executes `bazel mod show_repo` for each input repo in best-effort mode, using dump_repo_mapping
 * to translate canonical/synthetic names into apparent names.
 *
 * Input: repo names as strings (with or without leading @/@@), one per line.
 * Output: the full mod show_repo output blocks for repos that succeed.
 */

//TODO: This needs more research and testing to ensure it works as intended. Not yet included in the flow.
public class IntermediateStepExecuteShowRepoWithMapping implements IntermediateStep {
    private static final Logger logger = LoggerFactory.getLogger(IntermediateStepExecuteShowRepoWithMapping.class);

    private final BazelCommandExecutor bazel;
    private final RepoNameMappingResolver mapping;

    // Hard-coded toggles per request
    private final boolean bestEffort = true;
    private final boolean resolveNames = true;

    public IntermediateStepExecuteShowRepoWithMapping(BazelCommandExecutor bazel, RepoNameMappingResolver mapping) {
        this.bazel = bazel;
        this.mapping = mapping;
    }

    @Override
    public List<String> process(List<String> input) throws DetectableException {
        List<String> out = new ArrayList<>();
        if (input == null || input.isEmpty()) return out;

        int successes = 0, failures = 0;
        for (String raw : input) {
            if (raw == null || raw.trim().isEmpty()) continue;
            String token = raw.trim();
            // Normalize: strip leading at-signs; detect canonical if starts with @@ in original form
            boolean sawCanonical = token.startsWith("@@");
            String bare = stripAt(token);

            // Derive apparent and canonical candidates
            String apparentCandidate = null;
            String canonicalCandidate = null;

            if (resolveNames) {
                if (sawCanonical || looksSynthetic(bare)) {
                    apparentCandidate = mapping.toApparent(bare).orElse(null);
                    canonicalCandidate = bare; // original looked canonical/synthetic
                } else {
                    apparentCandidate = bare;
                    canonicalCandidate = mapping.toCanonical(bare).orElse(null);
                }
            } else {
                apparentCandidate = bare;
            }

            boolean success = false;
            // Try apparent first
            if (apparentCandidate != null) {
                success = tryShowRepoAddOutput("@" + apparentCandidate, out);
            }
            // Fallback to canonical (with @@) when available
            if (!success && canonicalCandidate != null) {
                success = tryShowRepoAddOutput("@@" + canonicalCandidate, out);
            }

            if (success) {
                successes++;
            } else {
                failures++;
                String mode = sawCanonical ? "canonical" : "apparent";
                String msg = String.format("mod show_repo failed for repo (%s). raw='%s' apparent='%s' canonical='%s'",
                    mode, token, apparentCandidate, canonicalCandidate);
                if (bestEffort) {
                    logger.info(msg + "; skipping.");
                } else {
                    throw new DetectableException(msg);
                }
            }
        }
        logger.info("bzlmod HTTP show_repo summary: successes={}, failures={}", successes, failures);
        return out;
    }

    private String stripAt(String token) {
        String t = token;
        while (t.startsWith("@")) t = t.substring(1);
        return t;
    }

    private boolean looksSynthetic(String name) {
        // Treat '+' (Bazel 8) and '~' (Bazel 7) as synthetic markers; any presence suggests mapping is needed
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

