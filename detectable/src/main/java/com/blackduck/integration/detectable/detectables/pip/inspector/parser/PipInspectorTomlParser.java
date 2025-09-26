package com.blackduck.integration.detectable.detectables.pip.inspector.parser;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class PipInspectorTomlParser {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String PROJECT_KEY = "project";
    private final File pyprojectTomlFile;

    public PipInspectorTomlParser(File pyprojectTomlFile) {
        this.pyprojectTomlFile = pyprojectTomlFile;
    }

    public Boolean checkIfProjectKeyExists(TomlParseResult parsedToml) {
        return parsedToml != null && parsedToml.contains(PROJECT_KEY);
    }
    public TomlParseResult parseToml() {
        TomlParseResult parsedToml = null;
        try {
            String tomlContents = FileUtils.readFileToString(pyprojectTomlFile, StandardCharsets.UTF_8);
            parsedToml = Toml.parse(tomlContents);
        } catch (Exception e) {
            logger.warn("Unable to read Toml file: " + pyprojectTomlFile.getAbsolutePath(), e);
        }

        return parsedToml;
    }

}
