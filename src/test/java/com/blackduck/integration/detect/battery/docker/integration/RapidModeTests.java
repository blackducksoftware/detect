package com.blackduck.integration.detect.battery.docker.integration;

import java.io.IOException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detect.battery.docker.provider.BuildDockerImageProvider;
import com.blackduck.integration.detect.battery.docker.util.DetectCommandBuilder;
import com.blackduck.integration.detect.battery.docker.util.DetectDockerTestRunner;
import com.blackduck.integration.detect.battery.docker.util.DockerAssertions;
import com.blackduck.integration.detect.battery.docker.util.SharedDockerTestRunner;
import com.blackduck.integration.detect.configuration.DetectProperties;
import com.blackduck.integration.detect.configuration.enumeration.DetectTool;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.util.NameVersion;

@Tag("integration")
public class RapidModeTests {
    SharedDockerTestRunner anyProjectWithRapidResultsInBlackDuck(String testId, NameVersion projectNameVersion) throws IOException, IntegrationException {
        try (DetectDockerTestRunner runner = new DetectDockerTestRunner(testId, "gradle-simple:3.0.0")) {
            runner.withImageProvider(BuildDockerImageProvider.forDockerfilResourceNamed("SimpleGradle.dockerfile"));

            BlackDuckTestConnection blackDuckTestConnection = BlackDuckTestConnection.fromEnvironment();
            BlackDuckAssertions blackduckAssertions = blackDuckTestConnection.projectVersionAssertions(projectNameVersion);
            blackduckAssertions.emptyOnBlackDuck();

            DetectCommandBuilder commandBuilder = new DetectCommandBuilder().defaults().defaultDirectories(runner);
            commandBuilder.connectToBlackDuck(blackDuckTestConnection);
            commandBuilder.projectNameVersion(blackduckAssertions);
            commandBuilder.tools(DetectTool.DETECTOR); //All that is needed for a BOM in black duck.

            return new SharedDockerTestRunner(runner, blackDuckTestConnection, blackduckAssertions, commandBuilder);
        }
    }

    @Test
    void rapidModeSmokeTest() throws IOException, IntegrationException {
        SharedDockerTestRunner test = anyProjectWithRapidResultsInBlackDuck("rapid-mode-smoke-test", new NameVersion("rapid-mode", "smoke-test"));

        //Ensuring regardless of the source or working directory being chosen, this test still produces a risk report in the same location.
        test.command.property(DetectProperties.DETECT_BLACKDUCK_SCAN_MODE, "RAPID");

        DockerAssertions dockerAssertions = test.run();
        dockerAssertions.logContains("Critical and blocking policy violations for");
        dockerAssertions.logContains("* Components: 0");
        dockerAssertions.successfulOperation("Generate Rapid Json File");
    }

}
