package com.blackduck.integration.detectable.detectables.sbt.dot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SbtDotOutputParser {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public List<File> parseGeneratedGraphFiles(List<String> dotOutput) {
        List<File> graphs = new ArrayList<>();

        for (String line : dotOutput) {
            String potentialFile = parseDotGraphFromLine(line);
            if (potentialFile != null) {
                logger.debug("Found graph: " + potentialFile);
                graphs.add(new File(potentialFile));
            }
        }
        if (graphs.size() == 0) {
            logger.warn("Sbt found no graphs! This may be an issue with your project.");
        }
        return graphs;
    }

    @Nullable
    private String parseDotGraphFromLine(String line) {
        final String DOT_PREFIX = "Wrote dependency graph to '";
        if (line.contains(DOT_PREFIX)) {
            final String DOT_SUFFIX = "'";
            return StringUtils.substringBetween(line, DOT_PREFIX, DOT_SUFFIX);
        } else {
            return null;
        }
    }
}
