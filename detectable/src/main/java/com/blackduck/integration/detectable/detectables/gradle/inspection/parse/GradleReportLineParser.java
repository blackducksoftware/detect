package com.blackduck.integration.detectable.detectables.gradle.inspection.parse;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;


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
    // tracks the project where rich version was declared
    private String richVersionProject = null;
    // tracks if rich version was declared while parsing the previous line
    private boolean richVersionDeclared = false;
    private String projectName;
    private String rootProjectName;
    private String projectPath;
    private int level;
    private String depthNumber;
    public static final String PROJECT_NAME_PREFIX = "projectName:";
    public static final String ROOT_PROJECT_NAME_PREFIX = "rootProjectName:";
    public static final String PROJECT_PATH_PREFIX = "projectPath:";
    public static final String FILE_NAME_PREFIX = "fileName:";

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
                String version = StringUtils.substringBefore(gav.get(2), " ");
                return GradleTreeNode.newGav(level, group, name, version);
            }
        }
    }

    private String extractSubProjectName(String line) {
        // A subProject dependency line looks exactly like: "+--- project :subProjectName" where subProjectName can
        // be a nested subProject (for example  "+--- project :subProjectA:nestedSubProjectB:furtherNestedSubProjectC")
        String[] parts = line.split(PROJECT_INDICATORS[0]);
        if (parts.length == 2) {
            // line looks as expected
            String subprojName = parts[1].trim();
            if (subprojName.startsWith(":")) {
                // Drop the leading ":"
                subprojName = subprojName.substring(1);
                // In a Gradle dependencies tree, dependencies listed previously will have a " (*)" suffix
                subprojName = removeSuffixes(subprojName);
                return subprojName;
            }
        }
        // line didn't look as we expected
        logger.debug("Could not extract subProject name from Gradle dependency tree report for line: " + line);
        return "";
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

        projectName = metadata.getOrDefault(PROJECT_NAME_PREFIX, "orphanProject"); // get project name from metadata
        rootProjectName = metadata.getOrDefault(ROOT_PROJECT_NAME_PREFIX, "")+"_0"; // get root project name
        String fileName = metadata.getOrDefault(FILE_NAME_PREFIX, "");
        projectPath = metadata.getOrDefault(PROJECT_PATH_PREFIX, ""); // get project path Eg: :sub:foo


        // To avoid a bug caused by an edge case where child and parent modules have the same name causing the loop for checking rich version to stuck
        // in an infinite state, we are going to suffix the name of the project with the depth number
        if(fileName != null && !fileName.isEmpty()) {
            int s = fileName.lastIndexOf("depth") + 5; // File name is like project__projectname__depth3_dependencyGraph.txt, we extract the number after depth
            int e = fileName.indexOf("_dependencyGraph");
            depthNumber = fileName.substring(s, e);
            projectName = projectName + "_" + depthNumber;
        }

        // Example of dependency using rich version:
        // --- com.graphql-java:graphql-java:{strictly [21.2, 21.3]; prefer 21.3; reject [20.6, 19.5, 18.2]} -> 21.3 direct depenendency, will be stored in rich versions, richVersionProject value will be current project
        //        +--- com.graphql-java:java-dataloader:3.2.1 transitive needs to be stored
        //          |    \--- org.slf4j:slf4j-api:1.7.30 -> 2.0.4 transitive needs to be stored

        if(gavPieces.size() == 3) {
            String dependencyGroupName = gavPieces.get(0) + ":" + gavPieces.get(1);
            if(level == 0 && checkRichVersionUse(cleanedOutput)) { // we only track rich versions if they are declared in direct dependencies
                storeDirectRichVersion(dependencyGroupName, gavPieces);
            } else {
                storeOrUpdateRichVersion(dependencyGroupName, gavPieces);
            }
        }

        return gavPieces;
    }

    // store the dependency where rich version was declared and update the global tracking values
    private void storeDirectRichVersion(String dependencyGroupName, List<String> gavPieces) {
        gradleRichVersions.computeIfAbsent(projectName, value -> new HashMap<>()).putIfAbsent(dependencyGroupName, gavPieces.get(2));
        richVersionProject = projectName;
        richVersionDeclared = true;
    }

    private void storeOrUpdateRichVersion(String dependencyGroupName, List<String> gavPieces) {
        // this condition is checking for rich version use for current direct dependency in one of the parent submodule of the current module and updates the current version
        if (checkParentRichVersion(dependencyGroupName)) {
            gavPieces.set(2, gradleRichVersions.get(richVersionProject).get(dependencyGroupName));
        } else if(checkIfTransitiveRichVersion() && transitiveRichVersions.containsKey(richVersionProject) && transitiveRichVersions.get(richVersionProject).containsKey(dependencyGroupName)) {
            // this is checking if we are parsing a transitive dependency and that transitive
            // dependency has already been memoized for the use of rich version
            gavPieces.set(2, transitiveRichVersions.get(richVersionProject).get(dependencyGroupName));
        } else if (checkIfTransitiveRichVersion() && richVersionDeclared) {
            // if while parsing the last direct dependency, we found the use of rich version, we store the version resolved for this transitive dependency
            transitiveRichVersions.computeIfAbsent(richVersionProject, value -> new HashMap<>()).putIfAbsent(dependencyGroupName, gavPieces.get(2));
        } else {
            // no use of rich versions found
            richVersionDeclared = false;
            richVersionProject = null;
        }
    }

    private boolean checkParentRichVersion(String dependencyGroupName) {
        // this method checks all the parent modules for the current submodule upto rootProject for the use of the rich version for the current dependency
        // if the rich version is used return true and update the richVersionProject
        // We will check if rich version was declared in root project, if yes immediately apply it, otherwise parse the whole project path for the current submodule
        // path will start from level 1 Eg: :sub:foo, we will check dependency in :sub_1 first foo_2 next where the name is similar to project name we put in the gradle Rich versions map.
        //Eg: if sub declares rich version and foo is child of both sub and subtwo, we change version if :sub:foo is the path we are parsing and do not change if we are parsing :subtwo:foo

        if(gradleRichVersions.containsKey(rootProjectName) && gradleRichVersions.get(rootProjectName).containsKey(dependencyGroupName)) {
            richVersionProject = rootProjectName;
            return true;
        }

        String[] pathParts = projectPath.split(":");
        for(int depth = 1; depth < pathParts.length; depth++) { // Since path is like :sub:foo we start at the first index which will be the parent at first level
            if(gradleRichVersions.containsKey(pathParts[depth]+"_"+depth) && gradleRichVersions.get(pathParts[depth]+"_"+depth).containsKey(dependencyGroupName)) {
                richVersionProject = pathParts[depth]+"_"+depth;
                return true;
            }
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
