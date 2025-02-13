package com.blackduck.integration.detect.battery.docker;


import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

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
public class NugetInspectorTests {

    private static final String PROJECT_NAME = "nuget-CPM-docker";
    public static String ARTIFACTORY_URL = System.getenv().get("SNPS_INTERNAL_ARTIFACTORY");

    @Test
    void nugetCPMStandardTest() throws IOException,IntegrationException {
        try(DetectDockerTestRunner test = new DetectDockerTestRunner("detect-nuget-inspector-CPM", "detect-dotnet-seven:1.0.1")) {
            test.withImageProvider(BuildDockerImageProvider.forDockerfilResourceNamed("CPM_standard.dockerfile"));

            String projectVersion = PROJECT_NAME + "-normal";
            BlackDuckTestConnection blackDuckTestConnection = BlackDuckTestConnection.fromEnvironment();
            BlackDuckAssertions blackduckAssertions = blackDuckTestConnection.projectVersionAssertions(PROJECT_NAME, projectVersion);
            blackduckAssertions.emptyOnBlackDuck();

            DetectCommandBuilder commandBuilder = new DetectCommandBuilder().defaults().defaultDirectories(test);
            commandBuilder.connectToBlackDuck(blackDuckTestConnection);
            commandBuilder.projectNameVersion(blackduckAssertions);
            commandBuilder.waitForResults();

            commandBuilder.property(DetectProperties.DETECT_TOOLS, "DETECTOR");
            commandBuilder.property(DetectProperties.DETECT_INCLUDED_DETECTOR_TYPES, DetectorType.NUGET.toString());
            DockerAssertions dockerAssertions = test.run(commandBuilder);

            dockerAssertions.logContains("NuGet Solution Native Inspector: SUCCESS");
            dockerAssertions.atLeastOneBdioFile();

            blackduckAssertions.hasComponents("System.Text.Encodings.Web");
            blackduckAssertions.hasComponents("MessagePack");
            blackduckAssertions.hasComponents("Microsoft.CodeAnalysis.CSharp");
            blackduckAssertions.hasComponents("CSharpIsNullAnalyzer");
        }
    }

    @Test
    void nugetMultiplePropsFileTest() throws IOException, IntegrationException {
        try(DetectDockerTestRunner test = new DetectDockerTestRunner("detect-nuget-inspector-multiple-CPM", "detect-dotnet-seven:1.0.2")) {
            test.withImageProvider(BuildDockerImageProvider.forDockerfilResourceNamed("CPM_multiple_propsfile.dockerfile"));

            String projectVersion = PROJECT_NAME + "-multiple_props_file";
            BlackDuckTestConnection blackDuckTestConnection = BlackDuckTestConnection.fromEnvironment();
            BlackDuckAssertions blackduckAssertions = blackDuckTestConnection.projectVersionAssertions(PROJECT_NAME, projectVersion);
            blackduckAssertions.emptyOnBlackDuck();

            DetectCommandBuilder commandBuilder = new DetectCommandBuilder().defaults().defaultDirectories(test);
            commandBuilder.connectToBlackDuck(blackDuckTestConnection);
            commandBuilder.projectNameVersion(blackduckAssertions);
            commandBuilder.waitForResults();

            commandBuilder.property(DetectProperties.DETECT_TOOLS, "DETECTOR");
            commandBuilder.property(DetectProperties.DETECT_INCLUDED_DETECTOR_TYPES, DetectorType.NUGET.toString());
            commandBuilder.property(DetectProperties.DETECT_DETECTOR_SEARCH_DEPTH, String.valueOf(5));
            DockerAssertions dockerAssertions = test.run(commandBuilder);

            dockerAssertions.logContains("NuGet Solution Native Inspector: SUCCESS");
            dockerAssertions.atLeastOneBdioFile();

            blackduckAssertions.hasComponents("Uno.Core.Extensions.Collections");
            blackduckAssertions.hasComponents("Xamarin.UITest");
            blackduckAssertions.hasComponents("Microsoft.NET.Test.Sdk");
            blackduckAssertions.hasComponents("Microsoft.WindowsAppSDK");
            blackduckAssertions.hasComponents("Microsoft.UI.Xaml");
        }
    }

    @Test
    void nugetExcludeDevDependencyTest() throws IOException, IntegrationException {
        try(DetectDockerTestRunner test = new DetectDockerTestRunner("detect-nuget-inspector-exclude-dependency","detect-dotnet-seven:1.0.3")) {
            test.withImageProvider(BuildDockerImageProvider.forDockerfilResourceNamed("Nuget_ExcludeDevDependency.dockerfile"));

            String projectVersion = PROJECT_NAME + "-exclude_dev_dependency";
            BlackDuckTestConnection blackDuckTestConnection = BlackDuckTestConnection.fromEnvironment();
            BlackDuckAssertions blackduckAssertions = blackDuckTestConnection.projectVersionAssertions(PROJECT_NAME, projectVersion);
            blackduckAssertions.emptyOnBlackDuck();

            DetectCommandBuilder commandBuilder = new DetectCommandBuilder().defaults().defaultDirectories(test);
            commandBuilder.connectToBlackDuck(blackDuckTestConnection);
            commandBuilder.projectNameVersion(blackduckAssertions);
            commandBuilder.waitForResults();

            commandBuilder.property(DetectProperties.DETECT_TOOLS, "DETECTOR");
            commandBuilder.property(DetectProperties.DETECT_INCLUDED_DETECTOR_TYPES, DetectorType.NUGET.toString());
            commandBuilder.property(DetectProperties.DETECT_NUGET_DEPENDENCY_TYPES_EXCLUDED,"DEV");
            DockerAssertions dockerAssertions = test.run(commandBuilder);

            dockerAssertions.logContains("NuGet Solution Native Inspector: SUCCESS");
            dockerAssertions.atLeastOneBdioFile();

            blackduckAssertions.doesNotHaveComponents("Microsoft.CodeAnalysis.NetAnalyzers");
            blackduckAssertions.doesNotHaveComponents("Microsoft.Windows.CsWin32");
            blackduckAssertions.hasComponents("CommunityToolkit.WinUI.Animations");
            blackduckAssertions.hasComponents("Microsoft.Extensions.Logging");
        }
    }

    @Test
    void nugetArtifactsPathTest() throws IOException, IntegrationException {
        try(DetectDockerTestRunner test = new DetectDockerTestRunner("detect-nuget-inspector-artifacts","detect-dotnet-seven:1.0.4")) {
            Map<String, String> artifactoryArgs = new HashMap<>();
            artifactoryArgs.put("ARTIFACTORY_URL", ARTIFACTORY_URL);

            BuildDockerImageProvider buildDockerImageProvider = BuildDockerImageProvider.forDockerfilResourceNamed("NugetArtifactsPath.dockerfile");
            buildDockerImageProvider.setBuildArgs(artifactoryArgs);
            test.withImageProvider(buildDockerImageProvider);

            String projectVersion = PROJECT_NAME + "-artifacts_path";
            BlackDuckTestConnection blackDuckTestConnection = BlackDuckTestConnection.fromEnvironment();
            BlackDuckAssertions blackduckAssertions = blackDuckTestConnection.projectVersionAssertions(PROJECT_NAME, projectVersion);
            blackduckAssertions.emptyOnBlackDuck();

            DetectCommandBuilder commandBuilder = new DetectCommandBuilder().defaults().defaultDirectories(test);
            commandBuilder.connectToBlackDuck(blackDuckTestConnection);
            commandBuilder.projectNameVersion(blackduckAssertions);
            commandBuilder.waitForResults();

            commandBuilder.property(DetectProperties.DETECT_TOOLS, "DETECTOR");
            commandBuilder.property(DetectProperties.DETECT_INCLUDED_DETECTOR_TYPES, DetectorType.NUGET.toString());
            commandBuilder.property(DetectProperties.DETECT_NUGET_ARTIFACTS_PATH,"/opt/project/src/b");

            DockerAssertions dockerAssertions = test.run(commandBuilder);

            dockerAssertions.logContains("NuGet Solution Native Inspector: SUCCESS");
            dockerAssertions.atLeastOneBdioFile();

            blackduckAssertions.hasComponents("Json.NET");
            blackduckAssertions.hasComponents("Microsoft.Extensions.Logging");
            blackduckAssertions.checkComponentVersionExists("System.Security.AccessControl","6.0.1");
        }
    }

}
