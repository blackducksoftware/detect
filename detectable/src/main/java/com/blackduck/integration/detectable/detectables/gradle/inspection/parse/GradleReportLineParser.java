package com.blackduck.integration.detectable.detectables.gradle.inspection.parse;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detectable.detectable.util.DetectableStringUtils;
import com.blackduck.integration.detectable.detectables.gradle.inspection.model.GradleTreeNode;

public class GradleReportLineParser {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String[] TREE_LEVEL_TERMINALS = new String[] { "+---", "\\---" };
    private static final String[] PROJECT_INDICATORS = new String[] { "--- project " };
    private static final String COMPONENT_PREFIX = "--- ";
    private static final String[] REMOVE_SUFFIXES = new String[] { " (*)", " (c)", " (n)" };
    private static final String WINNING_INDICATOR = " -> ";
    private static final String STRICTLY = "strictly";
    private static final String REQUIRE = "require";
    private static final String PREFER = "prefer";
    private static final String REJECT = "reject";

    // This map handles all the dependencies which uses rich version
    // declarations with the respect to the project they are declared in.
    // The nested map has dependencies with their versions and each
    // module will have its own nested map.
    private final Map<String, Map<String, String>> gradleRichVersions = new HashMap<>();

    // This map handles all the transitives whose parent uses rich version
    // declarations with the respect to the project they are declared in.
    // The nested map has dependencies with their versions and each
    // module will have its own nested map.
    private final Map<String, Map<String, String>> transitiveRichVersions = new HashMap<>();

    // This map is handling all the child-parent relationships which are found in the entire project
    // with each child as a key and their respective parent as the value.
    private final Map<String, String> relationsMap = new HashMap<>();
    private String richVersionProject = null;
    private boolean richVersionDeclared = false;
    private String projectName;
    private String rootProjectName;
    private String projectParent;
    private int level;
    public static final String PROJECT_NAME_PREFIX = "projectName:";
    public static final String ROOT_PROJECT_NAME_PREFIX = "rootProjectName:";
    public static final String PROJECT_PARENT_PREFIX = "projectParent:";

    public GradleTreeNode parseLine(String line, Map<String, String> metadata) {
        level = parseTreeLevel(line);
        if (!line.contains(COMPONENT_PREFIX)) {
            return GradleTreeNode.newUnknown(level);
        } else if (StringUtils.containsAny(line, PROJECT_INDICATORS)) {
            String subProjectName = extractSubProjectName(line);
            return GradleTreeNode.newProject(level, subProjectName);
        } else {
            List<String> gav = parseGav(line, metadata);
            if (gav.size() != 3) {
                logger.trace(String.format(
                    "The line can not be reasonably split in to the necessary parts: %s",
                    line
                )); //All project lines: +--- org.springframework.boot:spring-boot-starter-activemq (n)
                return GradleTreeNode.newUnknown(level);
            } else {
                String group = gav.get(0);
                String name = gav.get(1);
                String version = gav.get(2);
                return GradleTreeNode.newGav(level, group, name, version);
            }
        }
    }

    private String extractSubProjectName(String line) {
        // A subProject dependency line looks exactly like: "+--- project :subProjectName" where subProjectName can
        // be a nested subProject (for example  "+--- project :subProjectA:nestedSubProjectB:furtherNestedSubProjectC")
        try {
            String[] parts = line.split(PROJECT_INDICATORS[0]);
            String subprojName = parts[1].trim();
            if (subprojName.startsWith(":")) {
                // Drop the leading ":"
                subprojName = subprojName.substring(1);
                // In a Gradle dependencies tree, dependencies listed previously will have a " (*)" suffix
                subprojName = removeSuffixes(subprojName);
                return subprojName;
            }
            // line didn't look as we expected
            return "";
        } catch (IndexOutOfBoundsException e) {
            logger.debug("Could not extract subProject name from Gradle dependency tree report.");
            return "";
        }
    }

    private String removeSuffixes(String line) {
        for (String suffix : REMOVE_SUFFIXES) {
            if (line.endsWith(suffix)) {
                int lastSeenElsewhereIndex = line.lastIndexOf(suffix);
                line = line.substring(0, lastSeenElsewhereIndex);
            }
        }
        return line;
    }

    private List<String> parseGav(String line, Map<String, String> metadata) {
        String cleanedOutput = StringUtils.trimToEmpty(line);
        cleanedOutput = cleanedOutput.substring(cleanedOutput.indexOf(COMPONENT_PREFIX) + COMPONENT_PREFIX.length());

        cleanedOutput = removeSuffixes(cleanedOutput);

        // we might need to modify the returned list, so it needs to be an actual ArrayList
        List<String> gavPieces = new ArrayList<>(Arrays.asList(cleanedOutput.split(":")));
        if (cleanedOutput.contains(WINNING_INDICATOR)) {
            // WINNING_INDICATOR can point to an entire GAV not just a version
            String winningSection = cleanedOutput.substring(cleanedOutput.indexOf(WINNING_INDICATOR) + WINNING_INDICATOR.length());
            if (winningSection.contains(":")) {
                gavPieces = Arrays.asList(winningSection.split(":"));
            } else {
                // the WINNING_INDICATOR is not always preceded by a : so if isn't, we need to clean up from the original split
                if (gavPieces.get(1).contains(WINNING_INDICATOR)) {
                    String withoutWinningIndicator = gavPieces.get(1).substring(0, gavPieces.get(1).indexOf(WINNING_INDICATOR));
                    gavPieces.set(1, withoutWinningIndicator);
                    // since there was no : we don't have a gav piece for version yet
                    gavPieces.add("");
                }
                gavPieces.set(2, winningSection);
            }
        }

        projectName = metadata.getOrDefault(PROJECT_NAME_PREFIX, "orphanProject");
        rootProjectName = metadata.getOrDefault(ROOT_PROJECT_NAME_PREFIX, "");
        projectParent = metadata.getOrDefault(PROJECT_PARENT_PREFIX, "null");

        addRelation();

        if(gavPieces.size() == 3) {
            String dependencyGroupName = gavPieces.get(0) + ":" + gavPieces.get(1);
            if(level == 0 && checkRichVersionUse(cleanedOutput)) {
                storeDirectRichVersion(dependencyGroupName, gavPieces);
            } else {
                storeOrUpdateRichVersion(dependencyGroupName, gavPieces);
            }
        }

        return gavPieces;
    }

    private void storeDirectRichVersion(String dependencyGroupName, List<String> gavPieces) {
        gradleRichVersions.computeIfAbsent(projectName, value -> new HashMap<>()).putIfAbsent(dependencyGroupName, gavPieces.get(2));
        richVersionProject = projectName;
        richVersionDeclared = true;
    }

    private void storeOrUpdateRichVersion(String dependencyGroupName, List<String> gavPieces) {
        if (checkParentRichVersion(dependencyGroupName)) {
            gavPieces.set(2, gradleRichVersions.get(richVersionProject).get(dependencyGroupName));
        } else if(checkIfTransitiveRichVersion() && transitiveRichVersions.containsKey(richVersionProject) && transitiveRichVersions.get(richVersionProject).containsKey(dependencyGroupName)) {
            gavPieces.set(2, transitiveRichVersions.get(richVersionProject).get(dependencyGroupName));
        } else if (checkIfTransitiveRichVersion() && richVersionDeclared) {
            transitiveRichVersions.computeIfAbsent(richVersionProject, value -> new HashMap<>()).putIfAbsent(dependencyGroupName, gavPieces.get(2));
        } else {
            richVersionDeclared = false;
            richVersionProject = null;
        }
    }

    private void addRelation() {
        if (!projectParent.equals("null") && !projectParent.contains("root project")) {
            String parentString = projectParent.substring(projectParent.lastIndexOf(":") + 1, projectParent.lastIndexOf("'"));
            relationsMap.putIfAbsent(projectName, parentString);
        } else if (!projectParent.equals("null") && !projectName.equals(rootProjectName)) {
            relationsMap.putIfAbsent(projectName, rootProjectName);
        } else {
            relationsMap.putIfAbsent(rootProjectName, null);
        }
    }

    private boolean checkParentRichVersion(String dependencyGroupName) {
        String currentProject = projectName;
        while (currentProject != null) {
            if (gradleRichVersions.containsKey(currentProject) && gradleRichVersions.get(currentProject).containsKey(dependencyGroupName)) {
                richVersionProject = currentProject;
                return true;
            }
            currentProject = relationsMap.getOrDefault(currentProject, null);
        }
       return false;
    }

    private boolean checkRichVersionUse(String dependencyLine) {
        return dependencyLine.contains(STRICTLY) || dependencyLine.contains(REJECT) || dependencyLine.contains(REQUIRE) || dependencyLine.contains(PREFER);
    }

    private boolean checkIfTransitiveRichVersion() {
       return richVersionProject != null && level != 0;
    }

    private int parseTreeLevel(String line) {
        if (StringUtils.startsWithAny(line, TREE_LEVEL_TERMINALS)) {
            return 0;
        }

        String modifiedLine = DetectableStringUtils.removeEvery(line, TREE_LEVEL_TERMINALS);

        if (!modifiedLine.startsWith("|") && modifiedLine.startsWith(" ")) {
            modifiedLine = "|" + modifiedLine;
        }
        modifiedLine = modifiedLine.replace("     ", "    |");
        modifiedLine = modifiedLine.replace("||", "|");
        if (modifiedLine.endsWith("|")) {
            modifiedLine = modifiedLine.substring(0, modifiedLine.length() - 5);
        }

        return StringUtils.countMatches(modifiedLine, "|");
    }

}
