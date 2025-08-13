package com.blackduck.integration.detectable.detectables.poetry.parser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import com.blackduck.integration.detectable.detectable.util.TomlFileUtils;
import com.blackduck.integration.detectable.detectables.poetry.PoetryOptions;


public class ToolPoetrySectionParser {
    public static final String TOOL_POETRY_KEY = "tool.poetry";
    public static final String MAIN_DEPENDENCY_GROUP_KEY = "tool.poetry.dependencies";

    public static final String LEGACY_DEV_DEPENDENCY_GROUP_KEY = "tool.poetry.dev-dependencies";

    public static final String DEPENDENCY_GROUP_KEY_PREFIX = "tool.poetry.group.";
    public static final String DEPENDENCY_GROUP_KEY_SUFFIX = ".dependencies";

    public static final String DEFAULT_DEV_GROUP_NAME = "dev";

    public static final String PYTHON_COMPONENT_NAME = "python";
    
    // Poetry 2.x support: PEP 621 compliant [project] section
    public static final String PROJECT_DEPENDENCIES_KEY = "project.dependencies";

    private Map<File, TomlParseResult> fileParseResults = new HashMap<>();

    public ToolPoetrySectionResult parseToolPoetrySection(@Nullable File pyprojectToml) {
        if (pyprojectToml != null) {
            try {
                TomlParseResult parseResult = memoizedTomlParseResult(pyprojectToml);
                if (parseResult.get(TOOL_POETRY_KEY) != null) {
                    TomlTable poetrySection = parseResult.getTable(TOOL_POETRY_KEY);
                    return ToolPoetrySectionResult.FOUND(poetrySection);
                }
            } catch (IOException e) {
                return ToolPoetrySectionResult.NOT_FOUND();
            }
        }
        return ToolPoetrySectionResult.NOT_FOUND();
    }

    public TomlTable parseProjectSection(File pyprojectTomlFile) {
        if (pyprojectTomlFile == null)
            return null;

        TomlParseResult parseResult;
        try {
            parseResult = memoizedTomlParseResult(pyprojectTomlFile);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read pyproject.toml file: " + pyprojectTomlFile.getAbsolutePath(), e);
        }
        return parseResult.getTable("project");
    }

    public Set<String> parseRootPackages(File pyprojectToml, PoetryOptions options) {
        if (options.getExcludedGroups().isEmpty() || pyprojectToml == null) {
            return null;
        }

        Set<String> result = new HashSet<>();

        TomlParseResult parseResult;
        try {
            parseResult = memoizedTomlParseResult(pyprojectToml);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read pyproject.toml file");
        }

        for (String key : parseResult.dottedKeySet(true)) {
            processKeyForRootPackages(parseResult, options, result, key);
        }

        // Poetry 2.x support: Parse dependencies from [project] section (PEP 621 compliant)
        processProjectDependencies(parseResult, result);

        return result;
    }

    private TomlParseResult memoizedTomlParseResult(File tomlFile) throws IOException {
        if (!fileParseResults.containsKey(tomlFile)) {
            fileParseResults.put(tomlFile, TomlFileUtils.parseFile(tomlFile));
        }
        return fileParseResults.get(tomlFile);
    }

    private void processKeyForRootPackages(TomlParseResult parseResult, PoetryOptions options, Set<String> result, String key) {
        if (!parseResult.isTable(key)) {
            return;
        }

        TomlTable table = parseResult.getTable(key);

        if (key.equals(MAIN_DEPENDENCY_GROUP_KEY)) {
            addAllPackageNamesToSet(result, table);
        } else if (key.equals(LEGACY_DEV_DEPENDENCY_GROUP_KEY)) { // in Poetry 1.0 to 1.2 this was the way of specifying dev dependencies
            if (!options.getExcludedGroups().contains(DEFAULT_DEV_GROUP_NAME)) {
                addAllPackageNamesToSet(result, table);
            }
        } else if (key.startsWith(DEPENDENCY_GROUP_KEY_PREFIX) && key.endsWith(DEPENDENCY_GROUP_KEY_SUFFIX)) {
            String group = key.substring(DEPENDENCY_GROUP_KEY_PREFIX.length(), key.length() - DEPENDENCY_GROUP_KEY_SUFFIX.length());

            if (!options.getExcludedGroups().contains(group)) {
                addAllPackageNamesToSet(result, table);
            }
        }
    }

    private void addAllPackageNamesToSet(Set<String> set, TomlTable table) {
        for (List<String> key : table.keyPathSet()) {
            String packageName = key.get(0);

            if (packageName.equalsIgnoreCase(PYTHON_COMPONENT_NAME))
                continue;

            set.add(packageName);
        }
    }

    // Poetry 2.x support: Process dependencies from [project] section (PEP 621 compliant)
    private void processProjectDependencies(TomlParseResult parseResult, Set<String> result) {
        if (parseResult.isArray(PROJECT_DEPENDENCIES_KEY)) {
            parseResult.getArray(PROJECT_DEPENDENCIES_KEY).toList().forEach(dependency -> {
                if (dependency instanceof String) {
                    String packageName = extractPackageNameFromDependencyString((String) dependency);
                    if (packageName != null && !packageName.equalsIgnoreCase(PYTHON_COMPONENT_NAME)) {
                        result.add(packageName);
                    }
                }
            });
        }
    }

    // Poetry 2.x support: Extract package name from dependency string
    // Handles: "requests>=2.25.1", "requests[security]>=2.25.1", "requests (>=2.25.1)", etc.
    private String extractPackageNameFromDependencyString(String dependencyString) {
        if (dependencyString == null) {
            return null;
        }

        // Extract the first sequence of valid package name characters
        // Python package names can contain letters, numbers, hyphens, underscores, and dots
        Pattern pattern = Pattern.compile("^([a-zA-Z][a-zA-Z0-9._-]+)");
        Matcher matcher = pattern.matcher(dependencyString.trim());

        return matcher.find() ? matcher.group(1) : null;
    }
}
