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
        Map<String, Boolean> repoSawCanonical = new HashMap<>();
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
                // Track if canonical repo name was seen
                if (line.startsWith("@@")) {
                    repoSawCanonical.put(repo, Boolean.TRUE);
                } else {
                    repoSawCanonical.putIfAbsent(repo, Boolean.FALSE);
                }
            }
        }
        if (repoLabels.isEmpty()) {
            return false;
        }

        int maxRepos = 30;
        int checkedRepos = 0;
        boolean bzlmodActive = (mode == BazelEnvironmentAnalyzer.Mode.BZLMOD);
        // Probe each repository for HTTP family characteristics
        for (Map.Entry<String, LinkedHashSet<String>> entry : repoLabels.entrySet()) {
            if (checkedRepos++ >= maxRepos) {
                logger.info("HTTP family probe repo cap reached ({}).", maxRepos);
                break;
            }
            String repo = entry.getKey();
            boolean sawCanonical = repoSawCanonical.getOrDefault(repo, Boolean.FALSE);

            if (bzlmodActive) {
                try {
                    // Try mod show_repo for non-canonical and canonical repo names
                    if (tryModShowRepoEnableHttp(repo, false)) {
                        return true;
                    }
                    if (sawCanonical && tryModShowRepoEnableHttp(repo, true)) {
                        return true;
                    }
                } catch (Exception e) {
                    logger.info("mod show_repo probe failed for repo {}: {}", repo, e.getMessage());
                }
                // Under bzlmod, do not use legacy fallbacks; continue to next repo.
                continue;
            }

            // Legacy/workspace-safe fallback: check if root package has rules via label_kind
            try {
                Optional<String> rootKind = bazel.executeToString(Arrays.asList(
                    "query", "kind('rule', @" + repo + "//:all)", "--output", "label_kind"
                ));
                if (rootKind.isPresent() && !rootKind.get().trim().isEmpty()) {
                    logger.info("Repo {} root package has build targets (label_kind); enabling HTTP.", repo);
                    return true;
                }
            } catch (Exception e) {
                logger.info("label_kind root-package probe failed for repo {}: {}", repo, e.getMessage());
            }

            // Secondary fallback: try specific harvested labels to avoid scanning the whole repo
            try {
                List<String> samples = new ArrayList<>();
                int maxSamples = 3;
                for (String lbl : entry.getValue()) {
                    samples.add(lbl);
                    if (samples.size() >= maxSamples) break;
                }
                if (!samples.isEmpty()) {
                    Optional<String> lkOut = bazel.executeToString(Arrays.asList(
                        "query", "kind('rule', set(" + String.join(" ", samples) + "))", "--output", "label_kind"
                    ));
                    if (lkOut.isPresent() && !lkOut.get().trim().isEmpty()) {
                        logger.info("Repo {} specific-label probe found build targets; enabling HTTP.", repo);
                        return true;
                    }
                }
            } catch (Exception e) {
                logger.info("label_kind specific-label probe failed for repo {}: {}", repo, e.getMessage());
            }
        }
        return false;
    }

    /**
     * Returns true if the repo name is in the list of excluded (non-HTTP) repositories.
     */
    private boolean isExcludedRepo(String repo) {
        String r = repo;
        return r.startsWith("bazel_tools")
            || r.startsWith("local_config_")
            || r.startsWith("remotejdk")
            || r.startsWith("platforms")
            || r.startsWith("rules_python")
            || r.startsWith("rules_java")
            || r.startsWith("rules_cc")
            || r.startsWith("maven")
            || r.startsWith("unpinned_maven")
            || r.startsWith("rules_jvm_external");
    }

    /**
     * Uses 'bazel mod show_repo' to classify a repo as HTTP family or not.
     * @param repo Repository name
     * @param canonical Whether to use canonical repo name (starts with @@)
     * @return true if HTTP family is detected, false otherwise
     * @throws Exception if Bazel command execution fails
     */
    private boolean tryModShowRepoEnableHttp(String repo, boolean canonical) throws Exception {
        String at = canonical ? "@@" : "@";
        Optional<String> modOut = bazel.executeToString(Arrays.asList(
            "mod", "show_repo", at + repo
        ));
        if (!modOut.isPresent()) {
            return false;
        }
        String info = modOut.get();
        // Check for known Maven extension
        if (looksLikeKnownMavenExtension(info)) {
            logger.info("Repo {} classified as Maven-related by mod show_repo; skipping HTTP.", repo);
            return false;
        }
        // Check for HTTP archive family rule class/source
        if (looksLikeSourceArchive(info)) {
            logger.info("Repo {} classified as HTTP family by mod show_repo (rule class/source).", repo);
            return true;
        }
        // Check for module extension or direct Bazel dependency
        if (looksLikeModuleExtension(info) || looksLikeDirectBazelDep(info)) {
            logger.info("Repo {} classified as extension/direct dep by mod show_repo; enabling HTTP.", repo);
            return true;
        }
        logger.info("Repo {} mod show_repo returned unrecognized classification; trying fallback.", repo);
        return false;
    }

    /**
     * Returns true if the mod show_repo output indicates a known Maven extension.
     */
    private boolean looksLikeKnownMavenExtension(String modShowRepoOutput) {
        String s = modShowRepoOutput.toLowerCase();
        return s.contains("rules_jvm_external") || s.contains("maven_install");
    }

    /**
     * Returns true if the mod show_repo output indicates a module extension.
     */
    private boolean looksLikeModuleExtension(String modShowRepoOutput) {
        String s = modShowRepoOutput.toLowerCase();
        return s.contains("module_extension") || s.contains("module extension") || s.contains("origin: module extension");
    }

    /**
     * Returns true if the mod show_repo output indicates a direct Bazel dependency.
     */
    private boolean looksLikeDirectBazelDep(String modShowRepoOutput) {
        String s = modShowRepoOutput.toLowerCase();
        return s.contains("bazel_dep");
    }

    /**
     * Returns true if the mod show_repo output indicates a source archive rule (http_archive, git_repository, etc.).
     */
    private boolean looksLikeSourceArchive(String modShowRepoOutput) {
        String s = modShowRepoOutput.toLowerCase();
        return s.contains("http_archive") || s.contains("git_repository") || s.contains("go_repository") || s.contains("http_file");
    }
}
