package com.blackduck.integration.detectable.detectables.gradle.inspection.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.blackduck.integration.detectable.detectables.gradle.inspection.model.GradleTreeNode;
import org.apache.commons.lang3.StringUtils;

import com.blackduck.integration.detectable.detectables.gradle.inspection.model.GradleConfiguration;

public class GradleReportConfigurationParser {
    private static final String UNRESOLVED_SUFFIX = "(n)";

    private final GradleReportLineParser parser = new GradleReportLineParser();

    public GradleConfiguration parse(String header, List<String> dependencyLines, Map<String, String> metadata) {
        GradleConfiguration configuration = new GradleConfiguration();

        configuration.setName(parseConfigurationName(header));
        configuration.setUnresolved(parseUnresolved(header));
        List<GradleTreeNode> children = new ArrayList<>();
        for (String line: dependencyLines) {
            if (!line.trim().endsWith("(c)")) {
                children.add(parser.parseLine(line, metadata));
            }
        }
        configuration.setChildren(children);

        return configuration;
    }

    private boolean parseUnresolved(String header) {
        return StringUtils.endsWith(header, UNRESOLVED_SUFFIX);
    }

    private String parseConfigurationName(String header) {
        if (header.contains(" - ")) {
            return header.substring(0, header.indexOf(" - ")).trim();
        } else {
            return header.trim();
        }
    }

}
