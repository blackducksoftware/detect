package com.blackduck.integration.detectable.detectables.bazel;

import com.blackduck.integration.detectable.detectable.ai.AiContext;

import java.util.List;

/**
 * Holds the minimal signals extracted from a Bazel project's build files that are
 * needed to drive AI-assisted configuration Q&A.
 *
 * <p>Signals map directly to the three demo questions:</p>
 * <ul>
 *   <li>{@link #buildTargets}              → Q1 (which target to scan?)</li>
 *   <li>{@link #isHybridRepo}              → Q2 (force a Bazel mode?)</li>
 *   <li>{@link #workspaceDependencySources}→ Q3 (skip dependency probing?)</li>
 * </ul>
 */
public class BazelAiContext implements AiContext {

    /**
     * Formatted Bazel target labels discovered in BUILD / BUILD.bazel files,
     * e.g. {@code "//myapp:myapp (java_binary)"}.
     * Empty if no recognisable targets were found.
     */
    public final List<String> buildTargets;

    /**
     * True if both {@code WORKSPACE} and {@code MODULE.bazel} exist in the project root,
     * indicating a hybrid repository where auto-mode detection may be unreliable.
     */
    public final boolean isHybridRepo;

    /**
     * Enum names of dependency sources found in the {@code WORKSPACE} file,
     * e.g. {@code ["MAVEN_INSTALL", "HTTP_ARCHIVE"]}.
     * Empty if WORKSPACE is absent or no known sources were found.
     */
    public final List<String> workspaceDependencySources;

    public BazelAiContext(List<String> buildTargets, boolean isHybridRepo, List<String> workspaceDependencySources) {
        this.buildTargets               = buildTargets;
        this.isHybridRepo               = isHybridRepo;
        this.workspaceDependencySources = workspaceDependencySources;
    }

    @Override
    public String toPromptString() {
        return "buildTargets: " + buildTargets
            + "\nisHybridRepo: " + isHybridRepo
            + "\nworkspaceDependencySources: " + workspaceDependencySources;
    }
}

