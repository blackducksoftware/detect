package com.blackduck.integration.detect.workflow.blackduck.report.json;

import com.blackduck.integration.detect.workflow.blackduck.report.ReportData;
import com.blackduck.integration.detect.workflow.blackduck.report.util.ReportFileUtil;
import com.blackduck.integration.util.IntegrationEscapeUtil;
import com.google.gson.GsonBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RiskReportJsonWriter {

    private static final Logger logger = LoggerFactory.getLogger(RiskReportJsonWriter.class);

    public static File createRiskReportJsonFile(File outputDirectory, ReportData reportData) throws IOException {
        File jsonFile = ReportFileUtil.createReportFile(outputDirectory, reportData.getProjectName(), reportData.getProjectVersion(), "json");
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
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
        return gson.toJson(reportData);
    }

    public static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime> {
        @Override
        public JsonElement serialize(LocalDateTime dateTime, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
    }

}
