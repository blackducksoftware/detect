package com.blackduck.integration.detectable.detectables.uv.transform;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.uv.UVDetectorOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Arrays;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class UVTreeDependencyGraphTransformer {

    private final ExternalIdFactory externalIdFactory;
    private static final Logger logger = LoggerFactory.getLogger(UVTreeDependencyGraphTransformer.class);
    private final List<String> prefixStrings = Arrays.asList("├── ","│   ","└── ","    "); // common indentation strings for depenendency lines
    private int depth;
    private boolean isMemberExcluded = false; // check if workspace Member is excluded
    private int excludedMemberDependencyDepth; // depth at which member dependency was found
    List<CodeLocation> codeLocations = new ArrayList<>();
    private DependencyGraph dependencyGraph;
    private List<String> workSpaceMembers = new ArrayList<>();

    public UVTreeDependencyGraphTransformer(ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
    }

    public List<CodeLocation> transform(List<String> uvTreeOutput, UVDetectorOptions detectorOptions) {
        dependencyGraph = new BasicDependencyGraph();
        Deque<Dependency> dependencyStack = new ArrayDeque<>();

        populateWorkSpaces(uvTreeOutput);
        for(String line: uvTreeOutput) {
            parseLine(line, detectorOptions, dependencyStack);
        }

        return codeLocations;
    }

    // getting all workspace members as it might be needed for figuring out what workspaces need to be excluded if it is not set in the included workspace property
    // not possible to get this list using toml file as it maybe wildcard characters
    private void populateWorkSpaces(List<String> uvTreeOutput) {
        for(String line: uvTreeOutput) {
            findDepth(line);

            if(depth == 0) {
                String[] parts = line.split(" ");
                workSpaceMembers.add(parts[0]);
            }
        }
    }

    private void parseLine(String line, UVDetectorOptions detectorOptions, Deque<Dependency> dependencyStack) {
        if(line.isEmpty()) {
            return;
        }

        int previousDepth = depth;
        String cleanedLine = findDepth(line);

        // Check if a previous workspace member was excluded and we are parsing transitives of that member
        if(depth > excludedMemberDependencyDepth && isMemberExcluded) {
            return;
        } else {
            isMemberExcluded = false;
        }

        Dependency dependency = getDependency(cleanedLine, detectorOptions);

        // if depth is greater than 1 and dependency was found
        if(depth > 0 && dependency != null) {
            addDependencyToGraph(dependency, dependencyStack, previousDepth);
            dependencyStack.push(dependency);
        } else if (dependency != null && depth == 0) {
            String[] parts = line.split(" "); // parse the project line
            if(parts.length < 2) {
                logger.warn("Unable to parse workspace member from line: {}", line);
                initializeProject(parts[0] != null ? parts[0] : "uvProject", "defaultVersion"); // if version doesn't exist, then create a code location with just project name and default version
                return;
            }
            String memberName = parts[0];
            String memberVersion = parts[1].replace("v", "");

            initializeProject(memberName, memberVersion); // initialize the project with a new code location
        }
    }

    private void addDependencyToGraph(Dependency dependency, Deque<Dependency> dependencyStack, int previousDepth) {
        // direct dependency
        if(depth == 1) {
            dependencyGraph.addDirectDependency(dependency);
            dependencyStack.clear();
        } else if (previousDepth == depth) {
            // sibling of previous dependency
            dependencyStack.pop();
            dependencyGraph.addChildWithParent(dependency, dependencyStack.peek());
        } else if (previousDepth > depth) {
            // a child of the dependency before parsing the tree for previous dependency
            while(!dependencyStack.isEmpty() && dependencyStack.size() >= depth) {
                dependencyStack.pop();
            }
            dependencyGraph.addChildWithParent(dependency, dependencyStack.peek());
        } else {
            // a child of the previous dependency
            dependencyGraph.addChildWithParent(dependency, dependencyStack.peek());
        }
    }

    private boolean checkIfMemberExcluded(String memberName, UVDetectorOptions detectorOptions) {
        if(!detectorOptions.getExcludedWorkspaceMembers().isEmpty() && detectorOptions.getExcludedWorkspaceMembers().contains(memberName)) { // checking if current member is excluded
            return true;
        } else if(!detectorOptions.getIncludedWorkspaceMembers().isEmpty()){
            return !detectorOptions.getIncludedWorkspaceMembers().contains(memberName) && workSpaceMembers.contains(memberName); // checking if current member is not included, and checking if member is a workspace, an earlier bug was discarding components
        } else {
            return false;
        }
    }

    //create a new code location for a new workspace member
    private void initializeProject(String projectName, String projectVersion) {
        ExternalId externalId = externalIdFactory.createNameVersionExternalId(Forge.PYPI ,projectName, projectVersion);
        Dependency projectDependency = new Dependency(projectName, projectVersion, externalId);
        dependencyGraph = new BasicDependencyGraph();
        CodeLocation codeLocation = new CodeLocation(dependencyGraph, projectDependency.getExternalId(), new File(projectDependency.getName()));
        codeLocations.add(codeLocation);
    }

    // find depth of the dependency line that is being parsed
    private String findDepth(String line) {
        depth = 0;
        String cleanedLine = line;
        for(String prefix: prefixStrings) {
            while(cleanedLine.contains(prefix)) {
                depth++;
                cleanedLine = cleanedLine.replaceFirst(Pattern.quote(prefix), "");
            }
        }

        return cleanedLine;
    }

    private Dependency getDependency(String line, UVDetectorOptions detectorOptions) {

        // sometimes the dependency contains some extra data in the line, we strip it off
        // Eg: dbgpt[agent, cli, client, code, framework, simple-framework] v0.7.0 (extra: base), we do not need "[agent, cli, client, code, framework, simple-framework]"
        if(line.contains("[")) {
            String parenthesis = line.substring(line.indexOf("["), line.indexOf("]") + 1);
            line = line.replace(parenthesis, "");
        }

        // we keep the limit three to split it in three parts if we have extra information such as group or extra, as an example "pytest v8.3.4 (group: dev)"
        String[] parts = line.split(" ",3);
        if(parts.length < 2) {
            logger.warn("Unable to parse dependency from line: {}", line);
            return null;
        }

        String dependencyName = parts[0];
        String dependencyVersion = parts[1].replace("v", "");

        // check if the member is excluded and set flags for excluding transitives of that dependency
        if (checkIfMemberExcluded(dependencyName, detectorOptions)) {
            isMemberExcluded = true;
            excludedMemberDependencyDepth = depth;
            logger.info("Skipping member '{}' as set in the Detect workspace property.", dependencyName);
            return null;
        }

        String normalizedDependencyName = normalizePackageName(dependencyName);
        ExternalId externalId = externalIdFactory.createNameVersionExternalId(Forge.PYPI, normalizedDependencyName, dependencyVersion);
        return new Dependency(normalizedDependencyName, dependencyVersion, externalId);
    }

    private String normalizePackageName(String packageName) {
        return packageName.replaceAll("[_.-]+", "-").toLowerCase();
    }
}
