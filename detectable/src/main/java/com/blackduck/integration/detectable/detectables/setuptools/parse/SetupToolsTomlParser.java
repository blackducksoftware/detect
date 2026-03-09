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

    private TomlParseResult parsedToml;
    private List<String> rawDependencyLines;

    public SetupToolsTomlParser(TomlParseResult parsedToml) {
        this.parsedToml = parsedToml;
        this.rawDependencyLines = new ArrayList<>();
    }

    @Override
    public SetupToolsParsedResult parse() throws IOException {
        List<PythonDependency> parsedDirectDependencies = parseDirectDependencies(parsedToml);
        String projectName = parsedToml.getString("project.name");
        String projectVersion = parsedToml.getString("project.version");

        // Parse optional-dependencies and build extras transitives
        Map<String, List<String>> optionalDepsMap = parseOptionalDependencies(parsedToml);
        Map<String, List<PythonDependency>> extrasTransitives = buildExtrasTransitives(rawDependencyLines, optionalDepsMap);

        return new SetupToolsParsedResult(projectName, projectVersion, parsedDirectDependencies, extrasTransitives);
    }

    public List<PythonDependency> parseDirectDependencies(TomlParseResult tomlParseResult) throws IOException {
        List<PythonDependency> results = new LinkedList<>();
        PythonDependencyTransformer dependencyTransformer = new PythonDependencyTransformer();

        TomlArray dependencies = tomlParseResult.getArray("project.dependencies");

        for (int i = 0; i < dependencies.size(); i++) {
            String dependencyLine = dependencies.getString(i);
            rawDependencyLines.add(dependencyLine);

            PythonDependency dependency = dependencyTransformer.transformLine(dependencyLine);

            // If we have a ; in our requirements line then there is a condition on this dependency.
            // We want to know this so we don't consider it a failure later if we try to run pip show
            // on it and we don't find it.
            if (dependencyLine.contains(";")) {
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

        TomlTable optionalDepsTable = tomlParseResult.getTable("project.optional-dependencies");
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
