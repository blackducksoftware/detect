package com.synopsys.integration.detect.workflow.componentlocationanalysis;

import java.io.IOException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.detect.battery.docker.provider.BuildDockerImageProvider;
import com.synopsys.integration.detect.battery.docker.util.DetectCommandBuilder;
import com.synopsys.integration.detect.battery.docker.util.DetectDockerTestRunner;
import com.synopsys.integration.detect.battery.docker.util.DockerAssertions;
import com.synopsys.integration.detect.configuration.DetectProperties;

@Disabled
@Tag("integration")
public class GenerateComponentLocationAnalysisOperationIT {
    @Test
    void testOfflinePkgMngrScan_analysisEnabled() throws IOException {
        try (DetectDockerTestRunner test = new DetectDockerTestRunner("component-location-analysis-test", "gradle-simple:1.0.0")) {
            test.withImageProvider(BuildDockerImageProvider.forDockerfilResourceNamed("SimpleGradle.dockerfile"));

            DetectCommandBuilder commandBuilder = DetectCommandBuilder.withOfflineDefaults().defaultDirectories(test);
            commandBuilder.property(DetectProperties.DETECT_COMPONENT_LOCATION_ANALYSIS_ENABLED, "true");
            commandBuilder.property(DetectProperties.BLACKDUCK_OFFLINE_MODE, "true");
            commandBuilder.property(DetectProperties.BLACKDUCK_OFFLINE_MODE_FORCE_BDIO, "true");
            commandBuilder.property(DetectProperties.DETECT_INCLUDED_DETECTOR_TYPES, "DETECTOR");
            commandBuilder.property(DetectProperties.LOGGING_LEVEL_COM_SYNOPSYS_INTEGRATION, "DEBUG");

            DockerAssertions dockerAssertions = test.run(commandBuilder);

            dockerAssertions.successfulOperation(GenerateComponentLocationAnalysisOperation.OPERATION_NAME);
            dockerAssertions.logContainsPattern("Component Location Analysis File: .*components-with-locations\\.json");
            dockerAssertions.logDoesNotContain("COMPONENT_LOCATOR: SUCCESS");
        }
    }

    @Test
    void onlineRapidPkgMngrScan_analysisEnabled() throws IOException {
        try (DetectDockerTestRunner test = new DetectDockerTestRunner("component-location-analysis-test", "gradle-simple:1.0.0")) {
            test.withImageProvider(BuildDockerImageProvider.forDockerfilResourceNamed("SimpleGradle.dockerfile"));

            DetectCommandBuilder commandBuilder = DetectCommandBuilder.withOfflineDefaults().defaultDirectories(test);
            commandBuilder.property(DetectProperties.DETECT_COMPONENT_LOCATION_ANALYSIS_ENABLED, "true");
            commandBuilder.property(DetectProperties.DETECT_BLACKDUCK_SCAN_MODE, "RAPID");
            commandBuilder.property(DetectProperties.LOGGING_LEVEL_COM_SYNOPSYS_INTEGRATION, "DEBUG");

            DockerAssertions dockerAssertions = test.run(commandBuilder);

            dockerAssertions.successfulOperation(GenerateComponentLocationAnalysisOperation.OPERATION_NAME);
            dockerAssertions.logContainsPattern("Component Location Analysis File: .*components-with-locations\\.json");
            dockerAssertions.logDoesNotContain("COMPONENT_LOCATOR: SUCCESS");

        }
    }

    @Test
    void testOfflinePkgMngrScan_analysisEnabled_affectsStatus() throws IOException {
        try (DetectDockerTestRunner test = new DetectDockerTestRunner("component-location-analysis-test", "gradle-simple:1.0.0")) {
            test.withImageProvider(BuildDockerImageProvider.forDockerfilResourceNamed("SimpleGradle.dockerfile"));

            DetectCommandBuilder commandBuilder = DetectCommandBuilder.withOfflineDefaults().defaultDirectories(test);
            commandBuilder.property(DetectProperties.DETECT_COMPONENT_LOCATION_ANALYSIS_ENABLED, "true");
            commandBuilder.property(DetectProperties.DETECT_COMPONENT_LOCATION_ANALYSIS_STATUS, "true");
            commandBuilder.property(DetectProperties.BLACKDUCK_OFFLINE_MODE, "true");
            commandBuilder.property(DetectProperties.BLACKDUCK_OFFLINE_MODE_FORCE_BDIO, "true");
            commandBuilder.property(DetectProperties.DETECT_INCLUDED_DETECTOR_TYPES, "DETECTOR");
            commandBuilder.property(DetectProperties.LOGGING_LEVEL_COM_SYNOPSYS_INTEGRATION, "DEBUG");

            DockerAssertions dockerAssertions = test.run(commandBuilder);

            dockerAssertions.successfulOperation(GenerateComponentLocationAnalysisOperation.OPERATION_NAME);
            dockerAssertions.logContainsPattern("Component Location Analysis File: .*components-with-locations\\.json");
            dockerAssertions.logContains("COMPONENT_LOCATOR: SUCCESS");
        }
    }

    @Test
    void onlineRapidPkgMngrScan_analysisEnabled_affectsStatus() throws IOException {
        try (DetectDockerTestRunner test = new DetectDockerTestRunner("component-location-analysis-test", "gradle-simple:1.0.0")) {
            test.withImageProvider(BuildDockerImageProvider.forDockerfilResourceNamed("SimpleGradle.dockerfile"));

            DetectCommandBuilder commandBuilder = DetectCommandBuilder.withOfflineDefaults().defaultDirectories(test);
            commandBuilder.property(DetectProperties.DETECT_COMPONENT_LOCATION_ANALYSIS_ENABLED, "true");
            commandBuilder.property(DetectProperties.DETECT_COMPONENT_LOCATION_ANALYSIS_STATUS, "true");
            commandBuilder.property(DetectProperties.DETECT_BLACKDUCK_SCAN_MODE, "RAPID");
            commandBuilder.property(DetectProperties.LOGGING_LEVEL_COM_SYNOPSYS_INTEGRATION, "DEBUG");

            DockerAssertions dockerAssertions = test.run(commandBuilder);

            dockerAssertions.successfulOperation(GenerateComponentLocationAnalysisOperation.OPERATION_NAME);
            dockerAssertions.logContainsPattern("Component Location Analysis File: .*components-with-locations\\.json");
            dockerAssertions.logContains("COMPONENT_LOCATOR: SUCCESS");
        }
    }
}
