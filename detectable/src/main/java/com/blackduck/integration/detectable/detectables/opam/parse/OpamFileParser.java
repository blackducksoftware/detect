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

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class OpamFileParser {

    private static final String VERSION = "version";
    private static final String DEPENDS = "depends";
    private static final String NAME = "name";

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
        Pattern pattern = Pattern.compile("\"([^\"]+)\"");
        OpamDependsScanner dependsScanner = new OpamDependsScanner();

        for (String line : lines) {
            if (dependsScanner.isActive()) {
                dependsScanner.consume(line); // continuation of a multi-line depends block
                continue;
            }

            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
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

            if (line.startsWith("depends:")) {
                dependsScanner.start();
                dependsScanner.consume(line.substring("depends:".length()));
            }
        }

        Set<String> dependsSection = dependsScanner.getDependencies();
        if (!dependsSection.isEmpty()) {
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
}
