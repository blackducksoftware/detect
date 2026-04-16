package com.blackduck.integration.detectable.detectables.gradle.ai;

import com.blackduck.integration.detectable.detectable.ai.AiContext;
import com.blackduck.integration.detectable.detectable.ai.AiContextAdapter;
import com.blackduck.integration.detectable.detectable.ai.AiQuestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GradleAiContextAdapter implements AiContextAdapter {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public boolean isApplicable(File sourceDirectory) {
        return new File(sourceDirectory, "build.gradle").exists()
                || new File(sourceDirectory, "build.gradle.kts").exists();
    }

    @Override
    public boolean isExtractable(File sourceDirectory) {
        // Prefer the Gradle wrapper
        if (new File(sourceDirectory, "gradlew").exists()
                || new File(sourceDirectory, "gradlew.bat").exists()) {
            return true;
        }

        // Fall back to checking the system PATH
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"gradle", "--version"});
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.debug("Interrupted while checking for gradle on PATH");
            return false;
        } catch (Exception e) {
            logger.debug("Gradle not found on PATH: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public GradleAiContext extractContext(File sourceDirectory) {
        List<String> subProjects = new ArrayList<>();
        boolean isAndroidProject = false;
        List<String> detectedTestConfigs = new ArrayList<>();
        List<String> detectedUnresolvedConfigs = new ArrayList<>();

        File buildFile = new File(sourceDirectory, "build.gradle");
        if (!buildFile.exists()) {
            buildFile = new File(sourceDirectory, "build.gradle.kts");
        }

        if (buildFile.exists()) {
            try {
                String content = new String(Files.readAllBytes(buildFile.toPath()));
                scanTestConfigs(content, detectedTestConfigs);
                scanUnresolvedConfigs(content, detectedUnresolvedConfigs);

                if (content.contains("com.android.application") || content.contains("com.android.library") || content.contains("android {")) {
                    isAndroidProject = true;
                }
            } catch (Exception e) {
                logger.debug("Failed to read build file for AI context extraction", e);
            }
        }

        File[] children = sourceDirectory.listFiles();
        if (children != null) {
            for (File child : children) {
                if (!child.isDirectory()) continue;
                File childBuild = new File(child, "build.gradle");
                if (!childBuild.exists()) {
                    childBuild = new File(child, "build.gradle.kts");
                }
                if (childBuild.exists()) {
                    try {
                        String content = new String(Files.readAllBytes(childBuild.toPath()));
                        scanTestConfigs(content, detectedTestConfigs);
                        scanUnresolvedConfigs(content, detectedUnresolvedConfigs);
                    } catch (Exception e) {
                        logger.debug("Failed to read submodule build file for AI context extraction: {}", child.getName(), e);
                    }
                }
            }
        }

        boolean hasTestConfigs = !detectedTestConfigs.isEmpty();
        boolean hasUnresolvedConfigurations = !detectedUnresolvedConfigs.isEmpty();

        File settingsFile = new File(sourceDirectory, "settings.gradle");
        if (!settingsFile.exists()) {
            settingsFile = new File(sourceDirectory, "settings.gradle.kts");
        }

        if (settingsFile.exists()) {
            try {
                String content = new String(Files.readAllBytes(settingsFile.toPath()));
                Matcher m = Pattern.compile("include\\s+['\"]([^'\"]+)['\"]").matcher(content);
                while (m.find()) {
                    String projectName = m.group(1).replace(":", "");
                    if (!projectName.isEmpty()) {
                        subProjects.add(projectName);
                    }
                }
            } catch (Exception e) {
                logger.debug("Failed to read settings file for AI context extraction", e);
            }
        }

        return new GradleAiContext(hasTestConfigs, subProjects, isAndroidProject, hasUnresolvedConfigurations, detectedTestConfigs, detectedUnresolvedConfigs);
    }

    private void scanTestConfigs(String content, List<String> detectedTestConfigs) {
        String[] testConfigKeywords = {"testImplementation", "testCompile", "testRuntimeOnly", "testCompileOnly"};
        for (String keyword : testConfigKeywords) {
            if (content.contains(keyword) && !detectedTestConfigs.contains(keyword)) {
                detectedTestConfigs.add(keyword);
            }
        }
    }

    private void scanUnresolvedConfigs(String content, List<String> detectedUnresolvedConfigs) {
        String[] unresolvedKeywords = {"compileOnly", "annotationProcessor", "kapt"};
        for (String keyword : unresolvedKeywords) {
            if (content.contains(keyword) && !detectedUnresolvedConfigs.contains(keyword)) {
                detectedUnresolvedConfigs.add(keyword);
            }
        }
    }

    @Override
    public String getDetectorName() {
        return "GRADLE";
    }

    @Override
    public List<AiQuestion> getQuestions(AiContext context) {
        GradleAiContext ctx = (GradleAiContext) context;
        List<AiQuestion> questions = new ArrayList<>();

        if (ctx.isAndroidProject) {
            questions.add(new AiQuestion(
                "Android build variants detected. Exclude debug/test configurations from the security scan?",
                AiQuestion.Type.YES_NO,
                "Android projects generate noisy BOMs from Debug, Release, Test variants. Excluding them produces a clean production-only scan."
            ));
        } else {
            String testHint = ctx.hasTestConfigurations
                ? "Test configurations detected in build.gradle: " + String.join(", ", ctx.detectedTestConfigs)
                : "No test configurations found in build script.";
            questions.add(new AiQuestion(
                "Exclude test configurations from the scan? (recommended for a production BOM)",
                AiQuestion.Type.YES_NO,
                testHint
            ));
        }

        String projectHint = ctx.subProjects.isEmpty()
            ? "No sub-projects detected in settings.gradle."
            : "Sub-projects detected in settings.gradle: " + String.join(", ", ctx.subProjects);
        questions.add(new AiQuestion(
            "Exclude any sub-projects from the scan? Enter project name(s) or press Enter to skip:",
            AiQuestion.Type.TEXT,
            projectHint
        ));

        String unresolvedHint = ctx.hasUnresolvedConfigurations
            ? "Unresolved configurations detected (" + String.join(", ", ctx.detectedUnresolvedConfigs) + ")."
            : "No unresolved configurations detected.";
        questions.add(new AiQuestion(
            "Unresolved configurations can produce inaccurate dependency versions. Exclude them?",
            AiQuestion.Type.YES_NO,
            unresolvedHint
        ));

        if (!ctx.subProjects.isEmpty()) {
            questions.add(new AiQuestion(
                "Would you like to scan only the root project and ignore all subprojects?",
                AiQuestion.Type.YES_NO,
                "Useful for large projects where only the root-level BOM matters."
            ));
        }

        return questions;
    }
}
