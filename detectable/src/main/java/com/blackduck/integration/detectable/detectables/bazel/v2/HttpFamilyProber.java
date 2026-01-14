package com.blackduck.integration.detectable.detectables.bazel.v2;

import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Encapsulates era-aware probing for HTTP archive family (http_archive, git_repository, go_repository, http_file).
 * Era is provided centrally via BazelEnvironmentAnalyzer.
 */
public class HttpFamilyProber {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final BazelCommandExecutor bazel;
    private final BazelEnvironmentAnalyzer.Era era;

    public HttpFamilyProber(BazelCommandExecutor bazel, BazelEnvironmentAnalyzer.Era era) {
        this.bazel = bazel;
        this.era = era;
    }

    public boolean detect(String target) throws Exception {
        Optional<String> depsOut = bazel.executeToString(Arrays.asList(
            "query", "kind(.*library, deps(" + target + "))"
        ));
        if (!depsOut.isPresent()) {
            return false;
        }
        List<String> lines = Arrays.asList(depsOut.get().split("\r?\n"));
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

        int maxRepos = 30;
        int checkedRepos = 0;
        boolean bzlmodActive = (era == BazelEnvironmentAnalyzer.Era.BZLMOD);
        for (Map.Entry<String, LinkedHashSet<String>> entry : repoLabels.entrySet()) {
            if (checkedRepos++ >= maxRepos) {
                logger.info("HTTP family probe repo cap reached ({}).", maxRepos);
                break;
            }
            String repo = entry.getKey();
            boolean sawCanonical = repoSawCanonical.getOrDefault(repo, Boolean.FALSE);

            if (bzlmodActive) {
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

    private boolean tryModShowRepoEnableHttp(String repo, boolean canonical) throws Exception {
        String at = canonical ? "@@" : "@";
        Optional<String> modOut = bazel.executeToString(Arrays.asList(
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
        logger.info("Repo {} mod show_repo returned unrecognized classification; trying fallback.");
        return false;
    }

    private boolean looksLikeKnownMavenExtension(String modShowRepoOutput) {
        String s = modShowRepoOutput.toLowerCase();
        return s.contains("rules_jvm_external") || s.contains("maven_install");
    }

    private boolean looksLikeModuleExtension(String modShowRepoOutput) {
        String s = modShowRepoOutput.toLowerCase();
        return s.contains("module_extension") || s.contains("module extension") || s.contains("origin: module extension");
    }

    private boolean looksLikeDirectBazelDep(String modShowRepoOutput) {
        String s = modShowRepoOutput.toLowerCase();
        return s.contains("bazel_dep");
    }

    private boolean looksLikeSourceArchive(String modShowRepoOutput) {
        String s = modShowRepoOutput.toLowerCase();
        return s.contains("http_archive") || s.contains("git_repository") || s.contains("go_repository") || s.contains("http_file");
    }
}

