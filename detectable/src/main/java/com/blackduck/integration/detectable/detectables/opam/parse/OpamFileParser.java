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
import java.util.LinkedHashSet;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpamFileParser {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
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
        Set<String> dependsSection = new LinkedHashSet<>();
        OpamLexicalScanner scanner = new OpamLexicalScanner(lines);
        String field;
        while ((field = scanner.nextField()) != null) {
            scanner.skipSpacesAndComments();
            if (DEPENDS.equals(field)) {
                OpamDependencyNameParser.Result result = OpamDependencyNameParser.read(scanner);
                if (result.isDependsValue()) {
                    dependsSection.addAll(result.getPackageNames());
                } else {
                    logger.debug("Ignoring a depends value that is not a list of opam packages.");
                }
            } else if ((NAME.equals(field) || VERSION.equals(field)) && scanner.peek() == OpamSyntax.QUOTE) {
                scanner.advance();
                String value = scanner.readString();
                if (value != null) {
                    output.put(field, value);
                }
            } else {
                scanner.skipValue();
            }
        }

        if(!dependsSection.isEmpty()) {
            String dependencies = String.join(", ", dependsSection); // put dependencies in single line to generate a map
            output.put(DEPENDS, dependencies);
        }

        return output;
    }
}
