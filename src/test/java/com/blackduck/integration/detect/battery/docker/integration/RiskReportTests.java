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
public class RiskReportTests {
    // create any bom for a risk report
    SharedDockerTestRunner anyProjectWithRiskReportResultsInBlackDuck(String testId, NameVersion projectNameVersion) throws IOException, IntegrationException {
        try (DetectDockerTestRunner runner = new DetectDockerTestRunner(testId, "gradle-simple:1.0.0")) {
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
    void riskReportPdfSmokeTest() throws IOException, InterruptedException, IntegrationException {
        SharedDockerTestRunner test = anyProjectWithRiskReportResultsInBlackDuck("risk-report-pdf-smoke-test", new NameVersion("risk-reports", "pdf-smoke-test"));

        //Ensuring regardless of the source or working directory being chosen, this test still produces a risk report in the same location.
        test.runner.withWorkingDirectory("/opt/project/src");
        test.command.property(DetectProperties.DETECT_SOURCE_PATH, "/opt/project/src");
        test.command.property(DetectProperties.DETECT_RISK_REPORT_PDF, "true");

        DockerAssertions dockerAssertions = test.run();

        String expectedFileName = "/opt/project/src/risk_reports_pdf_smoke_test_BlackDuck_RiskReport.pdf";
        dockerAssertions.resultProducedAtLocation(expectedFileName);
        dockerAssertions.resultNotPresentAtLocation(expectedFileName.replace(".pdf", ".json"));
    }

    @Test
    void riskReportJsonSmokeTest() throws IOException, InterruptedException, IntegrationException {
        SharedDockerTestRunner test = anyProjectWithRiskReportResultsInBlackDuck("risk-report-json-smoke-test", new NameVersion("risk-reports", "json-smoke-test"));

        //Ensuring regardless of the source or working directory being chosen, this test still produces a risk report in the same location.
        test.runner.withWorkingDirectory("/opt/project/src");
        test.command.property(DetectProperties.DETECT_SOURCE_PATH, "/opt/project/src");
        test.command.property(DetectProperties.DETECT_RISK_REPORT_JSON, "true");

        DockerAssertions dockerAssertions = test.run();

        String expectedFileName = "/opt/project/src/risk_reports_json_smoke_test_BlackDuck_RiskReport.json";
        dockerAssertions.resultProducedAtLocation(expectedFileName);
        dockerAssertions.resultNotPresentAtLocation(expectedFileName.replace(".json", ".pdf"));
    }

    @Test
    void riskReportNotCreatedInWorkingDir() throws IOException, InterruptedException, IntegrationException {
        SharedDockerTestRunner test = anyProjectWithRiskReportResultsInBlackDuck(
            "risk-report-does-not-use-working-directory",
            new NameVersion("risk-reports", "default-not-working-directory")
        );

        test.runner.withWorkingDirectory("/opt/random");
        test.command.property(DetectProperties.DETECT_SOURCE_PATH, "/opt/project/src");
        test.command.property(DetectProperties.DETECT_RISK_REPORT_PDF, "true");

        DockerAssertions dockerAssertions = test.run();
        dockerAssertions.resultProducedAtLocation("/opt/project/src/risk_reports_default_not_working_directory_BlackDuck_RiskReport.pdf");
    }

    @Test
    void riskReportCreatedInCustomDirectoryEvenIfItDoesntExist() throws IOException, InterruptedException, IntegrationException {
        SharedDockerTestRunner test = anyProjectWithRiskReportResultsInBlackDuck(
            "risk-report-directory-does-not-exist",
            new NameVersion("risk-reports", "directory-does-not-exist")
        );

        test.command.property(DetectProperties.DETECT_RISK_REPORT_PDF, "true");
        test.command.property(DetectProperties.DETECT_RISK_REPORT_JSON, "true");

        String reportDir = "/opt/report/"; //simply using a directory that does not exist
        test.command.property(DetectProperties.DETECT_RISK_REPORT_PDF_PATH, reportDir);
        test.command.property(DetectProperties.DETECT_RISK_REPORT_JSON_PATH, reportDir);

        DockerAssertions dockerAssertions = test.run();

        String riskReportFileNameNoExt = reportDir + "risk_reports_directory_does_not_exist_BlackDuck_RiskReport";

        dockerAssertions.resultProducedAtLocation(riskReportFileNameNoExt + ".json");
        dockerAssertions.resultProducedAtLocation(riskReportFileNameNoExt + ".json");
    }
}
