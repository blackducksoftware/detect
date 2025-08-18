package com.blackduck.integration.detect.battery.docker;

import java.io.IOException;

import com.blackduck.integration.detect.battery.docker.integration.BlackDuckAssertions;
import com.blackduck.integration.detect.battery.docker.integration.BlackDuckTestConnection;
import com.blackduck.integration.exception.IntegrationException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detect.battery.docker.provider.BuildDockerImageProvider;
import com.blackduck.integration.detect.battery.docker.util.DetectCommandBuilder;
import com.blackduck.integration.detect.battery.docker.util.DetectDockerTestRunner;
import com.blackduck.integration.detect.battery.docker.util.DockerAssertions;
import com.blackduck.integration.detect.configuration.DetectProperties;
import com.blackduck.integration.detector.base.DetectorType;

@Tag("integration")
public class MavenShadedDependenciesTest {

    private static final String PROJECT_NAME = "maven-shaded-dependency";

    @Test
    void mavenShadedDependencyTest() throws IOException,IntegrationException {
        try(DetectDockerTestRunner test = new DetectDockerTestRunner("detect-maven-shaded-dependency", "detect-maven:3.9.11")) {
            test.withImageProvider(BuildDockerImageProvider.forDockerfilResourceNamed("MavenShadedDependencies.dockerfile"));

            String projectVersion = PROJECT_NAME + "-PI";
            BlackDuckTestConnection blackDuckTestConnection = BlackDuckTestConnection.fromEnvironment();
            BlackDuckAssertions blackduckAssertions = blackDuckTestConnection.projectVersionAssertions(PROJECT_NAME, projectVersion);
            blackduckAssertions.emptyOnBlackDuck();

            DetectCommandBuilder commandBuilder = new DetectCommandBuilder().defaults().defaultDirectories(test);
            commandBuilder.connectToBlackDuck(blackDuckTestConnection);
            commandBuilder.projectNameVersion(blackduckAssertions);
            commandBuilder.waitForResults();

            commandBuilder.property(DetectProperties.DETECT_TOOLS, "DETECTOR");
            commandBuilder.property(DetectProperties.DETECT_INCLUDED_DETECTOR_TYPES, DetectorType.MAVEN.toString());
            commandBuilder.property(DetectProperties.DETECT_MAVEN_INCLUDE_SHADED_DEPENDENCIES, String.valueOf(true));
            DockerAssertions dockerAssertions = test.run(commandBuilder);

            dockerAssertions.logContains("Maven CLI: SUCCESS");
            dockerAssertions.atLeastOneBdioFile();

            blackduckAssertions.hasComponents("ch.randelshofer:fastdoubleparser");
            blackduckAssertions.hasComponents("JCTTools");
            blackduckAssertions.hasComponents("Byte Buddy (with dependencies)");
        }
    }

}
