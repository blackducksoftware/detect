package com.blackduck.integration.detect.workflow.blackduck.report.json;

import com.blackduck.integration.detect.workflow.blackduck.report.ReportData;
import com.blackduck.integration.util.IntegrationEscapeUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class RiskReportJsonWriter {


    public static File createRiskReportJsonFile(File outputDirectory, ReportData reportData) throws IOException {
        IntegrationEscapeUtil escapeUtil = new IntegrationEscapeUtil();

        String escapedProjectName = escapeUtil.replaceWithUnderscore(reportData.getProjectName());
        String escapedProjectVersionName = escapeUtil.replaceWithUnderscore(reportData.getProjectVersion());
        File jsonFile = new File(outputDirectory, escapedProjectName + "_" + escapedProjectVersionName + "_BlackDuck_RiskReport.json");

        String serializedReportData = serializeRiskReport(reportData);

        try (FileWriter fw = new FileWriter(jsonFile)) {
            fw.write(serializedReportData);
            fw.flush();
        } catch (IOException e) {
            throw e;
        }

        return jsonFile;
    }

    private static String serializeRiskReport(ReportData reportData) {
        Gson gson = new GsonBuilder()
                .setLenient()
                .setPrettyPrinting()
                .create();
        return gson.toJson(reportData);
    }

}
