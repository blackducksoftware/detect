package com.blackduck.integration.detect.configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

/**
 * Utility class for reading and validating JSON project settings files using Gson.
 */
public class ProjectSettingsJsonFileReader {
    private static final Logger logger = LoggerFactory.getLogger(ProjectSettingsJsonFileReader.class);
    private final Gson gson = new Gson();

    /**
     * Reads and parses a JSON file into a ProjectSettings object.
     * 
     * @param filePath the path to the JSON file
     * @return the parsed ProjectSettings object, or null if the file cannot be read or parsed
     */
    public ProjectSettings readProjectSettings(Path filePath) {
        if (filePath == null) {
            return null;
        }

        if (!Files.exists(filePath)) {
            logger.error("Project settings file does not exist: {}", filePath);
            return null;
        }

        if (!Files.isRegularFile(filePath)) {
            logger.error("Project settings path is not a regular file: {}", filePath);
            return null;
        }

        if (!Files.isReadable(filePath)) {
            logger.error("Project settings file is not readable: {}", filePath);
            return null;
        }

        try {
            String jsonContent = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
            if (jsonContent == null || jsonContent.trim().isEmpty()) {
                logger.warn("Project settings file is empty: {}", filePath);
                return null;
            }

            ProjectSettings projectSettings = gson.fromJson(jsonContent, ProjectSettings.class);
            
            if (projectSettings == null) {
                logger.warn("Project settings file contains null JSON: {}", filePath);
                return null;
            }

            logger.debug("Successfully parsed project settings from file: {}", filePath);
            return projectSettings;

        } catch (IOException e) {
            logger.error("Failed to read project settings file: {}: {}", filePath, e.getMessage());
            return null;
        } catch (JsonSyntaxException e) {
            logger.error("Invalid JSON syntax in project settings file: {}: {}", filePath, e.getMessage());
            return null;
        } catch (JsonIOException e) {
            logger.error("JSON IO error reading project settings file: {}: {}", filePath, e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error reading project settings file: {}: {}", filePath, e.getMessage());
            return null;
        }
    }
}