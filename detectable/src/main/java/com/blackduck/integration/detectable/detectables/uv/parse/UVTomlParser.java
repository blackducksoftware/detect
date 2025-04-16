package com.blackduck.integration.detectable.detectables.uv.parse;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import com.blackduck.integration.util.NameVersion;
import org.tomlj.TomlTable;

public class UVTomlParser {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String NAME_KEY = "name";
    private static final String VERSION_KEY = "version";
    private static final String PROJECT_KEY = "project";

    private static final String MANAGED_KEY = "managed";
    private static final String UV_TOOL_KEY = "tool.uv";

    private TomlParseResult uvToml;

    public UVTomlParser(File uvTomlFile) {
        parseUVToml(uvTomlFile);
    }

    public Optional<NameVersion> parseNameVersion() {
        if (uvToml.contains(PROJECT_KEY)) {
            return Optional.ofNullable(uvToml.getTable(PROJECT_KEY))
                    .filter(info -> info.contains(NAME_KEY))
                    .filter(info -> info.contains(VERSION_KEY))
                    .map(info -> new NameVersion(info.getString(NAME_KEY), info.getString(VERSION_KEY)));
        }
        return Optional.empty();
    }

    public String getProjectName() {
        TomlParseResult uvTomlObject = Toml.parse(uvTomlContents);
        if (uvTomlObject.contains(PROJECT_KEY)) {
            return uvTomlObject.getTable(PROJECT_KEY).getString(NAME_KEY);
        }

        return "uvProject";
    }


    // check [tool.uv] managed setting
    public boolean parseManagedKey() {
        if (uvToml.contains(UV_TOOL_KEY)) {
            TomlTable uvToolTable = uvToml.getTable(UV_TOOL_KEY);
            if (uvToolTable.contains(MANAGED_KEY)) {
                return uvToolTable.getBoolean(MANAGED_KEY);
            }
            return true;
        }
        return true;
    }

    public void parseUVToml(File uvTomlFile) {
        try {
            String uvTomlContents = FileUtils.readFileToString(uvTomlFile, StandardCharsets.UTF_8);
            uvToml = Toml.parse(uvTomlContents);
        } catch (Exception e) {
            logger.warn("Unable to read UV Toml file: " + uvTomlFile.getAbsolutePath(), e);
        }
    }

}
