package com.blackduck.integration.detectable.detectables.uv.parse;

import com.blackduck.integration.util.NameVersion;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class UVTomlParser {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String NAME_KEY = "name";
    private static final String VERSION_KEY = "version";
    private static final String PROJECT_KEY = "project";

    private static final String MANAGED_KEY = "managed";
    private static final String UV_TOOL_KEY = "tool.uv";

    private String projectName = "uvProject";

    private final File uvTomlFile;
    private TomlParseResult uvToml;

    public UVTomlParser(File uvTomlFile) {
        this.uvTomlFile = uvTomlFile;
    }

    public Optional<NameVersion> parseNameVersion() {
        if (uvToml != null && uvToml.contains(PROJECT_KEY)) {

            TomlTable projectTable = uvToml.getTable(PROJECT_KEY);
            if(projectTable.contains(NAME_KEY)) {
                projectName = projectTable.getString(NAME_KEY);
            } else {
                return Optional.empty();
            }

            if(projectTable.contains(VERSION_KEY)) {
                String version = projectTable.getString(VERSION_KEY);
                return Optional.of(new NameVersion(projectName, version));
            } else {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }


    // check [tool.uv] managed setting
    public boolean parseManagedKey() {
        parseUVToml();
        if (uvToml != null && uvToml.contains(UV_TOOL_KEY)) {
            TomlTable uvToolTable = uvToml.getTable(UV_TOOL_KEY);
            if (uvToolTable.contains(MANAGED_KEY)) {
                return uvToolTable.getBoolean(MANAGED_KEY);
            }
            return true;
        }
        return true;
    }

    public void parseUVToml() {
        try {
            String uvTomlContents = FileUtils.readFileToString(uvTomlFile, StandardCharsets.UTF_8);
            uvToml = Toml.parse(uvTomlContents);
        } catch (Exception e) {
            logger.warn("Unable to read UV Toml file: " + uvTomlFile.getAbsolutePath(), e);
        }
    }

    public String getProjectName() {
        return projectName;
    }
}
