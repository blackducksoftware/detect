package com.blackduck.integration.detectable.detectables.bazel.v2;

import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import com.blackduck.integration.detectable.detectables.bazel.query.BazelQueryBuilder;
import com.blackduck.integration.detectable.detectables.bazel.query.OutputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Encapsulates mode-aware probing for HTTP archive family (http_archive, git_repository, go_repository, http_file).
 * Mode is provided centrally via BazelEnvironmentAnalyzer.
 */
public class HttpFamilyProber {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final BazelCommandExecutor bazel;
    // Bazel environment mode (e.g., BZLMOD)
    private final BazelEnvironmentAnalyzer.Mode mode;

    // Bazel rule kind patterns for queries
    private static final String LIBRARY_RULE_PATTERN = ".*library";
    private static final String RULE_PATTERN = "'rule'";

    // Classification keyword constants
    private static final List<String> MAVEN_KEYWORDS = Arrays.asList(
        "rules_jvm_external", "maven_install"
    );

    private static final List<String> SOURCE_ARCHIVE_KEYWORDS = Arrays.asList(
        "http_archive", "git_repository", "go_repository", "http_file"
    );

    private static final List<String> MODULE_EXTENSION_KEYWORDS = Arrays.asList(
        "module_extension", "module extension", "origin: module extension"
    );

    private static final List<String> BAZEL_DEP_KEYWORDS = Arrays.asList(
        "bazel_dep"
    );

    // Excluded repository prefixes
    private static final List<String> EXCLUDED_REPO_PREFIXES = Arrays.asList(
        "bazel_tools",
        "local_config_",
        "remotejdk",
        "platforms",
        "rules_python",
        "rules_java",
        "rules_cc",
        "maven",
        "unpinned_maven",
        "rules_jvm_external"
    );

    // TODO: Consider making this configurable if customers report performance issues on very large
    //       targets (500+ external repos). For now, hardcoded threshold with automatic HTTP pipeline
    //       enablement for large targets provides the best balance of performance and completeness.
    private static final int LARGE_TARGET_THRESHOLD = 150;

    // Extracted string constants for repo prefix markers
    private static final String REPO_PREFIX_SINGLE = "@";
    private static final String REPO_PREFIX_CANONICAL = "@@";
    // Repo path separator used in Bazel labels
    private static final String REPO_PATH_SEPARATOR = "//";


    /**
     * Constructor for HttpFamilyProber
     * @param bazel Bazel command executor
     * @param mode Bazel environment mode
     */
    public HttpFamilyProber(BazelCommandExecutor bazel, BazelEnvironmentAnalyzer.Mode mode) {
        this.bazel = bazel;
        this.mode = mode;
    }

    /**
     * Probes the Bazel dependency graph for HTTP archive family repositories (http_archive, git_repository, etc.).
     * Uses different probing strategies depending on the Bazel mode (BZLMOD or WORKSPACE).
     * For large projects, skips probing and enables HTTP pipeline to ensure completeness.
     * @param target Bazel target to analyze
     * @return true if HTTP family repository is detected, false otherwise
     * @throws Exception if Bazel command execution fails
     */
    public boolean detect(String target) throws Exception {
        // We use `query` here because HTTP detection is primarily a static inspection of rule declarations
        // (we are looking for external repository labels and rule kinds). `query` provides convenient outputs
        // like label_kind and XML that are easy to parse for attributes; it's also lighter-weight for these
        // workspace-style heuristics compared to cquery's action-graph outputs.
        List<String> queryArgs = BazelQueryBuilder.query()
            .kind(LIBRARY_RULE_PATTERN, BazelQueryBuilder.deps(target))
            .build();

        Optional<String> depsOut = bazel.executeToString(queryArgs);
        if (!depsOut.isPresent()) {
            return false;
        }
        // Split output into lines and process repository labels
        String[] lines = depsOut.get().split("\r?\n");
        Map<String, LinkedHashSet<String>> repoLabels = new HashMap<>();
        for (String line : lines) {
            // Look for external repository labels (start with @ or @@)
            if (line.startsWith(REPO_PREFIX_SINGLE) && line.contains(REPO_PATH_SEPARATOR)) {
                int start = line.startsWith(REPO_PREFIX_CANONICAL) ? 2 : 1; // Support canonical names starting with @@
                String repo = line.substring(start, line.indexOf(REPO_PATH_SEPARATOR));
                if (isExcludedRepo(repo)) {
                    continue;
                }
                // Track all labels for each repo
                repoLabels.computeIfAbsent(repo, r -> new LinkedHashSet<>()).add(line.trim());
            }
        }
        if (repoLabels.isEmpty()) {
            return false;
        }

        int totalRepos = repoLabels.size();

        // Strategy: Large targets (>150 repos in dependency graph) can't be probed exhaustively
        // within reasonable time, so we enable HTTP pipeline unconditionally to ensure completeness.
        // Small/medium targets are fully probed for precise detection.
        if (totalRepos > LARGE_TARGET_THRESHOLD) {
            logger.info("Target has {} external repository dependencies (>{}). " +
                       "Enabling HTTP pipeline to ensure completeness without probing overhead.",
                       totalRepos, LARGE_TARGET_THRESHOLD);
            return true;
        }

        // Small/medium target: probe all repositories for precise HTTP detection
        logger.debug("Target has {} external repository dependencies (≤{}). Probing all repositories for HTTP dependencies.",
                    totalRepos, LARGE_TARGET_THRESHOLD);

        int checkedRepos = 0;
        // Probe each repository for HTTP family characteristics
        for (Map.Entry<String, LinkedHashSet<String>> entry : repoLabels.entrySet()) {
            checkedRepos++;
            if (probeRepo(entry.getKey(), entry.getValue())) {
                logger.info("HTTP repository '{}' detected at probe #{} of {}. Enabling HTTP pipeline.",
                           entry.getKey(), checkedRepos, totalRepos);
                return true;
            }
        }

        logger.debug("Probed all {} repositories, no HTTP dependencies detected.", checkedRepos);
        return false;
    }


    /**
     * Dispatches repository probing based on the Bazel environment mode.
     * @param repo Repository name
     * @param labels Set of labels discovered for this repo
     * @return true if HTTP family repository is detected, false otherwise
     */
    private boolean probeRepo(String repo, Set<String> labels) {
        switch (mode) {
            case BZLMOD:
                // In BZLMOD-capable environments, mod show_repo is allowed but may not classify
                // repos wired via legacy WORKSPACE in hybrid setups. A controlled fallback to
                // a lightweight label_kind probe prevents false negatives without adding new
                // global environment detection.
                if (probeBzlmodRepo(repo)) {
                    return true;
                }
                return probeLegacyRepo(repo, labels);
            case WORKSPACE:
                return probeLegacyRepo(repo, labels);
            default:
                throw new IllegalStateException("Unknown Bazel mode: " + mode);
        }
    }

    /**
     * Probes a repository using BZLMOD-specific strategy (bazel mod show_repo).
     * Tries both non-canonical (@repo) and canonical (@@repo) forms for maximum compatibility.
     * @param repo Repository name
     * @return true if HTTP family repository is detected, false otherwise
     */
    private boolean probeBzlmodRepo(String repo) {
        try {
            // Try non-canonical first (most common), then canonical as fallback
            return classifyRepoByModShowRepo(repo, false)
                || classifyRepoByModShowRepo(repo, true);
        } catch (Exception e) {
            logger.debug("mod show_repo probe failed for repo {}: {}", repo, e.getMessage());
            return false;
        }
    }

    /**
     * Probes a repository using legacy WORKSPACE-compatible strategy (bazel query with label_kind).
     * @param repo Repository name
     * @param labels Set of labels discovered for this repo
     * @return true if HTTP family repository is detected, false otherwise
     */
    private boolean probeLegacyRepo(String repo, Set<String> labels) {
        // Strategy 1: Check if root package has build targets
        if (probeRepoWithLabelKind(repo, REPO_PREFIX_SINGLE + repo + REPO_PATH_SEPARATOR + ":all", "root package")) {
            return true;
        }

        // Strategy 2: Query all discovered labels to avoid false negatives from sampling
        // Since repos are capped at DEFAULT_MAX_REPOS_TO_PROBE, querying all labels per repo
        // is acceptable (typically 10-100 labels from dependency graph)
        if (!labels.isEmpty()) {
            String targets = "set(" + String.join(" ", labels) + ")";
            if (probeRepoWithLabelKind(repo, targets, "all discovered labels")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Probes a repository using label_kind query to detect build targets.
     * @param repo Repository name
     * @param queryTarget The Bazel query target expression
     * @param strategyName Description of the strategy for logging
     * @return true if build targets are found, false otherwise
     */
    private boolean probeRepoWithLabelKind(String repo, String queryTarget, String strategyName) {
        try {
            // Label_kind is a query-specific output that lists rule kinds for targets; it's convenient and
            // lightweight to use `query` + label_kind when checking whether a repository contains build targets.
            List<String> queryArgs = BazelQueryBuilder.query()
                .kind(RULE_PATTERN, queryTarget)
                .withOutput(OutputFormat.LABEL_KIND)
                .build();

            Optional<String> result = bazel.executeToString(queryArgs);
            if (result.isPresent() && !result.get().trim().isEmpty()) {
                logger.info("HTTP pipeline enabled for repo {}: {} probe found build targets", repo, strategyName);
                return true;
            }
        } catch (Exception e) {
            logger.debug("label_kind probe ({}) failed for repo {}: {}", strategyName, repo, e.getMessage());
        }
        return false;
    }


    /**
     * Returns true if the repo name is in the list of excluded (non-HTTP) repositories.
     */
    private boolean isExcludedRepo(String repo) {
        return EXCLUDED_REPO_PREFIXES.stream().anyMatch(repo::startsWith);
    }

    /**
     * Classifies a repository as HTTP-family using 'bazel mod show_repo' output analysis.
     * @param repo Repository name
     * @param canonical Whether to use canonical repo name (starts with @@)
     * @return true if HTTP family is detected, false otherwise
     * @throws Exception if Bazel command execution fails
     */
    private boolean classifyRepoByModShowRepo(String repo, boolean canonical) throws Exception {
        List<String> modArgs = BazelQueryBuilder.mod()
            .showRepo(repo, canonical)
            .build();

        Optional<String> modOut = bazel.executeToString(modArgs);
        if (!modOut.isPresent()) {
            return false;
        }
        String info = modOut.get();
        // Check for known Maven extension
        if (checkForKnownMavenExtension(info)) {
            logger.debug("Repo {} classified as Maven-related by mod show_repo; skipping HTTP.", repo);
            return false;
        }
        // Check for HTTP archive family rule class/source
        if (checkForSourceArchive(info)) {
            logger.debug("Repo {} classified as HTTP family by mod show_repo (rule class/source).", repo);
            return true;
        }
        // Check for module extension or direct Bazel dependency
        if (checkForModuleExtension(info) || checkForDirectBazelDep(info)) {
            logger.debug("Repo {} classified as extension/direct dep by mod show_repo; enabling HTTP.", repo);
            return true;
        }
        logger.debug("Repo {} mod show_repo returned unrecognized classification; trying fallback.", repo);
        return false;
    }

    /**
     * Helper method to check if text contains any of the provided keywords (case-insensitive).
     */
    private boolean containsAnyKeyword(String text, List<String> keywords) {
        String lower = text.toLowerCase();
        return keywords.stream().anyMatch(lower::contains);
    }

    /**
     * Returns true if the mod show_repo output indicates a known Maven extension.
     */
    private boolean checkForKnownMavenExtension(String modShowRepoOutput) {
        return containsAnyKeyword(modShowRepoOutput, MAVEN_KEYWORDS);
    }

    /**
     * Returns true if the mod show_repo output indicates a module extension.
     */
    private boolean checkForModuleExtension(String modShowRepoOutput) {
        return containsAnyKeyword(modShowRepoOutput, MODULE_EXTENSION_KEYWORDS);
    }

    /**
     * Returns true if the mod show_repo output indicates a direct Bazel dependency.
     */
    private boolean checkForDirectBazelDep(String modShowRepoOutput) {
        return containsAnyKeyword(modShowRepoOutput, BAZEL_DEP_KEYWORDS);
    }

    /**
     * Returns true if the mod show_repo output indicates a source archive rule (http_archive, git_repository, etc.).
     */
    private boolean checkForSourceArchive(String modShowRepoOutput) {
        return containsAnyKeyword(modShowRepoOutput, SOURCE_ARCHIVE_KEYWORDS);
    }
}
