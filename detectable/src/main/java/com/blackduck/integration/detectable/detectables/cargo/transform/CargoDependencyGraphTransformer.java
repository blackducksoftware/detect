package com.blackduck.integration.detectable.detectables.cargo.transform;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.util.NameVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class CargoDependencyGraphTransformer {

    private final ExternalIdFactory externalIdFactory;
    private static final Logger logger = LoggerFactory.getLogger(CargoDependencyGraphTransformer.class);

    public CargoDependencyGraphTransformer(ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
    }

    public List<CodeLocation> transform(List<String> cargoTreeOutput, Set<String> workspaceMembers) {
        List<CodeLocation> codeLocations = new LinkedList<>();
        List<String> currentWorkspace = new ArrayList<>();

        for (String line : cargoTreeOutput) {
            if (line.trim().isEmpty()) {
                if (!currentWorkspace.isEmpty()) {
                    CodeLocation workspaceCodeLocation = processWorkspace(currentWorkspace, workspaceMembers);
                    if (workspaceCodeLocation != null) {
                        codeLocations.add(workspaceCodeLocation);
                    }
                    currentWorkspace.clear();
                }
            } else {
                currentWorkspace.add(line.trim());
            }
        }

        // Process last workspace
        if (!currentWorkspace.isEmpty()) {
            CodeLocation workspaceCodeLocation = processWorkspace(currentWorkspace, workspaceMembers);
            if (workspaceCodeLocation != null) {
                codeLocations.add(workspaceCodeLocation);
            }
        }

        return codeLocations;
    }

    private CodeLocation processWorkspace(List<String> workspaceLines, Set<String> workspaceMembers) {
        if (workspaceLines.isEmpty()) {
            return null;
        }

        DependencyGraph graph = new BasicDependencyGraph();
        Deque<Dependency> dependencyStack = new ArrayDeque<>();

        // Process all lines to build the dependency graph
        for (String line : workspaceLines) {
            processLine(line, graph, dependencyStack);
        }

        // Extract workspace root from the first line (depth 0)
        Dependency workspaceRoot = extractWorkspaceRoot(workspaceLines);

        if (workspaceRoot != null) {
            String projectName = determineProjectName(workspaceLines, workspaceMembers, workspaceRoot);
            ExternalId projectExternalId = externalIdFactory.createNameVersionExternalId(
                Forge.CRATES,
                projectName,
                workspaceRoot.getVersion()
            );
            return new CodeLocation(graph, projectExternalId);
        }

        return new CodeLocation(graph);
    }

    private String determineProjectName(List<String> workspaceLines, Set<String> workspaceMembers, Dependency workspaceRoot) {
        String matchedMember = extractWorkspacePath(workspaceLines, workspaceMembers);

        // If matched a workspace member, use it
        if (matchedMember != null) {
            return matchedMember;
        }

        // Otherwise, use the dependency name
        return workspaceRoot.getName();
    }

    private Dependency extractWorkspaceRoot(List<String> workspaceLines) {
        if (workspaceLines.isEmpty()) {
            return null;
        }

        String firstLine = workspaceLines.get(0);
        int depth = extractDepth(firstLine);

        if (depth == 0) {
            String dependencyInfo = firstLine.substring(String.valueOf(depth).length()).trim();
            return parseDependency(dependencyInfo);
        }

        logger.warn("First line is not a workspace root (depth 0): {}", firstLine);
        return null;
    }

    private String extractWorkspacePath(List<String> workspaceLines, Set<String> workspaceMembers) {
        if (workspaceLines.isEmpty()) {
            return null;
        }

        String firstLine = workspaceLines.get(0);
        int depth = extractDepth(firstLine);

        if (depth == 0) {
            // Extract path from parentheses: "0globset v0.4.15 (C:\Users\...\ripgrep\crates\globset)"
            int startParen = firstLine.indexOf('(');
            int endParen = firstLine.indexOf(')', startParen);

            if (startParen > 0 && endParen > startParen) {
                String fullPath = firstLine.substring(startParen + 1, endParen).trim();
                fullPath = fullPath.replace('\\', '/'); // Normalize to forward slashes

                // Check if any workspace member matches the end of the normalized path
                for (String member : workspaceMembers) {
                    if (fullPath.endsWith(member)) {
                        return member; // Return the matched workspace member directly
                    }
                }
            }
        }

        return null;
    }

    private void processLine(String line, DependencyGraph graph, Deque<Dependency> dependencyStack) {
        if (line.isEmpty()) {
            return;
        }

        int currentLevel = extractDepth(line);
        String dependencyInfo = line.substring(String.valueOf(currentLevel).length()).trim();
        Dependency dependency = parseDependency(dependencyInfo);

        if (dependency != null) {
            updateDependencyStack(dependencyStack, currentLevel);
            if (currentLevel > 0) {
                addDependencyToGraph(graph, dependencyStack, dependency, currentLevel);
                dependencyStack.push(dependency);
            }
        }
    }

    private void updateDependencyStack(Deque<Dependency> dependencyStack, int currentLevel) {
        while (!dependencyStack.isEmpty() && dependencyStack.size() >= currentLevel) {
            dependencyStack.pop();
        }
    }

    private void addDependencyToGraph(DependencyGraph graph, Deque<Dependency> dependencyStack, Dependency dependency, int currentLevel) {
        if (currentLevel == 1) {
            graph.addDirectDependency(dependency);
            dependencyStack.clear();
        } else if (!dependencyStack.isEmpty()) {
            graph.addParentWithChild(dependencyStack.peek(), dependency);
        } else {
            logger.warn("No parent found for dependency: {} {}", dependency.getName(), dependency.getVersion());
        }
    }

    private int extractDepth(String line) {
        int depth = 0;
        while (depth < line.length() && Character.isDigit(line.charAt(depth))) {
            depth++;
        }
        return Integer.parseInt(line.substring(0, depth));
    }

    private Dependency parseDependency(String dependencyInfo) {
        String[] parts = dependencyInfo.split(" ");
        if (parts.length < 2) {
            logger.warn("Unable to parse dependency from line: {}", dependencyInfo);
            return null;
        }

        String name = parts[0];
        String version = parts[1].replace("v", "");
        ExternalId externalId = externalIdFactory.createNameVersionExternalId(Forge.CRATES, name, version);

        return new Dependency(name, version, externalId);
    }
}