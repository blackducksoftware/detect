package com.blackduck.integration.detect.battery.docker;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.blackduck.integration.detect.battery.docker.integration.BlackDuckAssertions;
import com.blackduck.integration.detect.battery.docker.integration.BlackDuckTestConnection;
import com.blackduck.integration.detector.base.DetectorType;
import com.blackduck.integration.exception.IntegrationException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detect.battery.docker.provider.BuildDockerImageProvider;
import com.blackduck.integration.detect.battery.docker.util.DetectCommandBuilder;
import com.blackduck.integration.detect.battery.docker.util.DetectDockerTestRunner;
import com.blackduck.integration.detect.battery.docker.util.DockerAssertions;
import com.blackduck.integration.detect.configuration.DetectProperties;
import com.blackduck.integration.detect.configuration.enumeration.DetectTool;

@Tag("integration")
public class SbtEncodingTest {

    public static String ARTIFACTORY_URL = System.getenv().get("SNPS_INTERNAL_ARTIFACTORY");

    @Test
    void sbtEncoding() throws IOException {
        try (DetectDockerTestRunner test = new DetectDockerTestRunner("detect-sbt-encoding", "detect-sbt-encoding:1.0.3")) {
            test.withImageProvider(BuildDockerImageProvider.forDockerfilResourceNamed("SbtEncoding.dockerfile"));

            DetectCommandBuilder commandBuilder = DetectCommandBuilder.withOfflineDefaults().defaultDirectories(test);
            commandBuilder.tools(DetectTool.DETECTOR);
            commandBuilder.property(DetectProperties.DETECT_SBT_ARGUMENTS, "-Dsbt.log.noformat=true");
            DockerAssertions dockerAssertions = test.run(commandBuilder);

            dockerAssertions.atLeastOneBdioFile();
            dockerAssertions.projectVersion("sbt-simple-project_2.12", "1.0.0-SNAPSHOT");
        }
    }

    @Test
    void sbtDetectorTest() throws IOException, IntegrationException {
        try (DetectDockerTestRunner test = new DetectDockerTestRunner("detect-sbt-detector", "detect-sbt-detector:1.0.1")) {
            Map<String, String> artifactoryArgs = new HashMap<>();
            artifactoryArgs.put("ARTIFACTORY_URL", ARTIFACTORY_URL);

            BuildDockerImageProvider buildDockerImageProvider = BuildDockerImageProvider.forDockerfilResourceNamed("Sbt_Detector.dockerfile");
            buildDockerImageProvider.setBuildArgs(artifactoryArgs);
            test.withImageProvider(buildDockerImageProvider);

            String projectVersion = "sbt-detector:1.0.1";
            BlackDuckTestConnection blackDuckTestConnection = BlackDuckTestConnection.fromEnvironment();
            BlackDuckAssertions blackduckAssertions = blackDuckTestConnection.projectVersionAssertions("sbt-detector", projectVersion);
            blackduckAssertions.emptyOnBlackDuck();

            DetectCommandBuilder commandBuilder =  new DetectCommandBuilder().defaults().defaultDirectories(test);
            commandBuilder.connectToBlackDuck(blackDuckTestConnection);
            commandBuilder.projectNameVersion(blackduckAssertions);
            commandBuilder.waitForResults();

            commandBuilder.property(DetectProperties.DETECT_TOOLS, DetectTool.DETECTOR.toString());
            DockerAssertions dockerAssertions = test.run(commandBuilder);

            dockerAssertions.atLeastOneBdioFile();
            dockerAssertions.logContains("SBT: SUCCESS");

            blackduckAssertions.checkComponentVersionExists("scala-compiler", "2.12.18");
            blackduckAssertions.checkComponentVersionExists("Apache Log4J API", "2.24.3");
            blackduckAssertions.checkComponentVersionExists("scopt", "3.7.1");
        }
    }
}
