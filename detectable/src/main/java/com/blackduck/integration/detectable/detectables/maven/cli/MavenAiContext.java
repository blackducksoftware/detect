package com.blackduck.integration.detectable.detectables.maven.cli;

import com.blackduck.integration.detectable.detectable.ai.AiContext;

import java.util.List;

/**
 * Holds the minimal signals extracted from a Maven {@code pom.xml} that are
 * needed to drive AI-assisted configuration Q&A.
 *
 * <p>Signals map directly to the three demo questions:</p>
 * <ul>
 *   <li>{@link #hasTestDependencies} → Q1 (exclude test scope?)</li>
 *   <li>{@link #profiles} → Q2 (activate a profile?)</li>
 *   <li>{@link #modules} → Q3 (exclude modules?)</li>
 * </ul>
 */
public class MavenAiContext implements AiContext {

    /** True if the pom.xml contains at least one {@code <scope>test</scope>} dependency. */
    public final boolean hasTestDependencies;

    /** List of profile IDs declared in {@code <profiles>}. Empty if no profiles defined. */
    public final List<String> profiles;

    /** List of sub-module names declared in {@code <modules>}. Empty for single-module projects. */
    public final List<String> modules;

    public MavenAiContext(boolean hasTestDependencies, List<String> profiles, List<String> modules) {
        this.hasTestDependencies = hasTestDependencies;
        this.profiles = profiles;
        this.modules = modules;
    }

    @Override
    public String toPromptString() {
        return "hasTestDependencies: " + hasTestDependencies
            + "\nprofiles: " + profiles
            + "\nmodules: " + modules;
    }
}

