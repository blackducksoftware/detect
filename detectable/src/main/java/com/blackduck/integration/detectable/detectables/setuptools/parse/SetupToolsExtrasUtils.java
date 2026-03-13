package com.blackduck.integration.detectable.detectables.setuptools.parse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.blackduck.integration.detectable.python.util.PythonDependency;
import com.blackduck.integration.detectable.python.util.PythonDependencyTransformer;

public class SetupToolsExtrasUtils {

    private static final char OPEN_BRACKET = '[';
    private static final char CLOSE_BRACKET = ']';
    private static final String EXTRAS_DELIMITER = ",";

    private SetupToolsExtrasUtils() {}

    /**
     * Extracts the extras specifier names from a raw dependency string.
     * For example, "requests[security,socks]==2.28.2" returns ["security", "socks"].
     * Returns an empty list if no extras specifier is present.
     */
    public static List<String> extractExtrasNames(String rawDep) {
        List<String> names = new ArrayList<>();
        int openBracket = rawDep.indexOf(OPEN_BRACKET);
        int closeBracket = rawDep.indexOf(CLOSE_BRACKET);
        if (openBracket >= 0 && closeBracket > openBracket) {
            String extrasContent = rawDep.substring(openBracket + 1, closeBracket);
            for (String name : extrasContent.split(EXTRAS_DELIMITER)) {
                String trimmed = name.trim();
                if (!trimmed.isEmpty()) {
                    names.add(trimmed);
                }
            }
        }
        return names;
    }

    /**
     * Builds a map of base package name to transitive dependencies by matching
     * extras specifiers in raw dependency lines against the extras group map.
     *
     * @param rawDependencyLines the raw dependency strings (e.g., "requests[security]==2.28.2")
     * @param extrasGroupMap map of extras group name to list of dependency strings in that group
     * @return map of base package name to list of transitive PythonDependency objects
     */
    public static Map<String, List<PythonDependency>> buildExtrasTransitives(
            List<String> rawDependencyLines,
            Map<String, List<String>> extrasGroupMap) {

        Map<String, List<PythonDependency>> extrasTransitives = new HashMap<>();
        PythonDependencyTransformer dependencyTransformer = new PythonDependencyTransformer();

        for (String rawDep : rawDependencyLines) {
            List<String> extrasNames = extractExtrasNames(rawDep);
            if (extrasNames.isEmpty()) {
                continue;
            }

            // Extract the base package name (everything before '[')
            int bracketIndex = rawDep.indexOf(OPEN_BRACKET);
            String baseName = rawDep.substring(0, bracketIndex).trim();

            resolveExtrasForDependency(baseName, extrasNames, extrasGroupMap, extrasTransitives, dependencyTransformer);
        }

        return extrasTransitives;
    }

    private static void resolveExtrasForDependency(
            String baseName,
            List<String> extrasNames,
            Map<String, List<String>> extrasGroupMap,
            Map<String, List<PythonDependency>> extrasTransitives,
            PythonDependencyTransformer dependencyTransformer) {

        for (String extrasName : extrasNames) {
            List<String> groupLines = extrasGroupMap.get(extrasName);
            if (groupLines == null) {
                continue;
            }
            List<PythonDependency> transitives = extrasTransitives.computeIfAbsent(baseName, k -> new LinkedList<>());
            for (String transitiveLine : groupLines) {
                PythonDependency dep = dependencyTransformer.transformLine(transitiveLine);
                if (dep != null) {
                    transitives.add(dep);
                }
            }
        }
    }
}

