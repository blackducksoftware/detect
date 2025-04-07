package com.blackduck.integration.detectable.detectables.go.gomod.parse;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detectable.detectables.go.gomod.model.GoGraphRelationship;
import com.blackduck.integration.util.NameVersion;

public class GoGraphParser {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public List<GoGraphRelationship> parseRelationshipsFromGoModGraph(Set<String> goModGraphOutput) {
        List<GoGraphRelationship> goGraphRelationships = new LinkedList<>();
        for (String line : goModGraphOutput) {
            //example: github.com/gomods/athens cloud.google.com/go@v0.26.0
            Optional<GoGraphRelationship> goGraphRelationship = parseLine(line);
            goGraphRelationship.ifPresent(goGraphRelationships::add);
        }

        return goGraphRelationships;
    }

    private Optional<GoGraphRelationship> parseLine(String line) {
        String[] parts = line.split(" ");
        if (parts.length != 2) {
            logger.warn("Unknown graph line format: {}", line);
            return Optional.empty();
        }

        NameVersion parent = extractNameVersion(parts[0]);
        NameVersion child = extractNameVersion(parts[1]);

        return Optional.of(new GoGraphRelationship(parent, child));
    }

    private NameVersion extractNameVersion(String dependency) {
        if (dependency.contains("@")) {
            String[] parts = dependency.split("@");
            if (parts.length != 2) {
                logger.warn("Unknown graph dependency format, using entire entry as name: {}", dependency);
                return new NameVersion(dependency, null);
            } else {
                String name = parts[0];
                String version = parts[1];
                return new NameVersion(name, version);
            }
        } else {
            return new NameVersion(dependency, null);
        }
    }
}
