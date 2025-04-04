package com.blackduck.integration.detectable.detectables.uv.parse;

import java.util.Optional;

import com.blackduck.integration.executable.ExecutableRunnerException;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import com.blackduck.integration.util.NameVersion;

public class UVTomlParser {
    private static final String NAME_KEY = "name";
    private static final String VERSION_KEY = "version";
    private static final String PROJECT_KEY = "project";

    public Optional<NameVersion> parseNameVersion(String uvTomlContents) throws ExecutableRunnerException {
        TomlParseResult uvTomlObject = Toml.parse(uvTomlContents);
        if (uvTomlObject.contains(PROJECT_KEY)) {
            return Optional.ofNullable(uvTomlObject.getTable(PROJECT_KEY))
                    .filter(info -> info.contains(NAME_KEY))
                    .map(info -> new NameVersion(info.getString(NAME_KEY), info.getString(VERSION_KEY)));
        }
        return Optional.empty();
    }

}
