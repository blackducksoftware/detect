package com.blackduck.integration.detectable.detectables.gradle.ai;

import com.blackduck.integration.detectable.detectable.ai.AiContext;

import java.util.List;

public class GradleAiContext implements AiContext {

    public final boolean hasTestConfigurations;
    public final List<String> subProjects;
    public final boolean isAndroidProject;
    public final boolean hasUnresolvedConfigurations;
    public final List<String> detectedTestConfigs;
    public final List<String> detectedUnresolvedConfigs;

    public GradleAiContext(boolean hasTestConfigurations, List<String> subProjects, boolean isAndroidProject, boolean hasUnresolvedConfigurations, List<String> detectedTestConfigs, List<String> detectedUnresolvedConfigs) {
        this.hasTestConfigurations = hasTestConfigurations;
        this.subProjects = subProjects;
        this.isAndroidProject = isAndroidProject;
        this.hasUnresolvedConfigurations = hasUnresolvedConfigurations;
        this.detectedTestConfigs = detectedTestConfigs;
        this.detectedUnresolvedConfigs = detectedUnresolvedConfigs;
    }

    @Override
    public String toPromptString() {
        return "hasTestConfigurations: " + hasTestConfigurations
            + "\ndetectedTestConfigs: " + detectedTestConfigs
            + "\nisAndroidProject: " + isAndroidProject
            + "\nhasUnresolvedConfigurations: " + hasUnresolvedConfigurations
            + "\ndetectedUnresolvedConfigs: " + detectedUnresolvedConfigs
            + "\nsubProjects: " + subProjects;
    }
}
