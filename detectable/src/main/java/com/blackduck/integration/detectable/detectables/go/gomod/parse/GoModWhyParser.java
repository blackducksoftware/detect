package com.blackduck.integration.detectable.detectables.go.gomod.parse;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class GoModWhyParser {
    public static final String[] UNUSED_MODULE_PREFIXES = new String[] {
        "(main module does not need module",
        "(main module does not need to vendor module"
    };
    private static final String[] UNUSED_MODULE_REPLACEMENTS = new String[] { "", "" };

    public Set<String> createModuleExclusionList(List<String> lines) {
        // find lines that look like the following and extract the module name i.e. cloud.google.com/go:
        // (main module does not need module cloud.google.com/go)
        return lines.stream()
            .map(String::trim)
            .filter(line -> StringUtils.startsWithAny(line, UNUSED_MODULE_PREFIXES))
            .map(line -> StringUtils.replaceEach(line, UNUSED_MODULE_PREFIXES, UNUSED_MODULE_REPLACEMENTS))
            .map(line -> StringUtils.removeEnd(line, ")"))
            .map(String::trim)
            .collect(Collectors.toSet());
    }
}
