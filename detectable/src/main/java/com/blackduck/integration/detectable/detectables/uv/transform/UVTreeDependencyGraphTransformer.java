package com.blackduck.integration.detectable.detectables.uv.transform;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.cargo.transform.CargoDependencyGraphTransformer;
import com.blackduck.integration.detectable.detectables.uv.UVDetectorOptions;
import com.blackduck.integration.util.ExcludedIncludedWildcardFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

public class UVTreeDependencyGraphTransformer {

    private final ExternalIdFactory externalIdFactory;
    private static final Logger logger = LoggerFactory.getLogger(UVTreeDependencyGraphTransformer.class);
    private final List<String> prefixStrings = Arrays.asList("├── ","│   ","└── ","    ");
    private int depth;
    private boolean isMemberExcluded = false;
    private boolean isExcludedGroup = false;
    private int devDependencyDepth;
    private int memberDependencyDepth;
    List<CodeLocation> codeLocations = new ArrayList<>();
    private DependencyGraph dependencyGraph;

    public UVTreeDependencyGraphTransformer(ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
    }

    public List<CodeLocation> transform(List<String> uvTreeOutput, UVDetectorOptions detectorOptions) {
        dependencyGraph = new BasicDependencyGraph();
        Deque<Dependency> dependencyStack = new ArrayDeque<>();

        for(String line: uvTreeOutput) {
            parseLine(line, detectorOptions, dependencyStack);
        }

        return codeLocations;
    }

    private void parseLine(String line, UVDetectorOptions detectorOptions, Deque<Dependency> dependencyStack) {
        if(line.isEmpty()) {
            return;
        }

        int previousDepth = depth;
        String cleanedLine = findDepth(line);

        if(depth > devDependencyDepth && isExcludedGroup) {
            return;
        } else {
            isExcludedGroup = false;
        }

        if(depth > memberDependencyDepth && isMemberExcluded) {
            return;
        } else {
            isMemberExcluded = false;
        }

        Dependency dependency = getDependency(cleanedLine, detectorOptions);

        if(depth > 0 && dependency != null) {
            addDependencyToGraph(dependency, dependencyStack, previousDepth);
            dependencyStack.push(dependency);
        } else if (dependency != null && depth == 0) {
            String[] parts = line.split(" ");
            if(parts.length < 2) {
                logger.warn("Unable to parse workspace member from line: {}", line);
                return;
            }
            String memberName = parts[0];
            String memberVersion = parts[1].replace("v", "");

            isMemberExcluded = checkIfMemberExcluded(memberName, detectorOptions);

            if(!isMemberExcluded) {
                initializeProject(memberName, memberVersion);
            }
        }
    }

    private void addDependencyToGraph(Dependency dependency, Deque<Dependency> dependencyStack, int previousDepth) {
        if(depth == 1) {
            dependencyGraph.addDirectDependency(dependency);
            dependencyStack.clear();
        } else if (previousDepth == depth) {
            dependencyStack.pop();
            dependencyGraph.addChildWithParent(dependency, dependencyStack.peek());
        } else if (previousDepth > depth) {
            while(!dependencyStack.isEmpty() && dependencyStack.size() >= depth) {
                dependencyStack.pop();
            }
            dependencyGraph.addChildWithParent(dependency, dependencyStack.peek());
        } else {
            dependencyGraph.addChildWithParent(dependency, dependencyStack.peek());
        }
    }

    private boolean checkIfMemberExcluded(String memberName, UVDetectorOptions detectorOptions) {
        if(!detectorOptions.getExcludedWorkspaceMembers().isEmpty() && detectorOptions.getExcludedWorkspaceMembers().contains(memberName)) {
            return true;
        } else if(!detectorOptions.getIncludedWorkspaceMembers().isEmpty()){
            return !detectorOptions.getIncludedWorkspaceMembers().contains(memberName);
        } else {
            return false;
        }
    }

    private void initializeProject(String projectName, String projectVersion) {
        ExternalId externalId = externalIdFactory.createNameVersionExternalId(Forge.PYPI ,projectName, projectVersion);
        Dependency projectDependency = new Dependency(projectName, projectVersion, externalId);
        dependencyGraph = new BasicDependencyGraph();
        CodeLocation codeLocation = new CodeLocation(dependencyGraph, projectDependency.getExternalId(), new File(projectDependency.getName()));
        codeLocations.add(codeLocation);
    }

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

        if(line.contains("[")) {
            String parenthesis = line.substring(line.indexOf("["), line.indexOf("]") + 1);
            line = line.replace(parenthesis, "");
        }

        String[] parts = line.split(" ",3);
        if(parts.length < 2) {
            logger.warn("Unable to parse dependency from line: {}", line);
            return null;
        }

        if(parts.length == 2) {
            String dependencyName = parts[0];
            String dependencyVersion = parts[1].replace("v", "");

            if (checkIfMemberExcluded(dependencyName, detectorOptions)) {
                isMemberExcluded = true;
                memberDependencyDepth = depth;
                return null;
            }

            ExternalId externalId = externalIdFactory.createNameVersionExternalId(Forge.PYPI, dependencyName, dependencyVersion);
            return new Dependency(dependencyName, dependencyVersion, externalId);
        } else if (parts.length == 3) {
            String dependencyName = parts[0];
            String dependencyVersion = parts[1].replace("v", "");

            String groupName = "";
            if (parts[2].contains("group:")) {

                try {
                    groupName = parts[2].split(":")[1].replace(")", "").trim();
                } catch (Exception e) {
                    logger.warn("Unable to parse group from dependency line: {}", line);
                }

                if (detectorOptions.getExcludedDependencyGroups().contains(groupName)) {
                    isExcludedGroup = true;
                    devDependencyDepth = depth;
                    return null;
                }
            }

            if (checkIfMemberExcluded(dependencyName, detectorOptions)) {
                isMemberExcluded = true;
                memberDependencyDepth = depth;
                return null;
            }

            ExternalId externalId = externalIdFactory.createNameVersionExternalId(Forge.PYPI, dependencyName, dependencyVersion);
            return new Dependency(dependencyName, dependencyVersion, externalId);
        } else {
            logger.warn("Unable to parse dependency from line: {}", line);
            return null;
        }
    }
}
