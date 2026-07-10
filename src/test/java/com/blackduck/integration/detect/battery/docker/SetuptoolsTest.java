package com.blackduck.integration.detect.battery.docker;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
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

@Tag("integration")
public class SetuptoolsTest {
    private static final String[] SETUPTOOLS_VERSIONS_TO_TEST = new String[] { "74.0.0" };
    private static final String PIP_VERSION = "24.2";
    public static String ARTIFACTORY_URL = System.getenv().get("SNPS_INTERNAL_ARTIFACTORY");

    private static final String PROJECT_NAME = "setuptools-docker-test-project";

    private static Stream<String> provideSetuptoolsVersionsToTest() {
        return Arrays.stream(SETUPTOOLS_VERSIONS_TO_TEST);
    }

    @ParameterizedTest
    @MethodSource("provideSetuptoolsVersionsToTest")
    public void setuptoolsExecutableTest(String setuptoolsVersion) throws IntegrationException, IOException {
        try (DetectDockerTestRunner test = new DetectDockerTestRunner("setuptools-docker-test   ", "setuptools-docker-test:" + setuptoolsVersion)) {

            Map<String, String> setuptoolsDockerfileArgs = new HashMap<>();
            setuptoolsDockerfileArgs.put("ARTIFACTORY_URL", ARTIFACTORY_URL);
            setuptoolsDockerfileArgs.put("PIP_VERSION", PIP_VERSION);
            setuptoolsDockerfileArgs.put("SETUPTOOLS_VERSION", setuptoolsVersion);

            BuildDockerImageProvider buildDockerImageProvider = BuildDockerImageProvider.forDockerfilResourceNamed("Setuptools.dockerfile");
            buildDockerImageProvider.setBuildArgs(setuptoolsDockerfileArgs);
            test.withImageProvider(buildDockerImageProvider);

            // Set up blackduck connection and environment
            String projectVersion = PROJECT_NAME + "-" + setuptoolsVersion;
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
            commandBuilder.property(DetectProperties.DETECT_INCLUDED_DETECTOR_TYPES, DetectorType.SETUPTOOLS.toString());
            commandBuilder.property(DetectProperties.DETECT_PIP_PATH, "/usr/local/bin/pip");
            commandBuilder.property(DetectProperties.DETECT_PYTHON_PATH, "/usr/bin/python3");
            commandBuilder.property(DetectProperties.DETECT_ACCURACY_REQUIRED, "NONE");
            DockerAssertions dockerAssertions = test.run(commandBuilder);

            // Detect specific assertions
            dockerAssertions.successfulDetectorType(DetectorType.SETUPTOOLS.toString());
            dockerAssertions.atLeastOneBdioFile();

            // Blackduck specific assertions
            validateComponentsForSampleSetuptoolsProject(blackduckAssertions);
        }
    }

    // If updating below components, make sure to refer the test project used by the corresponding dockerfile
    private void validateComponentsForSampleSetuptoolsProject(BlackDuckAssertions blackduckAssertions) throws IntegrationException {
        blackduckAssertions.hasComponents("onnx");
        blackduckAssertions.hasComponents("yarl");
        blackduckAssertions.checkComponentVersionExists("PyTZ - Python Time Zone Library", "2024.1");
        blackduckAssertions.checkComponentVersionExists("s3fs", "2023.5.0");
        blackduckAssertions.checkComponentVersionExists("tqdm", "4.66.5");
    }
}
