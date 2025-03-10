package com.blackduck.integration.detectable.detectables.gradle.inspection.inspector;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detectable.detectable.exception.DetectableException;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class GradleInspectorScriptCreator {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String GRADLE_SCRIPT_TEMPLATE_FILENAME = "init-script-gradle.ftl";

    private final Configuration configuration;

    public GradleInspectorScriptCreator(Configuration configuration) {
        this.configuration = configuration;
    }

    public File createGradleInspector(File targetFile, GradleInspectorScriptOptions scriptOptions) throws DetectableException {
        logger.debug("Generating the gradle script file.");
        Map<String, String> gradleScriptData = new HashMap<>();

        gradleScriptData.put("excludedProjectNames", toCommaSeparatedString(scriptOptions.getExcludedProjectNames()));
        gradleScriptData.put("includedProjectNames", toCommaSeparatedString(scriptOptions.getIncludedProjectNames()));
        gradleScriptData.put("excludedProjectPaths", StringEscapeUtils.escapeJava(toCommaSeparatedString(scriptOptions.getExcludedProjectPaths())));
        gradleScriptData.put("includedProjectPaths", StringEscapeUtils.escapeJava(toCommaSeparatedString(scriptOptions.getIncludedProjectPaths())));
        gradleScriptData.put("excludedConfigurationNames", toCommaSeparatedString(scriptOptions.getExcludedConfigurationNames()));
        gradleScriptData.put("includedConfigurationNames", toCommaSeparatedString(scriptOptions.getIncludedConfigurationNames()));
        gradleScriptData.put("rootOnlyOption", Boolean.toString(scriptOptions.isRootOnly()));

        try {
            populateGradleScriptWithData(targetFile, gradleScriptData);
        } catch (IOException | TemplateException e) {
            throw new DetectableException("Failed to generate the Gradle Inspector script from the given template file: " + targetFile.toString(), e);
        }
        logger.trace(String.format("Successfully created Gradle Inspector: %s", targetFile.toString()));
        return targetFile;
    }

    private void populateGradleScriptWithData(File targetFile, Map<String, String> gradleScriptData) throws IOException, TemplateException {
        Template gradleScriptTemplate = configuration.getTemplate(GRADLE_SCRIPT_TEMPLATE_FILENAME);
        try (Writer fileWriter = new FileWriter(targetFile)) {
            gradleScriptTemplate.process(gradleScriptData, fileWriter);
        }
    }

    private String toCommaSeparatedString(List<String> list) {
        return StringUtils.joinWith(",", list.toArray());
    }
}
