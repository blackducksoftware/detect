package com.blackduck.integration.detect.battery.docker;

import com.blackduck.integration.detect.battery.docker.integration.BlackDuckAssertions;
import com.blackduck.integration.detect.battery.docker.integration.BlackDuckTestConnection;
import com.blackduck.integration.detect.battery.docker.provider.BuildDockerImageProvider;
import com.blackduck.integration.detect.battery.docker.util.DetectCommandBuilder;
import com.blackduck.integration.detect.battery.docker.util.DetectDockerTestRunner;
import com.blackduck.integration.detect.battery.docker.util.DockerAssertions;
import com.blackduck.integration.detect.configuration.DetectProperties;
import com.blackduck.integration.detect.configuration.enumeration.DetectTool;
import com.blackduck.integration.detector.base.DetectorType;
import com.blackduck.integration.exception.IntegrationException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Tag("integration")
public class GradleNativeInspectorTests {

    public static String ARTIFACTORY_URL = System.getenv().get("SNPS_INTERNAL_ARTIFACTORY");
    public static String PROJECT_NAME = "gradle-rich-version";
    
    @Test
    void gradleInspector_7_6() throws IOException, InterruptedException {
        try (DetectDockerTestRunner test = new DetectDockerTestRunner("detect-gradle-native-inspector", "gradle-simple:1.0.0")) {
            test.withImageProvider(BuildDockerImageProvider.forDockerfilResourceNamed("SimpleGradle_7_6.dockerfile"));

            DetectCommandBuilder commandBuilder = DetectCommandBuilder.withOfflineDefaults().defaultDirectories(test);
            commandBuilder.property(DetectProperties.DETECT_TOOLS, "DETECTOR");
            commandBuilder.property(DetectProperties.BLACKDUCK_OFFLINE_MODE, "true");
            commandBuilder.property(DetectProperties.DETECT_ACCURACY_REQUIRED, "NONE");
            commandBuilder.property(DetectProperties.DETECT_INCLUDED_DETECTOR_TYPES, DetectorType.GRADLE.toString());
            DockerAssertions dockerAssertions = test.run(commandBuilder);

            dockerAssertions.logContains("Gradle Native Inspector: SUCCESS");
            dockerAssertions.logContains("GRADLE: SUCCESS");
            dockerAssertions.atLeastOneBdioFile();
        }
    }

    @Test
    void gradleInspector_9_0() throws IOException, InterruptedException {
        try (DetectDockerTestRunner test = new DetectDockerTestRunner("detect-gradle-native-inspector", "gradle-simple-9-0:1.0.0")) {
            test.withImageProvider(BuildDockerImageProvider.forDockerfilResourceNamed("SimpleGradle_9_0.dockerfile"));

            DetectCommandBuilder commandBuilder = DetectCommandBuilder.withOfflineDefaults().defaultDirectories(test);
            commandBuilder.property(DetectProperties.DETECT_TOOLS, "DETECTOR");
            commandBuilder.property(DetectProperties.BLACKDUCK_OFFLINE_MODE, "true");
            commandBuilder.property(DetectProperties.DETECT_ACCURACY_REQUIRED, "NONE");
            commandBuilder.property(DetectProperties.DETECT_DETECTOR_SEARCH_DEPTH, "1");
            commandBuilder.property(DetectProperties.DETECT_INCLUDED_DETECTOR_TYPES, DetectorType.GRADLE.toString());
            DockerAssertions dockerAssertions = test.run(commandBuilder);

            dockerAssertions.logContains("Gradle Native Inspector: SUCCESS");
            dockerAssertions.logContains("GRADLE: SUCCESS");
            dockerAssertions.atLeastOneBdioFile();
        }
    }

    @Test
    void gradleRichVersions() throws IntegrationException, IOException {
        try (DetectDockerTestRunner test = new DetectDockerTestRunner("gradle-rich-version", "gradle-rich-version:1.0.0")) {

            Map<String, String> artifactoryArgs = new HashMap<>();
            artifactoryArgs.put("artifactory_url", ARTIFACTORY_URL);

            BuildDockerImageProvider buildDockerImageProvider = BuildDockerImageProvider.forDockerfilResourceNamed("GradleRichVersions.dockerfile");
            buildDockerImageProvider.setBuildArgs(artifactoryArgs);
            test.withImageProvider(buildDockerImageProvider);

            // Set up blackduck connection and environment
            String projectVersion = PROJECT_NAME + "-1.0.0";
            BlackDuckTestConnection blackDuckTestConnection = BlackDuckTestConnection.fromEnvironment();
            BlackDuckAssertions blackduckAssertions = blackDuckTestConnection.projectVersionAssertions(PROJECT_NAME, projectVersion);
            blackduckAssertions.emptyOnBlackDuck();

            // Build command with BlackDuck config
            DetectCommandBuilder commandBuilder = new DetectCommandBuilder().defaults().defaultDirectories(test);
            commandBuilder.connectToBlackDuck(blackDuckTestConnection);
            commandBuilder.projectNameVersion(blackduckAssertions);
            commandBuilder.waitForResults();

            // Set up Detect properties
            commandBuilder.property(DetectProperties.DETECT_TOOLS, DetectTool.DETECTOR.toString());
            commandBuilder.property(DetectProperties.DETECT_INCLUDED_DETECTOR_TYPES, DetectorType.GRADLE.toString());
            DockerAssertions dockerAssertions = test.run(commandBuilder);

            // Detect specific assertions
            dockerAssertions.logContains("Gradle Native Inspector: SUCCESS");
            dockerAssertions.logContains("GRADLE: SUCCESS");
            dockerAssertions.atLeastOneBdioFile();

            blackduckAssertions.checkComponentVersionNotExists("Apache Log4j", "2.22.1");
            blackduckAssertions.checkComponentVersionExists("graphql-java", "18.2");
            blackduckAssertions.checkComponentVersionNotExists("SLF4J API Module", "2.0.4");
            blackduckAssertions.checkComponentVersionNotExists("Apache Log4J API", "2.22.1");

            String guavaVersion = "v29.0";
            try {
                blackduckAssertions.checkComponentVersionExists("googleguava", guavaVersion);
            } catch (AssertionFailedError e) {
                // the component name has flip-flopped a couple of times in HUB, so we're accepting either one for now
                blackduckAssertions.checkComponentVersionExists("google-guava", guavaVersion);
            }
        }
    }
}
