package com.blackduck.integration.detectable.detectables.bazel.v2;

import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
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

    /**
     * Performance limits for repository probing.
     * These caps prevent excessive subprocess calls when analyzing targets with many dependencies.
     *
     * MAX_REPOS_TO_PROBE: Default limit for the number of repositories checked per target.
     * Each probe involves subprocess execution (~100ms), so 30 repos = ~3 seconds worst case.
     * Can be overridden via detect.bazel.http.probe.limit property.
     *
     * MAX_LABEL_SAMPLES_PER_REPO: In WORKSPACE mode, limits label samples tested per repository.
     * Reduces query cost when repos have hundreds of labels. Testing first few labels is typically
     * sufficient to detect HTTP-family repos since most have build targets in root package.
     */
    private static final int DEFAULT_MAX_REPOS_TO_PROBE = 30;
    private static final int MAX_LABEL_SAMPLES_PER_REPO = 3;

    private final int maxReposToProbe;

    /**
     * Constructor for HttpFamilyProber with default probe limit
     * @param bazel Bazel command executor
     * @param mode Bazel environment mode
     */
    public HttpFamilyProber(BazelCommandExecutor bazel, BazelEnvironmentAnalyzer.Mode mode) {
        this(bazel, mode, DEFAULT_MAX_REPOS_TO_PROBE);
    }

    /**
     * Constructor for HttpFamilyProber with configurable probe limit
     * @param bazel Bazel command executor
     * @param mode Bazel environment mode
     * @param maxReposToProbe Maximum number of repositories to probe
     */
    public HttpFamilyProber(BazelCommandExecutor bazel, BazelEnvironmentAnalyzer.Mode mode, int maxReposToProbe) {
        this.bazel = bazel;
        this.mode = mode;
        this.maxReposToProbe = maxReposToProbe;
    }

    /**
     * Probes the Bazel dependency graph for HTTP archive family repositories (http_archive, git_repository, etc.).
     * Uses different probing strategies depending on the Bazel mode (BZLMOD or WORKSPACE).
     * @param target Bazel target to analyze
     * @return true if HTTP family repository is detected, false otherwise
     * @throws Exception if Bazel command execution fails
     */
    public boolean detect(String target) throws Exception {
        // Query for all library dependencies of the target
        Optional<String> depsOut = bazel.executeToString(Arrays.asList(
            "query", "kind(.*library, deps(" + target + "))"
        ));
        if (!depsOut.isPresent()) {
            return false;
        }
        // Split output into lines and process repository labels
        // Use Arrays.asList for Java 8 compatibility
        String[] lines = depsOut.get().split("\r?\n");
        Map<String, LinkedHashSet<String>> repoLabels = new HashMap<>();
        for (String line : lines) {
            // Look for external repository labels (start with @ or @@)
            if (line.startsWith("@") && line.contains("//")) {
                int start = line.startsWith("@@") ? 2 : 1; // Support canonical names starting with @@
                String repo = line.substring(start, line.indexOf("//"));
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

        int checkedRepos = 0;
        // Probe each repository for HTTP family characteristics
        for (Map.Entry<String, LinkedHashSet<String>> entry : repoLabels.entrySet()) {
            if (checkedRepos++ >= maxReposToProbe) {
                logger.warn("Repository probe limit reached ({} repos checked). " +
                           "If HTTP dependencies are missed, this target may have unusually many external repos. " +
                           "Consider analyzing a more specific target or increase the limit via detect.bazel.http.probe.limit property.",
                           maxReposToProbe);
                break;
            }
            if (probeRepo(entry.getKey(), entry.getValue())) {
                return true;
            }
        }
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
                return probeBzlmodRepo(repo);
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
        if (probeRepoWithLabelKind(repo, "@" + repo + "//:all", "root package")) {
            return true;
        }

        // Strategy 2: Sample specific labels to avoid scanning whole repo
        List<String> samples = selectSampleLabels(labels, MAX_LABEL_SAMPLES_PER_REPO);
        if (!samples.isEmpty()) {
            String targets = "set(" + String.join(" ", samples) + ")";
            if (probeRepoWithLabelKind(repo, targets, "specific labels")) {
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
            Optional<String> result = bazel.executeToString(Arrays.asList(
                "query", "kind('rule', " + queryTarget + ")", "--output", "label_kind"
            ));
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
     * Selects sample labels from a set for probing.
     * @param labels Set of labels to sample from
     * @param maxSamples Maximum number of samples to select
     * @return List of sample labels
     */
    private List<String> selectSampleLabels(Set<String> labels, int maxSamples) {
        List<String> samples = new ArrayList<>();
        for (String lbl : labels) {
            samples.add(lbl);
            if (samples.size() >= maxSamples) break;
        }
        return samples;
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
        String at = canonical ? "@@" : "@";
        Optional<String> modOut = bazel.executeToString(Arrays.asList(
            "mod", "show_repo", at + repo
        ));
        if (!modOut.isPresent()) {
            return false;
        }
        String info = modOut.get();
        // Check for known Maven extension
        if (checkForKnownMavenExtension(info)) {
            logger.info("Repo {} classified as Maven-related by mod show_repo; skipping HTTP.", repo);
            return false;
        }
        // Check for HTTP archive family rule class/source
        if (checkForSourceArchive(info)) {
            logger.info("Repo {} classified as HTTP family by mod show_repo (rule class/source).", repo);
            return true;
        }
        // Check for module extension or direct Bazel dependency
        if (checkForModuleExtension(info) || checkForDirectBazelDep(info)) {
            logger.info("Repo {} classified as extension/direct dep by mod show_repo; enabling HTTP.", repo);
            return true;
        }
        logger.info("Repo {} mod show_repo returned unrecognized classification; trying fallback.", repo);
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
