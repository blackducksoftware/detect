package com.blackduck.integration.detectable.detectables.opam.parse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class OpamFileParser {

    private static final String VERSION = "version";
    private static final String DEPENDS = "depends";
    private static final String NAME = "name";
    private boolean inVersionSection = false;
    private boolean inDependsSection = false;

    //have a separate parseData and parse method to use for parsing opam show command, as it gives output in file format
    public OpamParsedResult parse(File opamFile) {
        List<String> parsedOpamDependencies = new ArrayList<>();
        String projectName = "";
        String projectVersion = "";

        try (BufferedReader reader = new BufferedReader(new FileReader(opamFile))) {


            List<String> fileData = reader.lines().collect(Collectors.toList());

            Map<String, String> parsedOutput = parseData(fileData);

            if(parsedOutput.containsKey(VERSION)) {
                projectVersion = parsedOutput.get(VERSION); // get the version from parsed file
            }

            if(parsedOutput.containsKey(NAME)) {
                projectName = parsedOutput.get(NAME); // get the project name from parsed File
            }

            if(parsedOutput.containsKey(DEPENDS)) {
                parsedOpamDependencies = Arrays.asList(parsedOutput.get(DEPENDS).split(", ")); // get the depends section from parsed file
            }

        } catch (IOException e) {
            throw new RuntimeException("There was an error while parsing the opam file.", e);
        }

        return new OpamParsedResult(projectName, projectVersion, parsedOpamDependencies, opamFile);
    }

    public Map<String, String> parseData(List<String> lines) {
        Map<String, String> output = new HashMap<>();
        Set<String> dependsSection = new HashSet<>();
        Pattern pattern = Pattern.compile("\"([^\"]+)\"");


        for (String line : lines) {
            line = line.trim();
            // if the line contains os or system level dependencies skip them
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("{?") || line.contains("cygwin")) {
                continue;
            }

            if (line.startsWith("version:")) {
                //parse version
                addProjectNameAndVersion(line, output, pattern, VERSION);
            }

            if (line.startsWith("name:")) {
                // parse project name
                addProjectNameAndVersion(line, output, pattern, NAME);
            }

            checkDependsSection(line, dependsSection, pattern);

            if (inDependsSection) {
                addDependencyToList(line, dependsSection, pattern); // add dependency to list
            }
        }

        if(!dependsSection.isEmpty()) {
            String dependencies = String.join(", ", dependsSection); // put dependencies in single line to generate a map
            output.put(DEPENDS, dependencies);
        }

        return output;
    }

    private void addProjectNameAndVersion(String line, Map<String, String> output, Pattern pattern, String parsingValue) {
        String value = line.split(":")[1];
        Matcher matcher = pattern.matcher(value);
        if (matcher.find()) {
            value = matcher.group(1);
        }
        output.put(parsingValue, value);
    }

    private void checkDependsSection(String line, Set<String> dependsSection, Pattern pattern) {
        // parse depends section
        // sometimes, depends contains dependencies in one line, so we have to parse them differently
        // than normal opam files. Eg: depends: ["ocaml" "dune"], otherwise all the dependencies are present in different lines
        if (line.startsWith("depends:")) {
            inDependsSection = true;
            if(line.contains("]")) {
                handleSameLineDependencies(line, dependsSection, pattern);
                inDependsSection = false;
            }
        } else if (inDependsSection && line.startsWith("]")) {
            inDependsSection = false;
        }
    }

    //parse the opam depends line as Eg: depends: ["ocaml" "dune"]
    private void handleSameLineDependencies(String line, Set<String> dependsSection, Pattern pattern) {
        int beginIndex = line.indexOf("[") + 1;
        int endIndex = line.indexOf("]") - 1;
        String dependenciesInLine = line.substring(beginIndex, endIndex);

        String[] dependenciesByParts = dependenciesInLine.split(" ");
        for(String dependency: dependenciesByParts) {
           matchDependencyPattern(pattern, dependsSection, dependency);
        }
    }

    private void matchDependencyPattern(Pattern pattern, Set<String> dependsSection, String line) {
        // Match the name of the package with the pattern which is eg: "package-name" {>=version}
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String packageName = matcher.group(1);
            if(!packageName.equals("cc") && !packageName.equals("arm64") && !packageName.equals("win32") && !packageName.equals("x86_64")) {
                dependsSection.add(packageName);
            }
        }
    }

    private void addDependencyToList(String line, Set<String> dependsSection, Pattern pattern) {
        int openIndex = line.indexOf("{"); // used to represent version in opam files
        int closeIndex = line.indexOf("}");
        int openRoundIndex = line.indexOf("(");
        int closeRoundIndex = line.indexOf(")");
        int openDoubleRoundIndex = line.indexOf("((");
        int closeDoubleRoundIndex = line.indexOf("))");

        if ((openIndex != -1 && closeIndex == -1) || (openRoundIndex != -1 && closeRoundIndex == -1) || (openDoubleRoundIndex != -1 && closeDoubleRoundIndex == -1)) { // check if the version line ends, as sometimes it may span across multiple lines
            inVersionSection = true;
        }

        if (!inVersionSection) {
            matchDependencyPattern(pattern, dependsSection, line);
        }

        if (inVersionSection && openIndex == -1 && closeIndex != -1) {
            inVersionSection = false;
        }

        if (inVersionSection && openRoundIndex == -1 && closeRoundIndex != -1) {
            inVersionSection = false;
        }

        if (inVersionSection && openDoubleRoundIndex == -1 && closeDoubleRoundIndex != -1) {
            inVersionSection = false;
        }
    }
}
