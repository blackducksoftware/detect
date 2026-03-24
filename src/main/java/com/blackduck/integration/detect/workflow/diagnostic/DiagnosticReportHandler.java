package com.blackduck.integration.detect.workflow.diagnostic;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.configuration.config.PropertyConfiguration;
import com.blackduck.integration.detect.configuration.DetectInfo;
import com.blackduck.integration.detect.tool.detector.DetectorToolResult;
import com.blackduck.integration.detect.workflow.codelocation.DetectCodeLocation;
import com.blackduck.integration.detect.workflow.event.Event;
import com.blackduck.integration.detect.workflow.event.EventSystem;
import com.blackduck.integration.detect.workflow.profiling.DetectorTimings;
import com.blackduck.integration.detect.workflow.report.CodeLocationReporter;
import com.blackduck.integration.detect.workflow.report.ConfigurationReporter;
import com.blackduck.integration.detect.workflow.report.DetailedSearchSummaryReporter;
import com.blackduck.integration.detect.workflow.report.OverviewSummaryReporter;
import com.blackduck.integration.detect.workflow.report.ProfilingReporter;
import com.blackduck.integration.detect.workflow.report.SearchSummaryReporter;
import com.blackduck.integration.detect.workflow.report.writer.FileReportWriter;
import com.blackduck.integration.detect.workflow.report.writer.InfoLogReportWriter;
import com.blackduck.integration.detect.workflow.report.writer.ReportWriter;

public class DiagnosticReportHandler {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<ReportTypes, FileReportWriter> reportWriters = new HashMap<>();

    public enum ReportTypes {
        SEARCH("search_report", "Search DetectResult Report", "A breakdown of detector searching by directory."),
        SEARCH_DETAILED("search_detailed_report", "Search DetectResult Report", "A breakdown of detector searching by directory."),
        DETECTOR("detector_report", "Detector Report", "A breakdown of detector's that were applicableChildren and their preparation and extraction results."),
        DETECTOR_PROFILE("detector_profile_report", "Detector Profile Report", "A breakdown of timing and profiling for all detectors."),
        CODE_LOCATIONS("code_location_report", "Code Location Report", "A breakdown of code locations created, their dependencies and status results."),
        DEPENDENCY_COUNTS("dependency_counts_report", "Dependency Count Report", "A breakdown of how many dependencies each detector group generated in their graphs."),
        CONFIGURATION("detect_configuration", "Detect Configuration Report", "A complete set of all parameters detect used, including detect run and version.");

        String reportFileName;
        String reportTitle;
        String reportDescription;

        ReportTypes(String reportFileName, String reportTitle, String reportDescription) {
            this.reportFileName = reportFileName;
            this.reportTitle = reportTitle;
            this.reportDescription = reportDescription;
        }

        public String getReportFileName() {
            return reportFileName;
        }

        String getReportTitle() {
            return reportTitle;
        }

        String getReportDescription() {
            return reportDescription;
        }
    }

    private final File reportDirectory;
    private final String runId;

    public DiagnosticReportHandler(File reportDirectory, String runId, EventSystem eventSystem) {
        this.reportDirectory = reportDirectory;
        this.runId = runId;
        createReports();

        eventSystem.registerListener(Event.DetectorsComplete, this::completedBomToolEvaluations);
        eventSystem.registerListener(Event.DetectCodeLocationNamesCalculated, event -> completedCodeLocations(event.getCodeLocationNames()));
        eventSystem.registerListener(Event.DetectorsProfiled, this::detectorsProfiled);
    }

    public void finish() {
        closeReportWriters();
    }

    private DetectorToolResult detectorToolResult;

    public void completedBomToolEvaluations(DetectorToolResult detectorToolResult) {
        this.detectorToolResult = detectorToolResult;

        try {
            SearchSummaryReporter searchReporter = new SearchSummaryReporter();
            searchReporter.print(getReportWriter(ReportTypes.SEARCH), detectorToolResult.getDetectorReports());
        } catch (Exception e) {
            logger.error("Failed to write search report.", e);
        }

        try {
            DetailedSearchSummaryReporter searchReporter = new DetailedSearchSummaryReporter();
            searchReporter.print(getReportWriter(ReportTypes.SEARCH_DETAILED), detectorToolResult.getDetectorReports());
        } catch (Exception e) {
            logger.error("Failed to write detailed search report.", e);
        }

        try {
            OverviewSummaryReporter overviewSummaryReporter = new OverviewSummaryReporter();
            overviewSummaryReporter.writeReport(getReportWriter(ReportTypes.DETECTOR), detectorToolResult.getDetectorReports());
        } catch (Exception e) {
            logger.error("Failed to write detector report.", e);
        }
    }

    public void completedCodeLocations(Map<DetectCodeLocation, String> codeLocationNameMap) {
        if (detectorToolResult == null) {
            return;
        }

        try {
            ReportWriter clWriter = getReportWriter(ReportTypes.CODE_LOCATIONS);
            ReportWriter dcWriter = getReportWriter(ReportTypes.DEPENDENCY_COUNTS);
            CodeLocationReporter clReporter = new CodeLocationReporter();
            clReporter.writeCodeLocationReport(
                clWriter,
                dcWriter,
                detectorToolResult.getDetectorReports(),
                detectorToolResult.getCodeLocationMap(),
                codeLocationNameMap
            );
        } catch (Exception e) {
            logger.error("Failed to write code location report.", e);
        }
    }

    private void detectorsProfiled(DetectorTimings detectorTimings) {
        try {
            ReportWriter profileWriter = getReportWriter(ReportTypes.DETECTOR_PROFILE);
            ProfilingReporter reporter = new ProfilingReporter();
            reporter.writeReport(profileWriter, detectorTimings);
        } catch (Exception e) {
            logger.error("Failed to write profiling report.", e);
        }
    }

    public void configurationsReport(
        DetectInfo detectInfo,
        PropertyConfiguration propertyConfiguration,
        SortedMap<String, String> maskedRawPropertyValues
    ) {
        try {
            ReportWriter profileWriter = getReportWriter(ReportTypes.CONFIGURATION);
            ConfigurationReporter reporter = new ConfigurationReporter();
            reporter.writeReport(profileWriter, detectInfo, propertyConfiguration, maskedRawPropertyValues);
        } catch (Exception e) {
            logger.error("Failed to write profiling report.", e);
        }
    }

    public void appendBlackDuckServerProperties(String scanTypesValue) {
        try {
            ReportWriter profileWriter = getReportWriter(ReportTypes.CONFIGURATION);
            profileWriter.writeSeparator();
            profileWriter.writeLine("Black Duck SCA Global Properties");
            profileWriter.writeSeparator();
            profileWriter.writeLine("--property = value [notes]");
            profileWriter.writeLine("------------------------------------------------------------");
            profileWriter.writeLine("detect.blackduck.correlated.scanning.enabled = " + scanTypesValue + " [SCA]");
            profileWriter.writeLine("------------------------------------------------------------");
            profileWriter.writeLine("");
        } catch (Exception e) {
            logger.error("Failed to write Black Duck server properties to configuration report.", e);
        }
    }

    private void createReports() {
        for (ReportTypes reportType : ReportTypes.values()) {
            try {
                createReportWriter(reportType);
            } catch (Exception e) {
                logger.error("Failed to create report: " + reportType.toString(), e);
            }
        }
    }

    private ReportWriter createReportWriter(ReportTypes reportType) {
        try {
            File reportFile = new File(reportDirectory, reportType.getReportFileName() + ".txt");
            FileReportWriter fileReportWriter = new FileReportWriter(reportFile, reportType.getReportTitle(), reportType.getReportDescription(), runId);
            reportWriters.put(reportType, fileReportWriter);
            logger.info("Created report file: " + reportFile.getPath());
            return fileReportWriter;
        } catch (Exception e) {
            logger.error("Failed to create report writer: " + reportType.toString(), e);
        }
        return new InfoLogReportWriter();
    }

    public ReportWriter getReportWriter(ReportTypes type) {
        if (reportWriters.containsKey(type)) {
            return reportWriters.get(type);
        } else {
            return createReportWriter(type);
        }
    }

    private void closeReportWriters() {
        for (FileReportWriter writer : reportWriters.values()) {
            writer.finish();
        }
    }
}
