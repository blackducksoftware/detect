package com.blackduck.integration.detectable.detectables.pnpm.lockfile.process;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmLockYamlv5;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmProjectPackagev5;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.util.NameVersion;

public class PnpmLockYamlParserv5 {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    private static final Predicate<String> isNodeRoot = "."::equals;

    private PnpmYamlTransformerv5 pnpmTransformer;

    public PnpmLockYamlParserv5(PnpmYamlTransformerv5 pnpmTransformer) {
        this.pnpmTransformer = pnpmTransformer;
    }

    public List<CodeLocation> parse(File parentFile, PnpmLockYamlv5 pnpmLockYaml,
            PnpmLinkedPackageResolver linkedPackageResolver, @Nullable NameVersion projectNameVersion) throws IntegrationException {
        logger.warn("Please update your pnpm-lock.yaml file by running pnpm install using a newer version of pnpm. Support for the v5 file will be removed in the next major release.");
        List<CodeLocation> codeLocationsFromImports = createCodeLocationsFromImports(parentFile, pnpmLockYaml, linkedPackageResolver, projectNameVersion);
        if (codeLocationsFromImports.isEmpty()) {
            return createCodeLocationsFromRoot(parentFile, pnpmLockYaml, projectNameVersion, linkedPackageResolver);
        }
        return codeLocationsFromImports;
    }
    

    private List<CodeLocation> createCodeLocationsFromRoot(
        File sourcePath,
        PnpmLockYamlv5 pnpmLockYaml,
        @Nullable NameVersion projectNameVersion,
        PnpmLinkedPackageResolver linkedPackageResolver
    ) throws IntegrationException {
        if (pnpmLockYaml.packages == null) {
            logger.warn("The pnpm-lock.yaml file has no 'packages' section. No resolved dependencies are present. The scan will continue with an empty dependency graph.");
        }
        CodeLocation codeLocation = pnpmTransformer.generateCodeLocation(sourcePath, pnpmLockYaml, projectNameVersion, linkedPackageResolver);
        return Collections.singletonList(codeLocation);
    }

    private List<CodeLocation> createCodeLocationsFromImports(
        File sourcePath,
        PnpmLockYamlv5 pnpmLockYaml,
        PnpmLinkedPackageResolver linkedPackageResolver,
        @Nullable NameVersion projectNameVersion
    ) throws IntegrationException {
        if (MapUtils.isEmpty(pnpmLockYaml.importers)) {
            return Collections.emptyList();
        }

        logger.info("PNPM workspace detected in pnpm-lock.yaml. Found {} workspace module(s): {}",
            pnpmLockYaml.importers.size(), pnpmLockYaml.importers.keySet());
        logger.info("Subdirectory package.json files in workspace modules do not need to be processed separately; "
            + "all dependency information is already contained in the root pnpm-lock.yaml.");

        if (pnpmLockYaml.packages == null) {
            logger.warn("The pnpm-lock.yaml file contains {} importer(s) {} but has no 'packages' section. "
                + "No resolved dependencies are available. All workspaces will have empty dependency graphs.",
                pnpmLockYaml.importers.size(),
                pnpmLockYaml.importers.keySet());
        }

        List<CodeLocation> codeLocations = new LinkedList<>();
        for (Map.Entry<String, PnpmProjectPackagev5> projectPackageInfo : pnpmLockYaml.importers.entrySet()) {
            String projectKey = projectPackageInfo.getKey();
            PnpmProjectPackagev5 projectPackage = projectPackageInfo.getValue();
            NameVersion extractedNameVersion = extractProjectInfo(projectPackageInfo, linkedPackageResolver, projectNameVersion);

            String reportingProjectPackagePath = null;
            if (!isNodeRoot.evaluate(projectKey)) {
                reportingProjectPackagePath = projectKey;
            }
            File generatedSourcePath = generateCodeLocationSourcePath(sourcePath, reportingProjectPackagePath);

            CodeLocation codeLocation = pnpmTransformer.generateCodeLocation(
                generatedSourcePath,
                projectPackage,
                reportingProjectPackagePath,
                extractedNameVersion,
                pnpmLockYaml.packages,
                linkedPackageResolver
            );
            logWorkspaceModuleSummary(projectKey, codeLocation.getDependencyGraph());
            codeLocations.add(codeLocation);
        }

        return codeLocations;
    }

    private NameVersion extractProjectInfo(
        Map.Entry<String, PnpmProjectPackagev5> projectPackageInfo,
        PnpmLinkedPackageResolver linkedPackageResolver,
        @Nullable NameVersion projectNameVersion
    ) {
        if (isNodeRoot.evaluate(projectPackageInfo.getKey()) && projectNameVersion != null && projectNameVersion.getName() != null) {
            // resolve "." package to project root
            return projectNameVersion;
        }

        String projectPackageName = projectPackageInfo.getKey();
        String projectPackageVersion = linkedPackageResolver.resolveVersionOfLinkedPackage(null, projectPackageName);
        return new NameVersion(projectPackageName, projectPackageVersion);
    }

    private File generateCodeLocationSourcePath(File sourcePath, @Nullable String reportingProjectPackagePath) {
        if (StringUtils.isNotEmpty(reportingProjectPackagePath)) {
            return new File(sourcePath, reportingProjectPackagePath);
        }
        return sourcePath;
    }

    private void logWorkspaceModuleSummary(String projectKey, DependencyGraph graph) {
        String moduleLabel = isNodeRoot.evaluate(projectKey) ? "(root)" : projectKey;
        Set<Dependency> allDeps = collectAllDependencies(graph);
        int directCount = graph.getRootDependencies().size();
        int transitiveCount = allDeps.size() - directCount;
        logger.info("Workspace module '{}': {} direct and {} transitive dependencies discovered.",
            moduleLabel, directCount, transitiveCount);
        if (logger.isDebugEnabled()) {
            List<String> depNames = allDeps.stream()
                .map(dep -> dep.getName() + "@" + dep.getVersion())
                .sorted()
                .collect(Collectors.toList());
            logger.debug("Workspace module '{}' full dependency list: {}", moduleLabel, depNames);
        }
    }

    private Set<Dependency> collectAllDependencies(DependencyGraph graph) {
        Set<Dependency> visited = new HashSet<>();
        Queue<Dependency> queue = new LinkedList<>(graph.getRootDependencies());
        while (!queue.isEmpty()) {
            Dependency dep = queue.poll();
            if (visited.add(dep)) {
                queue.addAll(graph.getChildrenForParent(dep));
            }
        }
        return visited;
    }
}
