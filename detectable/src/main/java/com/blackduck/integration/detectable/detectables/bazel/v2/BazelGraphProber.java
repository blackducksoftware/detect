package com.blackduck.integration.detectable.detectables.bazel.v2;

import com.blackduck.integration.detectable.detectables.bazel.WorkspaceRule;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
            httpFamily = detectHttpArchiveFamily();
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
        boolean found = out.isPresent() && out.get().trim().length() > 0;
        if (found) {
            logger.info("Detected maven_jar artifacts via @repo//jar:jar labels.");
        }
        return found;
    }

    private boolean detectHaskellCabal() throws Exception {
        Optional<String> out = bazel.executeToString(java.util.Arrays.asList(
            "cquery", "--noimplicit_deps", "kind(haskell_cabal_library, deps(" + target + "))", "--output", "label_kind"
        ));
        boolean found = out.isPresent() && out.get().contains("haskell_cabal_library");
        if (found) {
            logger.info("Detected haskell_cabal_library rules.");
        }
        return found;
    }

    private boolean detectHttpArchiveFamily() throws Exception {
        Optional<String> depsOut = bazel.executeToString(java.util.Arrays.asList(
            "query", "kind(.*library, deps(" + target + "))"
        ));
        if (!depsOut.isPresent()) {
            return false;
        }
        java.util.List<String> lines = java.util.Arrays.asList(depsOut.get().split("\r?\n"));
        java.util.Set<String> repos = new java.util.HashSet<>();
        for (String line : lines) {
            if (line.startsWith("@") && line.contains("//")) {
                String repo = line.substring(1, line.indexOf("//"));
                if (repo.startsWith("bazel_tools") || repo.startsWith("local_config_") || repo.startsWith("remotejdk") || repo.startsWith("platforms") || repo.startsWith("maven") || repo.startsWith("unpinned_maven")) {
                    continue;
                }
                repos.add(repo);
            }
        }
        boolean any = false;
        for (String repo : repos) {
            Optional<String> xmlOut = bazel.executeToString(java.util.Arrays.asList(
                "query", "kind(.*, //external:" + repo + ")", "--output", "xml"
            ));
            if (xmlOut.isPresent()) {
                String xml = xmlOut.get();
                if (xml.contains("class=\"http_archive\"") || xml.contains("class=\"go_repository\"") || xml.contains("class=\"git_repository\"")) {
                    any = true;
                    break;
                }
            }
        }
        if (any) {
            logger.info("Detected http_archive/go_repository/git_repository rules.");
        }
        return any;
    }
}
