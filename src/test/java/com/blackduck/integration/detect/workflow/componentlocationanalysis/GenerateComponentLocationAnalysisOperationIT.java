package com.blackduck.integration.detect.workflow.componentlocationanalysis;

import java.io.IOException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detect.battery.docker.integration.BlackDuckTestConnection;
import com.blackduck.integration.detect.battery.docker.provider.BuildDockerImageProvider;
import com.blackduck.integration.detect.battery.docker.util.DetectCommandBuilder;
import com.blackduck.integration.detect.battery.docker.util.DetectDockerTestRunner;
import com.blackduck.integration.detect.battery.docker.util.DockerAssertions;
import com.blackduck.integration.detect.configuration.DetectProperties;
import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.detect.workflow.componentlocationanalysis.GenerateComponentLocationAnalysisOperation;

@Tag("integration")
public class GenerateComponentLocationAnalysisOperationIT {
    @Test
    void testOfflinePkgMngrScan_analysisEnabled() throws IOException {
        try (DetectDockerTestRunner test = new DetectDockerTestRunner("component-location-analysis-test", "gradle-simple:3.0.0")) {
            test.withImageProvider(BuildDockerImageProvider.forDockerfilResourceNamed("SimpleGradle.dockerfile"));

            DetectCommandBuilder commandBuilder = DetectCommandBuilder.withOfflineDefaults().defaultDirectories(test);
            commandBuilder.property(DetectProperties.DETECT_COMPONENT_LOCATION_ANALYSIS_ENABLED, "true");
            commandBuilder.property(DetectProperties.BLACKDUCK_OFFLINE_MODE, "true");
            commandBuilder.property(DetectProperties.BLACKDUCK_OFFLINE_MODE_FORCE_BDIO, "true");
            commandBuilder.property(DetectProperties.DETECT_TOOLS, "DETECTOR");
            commandBuilder.property(DetectProperties.LOGGING_LEVEL_DETECT, "DEBUG");

            DockerAssertions dockerAssertions = test.run(commandBuilder);

            dockerAssertions.successfulOperation(GenerateComponentLocationAnalysisOperation.OPERATION_NAME);
            dockerAssertions.logContainsPattern("Component Location Analysis File: .*components-with-locations\\.json");
            dockerAssertions.logDoesNotContain("COMPONENT_LOCATION_ANALYSIS: SUCCESS");
            dockerAssertions.logDoesNotContain("COMPONENT_LOCATION_ANALYSIS: FAILURE");
            dockerAssertions.exitCodeIs(ExitCodeType.SUCCESS.getExitCode());
        }
    }

    @Test
    void testOnlineRapidPkgMngrScan_analysisEnabled() throws IOException {
        try (DetectDockerTestRunner test = new DetectDockerTestRunner("component-location-analysis-test", "gradle-simple:3.0.0")) {
            test.withImageProvider(BuildDockerImageProvider.forDockerfilResourceNamed("SimpleGradle.dockerfile"));

            BlackDuckTestConnection blackDuckTestConnection = BlackDuckTestConnection.fromEnvironment();

            DetectCommandBuilder commandBuilder = new DetectCommandBuilder().defaults().defaultDirectories(test);
            commandBuilder.connectToBlackDuck(blackDuckTestConnection);
            commandBuilder.property(DetectProperties.DETECT_COMPONENT_LOCATION_ANALYSIS_ENABLED, "true");
            commandBuilder.property(DetectProperties.DETECT_BLACKDUCK_SCAN_MODE, "RAPID");
            commandBuilder.property(DetectProperties.DETECT_TOOLS, "DETECTOR");
            commandBuilder.property(DetectProperties.LOGGING_LEVEL_DETECT, "DEBUG");

            DockerAssertions dockerAssertions = test.run(commandBuilder);

            // currently this operation fails because in RAPID mode with no matching policies in BD, Component Locator does not get any components to look up
            dockerAssertions.logContains(GenerateComponentLocationAnalysisOperation.OPERATION_NAME + ": FAILURE");
            dockerAssertions.logDoesNotContain("COMPONENT_LOCATION_ANALYSIS: SUCCESS");
            dockerAssertions.logDoesNotContain("COMPONENT_LOCATION_ANALYSIS: FAILURE");
            dockerAssertions.exitCodeIs(ExitCodeType.SUCCESS.getExitCode());
        }
    }

    @Test
    void testOfflinePkgMngrScan_analysisEnabled_affectsStatus() throws IOException {
        try (DetectDockerTestRunner test = new DetectDockerTestRunner("component-location-analysis-test", "gradle-simple:3.0.0")) {
            test.withImageProvider(BuildDockerImageProvider.forDockerfilResourceNamed("SimpleGradle.dockerfile"));

            DetectCommandBuilder commandBuilder = DetectCommandBuilder.withOfflineDefaults().defaultDirectories(test);
            commandBuilder.property(DetectProperties.DETECT_COMPONENT_LOCATION_ANALYSIS_ENABLED, "true");
            commandBuilder.property(DetectProperties.DETECT_COMPONENT_LOCATION_ANALYSIS_STATUS, "true");
            commandBuilder.property(DetectProperties.BLACKDUCK_OFFLINE_MODE, "true");
            commandBuilder.property(DetectProperties.BLACKDUCK_OFFLINE_MODE_FORCE_BDIO, "true");
            commandBuilder.property(DetectProperties.DETECT_TOOLS, "DETECTOR");
            commandBuilder.property(DetectProperties.LOGGING_LEVEL_DETECT, "DEBUG");

            DockerAssertions dockerAssertions = test.run(commandBuilder);

            dockerAssertions.successfulOperation(GenerateComponentLocationAnalysisOperation.OPERATION_NAME);
            dockerAssertions.logContainsPattern("Component Location Analysis File: .*components-with-locations\\.json");
            dockerAssertions.logContains("COMPONENT_LOCATION_ANALYSIS: SUCCESS");
            dockerAssertions.logDoesNotContain("COMPONENT_LOCATION_ANALYSIS: FAILURE");
            dockerAssertions.exitCodeIs(ExitCodeType.SUCCESS.getExitCode());
        }
    }

    @Test
    void testOnlineRapidPkgMngrScan_analysisEnabled_affectsStatus() throws IOException {
        try (DetectDockerTestRunner test = new DetectDockerTestRunner("component-location-analysis-test", "gradle-simple:3.0.0")) {
            test.withImageProvider(BuildDockerImageProvider.forDockerfilResourceNamed("SimpleGradle.dockerfile"));

            BlackDuckTestConnection blackDuckTestConnection = BlackDuckTestConnection.fromEnvironment();

            DetectCommandBuilder commandBuilder = new DetectCommandBuilder().defaults().defaultDirectories(test);
            commandBuilder.connectToBlackDuck(blackDuckTestConnection);
            commandBuilder.property(DetectProperties.DETECT_COMPONENT_LOCATION_ANALYSIS_ENABLED, "true");
            commandBuilder.property(DetectProperties.DETECT_COMPONENT_LOCATION_ANALYSIS_STATUS, "true");
            commandBuilder.property(DetectProperties.DETECT_BLACKDUCK_SCAN_MODE, "RAPID");
            commandBuilder.property(DetectProperties.DETECT_TOOLS, "DETECTOR");
            commandBuilder.property(DetectProperties.LOGGING_LEVEL_DETECT, "DEBUG");

            DockerAssertions dockerAssertions = test.run(commandBuilder);

            // currently this operation fails because in RAPID mode with no matching policies in BD, Component Locator does not get any components to look up
            dockerAssertions.logContains(GenerateComponentLocationAnalysisOperation.OPERATION_NAME + ": FAILURE");
            dockerAssertions.logDoesNotContain("COMPONENT_LOCATION_ANALYSIS: SUCCESS");
            dockerAssertions.logContains("COMPONENT_LOCATION_ANALYSIS: FAILURE");
            dockerAssertions.exitCodeIs(ExitCodeType.FAILURE_COMPONENT_LOCATION_ANALYSIS.getExitCode());
        }
    }
}