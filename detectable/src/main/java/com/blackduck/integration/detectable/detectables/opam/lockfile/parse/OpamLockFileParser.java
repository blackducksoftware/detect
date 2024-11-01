package com.blackduck.integration.detectable.detectables.opam.lockfile.parse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;

public class OpamLockFileParser {

    private final Map<String, String> parsedLockedOpamDependencies = new HashMap<>();
    private final List<File> opamLockedFiles;

    public OpamLockFileParser(List<File> opamLockedFiles) {
        this.opamLockedFiles = opamLockedFiles;
    }

    public Map<String, String> parse() {

        for (File lockFile : opamLockedFiles) {
            readOpamLockFile(lockFile);
        }

        return parsedLockedOpamDependencies;
    }

    private void readOpamLockFile(File opamFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(opamFile))) {
            String line;
            // parse lock file dependencies with package name and version Eg: "package" {="version"}
            Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*\\{([^}]*)\\}");
            boolean inDependsSection = false;

            while ((line = reader.readLine()) != null) {
                // lock file will have resolved dependencies so there will be no exceptions as regular opam file
                if(line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if(line.startsWith("depends:")) {
                    inDependsSection = true;
                } else if(inDependsSection && line.startsWith("]")) {
                    inDependsSection = false;
                }

                if(inDependsSection) {
                    Matcher matcher = pattern.matcher(line);
                    while(matcher.find()) {
                        // match package and version with the line found
                        String packageName = matcher.group(1);
                        String version = matcher.group(2);
                        if(version.contains("=")) {
                            version = version.replace("= ",""); // remove = from version
                        }
                        parsedLockedOpamDependencies.put(packageName, version);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("There was an error while parsing the opam lock file.", e);
        }
    }

}
