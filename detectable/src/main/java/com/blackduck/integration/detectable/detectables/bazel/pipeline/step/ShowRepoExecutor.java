package com.blackduck.integration.detectable.detectables.bazel.pipeline.step;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detectable.detectables.bazel.query.BazelQueryBuilder;
import com.blackduck.integration.detectable.detectables.bazel.v2.BzlmodGraphJsonParser;
import com.blackduck.integration.detectable.detectables.bazel.v2.BzlmodRepoMappingResolver;

/**
 * Shared execution helpers for {@code bazel mod show_repo}: batched calls, output-block splitting,
 * and an ordered per-candidate fallback. Callers supply their own candidate arguments.
 */
public class ShowRepoExecutor {
    private static final Logger logger = LoggerFactory.getLogger(ShowRepoExecutor.class);

    // Repo blocks in batched show_repo output start with "## @@<name>:" or "## @<name>:".
    public static final String BLOCK_HEADER_PREFIX = "## @";
    private static final String BLOCK_SPLIT_REGEX = "(?=" + BLOCK_HEADER_PREFIX + ")";

    // Canonical header prefix (double-@): "## @@name~:" or "## @@name+:"
    private static final String BLOCK_HEADER_CANONICAL = BLOCK_HEADER_PREFIX + "@"; // "## @@"
    // Apparent header prefix (single-@): "## @name:" — same as BLOCK_HEADER_PREFIX

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

    /**
     * Resolves a set of BCR module keys to their {@code show_repo} block output.
     *
     * <p>Uses a single batched call first (fast path). If the batch returns nothing at all,
     * falls back to per-module calls for every key. If the batch partially succeeds, only the
     * missing keys are retried per-module.
     *
     * <p>The batch arg for each module is determined by
     * {@link BzlmodRepoMappingResolver#canonicalRepoArg}, which picks the best single form based
     * on the repo mapping (canonical {@code @@name~}, bare name, or suffix-appended form).
     * The per-module fallback uses {@link BzlmodRepoMappingResolver#candidateRepoArgs}, which tries
     * multiple forms in priority order to handle cross-version Bazel inconsistencies.
     *
     * @param moduleKeys set of module keys in {@code name@version} form (non-null, non-empty)
     * @param resolver   resolver loaded from {@code bazel mod dump_repo_mapping ""}
     * @return map from module key to its show_repo block output; keys with no output are absent
     */
    public Map<String, String> resolveWithFallback(Set<String> moduleKeys, BzlmodRepoMappingResolver resolver) {
        if (moduleKeys == null || moduleKeys.isEmpty()) {
            return Collections.emptyMap();
        }

        // Build primary batch args: canonicalRepoArg selects the best single form per module
        List<String> batchArgs = new ArrayList<>();
        for (String moduleKey : moduleKeys) {
            batchArgs.add(resolver.canonicalRepoArg(BzlmodGraphJsonParser.extractName(moduleKey)));
        }

        Map<String, String> result = tryBatchedResolve(moduleKeys, batchArgs, resolver);

        if (result.isEmpty()) {
            logger.debug("BZLMOD BCR: batched show_repo returned no results; falling back to per-module calls");
            return runPerModuleFallback(moduleKeys, resolver);
        }

        logger.debug("BZLMOD BCR: batched show_repo resolved {} of {} module(s)", result.size(), moduleKeys.size());

        // Retry any modules the batch did not resolve
        Set<String> missing = new LinkedHashSet<>(moduleKeys);
        missing.removeAll(result.keySet());
        if (!missing.isEmpty()) {
            logger.debug("BZLMOD BCR: {} module(s) not resolved by batch; retrying per-module", missing.size());
            result.putAll(runPerModuleFallback(missing, resolver));
        }

        return result;
    }

    /**
     * Runs a batched show_repo call and maps each returned block back to its module key.
     * Returns an empty map if the batch produces no output or no blocks can be header-matched.
     */
    private Map<String, String> tryBatchedResolve(Set<String> moduleKeys, List<String> batchArgs,
                                                   BzlmodRepoMappingResolver resolver) {
        if (batchArgs.isEmpty()) {
            return Collections.emptyMap();
        }
        Optional<String> batchOutput = runBatch(batchArgs);
        if (!batchOutput.isPresent()) {
            return Collections.emptyMap();
        }

        // Parse block headers: map module name → block
        List<String> blocks = splitIntoBlocks(batchOutput.get());
        Map<String, String> blocksByModuleName = new LinkedHashMap<>();
        for (String block : blocks) {
            String headerRepoName = extractHeaderRepoName(block);
            if (headerRepoName != null) {
                String moduleName = resolver.stripCanonicalSuffix(headerRepoName);
                logger.debug("BZLMOD BCR: batch block header: headerRepoName='{}' → moduleName='{}'",
                    headerRepoName, moduleName);
                blocksByModuleName.put(moduleName, block);
            }
        }

        // Map module keys to their corresponding blocks
        Map<String, String> result = new LinkedHashMap<>();
        for (String moduleKey : moduleKeys) {
            String block = blocksByModuleName.get(BzlmodGraphJsonParser.extractName(moduleKey));
            if (block != null) {
                result.put(moduleKey, block);
            }
        }
        return result;
    }

    /**
     * Runs per-module show_repo for each key using the resolver's ordered candidate list.
     * Each candidate that fails or throws is skipped (handles Bazel 7.x NPE on some canonical forms).
     */
    private Map<String, String> runPerModuleFallback(Set<String> moduleKeys, BzlmodRepoMappingResolver resolver) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String moduleKey : moduleKeys) {
            String name = BzlmodGraphJsonParser.extractName(moduleKey);
            Optional<String> output = runFirstSuccessful(resolver.candidateRepoArgs(name));
            if (output.isPresent()) {
                result.put(moduleKey, output.get());
            } else {
                logger.debug("BZLMOD BCR: per-module show_repo produced no output for '{}'", moduleKey);
            }
        }
        return result;
    }

    /**
     * Extracts the repo name from a show_repo block header.
     * Handles both canonical form ({@code ## @@name~:}) and apparent form ({@code ## @name:}).
     * Returns {@code null} if the header cannot be parsed.
     */
    private static String extractHeaderRepoName(String block) {
        if (block.startsWith(BLOCK_HEADER_CANONICAL)) {
            int colonIdx = block.indexOf(':');
            if (colonIdx > BLOCK_HEADER_CANONICAL.length()) {
                return block.substring(BLOCK_HEADER_CANONICAL.length(), colonIdx);
            }
        } else if (block.startsWith(BLOCK_HEADER_PREFIX)) {
            int colonIdx = block.indexOf(':');
            if (colonIdx > BLOCK_HEADER_PREFIX.length()) {
                return block.substring(BLOCK_HEADER_PREFIX.length(), colonIdx);
            }
        }
        return null;
    }
}
