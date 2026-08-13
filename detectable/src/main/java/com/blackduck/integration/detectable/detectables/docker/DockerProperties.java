package com.blackduck.integration.detectable.detectables.docker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerProperties {
    private final DockerDetectableOptions dockerDetectableOptions;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String BOOLEAN_PROPERTY_VALUE_TRUE = "true";
    private static final String BOOLEAN_PROPERTY_VALUE_FALSE = "false";

    public DockerProperties(DockerDetectableOptions dockerDetectableOptions) {
        this.dockerDetectableOptions = dockerDetectableOptions;
    }

    public void populatePropertiesFile(File dockerPropertiesFile, File outputDirectory) throws IOException {
        Properties dockerProperties = new Properties();

        dockerProperties.setProperty("logging.level.com.blackduck", dockerDetectableOptions.getDockerInspectorLoggingLevel().toString());
        dockerProperties.setProperty("upload.bdio", BOOLEAN_PROPERTY_VALUE_FALSE);
        dockerProperties.setProperty("output.path", outputDirectory.getAbsolutePath());
        dockerProperties.setProperty("phone.home", BOOLEAN_PROPERTY_VALUE_FALSE);
        dockerProperties.setProperty("caller.name", "Detect");
        dockerProperties.setProperty("working.dir.path", createDir(outputDirectory, "inspectorWorkingDir").getAbsolutePath());
        dockerProperties.setProperty("shared.dir.path.local", createDir(outputDirectory, "inspectorSharedDir").getAbsolutePath());

        // Request both of the following; DI pre-8.1.0 will only recognize/return containerfilesystem.
        // DI 8.1.0 and newer will provide both; Detect will prefer squashedimage
        dockerProperties.setProperty("output.include.containerfilesystem", BOOLEAN_PROPERTY_VALUE_TRUE);
        dockerProperties.setProperty("output.include.squashedimage", BOOLEAN_PROPERTY_VALUE_TRUE);
        dockerProperties.setProperty("bdio2.enabled", BOOLEAN_PROPERTY_VALUE_FALSE); // soon DI will return BDIO2 by default, which Detect can't consume
        dockerDetectableOptions.getDockerPlatformTopLayerId().ifPresent(id -> {
            SpelInjectionGuard.rejectIfContainsSpelTemplate("docker.platform.top.layer.id", id);
            dockerProperties.setProperty("docker.platform.top.layer.id", id);
        });

        // Refuse to forward passthrough values that contain SpEL templates ("#{...}"). Docker Inspector
        // binds these keys via @Value("${...}"), and Spring evaluates any "#{...}" in the resolved value
        // during context refresh. See CVE-2026-41849.
        Map<String, String> additionalDockerProperties = dockerDetectableOptions.getAdditionalDockerProperties();
        for (Map.Entry<String, String> entry : additionalDockerProperties.entrySet()) {
            SpelInjectionGuard.rejectIfContainsSpelTemplate(entry.getKey(), entry.getValue());
        }
        dockerProperties.putAll(additionalDockerProperties);

        logger.debug("Contents of application.properties passed to Docker Inspector: {}", dockerProperties);
        try (FileOutputStream fileOutputStream = new FileOutputStream(dockerPropertiesFile)) {
            dockerProperties.store(fileOutputStream, "");
        }
    }

    private File createDir(File parentDir, String newDirName) throws IOException {
        File newDir = new File(parentDir, newDirName);
        Files.createDirectories(newDir.toPath());
        return newDir;
    }
}
