package com.blackduck.integration.detectable.detectables.opam.buildexe.parse;


import com.blackduck.integration.detectable.detectables.opam.parse.OpamParsedResult;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.util.DetectableStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.io.File;

public class OpamTreeParser {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private int level = 0;
    private String currentProject;
    private static final String DIRECT_DEPENDENCY_INDICATOR = "|--"; // direct dependencies usually start with this prefix
    private static final String[] DEPENDENCY_INDICATOR = new String[] { "|", "|--", "'--" }; // indicates that line contains a dependency
    private static final String PACKAGE_PREFIX = "-- ";
    private final List<OpamParsedResult> codeLocations = new ArrayList<>();
    private DependencyGraph currentGraph;
    private Stack<Dependency> dependencyStack;
    private final File sourceDirectory;
    private final ExternalIdFactory externalIdFactory;

    public OpamTreeParser(File sourceDirectory, ExternalIdFactory externalIdFactory) {
        this.sourceDirectory = sourceDirectory;
        this.externalIdFactory = externalIdFactory;
    }

    public List<OpamParsedResult> parseTree(List<String> tree) {
        currentProject = null;
        currentGraph = new BasicDependencyGraph();
        dependencyStack = new Stack<>();


        for(String line: tree) {
            if(line.isEmpty() || line.startsWith("*") || line.startsWith("=")) {
                continue;
            }

            // if line starts with none of dependency indicator and starts with alphanumeric it may suggest the start of a new project
            if(!StringUtils.startsWithAny(line, DEPENDENCY_INDICATOR) && !line.startsWith(" ")) {
                initializeProject(line);
                continue;
            }

            int previousLevel = level;
            level = parseLevel(line);

            Dependency dependency = parseDependencyLine(line);

            if(dependency != null && currentProject != null) {
                addDependencyToGraph(dependency, previousLevel);
            }

        }

        return codeLocations;
    }

    private Dependency parseDependencyLine(String line) {
        String dependencyLine = StringUtils.trimToEmpty(line);

        //Usually the dependency line will start like this "|-- package.v.e.r [*] (conditional)", so we get the substring after the
        // package prefix and split it on basis of white space, after that we split on first occurrence of "."
        dependencyLine = dependencyLine.substring(dependencyLine.indexOf(PACKAGE_PREFIX) + PACKAGE_PREFIX.length());

        String[] dependencyLineParts = dependencyLine.split(" +");
        String dependencyString = dependencyLineParts[0];

        String[] dependencyStringParts = dependencyString.split("\\.", 2);
        if(dependencyStringParts.length == 2) {
            return createDependencyExternalId(dependencyStringParts[0], dependencyStringParts[1]);
        }

        return null;
    }

    private void addDependencyToGraph(Dependency dependency, int previousLevel) {
        if(level == 0) { // add direct dependency to graph
            currentGraph.addDirectDependency(dependency);
            dependencyStack.clear(); //clear dependency stack for new transitives
            dependencyStack.push(dependency);
        } else {
            if(level == previousLevel) { // indicates a sibling of previous dependency
                dependencyStack.pop(); // pop the previous dependency and add current to head
                currentGraph.addChildWithParent(dependency, dependencyStack.peek());
                dependencyStack.push(dependency);
            } else if(level > previousLevel) { // a child of dependency at head
                currentGraph.addChildWithParent(dependency, dependencyStack.peek());
                dependencyStack.push(dependency);
            } else { // a child of dependency encountered earlier in the tree
                while(previousLevel != level) {
                    dependencyStack.pop();
                    previousLevel--;
                }
                currentGraph.addChildWithParent(dependency, dependencyStack.peek());
                dependencyStack.push(dependency);
            }
        }
    }

    private int parseLevel(String line) {
        if(line.startsWith(DIRECT_DEPENDENCY_INDICATOR)) {
            return 0; // if direct return zero
        }

        String modifiedLine = DetectableStringUtils.removeEvery(line, DEPENDENCY_INDICATOR); // this will remove every thing after the start of the dependency

        if(!modifiedLine.startsWith("|") && modifiedLine.startsWith(" ")) {
            modifiedLine = "|" + modifiedLine;
        }

        modifiedLine = modifiedLine.replace("     ", "    |"); // replace with "|", if there are extra number of spaces in the middle
        return StringUtils.countMatches(modifiedLine, "|"); // the number of "|" will be the level of the transitive dependency
    }

    private void initializeProject(String line) {
        String[] projectParts = line.split("\\.", 2);

        // Usually the project syntax looks like: reason.3.8.2, so we split on the first occurrence of "."
        if(projectParts.length == 2) {
            Dependency projectDependency = createDependencyExternalId(projectParts[0], projectParts[1]); // create a dependency for the project
            currentGraph = new BasicDependencyGraph();
            currentProject = projectParts[0];
            String codeLocationPath = sourceDirectory.getPath();
            if(!codeLocationPath.endsWith(projectDependency.getName())) { // get the source code for the project using specific opam file
                codeLocationPath = "/" + projectDependency.getName();
            }
            CodeLocation codeLocation = new CodeLocation(currentGraph, projectDependency.getExternalId(), new File(codeLocationPath));
            codeLocations.add(new OpamParsedResult(projectParts[0], projectParts[1], codeLocation)); // create a new opam parsed result for the project being initialized
        } else {
            logger.debug(String.format("%s does not look like a dependency we can parse", line));
        }
    }

    private Dependency createDependencyExternalId(String name, String version) {
        ExternalId externalId = externalIdFactory.createNameVersionExternalId(Forge.NPMJS, name, version);
        return new Dependency(externalId);
    }
}
