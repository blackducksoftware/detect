package com.blackduck.integration.detectable.detectables.nuget;

import com.blackduck.integration.detectable.detectable.ai.AiContext;

import java.util.List;

/**
 * Holds the minimal signals extracted from a NuGet solution ({@code .sln} + {@code .csproj})
 * that are needed to drive AI-assisted configuration Q&A.
 *
 * <p>Signals map directly to the two demo questions:</p>
 * <ul>
 *   <li>{@link #hasDevDependencies} → Q1 (exclude dev dependencies?)</li>
 *   <li>{@link #testProjectNames}  → Q2 (exclude test projects?)</li>
 * </ul>
 */
public class NugetAiContext implements AiContext {

    /** True if any .csproj contains a dev-only PackageReference (PrivateAssets=all). */
    public final boolean hasDevDependencies;

    /** All project names parsed from the .sln file. */
    public final List<String> projectNames;

    /** Project names that look like test projects (e.g. *.Tests, *.IntegrationTests). */
    public final List<String> testProjectNames;

    public NugetAiContext(boolean hasDevDependencies, List<String> projectNames, List<String> testProjectNames) {
        this.hasDevDependencies = hasDevDependencies;
        this.projectNames = projectNames;
        this.testProjectNames = testProjectNames;
    }

    @Override
    public String toPromptString() {
        return "hasDevDependencies: " + hasDevDependencies
            + "\nprojectNames: " + projectNames
            + "\ntestProjectNames: " + testProjectNames;
    }
}

