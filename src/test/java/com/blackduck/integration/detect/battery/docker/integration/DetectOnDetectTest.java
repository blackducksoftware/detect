package com.blackduck.integration.detect.battery.docker.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;

import com.blackduck.integration.blackduck.service.model.ProjectVersionWrapper;
import com.blackduck.integration.detect.battery.docker.provider.BuildDockerImageProvider;
import com.blackduck.integration.detect.battery.docker.util.DetectCommandBuilder;
import com.blackduck.integration.detect.battery.docker.util.DetectDockerTestRunner;
import com.blackduck.integration.detect.battery.docker.util.DockerAssertions;
import com.blackduck.integration.detect.configuration.DetectProperties;
import com.blackduck.integration.detect.configuration.enumeration.DetectTool;
import com.blackduck.integration.detect.workflow.blackduck.report.service.ReportService;
import com.blackduck.integration.exception.IntegrationException;

//@Tag("integration")
public class DetectOnDetectTest {
    @Test
    void detectOnDetect() throws IOException, IntegrationException {
        try (DetectDockerTestRunner test = new DetectDockerTestRunner("detect-on-detect", "detect-7.1.0:1.0.0")) {
            test.withImageProvider(BuildDockerImageProvider.forDockerfilResourceNamed("Detect-7.1.0.dockerfile"));

            BlackDuckTestConnection blackDuckTestConnection = BlackDuckTestConnection.fromEnvironment();
            BlackDuckAssertions blackduckAssertions = blackDuckTestConnection.projectVersionAssertions("detect-on-detect-docker", "happy-path");
            blackduckAssertions.emptyOnBlackDuck();

            DetectCommandBuilder commandBuilder = new DetectCommandBuilder().defaults().defaultDirectories(test);
            commandBuilder.connectToBlackDuck(blackDuckTestConnection);
            commandBuilder.projectNameVersion(blackduckAssertions);
            commandBuilder.waitForResults();

            DockerAssertions dockerAssertions = test.run(commandBuilder);

            dockerAssertions.bdioFiles(1); //7 code locations, 6 bdio, 1 signature scanner

            blackduckAssertions.hasCodeLocations(
                "src/detect-on-detect-docker/happy-path signature"
            );

            blackduckAssertions.hasComponents("jackson-core");
        }
    }

    private static final long HALF_MILLION_BYTES = 500_000;

    @Test
    @ExtendWith(TempDirectory.class)
    public void testDryRunScanWithSnippetMatching(@TempDirectory.TempDir Path tempOutputDirectory) throws Exception {
        try (DetectDockerTestRunner test = new DetectDockerTestRunner("detect-on-detect-dryrun", "detect-7.1.0:1.0.0")) {
            test.withImageProvider(BuildDockerImageProvider.forDockerfilResourceNamed("Detect-7.1.0.dockerfile"));

            String projectName = "detect-junit";
            String projectVersionName = "dryrun-scan";
            BlackDuckTestConnection blackDuckTestConnection = BlackDuckTestConnection.fromEnvironment();
            BlackDuckAssertions blackDuckAssertions = blackDuckTestConnection.projectVersionAssertions(projectName, projectVersionName);

            blackDuckAssertions.emptyOnBlackDuck();

            DetectCommandBuilder commandBuilder = new DetectCommandBuilder().defaults().defaultDirectories(test);
            commandBuilder.projectNameVersion(blackDuckAssertions.getProjectNameVersion());
            commandBuilder.connectToBlackDuck(blackDuckTestConnection);
            commandBuilder.property(DetectProperties.DETECT_BLACKDUCK_SIGNATURE_SCANNER_SNIPPET_MATCHING, "SNIPPET_MATCHING");
            commandBuilder.property(DetectProperties.DETECT_BLACKDUCK_SIGNATURE_SCANNER_DRY_RUN, "true");

            DockerAssertions dockerAssertions = test.run(commandBuilder);
            assertDirectoryStructureForOfflineScan(dockerAssertions.getOutputDirectory().toPath());
        }
    }

    @Test
    //Simply verify a risk report is generated at the expected location.
    public void riskReportResultProduced() throws Exception {
        try (DetectDockerTestRunner test = new DetectDockerTestRunner("detect-on-detect-riskreport-default", "detect-7.1.0:1.0.0")) {
            test.withImageProvider(BuildDockerImageProvider.forDockerfilResourceNamed("Detect-7.1.0.dockerfile"));

            BlackDuckTestConnection blackDuckTestConnection = BlackDuckTestConnection.fromEnvironment();
            BlackDuckAssertions blackDuckAssertions = blackDuckTestConnection.projectVersionAssertions("detect-junit", "risk-report-default");
            blackDuckAssertions.emptyOnBlackDuck();

            DetectCommandBuilder commandBuilder = new DetectCommandBuilder().defaults().defaultDirectories(test);
            commandBuilder.connectToBlackDuck(blackDuckTestConnection);
            commandBuilder.projectNameVersion(blackDuckAssertions.getProjectNameVersion());
            commandBuilder.property(DetectProperties.DETECT_RISK_REPORT_PDF, "true");
            commandBuilder.property(DetectProperties.DETECT_RISK_REPORT_JSON, "true");
            commandBuilder.property(DetectProperties.DETECT_TIMEOUT, "1200");
            commandBuilder.tools(DetectTool.DETECTOR);

            DockerAssertions dockerAssertions = test.run(commandBuilder);
            dockerAssertions.resultProducedAtLocation("/opt/project/src/detect_junit_risk_report_default_BlackDuck_RiskReport.pdf");
            dockerAssertions.resultProducedAtLocation("/opt/project/src/detect_junit_risk_report_default_BlackDuck_RiskReport.json");
        }
    }

    @Test
    //Tests that a new project has an empty report, run detect to fill it, tests the report is filled, in a custom location
    public void riskReportPopulatedAtCustomPath() throws Exception {
        try (DetectDockerTestRunner test = new DetectDockerTestRunner("detect-on-detect-riskreport-custom", "detect-7.1.0:1.0.0")) {
            test.withImageProvider(BuildDockerImageProvider.forDockerfilResourceNamed("Detect-7.1.0.dockerfile"));

            BlackDuckTestConnection blackDuckTestConnection = BlackDuckTestConnection.fromEnvironment();
            ReportService reportService = blackDuckTestConnection.createReportService();

            BlackDuckAssertions blackDuckAssertions = blackDuckTestConnection.projectVersionAssertions("detect-junit", "risk-report-custom");
            ProjectVersionWrapper projectVersionWrapper = blackDuckAssertions.emptyOnBlackDuck();

            String reportDirectoryImagePath = "/opt/report";
            File reportDirectory = test.directories().createResultDirectory("report");
            test.directories().withBinding(reportDirectory, reportDirectoryImagePath);

            long initialFileLengthPdf = assertEmptyRiskReportPdf(reportDirectory, projectVersionWrapper, reportService);
            long initialFileLengthJson = assertEmptyRiskReportJson(reportDirectory, projectVersionWrapper, reportService);

            DetectCommandBuilder commandBuilder = new DetectCommandBuilder().defaults().defaultDirectories(test);
            commandBuilder.connectToBlackDuck(blackDuckTestConnection);
            commandBuilder.projectNameVersion(blackDuckAssertions.getProjectNameVersion());
            commandBuilder.property(DetectProperties.DETECT_RISK_REPORT_PDF, "true");
            commandBuilder.property(DetectProperties.DETECT_RISK_REPORT_JSON, "true");
            commandBuilder.property(DetectProperties.DETECT_TIMEOUT, "1200");
            commandBuilder.property(DetectProperties.DETECT_RISK_REPORT_PDF_PATH, reportDirectoryImagePath);
            commandBuilder.property(DetectProperties.DETECT_RISK_REPORT_JSON_PATH, reportDirectoryImagePath);
            commandBuilder.tools(DetectTool.DETECTOR);

            DockerAssertions dockerAssertions = test.run(commandBuilder);
            dockerAssertions.resultProducedAtLocation("/opt/report/detect_junit_risk_report_custom_BlackDuck_RiskReport.pdf");
            dockerAssertions.resultProducedAtLocation("/opt/report/detect_junit_risk_report_custom_BlackDuck_RiskReport.json");

            List<File> pdfFiles = getPdfFiles(reportDirectory);
            assertEquals(1, pdfFiles.size());
            long postLengthPdf = pdfFiles.get(0).length();
            assertTrue(postLengthPdf > initialFileLengthPdf);

            List<File> jsonFiles = getJsonFiles(reportDirectory);
            assertEquals(1, pdfFiles.size());
            long postLengthJson = jsonFiles.get(0).length();
            assertTrue(postLengthJson > initialFileLengthJson);
        }
    }

//    @Test
    public void testRunWithAutonomousEnabled() throws Exception {
        try (DetectDockerTestRunner test = new DetectDockerTestRunner("autonomous-scan-test", "detect-9.8.0:1.0.0")) {
            test.withImageProvider(BuildDockerImageProvider.forDockerfilResourceNamed("Detect-9.8.0.dockerfile"));

            BlackDuckTestConnection blackDuckTestConnection = BlackDuckTestConnection.fromEnvironment();
            BlackDuckAssertions blackduckAssertions = blackDuckTestConnection.projectVersionAssertions("autonomous-scan-test", "autonomous-scan");
            blackduckAssertions.emptyOnBlackDuck();

            DetectCommandBuilder commandBuilder = new DetectCommandBuilder().defaults().defaultDirectories(test);
            commandBuilder.connectToBlackDuck(blackDuckTestConnection);
            commandBuilder.projectNameVersion(blackduckAssertions);
            commandBuilder.waitForResults();

            commandBuilder.property(DetectProperties.DETECT_AUTONOMOUS_SCAN_ENABLED, String.valueOf(true));
            commandBuilder.property(DetectProperties.DETECT_TOOLS_EXCLUDED,"BINARY_SCAN");
            DockerAssertions dockerAssertions = test.run(commandBuilder);

            dockerAssertions.bdioFiles(1); //7 code locations, 6 bdio, 1 signature scanner
            dockerAssertions.locateScanSettingsFile();
            blackduckAssertions.hasComponents("jackson-core");
        }
    }

    private long assertEmptyRiskReportPdf(File reportDirectory, ProjectVersionWrapper projectVersionWrapper, ReportService reportService) throws IntegrationException {
        List<File> pdfFiles = getPdfFiles(reportDirectory);
        assertEquals(0, pdfFiles.size());
        File riskReportPdf = reportService.createReportPdfFile(reportDirectory, projectVersionWrapper.getProjectView(), projectVersionWrapper.getProjectVersionView());
        pdfFiles = getPdfFiles(reportDirectory);
        assertEquals(1, pdfFiles.size());
        long initialFileLength = pdfFiles.get(0).length();
        assertTrue(initialFileLength > 0);
        FileUtils.deleteQuietly(pdfFiles.get(0));
        pdfFiles = getPdfFiles(reportDirectory);
        assertEquals(0, pdfFiles.size());

        return initialFileLength;
    }

    private long assertEmptyRiskReportJson(File reportDirectory, ProjectVersionWrapper projectVersionWrapper, ReportService reportService) throws IntegrationException, IOException {
        List<File> pdfFiles = getJsonFiles(reportDirectory);
        assertEquals(0, pdfFiles.size());
        File riskReportPdf = reportService.createReportJsonFile(reportDirectory, projectVersionWrapper.getProjectView(), projectVersionWrapper.getProjectVersionView());
        pdfFiles = getJsonFiles(reportDirectory);
        assertEquals(1, pdfFiles.size());
        long initialFileLength = pdfFiles.get(0).length();
        assertTrue(initialFileLength > 0);
        FileUtils.deleteQuietly(pdfFiles.get(0));
        pdfFiles = getJsonFiles(reportDirectory);
        assertEquals(0, pdfFiles.size());

        return initialFileLength;
    }

    private List<File> getPdfFiles(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            return Arrays.stream(files)
                .filter(file -> file.getName().endsWith(".pdf"))
                .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private List<File> getJsonFiles(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            return Arrays.stream(files)
                    .filter(file -> file.getName().endsWith(".json"))
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private void assertDirectoryStructureForOfflineScan(Path tempOutputDirectory) {
        Path runsPath = tempOutputDirectory.resolve("runs");
        assertTrue(runsPath.toFile().exists());
        assertTrue(runsPath.toFile().isDirectory());

        File[] runDirectories = runsPath.toFile().listFiles();

        assertNotNull(runDirectories);
        assertEquals(1, runDirectories.length);

        File runDirectory = runDirectories[0];
        assertTrue(runDirectory.exists());
        assertTrue(runDirectory.isDirectory());

        File scanDirectory = new File(runDirectory, "scan");
        assertTrue(scanDirectory.exists());
        assertTrue(scanDirectory.isDirectory());

        File blackDuckScanOutput = new File(scanDirectory, "BlackDuckScanOutput");
        assertTrue(blackDuckScanOutput.exists());
        assertTrue(blackDuckScanOutput.isDirectory());

        File[] outputDirectories = blackDuckScanOutput.listFiles();
        assertNotNull(outputDirectories);
        assertEquals(1, outputDirectories.length);

        File outputDirectory = outputDirectories[0];
        assertTrue(outputDirectory.exists());
        assertTrue(outputDirectory.isDirectory());

        File dataDirectory = new File(outputDirectory, "data");
        assertTrue(dataDirectory.exists());
        assertTrue(dataDirectory.isDirectory());

        File[] dataFiles = dataDirectory.listFiles();
        assertNotNull(dataFiles);
        assertEquals(1, dataFiles.length);
        assertTrue(dataFiles[0].length() > HALF_MILLION_BYTES);
    }

}
