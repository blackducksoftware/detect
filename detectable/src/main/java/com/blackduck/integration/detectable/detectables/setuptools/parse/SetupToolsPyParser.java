package com.blackduck.integration.detectable.detectables.setuptools.parse;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import org.tomlj.TomlParseResult;

import com.blackduck.integration.detectable.python.util.PythonDependency;
import com.blackduck.integration.detectable.python.util.PythonDependencyTransformer;

public class SetupToolsPyParser implements SetupToolsParser {
    private static final Pattern INSTALL_REQUIRES_PATTERN = Pattern.compile(".*\\binstall_requires\\s*=\\s*(.*)$");
    private static final Pattern QUOTED_DEPENDENCY_PATTERN = Pattern.compile("'([^'\\\\]*(?:\\\\.[^'\\\\]*)*)'|\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"");
    private TomlParseResult parsedToml;
    private List<String> dependencies;
    
    public SetupToolsPyParser(TomlParseResult parsedToml) {
        this.parsedToml = parsedToml;
        this.dependencies = new ArrayList<>();
    }
    
    @Override
    public SetupToolsParsedResult parse() throws IOException {
        // Use a name from the toml if we have it. Do not parse names and versions from the setup.py
        // as the project will not always have a string (it could have variables or method calls)
        String tomlProjectName = parsedToml.getString("project.name");
        String projectVersion = parsedToml.getString("project.version");
        
        List<PythonDependency> parsedDirectDependencies = parseDirectDependencies();
        
        return new SetupToolsParsedResult(tomlProjectName, projectVersion, parsedDirectDependencies);
    }

    public List<String> load(String setupFile) throws IOException, DetectableException {
        dependencies.clear();

        try (BufferedReader reader = new BufferedReader(new FileReader(setupFile))) {
            String line;
            boolean foundInstallRequires = false;
            boolean inList = false;
            boolean listClosed = false;

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                if (!foundInstallRequires) {
                    String codePortion = stripComment(line);
                    Matcher installRequiresMatcher = INSTALL_REQUIRES_PATTERN.matcher(codePortion);
                    if (!installRequiresMatcher.matches()) {
                        continue;
                    }

                    foundInstallRequires = true;
                    String afterEquals = installRequiresMatcher.group(1).trim();

                    if (afterEquals.isEmpty()) {
                        continue;
                    }

                    if (!afterEquals.startsWith("[")) {
                        throw unsupportedInstallRequiresException();
                    }

                    inList = true;
                    listClosed = parseListLine(afterEquals);
                    if (listClosed) {
                        break;
                    }
                    continue;
                }

                if (!inList) {
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    if (trimmed.startsWith("[")) {
                        inList = true;
                        listClosed = parseListLine(trimmed);
                        if (listClosed) {
                            break;
                        }
                        continue;
                    }
                    throw unsupportedInstallRequiresException();
                }

                listClosed = parseListLine(trimmed);
                if (listClosed) {
                    break;
                }
            }

            if (foundInstallRequires && (!inList || !listClosed)) {
                throw unsupportedInstallRequiresException();
            }
        }

        return dependencies;
    }

    private boolean parseListLine(String line) throws DetectableException {
        int closingBracketIndex = line.indexOf(']');
        String relevantPortion = closingBracketIndex >= 0 ? line.substring(0, closingBracketIndex + 1) : line;

        Matcher quotedDependencyMatcher = QUOTED_DEPENDENCY_PATTERN.matcher(relevantPortion);
        while (quotedDependencyMatcher.find()) {
            String dependency = quotedDependencyMatcher.group(1) != null ? quotedDependencyMatcher.group(1) : quotedDependencyMatcher.group(2);
            dependencies.add(dependency);
        }

        String residue = QUOTED_DEPENDENCY_PATTERN.matcher(relevantPortion).replaceAll("");
        int commentIndex = residue.indexOf('#');
        if (commentIndex >= 0) {
            residue = residue.substring(0, commentIndex);
        }
        residue = residue.replace("[", "").replace("]", "").replace(",", "").trim();

        if (!residue.isEmpty()) {
            throw unsupportedInstallRequiresException();
        }

        return closingBracketIndex >= 0;
    }

    private String stripComment(String line) {
        int commentIndex = line.indexOf('#');
        if (commentIndex >= 0) {
            return line.substring(0, commentIndex);
        }
        return line;
    }

    private DetectableException unsupportedInstallRequiresException() {
        return new DetectableException(
            "install_requires must be a literal Python list (e.g., install_requires=['dep1', 'dep2']). Variable references, function calls, and other programmatic constructions are not supported."
        );
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
