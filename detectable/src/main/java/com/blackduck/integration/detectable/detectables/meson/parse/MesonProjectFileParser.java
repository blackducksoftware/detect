package com.blackduck.integration.detectable.detectables.meson.parse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.util.NameVersion;
import com.google.gson.Gson;

public class MesonProjectFileParser {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public MesonProjectFileParser() {
    }

    public NameVersion getProjectNameVersion(Gson gson, String jsonContents, String defaultProjectName,
            String defaultProjectVersion) {
        try {
            MesonProjectInfo projectInfo = gson.fromJson(jsonContents, MesonProjectInfo.class);

            String projectName = defaultProjectName;
            String projectVersion = defaultProjectVersion;

            if (StringUtils.isNotBlank(projectInfo.getDescriptiveName())) {
                projectName = projectInfo.getDescriptiveName();
                logger.debug("Extracted project name: {}", projectName);
            }

            if (StringUtils.isNotBlank(projectInfo.getVersion())) {
                projectVersion = projectInfo.getVersion();
                logger.debug("Extracted project version: {}", projectVersion);
            }

            return new NameVersion(projectName, projectVersion);
        } catch (Exception e) {
            logger.warn("Failed to parse Meson project JSON, using defaults", e);
            return new NameVersion(defaultProjectName, defaultProjectVersion);
        }
    }

    private static class MesonProjectInfo {
        private String version;
        private String descriptive_name;

        public String getVersion() {
            return version;
        }

        public String getDescriptiveName() {
            return descriptive_name;
        }
    }
}
