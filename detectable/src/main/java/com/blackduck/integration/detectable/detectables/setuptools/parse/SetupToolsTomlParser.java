package com.blackduck.integration.detectable.detectables.setuptools.parse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import com.blackduck.integration.detectable.python.util.PythonDependency;
import com.blackduck.integration.detectable.python.util.PythonDependencyTransformer;

import static com.blackduck.integration.detectable.detectables.setuptools.parse.SetupToolsExtrasUtils.buildExtrasTransitives;

public class SetupToolsTomlParser implements SetupToolsParser {

    private static final String TOML_PROJECT_NAME_KEY = "project.name";
    private static final String TOML_PROJECT_VERSION_KEY = "project.version";
    private static final String TOML_PROJECT_DEPENDENCIES_KEY = "project.dependencies";
    private static final String TOML_OPTIONAL_DEPENDENCIES_KEY = "project.optional-dependencies";

    private static final String CONDITIONAL_MARKER = ";";

    private final TomlParseResult parsedToml;
    private final List<String> rawDependencyLines;

    public SetupToolsTomlParser(TomlParseResult parsedToml) {
        this.parsedToml = parsedToml;
        this.rawDependencyLines = new ArrayList<>();
    }

    @Override
    public SetupToolsParsedResult parse() throws IOException {
        List<PythonDependency> parsedDirectDependencies = parseDirectDependencies(parsedToml);
        String projectName = parsedToml.getString(TOML_PROJECT_NAME_KEY);
        String projectVersion = parsedToml.getString(TOML_PROJECT_VERSION_KEY);

        // Parse optional-dependencies and build extras transitives
        Map<String, List<String>> optionalDepsMap = parseOptionalDependencies(parsedToml);
        Map<String, List<PythonDependency>> extrasTransitives = buildExtrasTransitives(rawDependencyLines, optionalDepsMap);

        return new SetupToolsParsedResult(projectName, projectVersion, parsedDirectDependencies, extrasTransitives);
    }

    public List<PythonDependency> parseDirectDependencies(TomlParseResult tomlParseResult) throws IOException {
        List<PythonDependency> results = new LinkedList<>();
        PythonDependencyTransformer dependencyTransformer = new PythonDependencyTransformer();

        TomlArray dependencies = tomlParseResult.getArray(TOML_PROJECT_DEPENDENCIES_KEY);

        for (int i = 0; i < dependencies.size(); i++) {
            String dependencyLine = dependencies.getString(i);
            rawDependencyLines.add(dependencyLine);

            PythonDependency dependency = dependencyTransformer.transformLine(dependencyLine);

            // If we have a ; in our requirements line then there is a condition on this dependency.
            // We want to know this so we don't consider it a failure later if we try to run pip show
            // on it and we don't find it.
            if (dependencyLine.contains(CONDITIONAL_MARKER)) {
                dependency.setConditional(true);
            }

            if (dependency != null) {
                results.add(dependency);
            }
        }

        return results;
    }

    private Map<String, List<String>> parseOptionalDependencies(TomlParseResult tomlParseResult) {
        Map<String, List<String>> optionalDepsMap = new HashMap<>();

        TomlTable optionalDepsTable = tomlParseResult.getTable(TOML_OPTIONAL_DEPENDENCIES_KEY);
        if (optionalDepsTable == null) {
            return optionalDepsMap;
        }

        for (String groupName : optionalDepsTable.keySet()) {
            TomlArray groupArray = optionalDepsTable.getArray(groupName);
            if (groupArray != null) {
                List<String> groupDeps = new ArrayList<>();
                for (int i = 0; i < groupArray.size(); i++) {
                    groupDeps.add(groupArray.getString(i));
                }
                optionalDepsMap.put(groupName, groupDeps);
            }
        }

        return optionalDepsMap;
    }
}
