package com.blackduck.integration.detect.battery.docker;

import java.io.IOException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detect.battery.docker.provider.BuildDockerImageProvider;
import com.blackduck.integration.detect.battery.docker.util.DetectCommandBuilder;
import com.blackduck.integration.detect.battery.docker.util.DetectDockerTestRunner;
import com.blackduck.integration.detect.battery.docker.util.DockerAssertions;
import com.blackduck.integration.detect.configuration.DetectProperties;
import com.blackduck.integration.detect.configuration.enumeration.DetectTool;
import com.blackduck.integration.detector.base.DetectorType;

@Tag("integration")
public class PerformanceTest {
    @Test
    void manyNonApplicableSubdirectoriesTest() throws IOException {
        // the source directory in the docker image contains thousands of non-applicable directories
        // this test ensures that there are no performance regressions around an optimization that was made for such cases
        try (DetectDockerTestRunner test = new DetectDockerTestRunner("detect-performance-smoke", "detect-performance-smoke:1.0.0")) {
            test.withImageProvider(BuildDockerImageProvider.forDockerfilResourceNamed("PerformanceTest.dockerfile"));

            DetectCommandBuilder commandBuilder = DetectCommandBuilder.withOfflineDefaults().defaultDirectories(test);
            commandBuilder.tools(DetectTool.DETECTOR);
            commandBuilder.property(DetectProperties.DETECT_DETECTOR_SEARCH_DEPTH, "64");
            commandBuilder.property(DetectProperties.DETECT_ACCURACY_REQUIRED, "NONE");

            DockerAssertions dockerAssertions = test.run(commandBuilder);

            dockerAssertions.atLeastOneBdioFile();
            dockerAssertions.successfulThingLogged(DetectorType.PIP.toString());

            // under normal circumstances this test runs in about 70-75 seconds
            // partially disabling optimizations in DetectableEnvironment caused this to go up to about 200 seconds
            // the following number was chosen based on the normal test runs with some padding added
            dockerAssertions.durationLessThan(95);
        }
    }
}