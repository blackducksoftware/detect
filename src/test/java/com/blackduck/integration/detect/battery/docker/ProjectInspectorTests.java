package com.blackduck.integration.detect.battery.docker;

import java.io.IOException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detect.battery.docker.provider.BuildDockerImageProvider;
import com.blackduck.integration.detect.battery.docker.util.DetectCommandBuilder;
import com.blackduck.integration.detect.battery.docker.util.DetectDockerTestRunner;
import com.blackduck.integration.detect.battery.docker.util.DockerAssertions;
import com.blackduck.integration.detect.configuration.DetectProperties;
import com.blackduck.integration.detector.base.DetectorType;

@Tag("integration")
public class ProjectInspectorTests {
    @Test
    void dotnetProjectInspector() throws IOException {
        try (DetectDockerTestRunner test = new DetectDockerTestRunner("detect-dotnet-project-inspector", "detect-dotnet-five:1.0.1")) {
            test.withImageProvider(BuildDockerImageProvider.forDockerfilResourceNamed("Dotnet5.dockerfile"));

            DetectCommandBuilder commandBuilder = DetectCommandBuilder.withOfflineDefaults().defaultDirectories(test);
            commandBuilder.property(DetectProperties.DETECT_TOOLS, "DETECTOR");
            commandBuilder.property(DetectProperties.BLACKDUCK_OFFLINE_MODE, "true");
            commandBuilder.property(DetectProperties.DETECT_ACCURACY_REQUIRED, "NONE");
            commandBuilder.property(DetectProperties.DETECT_INCLUDED_DETECTOR_TYPES, DetectorType.NUGET.toString());
            commandBuilder.property(DetectProperties.DETECT_NUGET_PACKAGES_REPO_URL, "invalidurl"); // force nuget inspector failure
            DockerAssertions dockerAssertions = test.run(commandBuilder);

            dockerAssertions.logContains("NuGet Project Inspector: SUCCESS");
            dockerAssertions.logContains("NuGet Solution Native Inspector: ATTEMPTED");
            dockerAssertions.atLeastOneBdioFile();
        }
    }

    @Test
    void gradleProjectInspector() throws IOException, InterruptedException {
        try (DetectDockerTestRunner test = new DetectDockerTestRunner("detect-gradle-project-inspector", "gradle-simple:3.0.1")) {
            test.withImageProvider(BuildDockerImageProvider.forDockerfilResourceNamed("SimpleGradle.dockerfile"));

            DetectCommandBuilder commandBuilder = DetectCommandBuilder.withOfflineDefaults().defaultDirectories(test);
            commandBuilder.property(DetectProperties.DETECT_TOOLS, "DETECTOR");
            commandBuilder.property(DetectProperties.BLACKDUCK_OFFLINE_MODE, "true");
            commandBuilder.property(DetectProperties.DETECT_ACCURACY_REQUIRED, "NONE");
            commandBuilder.property(DetectProperties.DETECT_INCLUDED_DETECTOR_TYPES, DetectorType.GRADLE.toString());
            commandBuilder.property(DetectProperties.DETECT_GRADLE_PATH, "/tmp"); // force cli failure
            DockerAssertions dockerAssertions = test.run(commandBuilder);

            dockerAssertions.logContains("Gradle Native Inspector: ATTEMPTED");
            dockerAssertions.logContains("Gradle Project Inspector: SUCCESS");
            dockerAssertions.atLeastOneBdioFile();
        }
    }

    @Test
    void mavenProjectInspectorNotRunIfCliSucceeds() throws IOException, InterruptedException {
        try (DetectDockerTestRunner test = new DetectDockerTestRunner("detect-maven-project-inspector-legacy", "maven-simple:2.0.0")) {
            test.withImageProvider(BuildDockerImageProvider.forDockerfilResourceNamed("SimpleMaven.dockerfile"));

            DetectCommandBuilder commandBuilder = DetectCommandBuilder.withOfflineDefaults().defaultDirectories(test);
            commandBuilder.property(DetectProperties.DETECT_TOOLS, "DETECTOR");
            commandBuilder.property(DetectProperties.BLACKDUCK_OFFLINE_MODE, "true");
            commandBuilder.property(DetectProperties.DETECT_ACCURACY_REQUIRED, "NONE");
            commandBuilder.property(DetectProperties.DETECT_INCLUDED_DETECTOR_TYPES, DetectorType.MAVEN.toString());
            DockerAssertions dockerAssertions = test.run(commandBuilder);

            dockerAssertions.logDoesNotContain("Maven Project Inspector:");
            dockerAssertions.logContains("Maven CLI: SUCCESS");
            dockerAssertions.atLeastOneBdioFile();
        }
    }

    @Test
    void mavenProjectInspectorRunIfCliFails() throws IOException, InterruptedException {
        try (DetectDockerTestRunner test = new DetectDockerTestRunner("detect-maven-project-inspector", "maven-simple:2.0.0")) {
            test.withImageProvider(BuildDockerImageProvider.forDockerfilResourceNamed("SimpleMaven.dockerfile"));

            DetectCommandBuilder commandBuilder = DetectCommandBuilder.withOfflineDefaults().defaultDirectories(test);
            commandBuilder.property(DetectProperties.DETECT_TOOLS, "DETECTOR");
            commandBuilder.property(DetectProperties.BLACKDUCK_OFFLINE_MODE, "true");
            commandBuilder.property(DetectProperties.DETECT_ACCURACY_REQUIRED, "NONE");
            commandBuilder.property(DetectProperties.DETECT_INCLUDED_DETECTOR_TYPES, DetectorType.MAVEN.toString());
            commandBuilder.property(DetectProperties.DETECT_MAVEN_PATH, "/tmp"); // force cli failure
            DockerAssertions dockerAssertions = test.run(commandBuilder);

            // TODO convenience methods on dockerAssertions for testing whether a detector was ATTEMPTED, SUCCESSFUL, ...
            dockerAssertions.logContains("Maven CLI: ATTEMPTED");
            dockerAssertions.logContains("Maven Project Inspector: SUCCESS");
            dockerAssertions.atLeastOneBdioFile();
        }
    }
}
