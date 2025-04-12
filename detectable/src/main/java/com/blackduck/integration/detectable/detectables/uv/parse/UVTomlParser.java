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

    private String uvTomlContents = "";

    public UVTomlParser(File uvTomlFile) {
        parseUVToml(uvTomlFile);
    }

    public Optional<NameVersion> parseNameVersion() {
        TomlParseResult uvTomlObject = Toml.parse(uvTomlContents);
        if (uvTomlObject.contains(PROJECT_KEY)) {
            return Optional.ofNullable(uvTomlObject.getTable(PROJECT_KEY))
                    .filter(info -> info.contains(NAME_KEY))
                    .filter(info -> info.contains(VERSION_KEY))
                    .map(info -> new NameVersion(info.getString(NAME_KEY), info.getString(VERSION_KEY)));
        }
        return Optional.empty();
    }


    // check [tool.uv] managed setting
    public boolean parseManagedKey() {
        TomlParseResult uvTomlObject = Toml.parse(uvTomlContents);
        if (uvTomlObject.contains(UV_TOOL_KEY)) {
            TomlTable uvToolTable = uvTomlObject.getTable(UV_TOOL_KEY);
            if (uvToolTable.contains(MANAGED_KEY)) {
                return uvToolTable.getBoolean(MANAGED_KEY);
            }
            return true;
        }
        return true;
    }

    public void parseUVToml(File uvTomlFile) {
        try {
            uvTomlContents = FileUtils.readFileToString(uvTomlFile, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.warn("Unable to read UV Toml file: " + uvTomlFile.getAbsolutePath(), e);
        }
    }

}
