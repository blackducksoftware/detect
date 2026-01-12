package com.blackduck.integration.detectable.detectables.bazel.v2;

import com.blackduck.integration.detectable.detectables.bazel.WorkspaceRule;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Probes the Bazel graph (deps(target)) to determine which supported pipelines should be enabled.
 * Scope is intentionally limited to existing rules: MAVEN_INSTALL, MAVEN_JAR, HASKELL_CABAL_LIBRARY, HTTP_ARCHIVE family.
 */
public class BazelGraphProber {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final BazelCommandExecutor bazel;
    private final String target;
    private final int queryTimeoutSeconds;

    public BazelGraphProber(BazelCommandExecutor bazel, String target, int queryTimeoutSeconds) {
        this.bazel = bazel;
        this.target = target;
        this.queryTimeoutSeconds = queryTimeoutSeconds;
    }

    public Set<WorkspaceRule> decidePipelines() {
        logger.info("Starting Bazel graph probing for target: {}", target);
        Set<WorkspaceRule> enabled = new HashSet<>();

        boolean mavenInstall = false;
        boolean mavenJar = false;
        boolean haskell = false;
        boolean httpFamily = false;

        // Each probe is best-effort: failures are logged and do not abort overall probing.
        try {
            mavenInstall = detectMavenInstall();
        } catch (Exception e) {
            logger.info("MAVEN_INSTALL probe failed: {}", e.getMessage());
        }
        try {
            mavenJar = detectMavenJar();
        } catch (Exception e) {
            logger.info("MAVEN_JAR probe failed: {}", e.getMessage());
        }
        try {
            haskell = detectHaskellCabal();
        } catch (Exception e) {
            logger.info("HASKELL_CABAL_LIBRARY probe failed: {}", e.getMessage());
        }
        try {
            httpFamily = detectHttpArchiveFamilyEraAware();
        } catch (Exception e) {
            logger.info("HTTP_ARCHIVE family probe failed: {}", e.getMessage());
        }

        if (mavenInstall) {
            enabled.add(WorkspaceRule.MAVEN_INSTALL);
            if (mavenJar) {
                logger.info("Both MAVEN_INSTALL and MAVEN_JAR indicated; preferring MAVEN_INSTALL and suppressing MAVEN_JAR.");
            }
        } else if (mavenJar) {
            enabled.add(WorkspaceRule.MAVEN_JAR);
        }
        if (haskell) {
            enabled.add(WorkspaceRule.HASKELL_CABAL_LIBRARY);
        }
        if (httpFamily) {
            enabled.add(WorkspaceRule.HTTP_ARCHIVE);
        }

        logger.info("Probing completed. Enabled pipelines: {}", enabled);
        return enabled;
    }

    private boolean detectMavenInstall() throws Exception {
        // Query j.*import rules under deps(target) and look for maven_coordinates tags in build output.
        Optional<String> out = bazel.executeToString(java.util.Arrays.asList(
            "cquery", "--noimplicit_deps", "kind(j.*import, deps(" + target + "))", "--output", "build"
        ));
        if (!out.isPresent()) {
            return false;
        }
        boolean found = out.get().contains("maven_coordinates=");
        if (found) {
            logger.info("Detected rules_jvm_external via j.*import with maven_coordinates.");
        }
        return found;
    }

    private boolean detectMavenJar() throws Exception {
        Optional<String> out = bazel.executeToString(java.util.Arrays.asList(
            "cquery", "filter('@.*:jar', deps(" + target + "))"
        ));
        boolean found = out.isPresent() && !out.get().trim().isEmpty();
        if (found) {
            logger.info("Detected maven_jar artifacts via @repo//jar:jar labels.");
        }
        return found;
    }

    private boolean detectHaskellCabal() throws Exception {
        Optional<String> out = bazel.executeToString(java.util.Arrays.asList(
            "cquery",
            "--noimplicit_deps",
            "--incompatible_require_linker_input_cc_api=false", //this flag is to maintain compatibility with bazel versions < 6
            "kind(haskell_cabal_library, deps(" + target + "))",
            "--output",
            "label_kind"
        ));
        boolean found = out.isPresent() && out.get().contains("haskell_cabal_library");
        if (found) {
            logger.info("Detected haskell_cabal_library rules.");
        }
        return found;
    }

    /**
     * Era-aware HTTP archive family detection.
     */
    private boolean detectHttpArchiveFamilyEraAware() throws Exception {
        // Determine if bzlmod is likely active by probing 'mod show_repo'.
        boolean bzlmodLikely = false;
        try {
            Optional<String> modProbe = bazel.executeToString(java.util.Arrays.asList("mod", "show_repo", "@bazel_tools"));
            bzlmodLikely = modProbe.isPresent();
        } catch (Exception ignored) {
            bzlmodLikely = false;
        }

        Optional<String> depsOut = bazel.executeToString(java.util.Arrays.asList(
            "query", "kind(.*library, deps(" + target + "))"
        ));
        if (!depsOut.isPresent()) {
            return false;
        }
        List<String> lines = java.util.Arrays.asList(depsOut.get().split("\r?\n"));
        Map<String, LinkedHashSet<String>> repoLabels = new HashMap<>();
        Map<String, Boolean> repoSawCanonical = new HashMap<>();
        for (String line : lines) {
            if (line.startsWith("@") && line.contains("//")) {
                int start = line.startsWith("@@") ? 2 : 1; // Support canonical names starting with @@
                String repo = line.substring(start, line.indexOf("//"));
                if (isExcludedRepo(repo)) {
                    continue;
                }
                repoLabels.computeIfAbsent(repo, r -> new LinkedHashSet<>()).add(line.trim());
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

        // Caps to keep it fast
        int maxRepos = 30;
        int checkedRepos = 0;
        for (Map.Entry<String, LinkedHashSet<String>> entry : repoLabels.entrySet()) {
            if (checkedRepos++ >= maxRepos) {
                logger.info("HTTP family probe repo cap reached ({}).", maxRepos);
                break;
            }
            String repo = entry.getKey();
            boolean sawCanonical = repoSawCanonical.getOrDefault(repo, Boolean.FALSE);

            // Bzlmod-native: try 'bazel mod show_repo' to classify provenance first
            try {
                if (tryModShowRepoEnableHttp(repo, false)) {
                    return true;
                }
                if (sawCanonical && tryModShowRepoEnableHttp(repo, true)) {
                    return true;
                }
            } catch (Exception e) {
                logger.info("mod show_repo probe failed for repo {}: {}", repo, e.getMessage());
            }

            // Under bzlmod, do not enable HTTP based on legacy fallbacks; continue to next repo.
            if (bzlmodLikely) {
                continue;
            }

            // Legacy/workspace-safe fallback: check if root package has rules via label_kind
            try {
                Optional<String> rootKind = bazel.executeToString(java.util.Arrays.asList(
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
                List<String> samples = new java.util.ArrayList<>();
                int maxSamples = 3;
                for (String lbl : entry.getValue()) {
                    samples.add(lbl);
                    if (samples.size() >= maxSamples) break;
                }
                if (!samples.isEmpty()) {
                    Optional<String> lkOut = bazel.executeToString(java.util.Arrays.asList(
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

    private boolean isExcludedRepo(String repo) {
        String r = repo; // repo name without leading @/@@
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

    private boolean tryModShowRepoEnableHttp(String repo, boolean canonical) throws Exception {
        String at = canonical ? "@@" : "@";
        Optional<String> modOut = bazel.executeToString(java.util.Arrays.asList(
            "mod", "show_repo", at + repo
        ));
        if (!modOut.isPresent()) {
            return false;
        }
        String info = modOut.get();
        if (looksLikeKnownMavenExtension(info)) {
            logger.info("Repo {} classified as Maven-related by mod show_repo; skipping HTTP.", repo);
            return false;
        }
        if (looksLikeSourceArchive(info)) {
            logger.info("Repo {} classified as HTTP family by mod show_repo (rule class/source).", repo);
            return true;
        }
        if (looksLikeModuleExtension(info) || looksLikeDirectBazelDep(info)) {
            logger.info("Repo {} classified as extension/direct dep by mod show_repo; enabling HTTP.", repo);
            return true;
        }
        logger.info("Repo {} mod show_repo returned unrecognized classification; trying fallback.", repo);
        return false;
    }

    private boolean looksLikeKnownMavenExtension(String modShowRepoOutput) {
        String s = modShowRepoOutput.toLowerCase();
        // Heuristic markers for Maven-related module extensions
        return s.contains("rules_jvm_external") || s.contains("maven_install");
    }

    private boolean looksLikeModuleExtension(String modShowRepoOutput) {
        String s = modShowRepoOutput.toLowerCase();
        // Indicates the repo is produced by a module extension
        return s.contains("module_extension") || s.contains("module extension") || s.contains("origin: module extension");
    }

    private boolean looksLikeDirectBazelDep(String modShowRepoOutput) {
        String s = modShowRepoOutput.toLowerCase();
        // Direct bazel_dep often implies source-style repos (archives/git) brought in directly
        return s.contains("bazel_dep");
    }

    private boolean looksLikeSourceArchive(String modShowRepoOutput) {
        String s = modShowRepoOutput.toLowerCase();
        // Prefer explicit rule class/source signals when available in Bazel 8
        return s.contains("http_archive") || s.contains("git_repository") || s.contains("go_repository") || s.contains("http_file");
    }
}
