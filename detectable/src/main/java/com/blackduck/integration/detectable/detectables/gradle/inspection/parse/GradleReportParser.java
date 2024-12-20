package com.blackduck.integration.detectable.detectables.gradle.inspection.parse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detectable.detectables.gradle.inspection.model.GradleConfiguration;
import com.blackduck.integration.detectable.detectables.gradle.inspection.model.GradleReport;

public class GradleReportParser {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String PROJECT_DIRECTORY_PREFIX = "projectDirectory:";
    public static final String PROJECT_GROUP_PREFIX = "projectGroup:";
    public static final String PROJECT_NAME_PREFIX = "projectName:";
    public static final String PROJECT_VERSION_PREFIX = "projectVersion:";
    public static final String PROJECT_PATH_PREFIX = "projectPath:";
    public static final String ROOT_PROJECT_NAME_PREFIX = "rootProjectName:";
    public static final String ROOT_PROJECT_VERSION_PREFIX = "rootProjectVersion:";
    public static final String DETECT_META_DATA_HEADER = "DETECT META DATA START";
    public static final String DETECT_META_DATA_FOOTER = "DETECT META DATA END";
    private final Map<String, String> metadata = new HashMap<>(); // important metadata to pass to line parsers for rich versions
    private final GradleReportConfigurationParser gradleReportConfigurationParser = new GradleReportConfigurationParser();

    public Optional<GradleReport> parseReport(File reportFile) {
        GradleReport gradleReport = new GradleReport();
        boolean processingMetaData = false;
        List<String> configurationLines = new ArrayList<>();
        try (InputStream dependenciesInputStream = new FileInputStream(reportFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(dependenciesInputStream, StandardCharsets.UTF_8))) {

            List<String> reportLines = reader.lines().collect(Collectors.toList());

            // we parse the last few lines of the extraction file, as we have the metadata information at the end of the file
            // such as project name, project parent etc. This information is helpful in getting the rich version information by storing parent information
            // and then parse childs to see if the rich versions declared are used.
            for(int i = reportLines.size()-1; i>=0; i--) {
                if (reportLines.get(i).startsWith(DETECT_META_DATA_FOOTER)) {
                    processingMetaData = true;
                } else if (reportLines.get(i).startsWith(DETECT_META_DATA_HEADER)) {
                    metadata.put("fileName:", reportFile.getName());
                    break;
                } else if (processingMetaData) {
                    setGradleReportInfo(gradleReport, reportLines.get(i));
                }
            }

            for(String line: reportLines) {
                if (StringUtils.isBlank(line)) {
                    parseConfigurationLines(configurationLines, gradleReport);
                    configurationLines.clear();
                } else if (line.startsWith(DETECT_META_DATA_HEADER)) {
                    break;
                } else {
                    configurationLines.add(line);
                }
            }

            parseConfigurationLines(configurationLines, gradleReport);
        } catch (Exception e) {
            logger.debug(String.format("Failed to read report file: %s", reportFile.getAbsolutePath()), e);
            gradleReport = null;
        }

        return Optional.ofNullable(gradleReport);
    }

    private void setGradleReportInfo(GradleReport gradleReport, String line) {
        if (line.startsWith(PROJECT_DIRECTORY_PREFIX)) {
            gradleReport.setProjectSourcePath(line.substring(PROJECT_DIRECTORY_PREFIX.length()).trim());
        } else if (line.startsWith(PROJECT_GROUP_PREFIX)) {
            gradleReport.setProjectGroup(line.substring(PROJECT_GROUP_PREFIX.length()).trim());
        } else if (line.startsWith(PROJECT_NAME_PREFIX)) {
            String projectName = line.substring(PROJECT_NAME_PREFIX.length()).trim(); // get project name
            gradleReport.setProjectName(projectName);
            metadata.put(PROJECT_NAME_PREFIX, projectName);
        } else if (line.startsWith(PROJECT_VERSION_PREFIX)) {
            gradleReport.setProjectVersionName(line.substring(PROJECT_VERSION_PREFIX.length()).trim());
        } else if (line.startsWith(ROOT_PROJECT_NAME_PREFIX)) {
            String rootProjectName = line.substring(ROOT_PROJECT_NAME_PREFIX.length()).trim();
            metadata.put(ROOT_PROJECT_NAME_PREFIX, rootProjectName);
        } else if (line.startsWith(PROJECT_PATH_PREFIX)) {
            String projectParent = line.substring(PROJECT_PATH_PREFIX.length()).trim(); // get current project's path Eg: :subtwo:foo
            metadata.put(PROJECT_PATH_PREFIX, projectParent);
        }
    }

    private void parseConfigurationLines(List<String> configurationLines, GradleReport gradleReport) {
        if (configurationLines.size() > 1 && isConfigurationHeader(configurationLines)) {
            String header = configurationLines.get(0);
            List<String> dependencyLines = configurationLines.stream().skip(1).collect(Collectors.toList());
            GradleConfiguration configuration = gradleReportConfigurationParser.parse(header, dependencyLines, metadata);
            gradleReport.getConfigurations().add(configuration);
        }
    }

    private boolean isConfigurationHeader(List<String> lines) {
        if (lines.get(0).contains(" - ")) {
            return true;
        } else {
            return StringUtils.isAlphanumeric(lines.get(0));
        }
    }
}
