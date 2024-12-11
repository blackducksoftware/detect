package com.blackduck.integration.detectable.detectables.gradle.inspection.parse;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.util.DependencyHistory;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.gradle.inspection.GradleConfigurationType;
import com.blackduck.integration.detectable.detectables.gradle.inspection.model.GradleConfiguration;
import com.blackduck.integration.detectable.detectables.gradle.inspection.model.GradleGav;
import com.blackduck.integration.detectable.detectables.gradle.inspection.model.GradleReport;
import com.blackduck.integration.detectable.detectables.gradle.inspection.model.GradleTreeNode;

//An example transform that uses our "Dependency History" class and is closer to the original Gradle implementation
public class GradleReportTransformer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final EnumListFilter<GradleConfigurationType> configurationTypeFilter;
    private final List<CodeLocation> allCodeLocationsWithinRoot = new ArrayList<>();
    private final Map<String, CodeLocation> subProjectCodeLocationMap = new HashMap<>();

    public GradleReportTransformer(EnumListFilter<GradleConfigurationType> configurationTypeFilter) {
        this.configurationTypeFilter = configurationTypeFilter;
    }

    public List<CodeLocation> transformRootReportOnly(GradleReport gradleReport) {
        DependencyGraph rootGraph = new BasicDependencyGraph();

        processConfigurations(gradleReport, rootGraph, true);

        ExternalId projectId = ExternalId.FACTORY.createMavenExternalId(gradleReport.getProjectGroup(), gradleReport.getProjectName(), gradleReport.getProjectVersionName());
        if (StringUtils.isNotBlank(gradleReport.getProjectSourcePath())) {
            CodeLocation rootCodeLocation = new CodeLocation(rootGraph, projectId, new File(gradleReport.getProjectSourcePath()));
            allCodeLocationsWithinRoot.add(rootCodeLocation);
        } else {
            CodeLocation rootCodeLocation = new CodeLocation(rootGraph, projectId);
            allCodeLocationsWithinRoot.add(rootCodeLocation);
        }

        return allCodeLocationsWithinRoot;
    }

    private void addConfigurationToRootAndSubProjectGraphs(DependencyGraph rootGraph, GradleConfiguration configuration, GradleReport rootReport) {
        DependencyHistory history = new DependencyHistory();

        TreeNodeSkipper treeNodeSkipper = new TreeNodeSkipper();
        for (GradleTreeNode currentNode : configuration.getChildren()) {
            if (treeNodeSkipper.shouldSkip(currentNode)) {
                continue;
            }

            if (currentNode.getNodeType() == GradleTreeNode.NodeType.GAV) {
                addGavToGraph(currentNode, currentNode.getLevel(), history, rootGraph);
            }
            else if (currentNode.getNodeType() == GradleTreeNode.NodeType.PROJECT) {
                // Let this method skip over the subProject section as usual
                treeNodeSkipper.skipUntilLineLevel(currentNode.getLevel());
                // Process subProject section separately
                processSubprojectAndCreateCodeLocation(currentNode, configuration.getChildren(), rootReport);
            } else {
                treeNodeSkipper.skipUntilLineLevel(currentNode.getLevel());
            }
        }
    }

    private void processSubprojectAndCreateCodeLocation(GradleTreeNode subProjectNode, List<GradleTreeNode> allTreeNodesInCurrentConfiguration, GradleReport rootReport) {
        String subProjectName = subProjectNode.getProjectName().get();
        logger.debug("Processing subProject node:" + subProjectName);
        int subProjectSectionStartIndex = allTreeNodesInCurrentConfiguration.indexOf(subProjectNode);
        int subProjectNodeLevel = subProjectNode.getLevel();

        DependencyGraph subProjectGraph = new BasicDependencyGraph();
        DependencyHistory history = new DependencyHistory();
        TreeNodeSkipper treeNodeSkipper = new TreeNodeSkipper();
        // Begin processing subProject section
        for (int i = subProjectSectionStartIndex+1; i < allTreeNodesInCurrentConfiguration.size(); i++) {
            GradleTreeNode currentNode = allTreeNodesInCurrentConfiguration.get(i);
            int currentNodeLevelRelativeToSubProject = (currentNode.getLevel() - 1) - subProjectNodeLevel;
            if (treeNodeSkipper.shouldSkip(currentNode)) {
                continue;
            }
            if (currentNodeLevelRelativeToSubProject != -1) {
                if (currentNode.getNodeType() == GradleTreeNode.NodeType.GAV) {
                    logger.trace("Adding dependency " + currentNode.getGav() + " for subProject " + subProjectName);
                    addGavToGraph(currentNode, currentNodeLevelRelativeToSubProject, history, subProjectGraph);
                } else {
                    // Process nested subproject node
                    treeNodeSkipper.skipUntilLineLevel(currentNode.getLevel());
                    processSubprojectAndCreateCodeLocation(currentNode, allTreeNodesInCurrentConfiguration, rootReport);
                }
            } else {
                // Current node is back at 0 so we are done processing subProject section
                break;
            }
        }

        if (codeLocationAlreadyCreatedFor(subProjectName)) {
            // Fetch existing code location for the subProject
            CodeLocation existingCodeLocation = subProjectCodeLocationMap.get(subProjectName);
            existingCodeLocation.getDependencyGraph().copyGraphToRoot(subProjectGraph);
            return;
        }
        CodeLocation newCodeLocation = createCodeLocationForSubProject(rootReport, subProjectGraph, subProjectName);
        subProjectCodeLocationMap.put(subProjectName, newCodeLocation);
        allCodeLocationsWithinRoot.add(newCodeLocation);
    }

    private boolean codeLocationAlreadyCreatedFor(String subProjectName) {
        return subProjectCodeLocationMap.containsKey(subProjectName);
    }

    private CodeLocation createCodeLocationForSubProject(GradleReport rootReport, DependencyGraph subProjectGraph, String subProjectName) {
        ExternalId projectId;
        if (subProjectName.contains(":")) {
            String[] nestedSubProjectParts = getProjectGroupAndProjectNameForNestedSubProject(subProjectName);
            projectId = ExternalId.FACTORY.createMavenExternalId(rootReport.getProjectName() + "." + nestedSubProjectParts[0] + "%2F" + nestedSubProjectParts[1], rootReport.getProjectVersionName());
        } else {
            projectId = ExternalId.FACTORY.createMavenExternalId(rootReport.getProjectGroup(), subProjectName, rootReport.getProjectVersionName());
        // for sub: ("", sub, unspecified)
        // for sub:foo: (rooted.sub, foo, unspecified)
        }
        // project group should be "rooted.sub" instead of "".... project group USUALLY comes from the subProject's build.gradle, can be blank in subproject
        // or rooted.subtwo.foo
        // project name should be "foo" instead of "sub:foo"
        // or "foofoo" instead of "sub:foo:foofoo"
        // project source path is "/Users/shanty/Downloads/rooted/sub/foo" but e skip this when root only
            return new CodeLocation(subProjectGraph, projectId);
    }

    private String[] getProjectGroupAndProjectNameForNestedSubProject(String fullNestedSubProjectName) {
        int lastColonIndex = fullNestedSubProjectName.lastIndexOf(":");
        String projectGroupName = fullNestedSubProjectName.substring(0, lastColonIndex).replace(":", ".");
        String subProjectName = fullNestedSubProjectName.substring(lastColonIndex + 1);

        String[] projectGroupAndSubProjectName = {projectGroupName, subProjectName};
        return projectGroupAndSubProjectName;

    }
    public CodeLocation transform(GradleReport gradleReport) {
        DependencyGraph graph = new BasicDependencyGraph();

        processConfigurations(gradleReport, graph, false);

        ExternalId projectId = ExternalId.FACTORY.createMavenExternalId(gradleReport.getProjectGroup(), gradleReport.getProjectName(), gradleReport.getProjectVersionName());
       // for rooted/sub: (rooted, sub, unspecified)
        if (StringUtils.isNotBlank(gradleReport.getProjectSourcePath())) {
            return new CodeLocation(graph, projectId, new File(gradleReport.getProjectSourcePath()));
        } else {
            return new CodeLocation(graph, projectId);
        }
    }

    private void processConfigurations(GradleReport gradleReport, DependencyGraph graph, boolean rootOnly) {
        for (GradleConfiguration configuration : gradleReport.getConfigurations()) {
            if (configuration.isResolved() || configurationTypeFilter.shouldInclude(GradleConfigurationType.UNRESOLVED)) {
                logger.trace("Adding configuration to the graph: {}", configuration.getName());
                if (rootOnly) {
                    addConfigurationToRootAndSubProjectGraphs(graph, configuration, gradleReport);
                } else {
                    addConfigurationToGraph(graph, configuration);
                }
            } else {
                logger.trace("Excluding unresolved configuration from the graph: {}", configuration.getName());
            }
        }
    }

    private void addConfigurationToGraph(DependencyGraph graph, GradleConfiguration configuration) {
        DependencyHistory history = new DependencyHistory();

        TreeNodeSkipper treeNodeSkipper = new TreeNodeSkipper();
        for (GradleTreeNode currentNode : configuration.getChildren()) {
            if (treeNodeSkipper.shouldSkip(currentNode)) {
                continue;
            }

            if (currentNode.getNodeType() == GradleTreeNode.NodeType.GAV) {
                addGavToGraph(currentNode, currentNode.getLevel(), history, graph);
            } else {
                treeNodeSkipper.skipUntilLineLevel(currentNode.getLevel());
            }
        }
    }

    private void addGavToGraph(GradleTreeNode currentNode, int currentNodeLevel, DependencyHistory history, DependencyGraph graph) {
        history.clearDependenciesDeeperThan(currentNodeLevel);
        Optional<GradleGav> currentNodeGavOptional = currentNode.getGav();
        if (currentNodeGavOptional.isPresent()) {
            GradleGav gav = currentNodeGavOptional.get();
            Dependency currentDependency = Dependency.FACTORY.createMavenDependency(gav.getGroup(), gav.getName(), gav.getVersion());
            if (history.isEmpty()) {
                graph.addDirectDependency(currentDependency);
            } else {
                graph.addChildWithParents(currentDependency, history.getLastDependency());
            }
            history.add(currentDependency);
        } else {
            // We know this is a GradleTreeNode.NodeType.GAV
            // So if its missing data, something is probably wrong.
            logger.debug("Missing expected GAV from known NodeType. {}", currentNode);
        }
    }

    private static class TreeNodeSkipper {
        private Optional<Integer> skipUntilLineLevel = Optional.empty();

        public boolean shouldSkip(GradleTreeNode nodeInQuestion) {
            if (skipUntilLineLevel.isPresent()) {
                if (nodeInQuestion.getLevel() > skipUntilLineLevel.get()) {
                    return true;
                } else {
                    skipUntilLineLevel = Optional.empty();
                    return false;
                }
            } else {
                return false;
            }
        }

        public void skipUntilLineLevel(int lineLevel) {
            skipUntilLineLevel = Optional.of(lineLevel);
        }
    }

}
