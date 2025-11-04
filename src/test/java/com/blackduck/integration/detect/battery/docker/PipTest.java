package com.blackduck.integration.detect.battery.docker;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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

//@Tag("integration")
public class PipTest {

    private static final String[] PIP_VERSIONS_TO_TEST = new String[] { "24.2" };
    public static String ARTIFACTORY_URL = System.getenv().get("SNPS_INTERNAL_ARTIFACTORY");

    private static final String PROJECT_NAME = "pip-docker-test-project";

    private static Stream<String> providePipVersionsToTest() {
        return Arrays.stream(PIP_VERSIONS_TO_TEST);
    }

    @ParameterizedTest
    @MethodSource("providePipVersionsToTest")
    public void pipExecutableTest(String pipVersion) throws IntegrationException, IOException {
        try (DetectDockerTestRunner test = new DetectDockerTestRunner("pip-docker-test", "pip-docker-test:" + pipVersion)) {

            Map<String, String> pipDockerfileArgs = new HashMap<>();
            pipDockerfileArgs.put("PIP_VERSION", pipVersion);
            pipDockerfileArgs.put("ARTIFACTORY_URL", ARTIFACTORY_URL);

            BuildDockerImageProvider buildDockerImageProvider = BuildDockerImageProvider.forDockerfilResourceNamed("Pip.dockerfile");
            buildDockerImageProvider.setBuildArgs(pipDockerfileArgs);
            test.withImageProvider(buildDockerImageProvider);

            // Set up blackduck connection and environment
            String projectVersion = PROJECT_NAME + "-" + pipVersion;
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
            commandBuilder.property(DetectProperties.DETECT_PIP_PATH, "/usr/local/bin/pip");
            commandBuilder.property(DetectProperties.DETECT_PYTHON_PATH, "/usr/bin/python3");
            commandBuilder.property(DetectProperties.DETECT_ACCURACY_REQUIRED, "NONE");
            DockerAssertions dockerAssertions = test.run(commandBuilder);

            // Detect specific assertions
            dockerAssertions.successfulDetectorType(DetectorType.PIP.toString());
            dockerAssertions.atLeastOneBdioFile();

            // Blackduck specific assertions
            validateComponentsForSamplePipProject(blackduckAssertions);
        }
    }

    // If updating below components, make sure to refer the test project used by the corresponding dockerfile
    private void validateComponentsForSamplePipProject(BlackDuckAssertions blackduckAssertions) throws IntegrationException {
        blackduckAssertions.hasComponents("jinjapython");
        blackduckAssertions.hasComponents("PyYAML");
        blackduckAssertions.checkComponentVersionExists("MarkupSafe", "2.1.5");
        blackduckAssertions.checkComponentVersionExists("Packaging", "24.1");
        blackduckAssertions.checkComponentVersionExists("pycparser", "2.22");
    }

    @Test
    public void pythonHybridTest() throws IntegrationException, IOException {
        try (DetectDockerTestRunner test = new DetectDockerTestRunner("python-hybrid-test", "python-hybrid-test:1.0.0" )) {

            Map<String, String> dockerfileArgs = new HashMap<>();
            dockerfileArgs.put("ARTIFACTORY_URL", ARTIFACTORY_URL);

            BuildDockerImageProvider buildDockerImageProvider = BuildDockerImageProvider.forDockerfilResourceNamed("PythonHybrid.dockerfile");
            buildDockerImageProvider.setBuildArgs(dockerfileArgs);
            test.withImageProvider(buildDockerImageProvider);

            // Set up blackduck connection and environment
            String projectVersion = "python-hybrid-test-project";
            BlackDuckTestConnection blackDuckTestConnection = BlackDuckTestConnection.fromEnvironment();
            BlackDuckAssertions blackduckAssertions = blackDuckTestConnection.projectVersionAssertions("python-hybrid-test", projectVersion);
            blackduckAssertions.emptyOnBlackDuck();

            // Build command with BlackDuck config
            DetectCommandBuilder commandBuilder = new DetectCommandBuilder().defaults().defaultDirectories(test);
            commandBuilder.connectToBlackDuck(blackDuckTestConnection);
            commandBuilder.projectNameVersion(blackduckAssertions);
            commandBuilder.waitForResults();

            // Set up Detect properties
            commandBuilder.property(DetectProperties.DETECT_TOOLS, DetectTool.DETECTOR.toString());
            commandBuilder.property(DetectProperties.DETECT_PIP_PATH, "/usr/local/bin/pip");
            commandBuilder.property(DetectProperties.DETECT_PYTHON_PATH, "/usr/bin/python3");
            commandBuilder.property(DetectProperties.DETECT_ACCURACY_REQUIRED, "NONE");
            DockerAssertions dockerAssertions = test.run(commandBuilder);

            // Detect specific assertions
            dockerAssertions.successfulDetectorType(DetectorType.UV.toString());
            dockerAssertions.logContains("SETUPTOOLS: SUCCESS");
            dockerAssertions.successfulDetectorType(DetectorType.POETRY.toString());
            dockerAssertions.atLeastOneBdioFile();

            blackduckAssertions.hasComponents("granian");
            blackduckAssertions.hasComponents("httpx");
            blackduckAssertions.checkComponentVersionExists("atomicwrites", "1.4.1");
            blackduckAssertions.checkComponentVersionExists("TOML", "0.10.2");
        }
    }
}
