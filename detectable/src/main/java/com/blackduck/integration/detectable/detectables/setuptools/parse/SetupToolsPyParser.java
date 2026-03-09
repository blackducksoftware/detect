package com.blackduck.integration.detectable.detectables.setuptools.parse;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tomlj.TomlParseResult;

import com.blackduck.integration.detectable.python.util.PythonDependency;
import com.blackduck.integration.detectable.python.util.PythonDependencyTransformer;

import static com.blackduck.integration.detectable.detectables.setuptools.parse.SetupToolsExtrasUtils.buildExtrasTransitives;

public class SetupToolsPyParser implements SetupToolsParser {

    // Regex pattern for dependencies enclosed in single quotes: ['requests==2.31.0'],
    private static final Pattern PATTERN_SINGLE_QUOTES = Pattern.compile("\\[?'(.*)'\\s*\\]?,?");

    // Regex pattern for dependencies enclosed in double quotes: ["requests==2.31.0"],
    private static final Pattern PATTERN_DOUBLE_QUOTES = Pattern.compile("\\[?\"(.*)\"\\s*\\]?,?");

    // Regex pattern for extras_require group key lines like "security": [ or 'http2': [
    private static final Pattern EXTRAS_GROUP_KEY_PATTERN = Pattern.compile("[\"'](.*?)[\"']\\s*:");

    // Regex pattern for extracting any double-quoted string value
    private static final Pattern EXTRACT_DOUBLE_QUOTES = Pattern.compile("\"(.*?)\"");

    // Regex pattern for extracting any single-quoted string value
    private static final Pattern EXTRACT_SINGLE_QUOTES = Pattern.compile("'(.*?)'");

    private static final String INSTALL_REQUIRES_PREFIX = "install_requires=";
    private static final String EXTRAS_REQUIRE_PREFIX = "extras_require=";

    private static final String TOML_PROJECT_NAME_KEY = "project.name";
    private static final String TOML_PROJECT_VERSION_KEY = "project.version";

    private static final String OPEN_BRACKET = "[";
    private static final String OPEN_BRACE = "{";
    private static final String CLOSE_BRACE = "}";
    private static final String CLOSE_BRACE_COMMA = "},";
    private static final String CLOSE_BRACKET = "]";
    private static final String CLOSE_BRACKET_COMMA = "],";

    private final TomlParseResult parsedToml;

    private final List<String> dependencies;

    private final Map<String, List<String>> extrasRequireMap;

    public SetupToolsPyParser(TomlParseResult parsedToml) {
        this.parsedToml = parsedToml;
        this.dependencies = new ArrayList<>();
        this.extrasRequireMap = new HashMap<>();
    }
    
    @Override
    public SetupToolsParsedResult parse() throws IOException {
        // Use a name from the toml if we have it. Do not parse names and versions from the setup.py
        // as the project will not always have a string (it could have variables or method calls)
        String tomlProjectName = parsedToml.getString(TOML_PROJECT_NAME_KEY);
        String projectVersion = parsedToml.getString(TOML_PROJECT_VERSION_KEY);

        List<PythonDependency> parsedDirectDependencies = parseDirectDependencies();

        Map<String, List<PythonDependency>> extrasTransitives = buildExtrasTransitives(dependencies, extrasRequireMap);

        return new SetupToolsParsedResult(tomlProjectName, projectVersion, parsedDirectDependencies, extrasTransitives);
    }
    
    public List<String> load(String setupFile) throws IOException {
        // The pattern "\\[?'(.*)'\\s*\\]?,?|\\[?\"(.*)\"\\s*\\]?,?" works as follows:
        // - "\\[?'(.*)'\\s*\\]?,?" matches dependencies that start with an optional '[' followed by a mandatory single quote,
        //   then any characters (the dependency name), ending with a single quote followed by optional whitespace and an optional ',' or ']'.
        // - "\\[?\"(.*)\"\\s*\\]?,?" is similar to the first part but for dependencies enclosed in double quotes.

        try (BufferedReader reader = new BufferedReader(new FileReader(setupFile))) {
            String line;
            boolean isInstallRequiresSection = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // If after removing all whitespace the line starts with install_requires=
                // then we have found the section we are after.
                if (line.replaceAll("\\s+", "").startsWith(INSTALL_REQUIRES_PREFIX)) {
                    isInstallRequiresSection = true;
                    continue;
                }
                if (isInstallRequiresSection) {
                    // If the [ is on its own line skip it, it doesn't contain a dependency
                    if (line.equals(OPEN_BRACKET)) {
                        continue;
                    }

                    checkLineForDependency(line);

                    // If the line ends with ] or ], it means we have reached the end of the dependencies list.
                    if (line.endsWith(CLOSE_BRACKET) || line.endsWith(CLOSE_BRACKET_COMMA)) {
                        break;
                    }
                }
            }
        }

        return dependencies;
    }

    public Map<String, List<String>> loadExtrasRequire(String setupFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(setupFile))) {
            String line;
            boolean isExtrasRequireSection = false;
            String currentGroup = null;

            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();

                if (trimmedLine.replaceAll("\\s+", "").startsWith(EXTRAS_REQUIRE_PREFIX)) {
                    isExtrasRequireSection = true;
                    continue;
                }

                if (!isExtrasRequireSection) {
                    continue;
                }

                if (trimmedLine.equals(OPEN_BRACE)) {
                    continue;
                }

                if (trimmedLine.equals(CLOSE_BRACE) || trimmedLine.equals(CLOSE_BRACE_COMMA)) {
                    break;
                }

                currentGroup = processExtrasLine(trimmedLine, currentGroup);
            }
        }

        return extrasRequireMap;
    }

    private String processExtrasLine(String trimmedLine, String currentGroup) {
        Matcher groupMatcher = EXTRAS_GROUP_KEY_PATTERN.matcher(trimmedLine);

        if (groupMatcher.find()) {
            currentGroup = groupMatcher.group(1);
            String afterColon = trimmedLine.substring(trimmedLine.indexOf(':') + 1).trim();
            addExtrasLineDeps(afterColon, currentGroup);
            if (afterColon.contains(CLOSE_BRACKET)) {
                return null;
            }
        } else if (currentGroup != null) {
            addExtrasLineDeps(trimmedLine, currentGroup);
            if (trimmedLine.endsWith(CLOSE_BRACKET) || trimmedLine.endsWith(CLOSE_BRACKET_COMMA)) {
                return null;
            }
        }

        return currentGroup;
    }

    private void addExtrasLineDeps(String line, String group) {
        List<String> deps = extractQuotedStrings(line);
        for (String dep : deps) {
            extrasRequireMap.computeIfAbsent(group, k -> new ArrayList<>()).add(dep);
        }
    }

    /**
     * Extracts the first quoted string from a line, trying double quotes first,
     * then single quotes. Used by both install_requires and extras_require parsing.
     */
    private List<String> extractQuotedStrings(String line) {
        List<String> results = new ArrayList<>();

        Matcher matcherDouble = EXTRACT_DOUBLE_QUOTES.matcher(line);
        boolean found = false;
        while (matcherDouble.find()) {
            String value = matcherDouble.group(1);
            if (!value.isEmpty()) {
                results.add(value);
                found = true;
            }
        }
        if (!found) {
            Matcher matcherSingle = EXTRACT_SINGLE_QUOTES.matcher(line);
            while (matcherSingle.find()) {
                String value = matcherSingle.group(1);
                if (!value.isEmpty()) {
                    results.add(value);
                }
            }
        }
        return results;
    }

    private void checkLineForDependency(String line) {
        // Try double quotes first, then fall back to single quotes.
        // Double quotes are preferred as lines sometimes use double quotes with
        // single quotes inside them to specify conditionals.
        Matcher matcherDoubleQuotes = PATTERN_DOUBLE_QUOTES.matcher(line);
        if (matcherDoubleQuotes.find()) {
            dependencies.add(matcherDoubleQuotes.group(1));
        } else {
            Matcher matcherSingleQuotes = PATTERN_SINGLE_QUOTES.matcher(line);
            if (matcherSingleQuotes.find()) {
                dependencies.add(matcherSingleQuotes.group(1));
            }
        }
    }

    private List<PythonDependency> parseDirectDependencies() {
        List<PythonDependency> results = new LinkedList<>();
        
        PythonDependencyTransformer dependencyTransformer = new PythonDependencyTransformer();

        for (String dependencyLine : dependencies) {            
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
}
