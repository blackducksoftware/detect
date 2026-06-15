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

            while (!listClosed && (line = reader.readLine()) != null) {
                String trimmed = line.trim();

                if (!foundInstallRequires) {
                    ParsedInstallRequires parsed = findInstallRequires(line);
                    if (parsed != null && parsed.afterEquals != null) {
                        foundInstallRequires = true;
                        inList = true;
                        listClosed = parseListLine(parsed.afterEquals);
                    } else if (parsed != null) {
                        foundInstallRequires = true;
                    }
                } else if (!inList) {
                    inList = !trimmed.isEmpty();
                    listClosed = inList && beginList(trimmed);
                } else {
                    listClosed = parseListLine(trimmed);
                }
            }

            if (foundInstallRequires && (!inList || !listClosed)) {
                throw unsupportedInstallRequiresException();
            }
        }

        return dependencies;
    }

    /**
     * Attempts to find install_requires on the given line.
     * Returns null if not found. Returns a result with afterEquals=null if the value
     * continues on the next line. Throws if the value is not a literal list.
     */
    private ParsedInstallRequires findInstallRequires(String line) throws DetectableException {
        String codePortion = stripComment(line);
        Matcher matcher = INSTALL_REQUIRES_PATTERN.matcher(codePortion);
        if (!matcher.matches()) {
            return null;
        }

        String afterEquals = matcher.group(1).trim();
        if (afterEquals.isEmpty()) {
            return new ParsedInstallRequires(null);
        }
        if (!afterEquals.startsWith("[")) {
            throw unsupportedInstallRequiresException();
        }
        return new ParsedInstallRequires(afterEquals);
    }

    /**
     * Handles lines after install_requires was found but before the list opening '[' is seen.
     * Returns true if the list was fully closed on this line.
     */
    private boolean beginList(String trimmedLine) throws DetectableException {
        if (trimmedLine.isEmpty()) {
            return false;
        }
        if (!trimmedLine.startsWith("[")) {
            throw unsupportedInstallRequiresException();
        }
        return parseListLine(trimmedLine);
    }

    private static class ParsedInstallRequires {
        final String afterEquals;

        ParsedInstallRequires(String afterEquals) {
            this.afterEquals = afterEquals;
        }
    }

    private boolean parseListLine(String line) throws DetectableException {
        Matcher quotedDependencyMatcher = QUOTED_DEPENDENCY_PATTERN.matcher(line);
        while (quotedDependencyMatcher.find()) {
            String dependency = quotedDependencyMatcher.group(1) != null ? quotedDependencyMatcher.group(1) : quotedDependencyMatcher.group(2);
            dependencies.add(dependency);
        }

        String residue = QUOTED_DEPENDENCY_PATTERN.matcher(line).replaceAll("");
        int commentIndex = residue.indexOf('#');
        if (commentIndex >= 0) {
            residue = residue.substring(0, commentIndex);
        }
        int closingBracketIndex = residue.indexOf(']');

        // Only validate content before the closing bracket. Anything after ']'
        // (e.g. the ')' from 'setup(...)') is surrounding Python code we should ignore.
        String toValidate = closingBracketIndex >= 0 ? residue.substring(0, closingBracketIndex) : residue;
        toValidate = toValidate.replace("[", "").replace(",", "").trim();

        if (!toValidate.isEmpty()) {
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
            if (dependency != null) {
                if (dependencyLine.contains(";")) {
                    dependency.setConditional(true);
                }
                results.add(dependency);
            }
        }
        
        return results;
    }
}
