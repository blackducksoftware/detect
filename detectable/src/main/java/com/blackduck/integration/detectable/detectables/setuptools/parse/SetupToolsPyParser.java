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
    
    private TomlParseResult parsedToml;
    
    private List<String> dependencies;

    private Map<String, List<String>> extrasRequireMap;

    public SetupToolsPyParser(TomlParseResult parsedToml) {
        this.parsedToml = parsedToml;
        this.dependencies = new ArrayList<>();
        this.extrasRequireMap = new HashMap<>();
    }
    
    @Override
    public SetupToolsParsedResult parse() throws IOException {
        // Use a name from the toml if we have it. Do not parse names and versions from the setup.py
        // as the project will not always have a string (it could have variables or method calls)
        String tomlProjectName = parsedToml.getString("project.name");
        String projectVersion = parsedToml.getString("project.version");
        
        List<PythonDependency> parsedDirectDependencies = parseDirectDependencies();

        Map<String, List<PythonDependency>> extrasTransitives = buildExtrasTransitives(dependencies, extrasRequireMap);

        return new SetupToolsParsedResult(tomlProjectName, projectVersion, parsedDirectDependencies, extrasTransitives);
    }
    
    public List<String> load(String setupFile) throws IOException {
        // The pattern "\\[?'(.*)'\\s*\\]?,?|\\[?\"(.*)\"\\s*\\]?,?" works as follows:
        // - "\\[?'(.*)'\\s*\\]?,?" matches dependencies that start with an optional '[' followed by a mandatory single quote,
        //   then any characters (the dependency name), ending with a single quote followed by optional whitespace and an optional ',' or ']'.
        // - "\\[?\"(.*)\"\\s*\\]?,?" is similar to the first part but for dependencies enclosed in double quotes.
        // Pattern for single quotes
        Pattern patternSingleQuotes = Pattern.compile("\\[?'(.*)'\\s*\\]?,?");

        // Pattern for double quotes
        Pattern patternDoubleQuotes = Pattern.compile("\\[?\"(.*)\"\\s*\\]?,?");

        try (BufferedReader reader = new BufferedReader(new FileReader(setupFile))) {
            String line;
            boolean isInstallRequiresSection = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();                
                
                // If after removing all whitespace the line starts with install_requires=
                // then we have found the section we are after.
                if (line.replaceAll("\\s+","").startsWith("install_requires=")) {
                    isInstallRequiresSection = true;
                    continue;
                }
                if (isInstallRequiresSection) {
                    // If the [ is on its own line skip it, it doesn't contain a dependency
                    if (line.equals("[")) {
                        continue;
                    }
                    
                    checkLineForDependency(line, patternSingleQuotes, patternDoubleQuotes);
                    
                    // If the line ends with ] or ], it means we have reached the end of the dependencies list.
                    if (line.endsWith("]") || line.endsWith("],")) {
                        break;
                    }
                }
            }
        }

        return dependencies;
    }

    public Map<String, List<String>> loadExtrasRequire(String setupFile) throws IOException {
        // Pattern for group key lines like "security": [ or 'http2': [
        Pattern groupKeyPattern = Pattern.compile("[\"'](.*?)[\"']\\s*:");

        try (BufferedReader reader = new BufferedReader(new FileReader(setupFile))) {
            String line;
            boolean isExtrasRequireSection = false;
            String currentGroup = null;

            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();

                if (trimmedLine.replaceAll("\\s+", "").startsWith("extras_require=")) {
                    isExtrasRequireSection = true;
                    continue;
                }

                if (!isExtrasRequireSection) {
                    continue;
                }

                if (trimmedLine.equals("{")) {
                    continue;
                }

                if (trimmedLine.equals("}") || trimmedLine.equals("},")) {
                    break;
                }

                currentGroup = processExtrasLine(trimmedLine, currentGroup, groupKeyPattern);
            }
        }

        return extrasRequireMap;
    }

    private String processExtrasLine(String trimmedLine, String currentGroup, Pattern groupKeyPattern) {
        Matcher groupMatcher = groupKeyPattern.matcher(trimmedLine);

        if (groupMatcher.find()) {
            currentGroup = groupMatcher.group(1);
            String afterColon = trimmedLine.substring(trimmedLine.indexOf(':') + 1).trim();
            addExtrasLineDeps(afterColon, currentGroup);
            if (afterColon.contains("]")) {
                return null;
            }
        } else if (currentGroup != null) {
            addExtrasLineDeps(trimmedLine, currentGroup);
            if (trimmedLine.endsWith("]") || trimmedLine.endsWith("],")) {
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
        Pattern patternDoubleQuotes = Pattern.compile("\"(.*?)\"");
        Pattern patternSingleQuotes = Pattern.compile("'(.*?)'");

        Matcher matcherDouble = patternDoubleQuotes.matcher(line);
        boolean found = false;
        while (matcherDouble.find()) {
            String value = matcherDouble.group(1);
            if (!value.isEmpty()) {
                results.add(value);
                found = true;
            }
        }
        if (!found) {
            Matcher matcherSingle = patternSingleQuotes.matcher(line);
            while (matcherSingle.find()) {
                String value = matcherSingle.group(1);
                if (!value.isEmpty()) {
                    results.add(value);
                }
            }
        }
        return results;
    }

    private void checkLineForDependency(String line, Pattern patternSingleQuotes, Pattern patternDoubleQuotes) {
        // Try double quotes first, then fall back to single quotes.
        // Double quotes are preferred as lines sometimes use double quotes with
        // single quotes inside them to specify conditionals.
        Matcher matcherDoubleQuotes = patternDoubleQuotes.matcher(line);
        if (matcherDoubleQuotes.find()) {
            dependencies.add(matcherDoubleQuotes.group(1));
        } else {
            Matcher matcherSingleQuotes = patternSingleQuotes.matcher(line);
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
