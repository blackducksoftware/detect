package com.blackduck.integration.detect.workflow.blackduck.report.util;

import com.blackduck.integration.util.IntegrationEscapeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class ReportFileUtil {

    private static final Logger logger = LoggerFactory.getLogger(ReportFileUtil.class);

    public static File createReportFile(File outputDirectory, String projectName, String projectVersion, String reportFileExtension, String reportName) {
        IntegrationEscapeUtil escapeUtil = new IntegrationEscapeUtil();
        String escapedProjectName = escapeUtil.replaceWithUnderscore(projectName);
        String escapedProjectVersionName = escapeUtil.replaceWithUnderscore(projectVersion);
        File reportFile = new File(outputDirectory, escapedProjectName + "_" + escapedProjectVersionName + reportName + "." + reportFileExtension);
        if (reportFile.exists()) {
            boolean deleted = reportFile.delete();
            if (!deleted) {
                logger.warn(String.format("Unable to delete existing file %s before re-creating it", reportFile.getAbsolutePath()));
            }
        }

        return reportFile;
    }

}
