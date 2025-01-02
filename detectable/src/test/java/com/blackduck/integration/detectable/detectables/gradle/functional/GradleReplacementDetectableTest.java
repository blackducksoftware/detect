package com.blackduck.integration.detectable.detectables.gradle.functional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import org.jetbrains.annotations.NotNull;

import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.gradle.inspection.GradleInspectorOptions;
import com.blackduck.integration.detectable.detectables.gradle.inspection.inspector.GradleInspectorScriptOptions;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.functional.DetectableFunctionalTest;
import com.blackduck.integration.detectable.util.ExtractionUtil;
import com.blackduck.integration.detectable.util.graph.MavenGraphAssert;
import com.blackduck.integration.executable.ExecutableOutput;
import com.blackduck.integration.rest.proxy.ProxyInfo;

public class GradleReplacementDetectableTest extends DetectableFunctionalTest {

    public GradleReplacementDetectableTest() throws IOException {
        super("gradle-replacement");
    }

    @Override
    protected void setup() throws IOException {
        addFile(Paths.get("build.gradle"), "no content");

        ExecutableOutput gradleDependenciesOutput = createStandardOutput("no content");

        addExecutableOutput(
            gradleDependenciesOutput,
            new File("gradle").getCanonicalPath(),
            "gatherDependencies",
            "--init-script=gradle-inspector",
            "-DGRADLEEXTRACTIONDIR=" + getOutputDirectory().toFile().getCanonicalPath(),
            "--info"
        );

        addOutputFile(Paths.get("rootProjectMetadata.txt"), Arrays.asList(
            "DETECT META DATA START",
            "rootProjectName:root",
            "DETECT META DATA END"
        ));

        addOutputFile(Paths.get("root_dependencyGraph.txt"), Arrays.asList(
            "compile - project a has replacement root does not have'.",
            "\\--- org.replacement:replaced:1.0.0 -> 3.0.0",
            "DETECT META DATA START",
            "projectName:root",
            "DETECT META DATA END"
        ));

        addOutputFile(Paths.get("projectA_dependencyGraph.txt"), Arrays.asList(
            "compile - project a has replacement root does not have'.",
            "\\--- org.replacement:replaced:1.0.0 -> 2.0.0",
            "DETECT META DATA START",
            "projectName:projectA",
            "DETECT META DATA END"
        ));

        addOutputFile(Paths.get("projectB_dependencyGraph.txt"), Arrays.asList(
            "compile - project b uses the unreplaced version'.",
            "\\--- org.replacement:replaced:1.0.0",
            "DETECT META DATA START",
            "projectName:projectB",
            "DETECT META DATA END"
        ));
    }

    @NotNull
    @Override
    public Detectable create(@NotNull DetectableEnvironment detectableEnvironment) {
        GradleInspectorOptions gradleInspectorOptions = new GradleInspectorOptions("",
            new GradleInspectorScriptOptions(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "",
                false
            ),
            ProxyInfo.NO_PROXY_INFO, EnumListFilter.excludeNone()
        );
        return detectableFactory.createGradleDetectable(
            detectableEnvironment,
            gradleInspectorOptions,
            () -> new File("gradle-inspector"),
            (environment) -> ExecutableTarget.forFile(new File("gradle"))
        );
    }

    @Override
    public void assertExtraction(@NotNull Extraction extraction) {
        ExtractionUtil.assertSuccessWithCodeLocationCount(extraction, 3);

        CodeLocation root = ExtractionUtil.assertAndGetCodeLocationNamed("root", extraction);
        CodeLocation projectA = ExtractionUtil.assertAndGetCodeLocationNamed("projectA", extraction);
        CodeLocation projectB = ExtractionUtil.assertAndGetCodeLocationNamed("projectB", extraction);

        MavenGraphAssert rootGraph = new MavenGraphAssert(root.getDependencyGraph());
        MavenGraphAssert projectAGraph = new MavenGraphAssert(projectA.getDependencyGraph());
        MavenGraphAssert projectBGraph = new MavenGraphAssert(projectB.getDependencyGraph());
        rootGraph.hasDependency("org.replacement:replaced:3.0.0");
        projectAGraph.hasRootDependency("org.replacement:replaced:2.0.0");
        projectBGraph.hasRootDependency("org.replacement:replaced:1.0.0");
    }
}
