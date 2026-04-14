package com.blackduck.integration.detectable.detectables.bazel;

import com.blackduck.integration.detectable.detectable.ai.AiContext;
import com.blackduck.integration.detectable.detectable.ai.AiContextAdapter;
import com.blackduck.integration.detectable.detectable.ai.AiQuestion;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI context adapter for the Bazel detector.
 * Co-located with {@link BazelDetectable} in the {@code detectables/bazel} package.
 *
 * <p>Can be instantiated without any injected dependencies — only requires a source directory.
 * The AI-assist phase parses static files only; the Bazel executable is never invoked here.</p>
 *
 * <p>Extracts three signals from the project's build files:</p>
 * <ul>
 *   <li>BUILD target labels  ({@code BUILD} / {@code BUILD.bazel} files)</li>
 *   <li>Hybrid-repo flag     (both {@code WORKSPACE} and {@code MODULE.bazel} present)</li>
 *   <li>Dependency sources   ({@code WORKSPACE} parsed using the same regex as
 *       {@link BazelWorkspaceFileParser})</li>
 * </ul>
 */
public class BazelAiContextAdapter implements AiContextAdapter {

    // Bazel build-definition file names
    private static final String WORKSPACE_FILENAME    = "WORKSPACE";
    private static final String MODULE_BAZEL_FILENAME = "MODULE.bazel";
    private static final List<String> BUILD_FILE_NAMES =
        Arrays.asList("BUILD", "BUILD.bazel");

    // Rule types whose targets are surfaced to the user as candidate scan targets
    private static final List<String> BINARY_RULE_TYPES = Arrays.asList(
        "java_binary", "java_library", "cc_binary", "cc_library",
        "py_binary", "java_test"
    );

    // Compiled once: matches  name = "some_name"  inside a rule block
    private static final Pattern NAME_ATTR_PATTERN =
        Pattern.compile("name\\s*=\\s*\"([^\"]+)\"");

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // ── AiContextAdapter contract ─────────────────────────────────────────────

    /**
     * Applicable if any of the characteristic Bazel files exist in the source directory:
     * {@code WORKSPACE}, {@code MODULE.bazel}, {@code BUILD}, or {@code BUILD.bazel}.
     */
    @Override
    public boolean isApplicable(File sourceDirectory) {
        if (new File(sourceDirectory, WORKSPACE_FILENAME).exists())    return true;
        if (new File(sourceDirectory, MODULE_BAZEL_FILENAME).exists()) return true;
        for (String buildFile : BUILD_FILE_NAMES) {
            if (new File(sourceDirectory, buildFile).exists()) return true;
        }
        return false;
    }

    /**
     * Always returns {@code true} for the AI-assist phase.
     *
     * <p>Unlike the real {@link BazelDetectable}, the AI-assist adapter only parses static
     * files — it never invokes the {@code bazel} executable. Requiring {@code bazel} on PATH
     * would silently skip the Q&A flow on machines that have the project checked out but have
     * not installed Bazel yet, which is exactly the scenario where AI-assist is most useful.</p>
     */
    @Override
    public boolean isExtractable(File sourceDirectory) {
        return true;
    }

    /**
     * Parses the project's build files and extracts the three signals needed for Q&A:
     * available BUILD targets, hybrid-repo status, and dependency sources in WORKSPACE.
     */
    @Override
    public BazelAiContext extractContext(File sourceDirectory) {
        List<String> buildTargets = parseBuildTargets(sourceDirectory);
        boolean isHybrid = new File(sourceDirectory, WORKSPACE_FILENAME).exists()
                        && new File(sourceDirectory, MODULE_BAZEL_FILENAME).exists();
        List<String> workspaceSources = parseWorkspaceSources(sourceDirectory);
        return new BazelAiContext(buildTargets, isHybrid, workspaceSources);
    }

    @Override
    public String getDetectorName() {
        return "BAZEL";
    }

    /**
     * Returns the three Bazel-specific questions, with hints populated from the signals
     * found in the project's build files so the user knows what was auto-detected.
     */
    @Override
    public List<AiQuestion> getQuestions(AiContext context) {
        BazelAiContext ctx = (BazelAiContext) context;
        List<AiQuestion> questions = new ArrayList<>();

        // Q1 — text: which Bazel target to scan? (REQUIRED — detector won't run without it)
        String targetHint = ctx.buildTargets.isEmpty()
            ? "No BUILD targets found in project root. Enter the target manually (e.g. //myapp:myapp)."
            : "BUILD targets found: " + String.join(", ", ctx.buildTargets);
        questions.add(new AiQuestion(
            "What is the Bazel target to scan? Enter a target label (e.g. //myapp:myapp):",
            AiQuestion.Type.TEXT,
            targetHint
        ));

        // Q2 — text: force a Bazel mode? (important for hybrid repos)
        String modeHint = ctx.isHybridRepo
            ? "Both WORKSPACE and MODULE.bazel detected — this is a hybrid Bazel repo.\n" +
              "     Auto-detection may pick the wrong mode in hybrid projects."
            : "Single Bazel project detected (WORKSPACE-only, no MODULE.bazel).";
        questions.add(new AiQuestion(
            "Force a Bazel mode? Enter workspace or bzlmod, or press Enter to auto-detect:",
            AiQuestion.Type.TEXT,
            modeHint
        ));

        // Q3 — text: specify dependency sources to skip probing (CI speed / determinism)
        String sourcesHint = ctx.workspaceDependencySources.isEmpty()
            ? "No dependency sources detected in WORKSPACE. Detect will auto-probe."
            : "Dependency sources detected in WORKSPACE: "
              + String.join(", ", ctx.workspaceDependencySources).toLowerCase();
        questions.add(new AiQuestion(
            "Enter dependency sources to use directly (e.g. MAVEN_INSTALL,HTTP_ARCHIVE), or press Enter to auto-probe:",
            AiQuestion.Type.TEXT,
            sourcesHint
        ));

        return questions;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Scans the source directory root and all immediate subdirectories for BUILD / BUILD.bazel
     * files, returning formatted Bazel target labels such as {@code "//myapp:myapp (java_binary)"}.
     */
    private List<String> parseBuildTargets(File sourceDirectory) {
        Set<String> targets = new LinkedHashSet<>();

        // Root BUILD files → package path ""  (labels like //:name)
        targets.addAll(targetsFromDirectory(sourceDirectory, ""));

        // First-level subdirectory BUILD files → package path = subdir name  (//subdir:name)
        File[] subdirs = sourceDirectory.listFiles(File::isDirectory);
        if (subdirs != null) {
            for (File subdir : subdirs) {
                if (!subdir.getName().startsWith(".")) {
                    targets.addAll(targetsFromDirectory(subdir, subdir.getName()));
                }
            }
        }

        return new ArrayList<>(targets);
    }

    /**
     * Returns targets from the first BUILD / BUILD.bazel found in {@code dir},
     * or an empty list if no build file is present.
     * {@code BUILD} takes precedence over {@code BUILD.bazel}.
     */
    private List<String> targetsFromDirectory(File dir, String packagePath) {
        for (String buildFileName : BUILD_FILE_NAMES) {
            File buildFile = new File(dir, buildFileName);
            if (buildFile.exists()) {
                return parseTargetsFromFile(buildFile, packagePath);
            }
        }
        return new ArrayList<>();
    }

    /**
     * Parses a single BUILD file and returns target labels for each recognised rule type.
     *
     * @param buildFile   the BUILD or BUILD.bazel file to parse
     * @param packagePath the Bazel package path relative to the workspace root
     *                    (empty string for root, {@code "myapp"} for {@code myapp/BUILD})
     */
    private List<String> parseTargetsFromFile(File buildFile, String packagePath) {
        List<String> targets = new ArrayList<>();
        // Root package prefix is "//:name"; sub-package prefix is "//pkg:name"
        String prefix = packagePath.isEmpty() ? "//:" : "//" + packagePath + ":";

        try {
            List<String> lines = FileUtils.readLines(buildFile, StandardCharsets.UTF_8);
            String currentRuleType = null;

            for (String line : lines) {
                String trimmed = line.trim();

                // Detect the start of a known rule type
                for (String ruleType : BINARY_RULE_TYPES) {
                    if (trimmed.startsWith(ruleType + "(") || trimmed.equals(ruleType + "(")) {
                        currentRuleType = ruleType;
                        break;
                    }
                }

                // Extract the name attribute inside the current rule block
                if (currentRuleType != null && trimmed.startsWith("name")) {
                    Matcher m = NAME_ATTR_PATTERN.matcher(trimmed);
                    if (m.find()) {
                        targets.add(prefix + m.group(1) + " (" + currentRuleType + ")");
                        currentRuleType = null; // reset after capturing the name
                    }
                }
            }
        } catch (IOException e) {
            logger.debug("Failed to parse BUILD file {}: {}", buildFile.getAbsolutePath(), e.getMessage());
        }
        return targets;
    }

    /**
     * Parses the WORKSPACE file for known {@link DependencySource} rule invocations using
     * the same line-level regex as {@link BazelWorkspaceFileParser}: a line that starts with
     * {@code <sourceName>(} (with optional leading whitespace) signals that source is present.
     *
     * @return ordered list of {@link DependencySource} enum names (e.g. {@code "MAVEN_INSTALL"})
     */
    private List<String> parseWorkspaceSources(File sourceDirectory) {
        File workspaceFile = new File(sourceDirectory, WORKSPACE_FILENAME);
        if (!workspaceFile.exists()) return new ArrayList<>();

        List<String> sources = new ArrayList<>();
        try {
            List<String> lines = FileUtils.readLines(workspaceFile, StandardCharsets.UTF_8);
            for (String line : lines) {
                for (DependencySource source : DependencySource.values()) {
                    if (line.matches(String.format("^\\s*%s\\s*\\(.*", source.getName()))) {
                        String enumName = source.name(); // e.g. "MAVEN_INSTALL"
                        if (!sources.contains(enumName)) {
                            sources.add(enumName);
                        }
                        break;
                    }
                }
            }
        } catch (IOException e) {
            logger.debug("Failed to parse WORKSPACE file {}: {}", workspaceFile.getAbsolutePath(), e.getMessage());
        }
        return sources;
    }
}


