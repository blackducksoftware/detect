package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.JavaCoordinates;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.JavaRepository;
import com.blackduck.integration.detectable.detectables.maven.resolver.graph.MavenGraphParser;
import com.blackduck.integration.detectable.detectables.maven.resolver.graph.MavenGraphTransformer;
import com.blackduck.integration.detectable.detectables.maven.resolver.graph.MavenParseResult;
import com.blackduck.integration.detectable.detectables.maven.resolver.mavendownload.MavenDownloader;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.pomxml.PomXml;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.pomxml.PomXmlPlugin;
import com.blackduck.integration.detectable.detectables.maven.resolver.pom.MavenProject;
import com.blackduck.integration.detectable.detectables.maven.resolver.pom.ProjectBuilder;
import com.blackduck.integration.detectable.detectables.maven.resolver.proxy.MavenProxyConfig;
import com.blackduck.integration.detectable.detectables.maven.resolver.proxy.MavenProxyConfigurator;
import com.blackduck.integration.detectable.detectables.maven.resolver.resolution.MavenDependencyResolver;
import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.ShadedDependencyInspector;
import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.methods.DeltaAnalysisInspector;
import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.methods.RecursiveMetadataInspector;
import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.model.DiscoveredDependency;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.DependencyNode;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

/**
 * Owns the full shaded dependency detection pipeline:
 * <ol>
 *   <li>Extract Aether dependencies from CollectResult</li>
 *   <li>Pre-filter: check POMs for maven-shade-plugin</li>
 *   <li>Download JARs</li>
 *   <li>Scan JARs for shaded dependencies (Delta Analysis + Recursive Metadata)</li>
 *   <li>Graft discovered shaded dependency sub-trees into BDIO graphs</li>
 * </ol>
 *
 * <p>Both MavenResolverDetectable (root module) and MavenModuleProcessor (sub-modules)
 * delegate to this class via a single {@link #processShading} call.
 *
 * @see DeltaAnalysisInspector
 * @see RecursiveMetadataInspector
 */
public class ShadedDependencyScanner {

    private static final Logger logger = LoggerFactory.getLogger(ShadedDependencyScanner.class);
    private static final String MAVEN_SCOPE_COMPILE = "compile";

    private final ExternalIdFactory externalIdFactory;
    private final ProjectBuilder projectBuilder;
    private final MavenDependencyResolver dependencyResolver;
    private final MavenGraphParser graphParser;
    private final MavenGraphTransformer graphTransformer;
    @Nullable
    private final MavenProxyConfig proxyConfig;

    public ShadedDependencyScanner(
            ExternalIdFactory externalIdFactory,
            ProjectBuilder projectBuilder,
            MavenDependencyResolver dependencyResolver,
            MavenGraphParser graphParser,
            MavenGraphTransformer graphTransformer,
            @Nullable MavenProxyConfig proxyConfig
    ) {
        this.externalIdFactory = externalIdFactory;
        this.projectBuilder = projectBuilder;
        this.dependencyResolver = dependencyResolver;
        this.graphParser = graphParser;
        this.graphTransformer = graphTransformer;
        this.proxyConfig = proxyConfig;
    }

    /**
     * Runs the full shaded dependency pipeline: extract → pre-filter → download → scan → graft.
     *
     * @param compileResult     Compile-scope Aether CollectResult
     * @param testResult        Test-scope Aether CollectResult (may be null)
     * @param compileGraph      Compile-scope BDIO DependencyGraph to graft onto
     * @param testGraph         Test-scope BDIO DependencyGraph to graft onto (may be null)
     * @param localRepoPath     Path to Aether's local repository (for POM cache reads)
     * @param downloadDir       Directory for downloading JARs and POMs
     * @param repositories      Maven repositories declared in the POM
     * @param shadedSubTreeCache Shared cache for resolved shaded sub-trees (reused across scopes/modules)
     * @param options           Maven resolver options (custom repo path, feature flags)
     */
    public void processShading(
            CollectResult compileResult,
            CollectResult testResult,
            DependencyGraph compileGraph,
            DependencyGraph testGraph,
            Path localRepoPath,
            Path downloadDir,
            List<JavaRepository> repositories,
            Map<String, DependencyGraph> shadedSubTreeCache,
            MavenResolverOptions options
    ) {
        try {
            // Step 1: Extract Aether dependencies from CollectResult trees
            List<org.eclipse.aether.graph.Dependency> aetherDependencies = new ArrayList<>();
            Set<String> seenGavs = new HashSet<>();

            if (compileResult != null && compileResult.getRoot() != null) {
                extractAetherDependenciesFromNode(compileResult.getRoot(), aetherDependencies, seenGavs);
                logger.debug("Extracted {} dependencies from compile scope", aetherDependencies.size());
            }

            if (aetherDependencies.isEmpty()) {
                logger.debug("No dependencies to process for shaded detection");
                return;
            }

            // Step 2: Pre-filter — only keep deps whose POM declares maven-shade-plugin
            aetherDependencies = filterToShadedCandidates(aetherDependencies, localRepoPath);
            if (aetherDependencies.isEmpty()) {
                logger.info("No dependencies use maven-shade-plugin, skipping JAR downloads");
                return;
            }

            // Steps 3-5 involve network operations — wrap with proxy set/restore
            MavenProxyConfigurator proxyConfigurator = null;
            if (proxyConfig != null) {
                proxyConfigurator = new MavenProxyConfigurator(proxyConfig);
                proxyConfigurator.configureSystemProxyProperties();
            }

            try {
                // Step 3: Download JARs
                Files.createDirectories(downloadDir);
                ArtifactDownloader artifactDownloader = new ArtifactDownloader(
                        options.getJarRepositoryPath(),
                        downloadDir,
                        repositories != null ? repositories : new ArrayList<>(),
                        options
                );

                logger.info("Starting artifact downloads for {} shaded candidates...", aetherDependencies.size());
                Map<Artifact, Path> downloadedJars = artifactDownloader.downloadArtifacts(aetherDependencies);
                logger.info("Downloaded {} JARs", downloadedJars.size());

                if (downloadedJars.isEmpty()) {
                    logger.debug("No JARs downloaded, skipping shaded detection");
                    return;
                }

                // Step 4: Scan JARs for shaded dependencies
                Map<Artifact, List<DiscoveredDependency>> shadedDepsMap = scanJarsForShadedDependencies(
                        downloadedJars, compileResult, testResult);

                logger.info("Shaded dependency scan complete. Found {} JARs with shaded dependencies.", shadedDepsMap.size());

                if (shadedDepsMap.isEmpty()) {
                    return;
                }

                // Step 5: Graft shaded dependency sub-trees into graphs
                File downloadDirFile = downloadDir.toFile();
                File localRepoFile = localRepoPath.toFile();
                List<JavaRepository> repos = repositories != null ? repositories : new ArrayList<>();

                int addedToCompile = addShadedDependenciesToGraph(
                        compileGraph, shadedDepsMap, downloadDirFile, localRepoFile, repos, shadedSubTreeCache);
                logger.info("Grafted {} shaded dependency nodes into compile graph", addedToCompile);

                if (testGraph != null) {
                    int addedToTest = addShadedDependenciesToGraph(
                            testGraph, shadedDepsMap, downloadDirFile, localRepoFile, repos, shadedSubTreeCache);
                    logger.info("Grafted {} shaded dependency nodes into test graph", addedToTest);
                }
            } finally {
                if (proxyConfigurator != null) {
                    proxyConfigurator.restoreOriginalProxyProperties();
                }
            }

        } catch (Exception e) {
            logger.warn("Failed to process shaded dependencies: {}", e.getMessage());
            logger.debug("Shaded dependency processing error:", e);
        }
    }

    // ==========================================
    // Step 1: Extract Aether Dependencies
    // ==========================================

    private void extractAetherDependenciesFromNode(
            DependencyNode node,
            List<org.eclipse.aether.graph.Dependency> dependencies,
            Set<String> seenGavs
    ) {
        if (node == null) {
            return;
        }

        org.eclipse.aether.graph.Dependency dependency = node.getDependency();
        if (dependency != null && dependency.getArtifact() != null) {
            String gavKey = dependency.getArtifact().getGroupId() + ":"
                    + dependency.getArtifact().getArtifactId() + ":"
                    + dependency.getArtifact().getVersion();
            if (seenGavs.add(gavKey)) {
                dependencies.add(dependency);
            }
        }

        if (node.getChildren() != null) {
            for (DependencyNode child : node.getChildren()) {
                extractAetherDependenciesFromNode(child, dependencies, seenGavs);
            }
        }
    }

    // ==========================================
    // Step 2: Pre-filter (maven-shade-plugin check)
    // ==========================================

    private List<org.eclipse.aether.graph.Dependency> filterToShadedCandidates(
            List<org.eclipse.aether.graph.Dependency> allDeps,
            Path localRepoPath
    ) {
        int total = allDeps.size();
        List<org.eclipse.aether.graph.Dependency> candidates = new ArrayList<>();
        int keptShaded = 0;
        int keptFailOpen = 0;
        int skipped = 0;

        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        for (org.eclipse.aether.graph.Dependency dep : allDeps) {
            Artifact artifact = dep.getArtifact();
            if (artifact == null) {
                continue;
            }

            String groupId = artifact.getGroupId();
            String artifactId = artifact.getArtifactId();
            String version = artifact.getVersion();

            Path pomPath = localRepoPath
                    .resolve(groupId.replace('.', '/'))
                    .resolve(artifactId)
                    .resolve(version)
                    .resolve(artifactId + "-" + version + ".pom");

            if (!Files.exists(pomPath)) {
                logger.debug("Pre-filter: POM not found for {}:{}:{}, keeping as candidate (fail open)", groupId, artifactId, version);
                candidates.add(dep);
                keptFailOpen++;
                continue;
            }

            try {
                PomXml pomXml = xmlMapper.readValue(pomPath.toFile(), PomXml.class);

                if (hasShadePlugin(pomXml)) {
                    logger.debug("Pre-filter: maven-shade-plugin found in {}:{}:{}", groupId, artifactId, version);
                    candidates.add(dep);
                    keptShaded++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                logger.debug("Pre-filter: failed to parse POM for {}:{}:{}, keeping as candidate: {}", groupId, artifactId, version, e.getMessage());
                candidates.add(dep);
                keptFailOpen++;
            }
        }

        logger.info("Pre-filter complete: {} total deps, {} with shade plugin, {} skipped (no plugin), {} kept due to missing/unparseable POM",
                total, keptShaded, skipped, keptFailOpen);
        return candidates;
    }

    private boolean hasShadePlugin(PomXml pomXml) {
        if (pomXml.getBuild() == null || pomXml.getBuild().getPlugins() == null) {
            return false;
        }
        for (PomXmlPlugin plugin : pomXml.getBuild().getPlugins()) {
            if ("maven-shade-plugin".equals(plugin.getArtifactId())) {
                return true;
            }
        }
        return false;
    }

    // ==========================================
    // Step 4: Scan JARs for shaded dependencies
    // ==========================================

    private Map<Artifact, List<DiscoveredDependency>> scanJarsForShadedDependencies(
            Map<Artifact, Path> artifactJarPaths,
            CollectResult compileResult,
            CollectResult testResult) {

        Map<Artifact, List<DiscoveredDependency>> results = new LinkedHashMap<>();

        if (artifactJarPaths == null || artifactJarPaths.isEmpty()) {
            logger.info("No JAR files provided for shaded dependency scanning.");
            return results;
        }

        int totalJars = artifactJarPaths.size();
        logger.info("Starting shaded dependency scan for {} JAR file(s)...", totalJars);

        Map<String, Set<String>> aetherDirectChildrenByGa = new HashMap<>();
        if (compileResult != null && compileResult.getRoot() != null) {
            buildAetherDirectChildrenMap(compileResult.getRoot(), aetherDirectChildrenByGa);
        }
        if (testResult != null && testResult.getRoot() != null) {
            buildAetherDirectChildrenMap(testResult.getRoot(), aetherDirectChildrenByGa);
        }

        logger.debug("Built Aether direct children map with {} GA entries.", aetherDirectChildrenByGa.size());

        int processedCount = 0;
        int jarsWithShadedDeps = 0;
        int totalShadedDepsFound = 0;

        for (Map.Entry<Artifact, Path> entry : artifactJarPaths.entrySet()) {
            Artifact artifact = entry.getKey();
            Path jarPath = entry.getValue();
            processedCount++;

            String artifactGav = formatArtifactGav(artifact);
            logger.debug("Scanning JAR {}/{}: {}", processedCount, totalJars, artifactGav);

            if (jarPath == null || !Files.exists(jarPath)) {
                logger.warn("JAR file not found for {}, skipping shaded dependency scan. Expected path: {}",
                        artifactGav, jarPath);
                continue;
            }

            List<ShadedDependencyInspector> inspectors = new ArrayList<>();
            inspectors.add(new DeltaAnalysisInspector(aetherDirectChildrenByGa, projectBuilder, artifact));
            inspectors.add(new RecursiveMetadataInspector(artifact));
            logger.debug("Created {} inspector(s) for artifact: {}", inspectors.size(), artifactGav);

            try {
                List<DiscoveredDependency> shadedDeps = scanSingleJar(jarPath, inspectors, artifactGav);

                if (!shadedDeps.isEmpty()) {
                    results.put(artifact, shadedDeps);
                    jarsWithShadedDeps++;
                    totalShadedDepsFound += shadedDeps.size();
                    logger.debug("Found {} shaded dependency(ies) in: {}", shadedDeps.size(), artifactGav);
                }
            } catch (Exception e) {
                logger.warn("Failed to scan JAR for shaded dependencies: {} - Error: {}", artifactGav, e.getMessage());
                logger.debug("Stack trace for failed JAR scan:", e);
            }
        }

        logger.info("Shaded dependency scan complete.");
        logger.info("  - JARs scanned: {}", totalJars);
        logger.info("  - JARs with shaded dependencies: {}", jarsWithShadedDeps);
        logger.info("  - Total shaded dependencies found: {}", totalShadedDepsFound);

        return results;
    }

    private List<DiscoveredDependency> scanSingleJar(Path jarPath, List<ShadedDependencyInspector> inspectors,
                                                      String artifactGav) throws Exception {
        Map<String, DiscoveredDependency> deduplicatedDeps = new LinkedHashMap<>();

        JarFile jarFile = null;
        try {
            jarFile = new JarFile(jarPath.toFile());

            for (ShadedDependencyInspector inspector : inspectors) {
                String inspectorName = inspector.getClass().getSimpleName();
                logger.debug("  Running {} on: {}", inspectorName, artifactGav);

                try {
                    List<DiscoveredDependency> inspectorResults = inspector.detectShadedDependencies(jarFile);

                    if (inspectorResults != null) {
                        for (DiscoveredDependency dep : inspectorResults) {
                            String identifier = dep.getIdentifier();

                            if (deduplicatedDeps.containsKey(identifier)) {
                                DiscoveredDependency existing = deduplicatedDeps.get(identifier);
                                logger.debug("    Duplicate found: {} (already detected by {})",
                                        identifier, existing.getDetectionSource());
                            } else {
                                deduplicatedDeps.put(identifier, dep);
                                logger.debug("    Discovered: {} via {}", identifier, dep.getDetectionSource());
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("  Inspector {} failed on {}: {}", inspectorName, artifactGav, e.getMessage());
                }
            }
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (Exception ignored) {
                }
            }
        }

        return new ArrayList<>(deduplicatedDeps.values());
    }

    // ==========================================
    // Step 5: Graft shaded dependencies into graphs
    // ==========================================

    private int addShadedDependenciesToGraph(
            DependencyGraph mainGraph,
            Map<Artifact, List<DiscoveredDependency>> shadedDepsMap,
            File downloadDir,
            File localRepoPath,
            List<JavaRepository> repositories,
            Map<String, DependencyGraph> shadedSubTreeCache) {

        int graftedNodeCount = 0;

        for (Map.Entry<Artifact, List<DiscoveredDependency>> entry : shadedDepsMap.entrySet()) {
            Artifact parentArtifact = entry.getKey();
            List<DiscoveredDependency> shadedDeps = entry.getValue();

            logger.debug("Processing {} shaded dependencies for parent artifact: {}:{}:{}",
                    shadedDeps.size(),
                    parentArtifact.getGroupId(),
                    parentArtifact.getArtifactId(),
                    parentArtifact.getVersion());

            ExternalId parentExternalId = externalIdFactory.createMavenExternalId(
                    parentArtifact.getGroupId(), parentArtifact.getArtifactId(), parentArtifact.getVersion());
            Dependency parentDependency = mainGraph.getDependency(parentExternalId);

            if (parentDependency == null) {
                logger.warn("Parent artifact {}:{}:{} not found in dependency graph. Skipping shaded dependencies for this artifact.",
                        parentArtifact.getGroupId(), parentArtifact.getArtifactId(), parentArtifact.getVersion());
                continue;
            }

            for (DiscoveredDependency shadedDep : shadedDeps) {
                try {
                    String identifier = shadedDep.getIdentifier();
                    if (identifier == null || identifier.isEmpty()) {
                        logger.warn("Skipping shaded dependency with null/empty identifier in parent: {}",
                                parentArtifact.getArtifactId());
                        continue;
                    }

                    String[] gavParts = identifier.split(":");
                    if (gavParts.length < 3) {
                        logger.warn("Skipping shaded dependency with invalid GAV format: {}", identifier);
                        continue;
                    }

                    String groupId = gavParts[0];
                    String artifactId = gavParts[1];
                    String version = gavParts[2];

                    if (groupId.isEmpty() || artifactId.isEmpty() || version.isEmpty() || "UNKNOWN".equals(version)) {
                        logger.debug("Skipping shaded dependency with incomplete coordinates: {}", identifier);
                        continue;
                    }

                    String gavKey = groupId + ":" + artifactId + ":" + version;
                    DependencyGraph isolatedShadedGraph = shadedSubTreeCache.get(gavKey);

                    if (isolatedShadedGraph == null) {
                        logger.debug("Resolving sub-tree for shaded dependency: {}", identifier);

                        JavaCoordinates coords = new JavaCoordinates(groupId, artifactId, version, "pom");
                        MavenDownloader downloader = new MavenDownloader(repositories, downloadDir.toPath());
                        File shadedPomFile = downloader.downloadPom(coords);

                        if (shadedPomFile == null || !shadedPomFile.exists()) {
                            logger.warn("Could not download POM for shaded dependency {}. Sub-tree resolution aborted.", identifier);
                            ExternalId fallbackId = externalIdFactory.createMavenExternalId(groupId, artifactId, version);
                            mainGraph.addChildWithParent(new Dependency(artifactId, version, fallbackId), parentDependency);
                            graftedNodeCount++;
                            logger.info("Added fallback single node for shaded dependency: {} (parent: {})",
                                    identifier, parentArtifact.getArtifactId());
                            continue;
                        }

                        logger.debug("Successfully downloaded POM for shaded dependency: {}", identifier);

                        MavenProject shadedProject = projectBuilder.buildProject(shadedPomFile);
                        logger.debug("Built MavenProject for shaded dependency: {} with {} direct dependencies",
                                identifier, shadedProject.getDependencies().size());

                        CollectResult collectResult = dependencyResolver.resolveDependencies(
                                shadedPomFile, shadedProject, localRepoPath, MAVEN_SCOPE_COMPILE);
                        logger.debug("Resolved Aether tree for shaded dependency: {}", identifier);

                        //TODO : RECURSIVE SHADED INSPECTION - FIND SHADED DEPENDENCIES OF THESE SHADED DEPENDENCIES
                        //Aether only resolves unshaded dependencies, leaving shaded deps as gaps
                        //in the tree which may result in false negatives for vulnerabilities.

                        //TODO : ADD CYCLE DETECTION BEFORE IMPLEMENTING RECURSIVE SHADED INSPECTION
                        //If A shades B and B shades A (or any longer chain), recursive inspection
                        //will loop infinitely or blow the stack. Before making the above TODO work,
                        //add a Set<String> currentlyInspecting (artifact GAV keys) — skip any artifact
                        //already in the set to break cycles.

                        MavenParseResult parseResult = graphParser.parse(collectResult);
                        isolatedShadedGraph = graphTransformer.transform(parseResult);
                        logger.debug("Transformed shaded dependency {} to BDIO graph with {} root dependencies",
                                identifier, isolatedShadedGraph.getRootDependencies().size());

                        shadedSubTreeCache.put(gavKey, isolatedShadedGraph);
                    } else {
                        logger.debug("Cache hit for shaded dependency sub-tree: {}", gavKey);
                    }

                    ExternalId shadedDepExternalId = externalIdFactory.createMavenExternalId(groupId, artifactId, version);
                    Dependency shadedDependencyNode = new Dependency(artifactId, version, shadedDepExternalId);
                    mainGraph.addChildWithParent(shadedDependencyNode, parentDependency);
                    graftedNodeCount++;
                    logger.debug("Added shaded dependency node {} under parent {}", identifier, parentArtifact.getArtifactId());

                    // TODO: [Graph Intersection] Cross-reference isolatedShadedGraph nodes against
                    // the physical shadedDepsMap to filter out transitives excluded by shade plugin config.
                    int nodesAdded = graftSubGraph(isolatedShadedGraph, mainGraph, shadedDependencyNode);
                    graftedNodeCount += nodesAdded;
                    logger.debug("Grafted {} transitive nodes from shaded dependency {} under {}",
                            nodesAdded, identifier, identifier);

                } catch (Exception e) {
                    logger.error("Failed to resolve and graft sub-tree for shaded dependency: {} - Error: {}",
                            shadedDep.getIdentifier(), e.getMessage());
                    logger.debug("Stack trace for shaded dependency resolution failure:", e);
                }
            }
        }

        logger.debug("Total grafted node count for this graph: {}", graftedNodeCount);
        return graftedNodeCount;
    }

    private int graftSubGraph(DependencyGraph sourceGraph, DependencyGraph destGraph, Dependency destParent) {
        int count = 0;

        Set<Dependency> sourceRoots = sourceGraph.getRootDependencies();
        if (sourceRoots == null || sourceRoots.isEmpty()) {
            logger.debug("Source graph has no root dependencies to graft");
            return count;
        }

        logger.debug("Grafting {} root dependencies under parent: {}", sourceRoots.size(), destParent.getName());

        Set<ExternalId> visitedNodes = new HashSet<>();

        for (Dependency sourceRoot : sourceRoots) {
            destGraph.addChildWithParent(sourceRoot, destParent);
            count++;

            count += copyGraphEdges(sourceGraph, destGraph, sourceRoot, visitedNodes);
        }

        return count;
    }

    private int copyGraphEdges(DependencyGraph sourceGraph, DependencyGraph destGraph,
                               Dependency currentParent, Set<ExternalId> visitedNodes) {
        int count = 0;

        ExternalId currentId = currentParent.getExternalId();
        if (currentId != null && visitedNodes.contains(currentId)) {
            logger.warn("Cycle detected in dependency graph at node: {}:{}:{}. Skipping to prevent infinite recursion.",
                    currentId.getGroup(), currentId.getName(), currentId.getVersion());
            return count;
        }

        if (currentId != null) {
            visitedNodes.add(currentId);
        }

        try {
            Set<Dependency> children = sourceGraph.getChildrenForParent(currentParent);

            if (children == null || children.isEmpty()) {
                return count;
            }

            for (Dependency child : children) {
                ExternalId childId = child.getExternalId();
                if (childId != null && visitedNodes.contains(childId)) {
                    logger.warn("Cycle detected: {} already visited in current path. Skipping child to prevent infinite loop.",
                            childId.getGroup() + ":" + childId.getName() + ":" + childId.getVersion());
                    continue;
                }

                destGraph.addChildWithParent(child, currentParent);
                count++;
                logger.trace("Copied edge: {} -> {}", currentParent.getName(), child.getName());

                count += copyGraphEdges(sourceGraph, destGraph, child, visitedNodes);
            }
        } finally {
            if (currentId != null) {
                visitedNodes.remove(currentId);
            }
        }

        return count;
    }

    // ==========================================
    // Utilities
    // ==========================================

    private String formatArtifactGav(Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
    }

    private void buildAetherDirectChildrenMap(DependencyNode node, Map<String, Set<String>> map) {
        if (node == null || node.getDependency() == null || node.getDependency().getArtifact() == null) {
            return;
        }

        Artifact artifact = node.getDependency().getArtifact();
        String ga = artifact.getGroupId() + ":" + artifact.getArtifactId();

        Set<String> childrenGavs = new HashSet<>();
        if (node.getChildren() != null) {
            for (DependencyNode child : node.getChildren()) {
                if (child.getDependency() != null && child.getDependency().getArtifact() != null) {
                    Artifact childArtifact = child.getDependency().getArtifact();
                    childrenGavs.add(childArtifact.getGroupId() + ":" +
                            childArtifact.getArtifactId() + ":" +
                            childArtifact.getVersion());
                }
            }
        }

        if (!map.containsKey(ga)) {
            map.put(ga, childrenGavs);
        }

        if (node.getChildren() != null) {
            for (DependencyNode child : node.getChildren()) {
                buildAetherDirectChildrenMap(child, map);
            }
        }
    }
}
