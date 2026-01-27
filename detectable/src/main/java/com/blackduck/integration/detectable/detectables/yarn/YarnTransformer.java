package com.blackduck.integration.detectable.detectables.yarn;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.graph.builder.LazyBuilderMissingExternalIdHandler;
import com.blackduck.integration.bdio.graph.builder.LazyExternalIdDependencyGraphBuilder;
import com.blackduck.integration.bdio.graph.builder.LazyExternalIdDependencyGraphBuilder.LazyDependencyInfo;
import com.blackduck.integration.bdio.graph.builder.LazyId;
import com.blackduck.integration.bdio.graph.builder.MissingExternalIdException;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.NullSafePackageJson;
import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLockDependency;
import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLockResult;
import com.blackduck.integration.detectable.detectables.yarn.parse.entry.YarnLockEntry;
import com.blackduck.integration.detectable.detectables.yarn.parse.entry.YarnLockEntryId;
import com.blackduck.integration.detectable.detectables.yarn.workspace.YarnWorkspace;
import com.blackduck.integration.detectable.detectables.yarn.workspace.YarnWorkspaces;
import com.blackduck.integration.util.ExcludedIncludedWildcardFilter;
import com.blackduck.integration.util.NameVersion;

public class YarnTransformer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public static final String STRING_ID_NAME_VERSION_SEPARATOR = "@";
    private final ExternalIdFactory externalIdFactory;
    private final Map<LazyId, Optional<NameVersion>> unMatchedDependencies = new HashMap<>();
    private final VersionUtility versionUtility = new VersionUtility();
    private final EnumListFilter<YarnDependencyType> yarnDependencyTypeFilter;

    public YarnTransformer(ExternalIdFactory externalIdFactory, EnumListFilter<YarnDependencyType> yarnDependencyTypeFilter) {
        this.externalIdFactory = externalIdFactory;
        this.yarnDependencyTypeFilter = yarnDependencyTypeFilter;
    }
    
    public List<CodeLocation> generateCodeLocations(YarnLockResult yarnLockResult, List<NameVersion> externalDependencies)
        throws MissingExternalIdException {
        List<CodeLocation> codeLocations = new LinkedList<>();
        logger.debug("Adding root dependencies for project: {}:{}, externalDeps: {}", yarnLockResult.getRootPackageJson().getNameString(), yarnLockResult.getRootPackageJson().getVersionString(), externalDependencies.size());
        LazyBuilderMissingExternalIdHandler lazyBuilderHandler = getLazyBuilderHandler(externalDependencies);
        ExternalIdDependencyGraphBuilder rootGraphBuilder = new ExternalIdDependencyGraphBuilder();
        addRootDependenciesForProject(yarnLockResult, yarnLockResult.getRootPackageJson(), rootGraphBuilder);
        DependencyGraph rootGraph = buildGraphForProject(lazyBuilderHandler, rootGraphBuilder, yarnLockResult);
        codeLocations.add(new CodeLocation(rootGraph));
        return codeLocations;
    }
    
    public List<CodeLocation> generateCodeLocations(YarnLockResult yarnLockResult, List<NameVersion> externalDependencies, @Nullable ExcludedIncludedWildcardFilter workspaceFilter)
        throws MissingExternalIdException {
        List<CodeLocation> codeLocations = new LinkedList<>();
        try {
            logger.debug("Adding root dependencies for project: {}:{}", yarnLockResult.getRootPackageJson().getNameString(), yarnLockResult.getRootPackageJson().getVersionString());
            DependencyGraph rootProjectGraph = buildGraphForProjectOrWorkspace(yarnLockResult, yarnLockResult.getRootPackageJson(), externalDependencies);
            codeLocations.add(new CodeLocation(rootProjectGraph));
            for (YarnWorkspace workspace : yarnLockResult.getWorkspaceData().getWorkspaces()) {
                if ((workspaceFilter == null) || workspaceFilter.shouldInclude(workspace.getWorkspacePackageJson().getDirRelativePath())) {
                    logger.debug("Adding root dependencies for workspace: {}", workspace.getWorkspacePackageJson().getDirRelativePath());
                    DependencyGraph workspaceGraph = buildGraphForProjectOrWorkspace(yarnLockResult, workspace.getWorkspacePackageJson().getPackageJson(), externalDependencies);
                    ExternalId workspaceExternalId = externalIdFactory.createNameVersionExternalId(Forge.NPMJS, workspace.getWorkspacePackageJson().getDirRelativePath(), "local");
                    codeLocations.add(new CodeLocation(workspaceGraph, workspaceExternalId));
                }
            }
        } catch(ArrayIndexOutOfBoundsException | NumberFormatException | NoSuchElementException ex) {
            logger.error("Encountered a runtime error: {}. Continuing with the scan.", ex.getMessage());
        }

        return codeLocations;
    }

    private DependencyGraph buildGraphForProjectOrWorkspace(
        YarnLockResult yarnLockResult,
        NullSafePackageJson projectOrWorkspacePackageJson,
        List<NameVersion> externalDependencies
    ) throws MissingExternalIdException {
        LazyExternalIdDependencyGraphBuilder graphBuilder = new LazyExternalIdDependencyGraphBuilder();
        addRootDependenciesForProjectOrWorkspace(graphBuilder, projectOrWorkspacePackageJson, yarnLockResult.getWorkspaceData());
        Map<String, Map<String, String>> resolvedEntryIdVersionMap = recordAllEntriesAndVersions(yarnLockResult);
        for (YarnLockEntry entry : yarnLockResult.getYarnLock().getEntries()) {
            String entryName = entry.getIds().get(0).getName();
            if (shouldInclude(entryName, entry.getVersion())) {
                // Yarn and patch libraries should not be included in the graph.
                for (YarnLockEntryId entryId : entry.getIds()) {
                    LazyId id = generateComponentDependencyId(entryId.getName(), entryId.getVersion());
                    graphBuilder.setDependencyInfo(id, entryId.getName(), entry.getVersion(), generateComponentExternalId(entryId.getName(), entry.getVersion()));
                    addYarnLockDependenciesToGraph(yarnLockResult, graphBuilder, entry, id);
                }
            }
        }
        return graphBuilder.build(getLazyBuilderHandler(externalDependencies, resolvedEntryIdVersionMap));
    }
    
    private Map<String, Map<String, String>> recordAllEntriesAndVersions(YarnLockResult yarnLockResult) {
        Map<String, Map<String, String>> resolvedEntryIdVersionMap = new HashMap<>();
        for (YarnLockEntry entry : yarnLockResult.getYarnLock().getEntries()) {
            String entryName = entry.getIds().get(0).getName();
            int start;
            if ((start = entryName.indexOf("@npm:")) > 0) {
                String parentEntryName = entryName.substring(0, start);
                recordEntryAndVersionIntoMap(resolvedEntryIdVersionMap, entry, parentEntryName);
                entryName = entryName.substring(start + "@npm:".length());
            }
            recordEntryAndVersionIntoMap(resolvedEntryIdVersionMap, entry, entryName);
        }
        return resolvedEntryIdVersionMap;
    }
    
    private void recordEntryAndVersionIntoMap(Map<String, Map<String, String>> resolvedEntryIdVersionMap, YarnLockEntry entry, String entryName) {
        Map<String, String> entryIdsToResolvedVersionMap = resolvedEntryIdVersionMap.computeIfAbsent(entryName, k -> new HashMap<>());
        if (shouldInclude(entryName, entry.getVersion())) {
            // Yarn and patch libraries should not be included in the graph.
            for (YarnLockEntryId entryId : entry.getIds()) {
                Optional<String> existingVersionvalue = Optional.ofNullable(entryIdsToResolvedVersionMap.putIfAbsent(entryId.getVersion(), entry.getVersion()));
                if (existingVersionvalue.isPresent() && !existingVersionvalue.get().equals(entry.getVersion())) {
                    logger.warn("Invalid data condition detected in the yarn.lock file: {} has the same entry {} mapped to different resolved versions. The later version will be chosen.", entryName, entryId.getVersion());
                    entryIdsToResolvedVersionMap.put(entryId.getVersion(), entry.getVersion());
                }
            }
        }
    }

    private void addYarnLockDependenciesToGraph(YarnLockResult yarnLockResult, LazyExternalIdDependencyGraphBuilder graphBuilder, YarnLockEntry entry, LazyId id) {
        for (YarnLockDependency dependency : entry.getDependencies()) {
            if (!isWorkspace(yarnLockResult.getWorkspaceData(), dependency)) {
                LazyId stringDependencyId = generateComponentDependencyId(dependency.getName(), dependency.getVersion());
                if (yarnDependencyTypeFilter.shouldInclude(YarnDependencyType.NON_PRODUCTION) || !dependency.isOptional()) {
                    graphBuilder.addChildWithParent(stringDependencyId, id);
                } else {
                    logger.trace("Excluding optional dependency: {}", stringDependencyId);
                }
            }
        }
    }
    
    private DependencyGraph buildGraphForProject(
            LazyBuilderMissingExternalIdHandler lazyBuilderHandler,
            ExternalIdDependencyGraphBuilder graphBuilder,
            YarnLockResult yarnLockResult
    ) throws MissingExternalIdException {
        BasicDependencyGraph mutableDependencyGraph = new BasicDependencyGraph();
        Map<String, Map<String, String>> resolvedEntryIdVersionMap = recordAllEntriesAndVersions(yarnLockResult);
        
        for (YarnLockEntry entry : yarnLockResult.getYarnLock().getEntries()) {
            String entryName = entry.getIds().get(0).getName();
            if (shouldInclude(entryName, entry.getVersion())) {
                LazyId id = generateComponentDependencyId(entryName, entry.getVersion());
                graphBuilder.setDependencyInfo(id, entryName, entry.getVersion(), generateComponentExternalId(entryName, entry.getVersion()));
                LazyExternalIdDependencyGraphBuilder.LazyDependencyInfo parentInfo = graphBuilder.checkAndHandleMissingExternalId(lazyBuilderHandler, id);
                Dependency parent = new Dependency(parentInfo.getName(), parentInfo.getVersion(), parentInfo.getExternalId(), null);
                mutableDependencyGraph.addDirectDependency(parent);
                collectYarnDependencies(lazyBuilderHandler, graphBuilder, mutableDependencyGraph, yarnLockResult, entry, resolvedEntryIdVersionMap, parent);
            }
        }
        return mutableDependencyGraph;
    }
    
    private boolean shouldInclude(String entryName, String entryVersion) {
        return !entryName.contains("@patch:") && !entryName.startsWith("yarnpkg") && !entryVersion.equalsIgnoreCase("0.0.0-use.local"); 
    }
    
    private void collectYarnDependencies(
            LazyBuilderMissingExternalIdHandler lazyBuilderHandler,
            ExternalIdDependencyGraphBuilder graphBuilder,
            BasicDependencyGraph mutableDependencyGraph,
            YarnLockResult yarnLockResult,
            YarnLockEntry entry,
            Map<String, Map<String, String>> resolvedEntryIdVersionMap,
            Dependency parent
            ) throws MissingExternalIdException {
        for (YarnLockDependency dependency : entry.getDependencies()) {
            if (!isWorkspace(yarnLockResult.getWorkspaceData(), dependency)) {
                String dependencyVersion = getDependencyVersion(resolvedEntryIdVersionMap, dependency);
                LazyId stringDependencyId = generateComponentDependencyId(dependency.getName(), dependencyVersion);
                includeNonProductionOrOptionalIfNeeded(dependency, dependencyVersion, parent,lazyBuilderHandler, graphBuilder, mutableDependencyGraph, stringDependencyId);
            }
        }
    }

    private String getDependencyVersion(Map<String, Map<String, String>> resolvedEntryIdVersionMap, YarnLockDependency dependency) {
        Map<String, String> idVersionMap = resolvedEntryIdVersionMap.get(dependency.getName());
        String dependencyVersion;
        if (idVersionMap != null) {
            dependencyVersion = idVersionMap.get(dependency.getVersion());
            if (dependencyVersion == null) {
                if (idVersionMap.values().isEmpty()) {
                    logger.warn("Dependency {} with version definition {} not found in the Yarn map entries {}", dependency.getName(), dependency.getVersion(), idVersionMap.toString());
                    dependencyVersion = dependency.getVersion();
                } else {
                    // 1. Choose first version.
                    dependencyVersion = (String) idVersionMap.values().toArray()[0];
                }
                // 2. Try to auto-resolve to one of the versions.
            }
        } else {
            dependencyVersion = dependency.getVersion();
        }
        return dependencyVersion;
    }

    private void includeNonProductionOrOptionalIfNeeded(YarnLockDependency dependency, String dependencyVersion, Dependency parent, LazyBuilderMissingExternalIdHandler lazyBuilderHandler, ExternalIdDependencyGraphBuilder graphBuilder, BasicDependencyGraph mutableDependencyGraph, LazyId stringDependencyId) throws MissingExternalIdException {
        if (yarnDependencyTypeFilter.shouldInclude(YarnDependencyType.NON_PRODUCTION) || !dependency.isOptional()) {
            graphBuilder.setDependencyInfo(stringDependencyId, dependency.getName(), dependencyVersion, generateComponentExternalId(dependency.getName(), dependencyVersion));
            LazyDependencyInfo childInfo = graphBuilder.checkAndHandleMissingExternalId(lazyBuilderHandler, stringDependencyId);
            Dependency child = new Dependency(childInfo.getName(), childInfo.getVersion(), childInfo.getExternalId(), null);
            mutableDependencyGraph.addChildWithParent(child, parent);

        } else {
            logger.trace("Excluding optional dependency: {}", stringDependencyId);
        }
    }

    private boolean isWorkspace(YarnWorkspaces yarnWorkspaces, YarnLockDependency dependency) {
        Optional<YarnWorkspace> dependencyWorkspace = yarnWorkspaces.lookup(dependency);
        return dependencyWorkspace.isPresent();
    }

    private LazyBuilderMissingExternalIdHandler getLazyBuilderHandler(List<NameVersion> externalDependencies) {
        return (dependencyId, lazyDependencyInfo) -> {
            Optional<NameVersion> externalDependency = externalDependencies.stream().filter(it -> it.getName().equals(lazyDependencyInfo.getName())).findFirst();
            Optional<ExternalId> externalId = externalDependency.map(it -> generateComponentExternalId(it.getName(), it.getVersion()));
            if (externalId.isPresent()) {
                return externalId.get();
            } else {
                Optional<NameVersion> nameVersionOptional;
                if (!unMatchedDependencies.containsKey(dependencyId)) {
                    String dependencyIdClean = dependencyId.toString().replace("\\/","/");
                    nameVersionOptional = versionUtility.getNameVersion(dependencyIdClean);
                    logger.warn("No standard NPM package identification details are available for '{}' from the yarn.lock file", dependencyIdClean);
                    unMatchedDependencies.put(dependencyId, nameVersionOptional);
                } else {
                    nameVersionOptional = unMatchedDependencies.get(dependencyId);
                }
                return generateComponentExternalId(nameVersionOptional.get().getName(), nameVersionOptional.get().getVersion());
            }
        };
    }

    private LazyBuilderMissingExternalIdHandler getLazyBuilderHandler(List<NameVersion> externalDependencies, Map<String, Map<String, String>> resolvedEntryIdVersionMap) {
        return (dependencyId, lazyDependencyInfo) -> {
            return getExternalIdForEachDependency(dependencyId, lazyDependencyInfo, externalDependencies, resolvedEntryIdVersionMap);
        };
    }
    
    private ExternalId getExternalIdForEachDependency(LazyId dependencyId, LazyExternalIdDependencyGraphBuilder.LazyDependencyInfo lazyDependencyInfo, List<NameVersion> externalDependencies, Map<String, Map<String, String>> resolvedEntryIdVersionMap) {
        String dependencyIdClean = dependencyId.toString().replace("\\/","/");
        Optional<NameVersion> nameVersionOptional = versionUtility.getNameVersion(dependencyIdClean);
        if (nameVersionOptional.isPresent() && resolvedEntryIdVersionMap.containsKey(nameVersionOptional.get().getName())) {
            String name = nameVersionOptional.get().getName();
            Map<String, String> entryIdVersionMap = resolvedEntryIdVersionMap.get(name);
            String version = nameVersionOptional.get().getVersion();
            if (entryIdVersionMap.containsKey(version)) {
                return generateComponentExternalId(name, entryIdVersionMap.get(version));
            } else {
                List<Version> versionList = entryIdVersionMap.values().stream().map(k -> versionUtility.buildVersion(k)).collect(Collectors.toList());
                Optional<String> resolvedVersion = versionUtility.resolveYarnVersion(versionList, version);
                if (resolvedVersion.isPresent()) {
                    return generateComponentExternalId(name, resolvedVersion.get());
                }
            }
        } else {
            Optional<NameVersion> externalDependency = externalDependencies.stream().filter(it -> it.getName().equals(lazyDependencyInfo.getName())).findFirst();
            Optional<ExternalId> externalId = externalDependency.map(it -> generateComponentExternalId(it.getName(), it.getVersion()));
            if (externalId.isPresent()) {
                return externalId.get();
            }
        }
        if (!unMatchedDependencies.containsKey(dependencyId)) {
            logger.warn("No standard NPM package identification details are available for '{}' from the yarn.lock file", dependencyIdClean);
            unMatchedDependencies.put(dependencyId, nameVersionOptional);
        } else {
            nameVersionOptional = unMatchedDependencies.get(dependencyId);
        }
        if (nameVersionOptional.isPresent()) {
            return generateComponentExternalId(nameVersionOptional.get().getName(), nameVersionOptional.get().getVersion());
        } else {
            return generateComponentExternalId(dependencyId);
        }
    }
    
    private void addRootDependenciesForProject(YarnLockResult yarnLockResult, NullSafePackageJson projectPackageJson, LazyExternalIdDependencyGraphBuilder graphBuilder) {
        addRootDependenciesToGraph(graphBuilder, projectPackageJson.getDependencies(), yarnLockResult.getWorkspaceData());
        addRootDependenciesToGraph(graphBuilder, projectPackageJson.getOptionalDependencies(), yarnLockResult.getWorkspaceData());
        addRootDependenciesToGraph(graphBuilder, projectPackageJson.getPeerDependencies(), yarnLockResult.getWorkspaceData());
        if (yarnDependencyTypeFilter.shouldInclude(YarnDependencyType.NON_PRODUCTION)) {
            addRootDependenciesToGraph(graphBuilder, projectPackageJson.getDevDependencies(), yarnLockResult.getWorkspaceData());
        }
    }

    private void addRootDependenciesForProjectOrWorkspace(LazyExternalIdDependencyGraphBuilder graphBuilder, NullSafePackageJson rootPackageJson, YarnWorkspaces workspaceData) {
        addRootDependenciesToGraph(graphBuilder, rootPackageJson.getDependencies(), workspaceData);
        addRootDependenciesToGraph(graphBuilder, rootPackageJson.getOptionalDependencies(), workspaceData);
        addRootDependenciesToGraph(graphBuilder, rootPackageJson.getPeerDependencies(), workspaceData);
        if (yarnDependencyTypeFilter.shouldInclude(YarnDependencyType.NON_PRODUCTION)) {
            addRootDependenciesToGraph(graphBuilder, rootPackageJson.getDevDependencies(), workspaceData);
        }
    }

    private void addRootDependenciesToGraph(LazyExternalIdDependencyGraphBuilder graphBuilder, Map<String, String> rootDependenciesToAdd, YarnWorkspaces workspaceData) {
        for (Map.Entry<String, String> rootDependency : rootDependenciesToAdd.entrySet()) {
            Optional<YarnWorkspace> dependencyWorkspace = workspaceData.lookup(rootDependency.getKey(), rootDependency.getValue());
            if (dependencyWorkspace.isPresent()) {
                logger.trace("Omitting dependency {}/{} because it's a workspace", rootDependency.getKey(), rootDependency.getValue());
            } else {
                LazyId stringDependencyId = generateComponentDependencyId(rootDependency.getKey(), rootDependency.getValue());
                logger.debug("Adding root dependency to graph: stringDependencyId: {}", stringDependencyId);
                graphBuilder.addChildToRoot(stringDependencyId);
            }
        }
    }

    private LazyId generateComponentDependencyId(String name, String version) {
        return LazyId.fromNameAndVersion(name, version);
    }

    private ExternalId generateComponentExternalId(String name, String version) {
        return externalIdFactory.createNameVersionExternalId(Forge.NPMJS, name, version);
    }

    private ExternalId generateComponentExternalId(LazyId dependencyId) {
        return externalIdFactory.createNameVersionExternalId(Forge.NPMJS, dependencyId.toString());
    }
}
