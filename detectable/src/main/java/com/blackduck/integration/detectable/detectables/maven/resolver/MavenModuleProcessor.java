package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.JavaCoordinates;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.JavaRepository;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.PomXml;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.PomXmlPlugin;
import com.blackduck.integration.detectable.detectables.maven.resolver.result.MavenParseResult;
import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.model.DiscoveredDependency;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.DependencyNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Processor for handling Maven multi-module projects recursively.
 *
 * <p>This class encapsulates the logic for processing Maven modules, including:
 * <ul>
 *   <li>Path resolution and canonicalization</li>
 *   <li>Cycle detection to prevent infinite recursion</li>
 *   <li>Building effective POMs for each module</li>
 *   <li>Resolving dependencies for compile and test scopes</li>
 *   <li>Generating dependency trees and code locations</li>
 *   <li>Recursive processing of nested modules</li>
 * </ul>
 *
 * <p><strong>Design Pattern:</strong> Strategy pattern - encapsulates the module processing algorithm,
 * allowing it to be used independently or as part of a larger workflow.
 *
 * <p><strong>Error Handling:</strong> This processor is designed to be resilient. If processing fails
 * for a specific module, it logs the error, writes an error marker file, and continues processing
 * other modules to maximize extraction coverage.
 */
class MavenModuleProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MavenModuleProcessor.class);
    private static final String POM_XML_FILENAME = "pom.xml";

    /**
     * Processes all modules declared in a Maven project.
     *
     * <p>This is the main entry point for module processing. It iterates through the list
     * of modules and processes each one recursively.
     *
     * @param modules List of module identifiers from the parent POM's {@code <modules>} section
     * @param parentDir The parent directory containing the modules
     * @param context Processing context containing all dependencies and shared state
     */
    public void processModules(List<String> modules, File parentDir, MavenModuleProcessingContext context) {
        if (modules == null || modules.isEmpty()) {
            return;
        }

        for (String module : modules) {
            processModuleRecursive(module, parentDir, context);
        }
    }

    /**
     * Recursively processes a single Maven module.
     *
     * <p>This method handles the complete workflow for a Maven module:
     * <ol>
     *   <li>Path resolution and validation</li>
     *   <li>Canonicalization and cycle detection</li>
     *   <li>Effective POM building</li>
     *   <li>Dependency resolution (compile and test scopes)</li>
     *   <li>Dependency tree file writing</li>
     *   <li>Graph transformation and CodeLocation creation</li>
     *   <li>Recursive processing of nested modules</li>
     * </ol>
     *
     * @param moduleEntry The module identifier (e.g., "submodule" or "path/to/module")
     * @param parentDir The parent directory containing this module
     * @param context Processing context with all dependencies and shared state
     */
    private void processModuleRecursive(String moduleEntry, File parentDir, MavenModuleProcessingContext context) {
        try {
            // STEP 1: Early exit for null or empty module entries
            if (moduleEntry == null || moduleEntry.trim().isEmpty()) {
                return;
            }
            String modulePathStr = moduleEntry.trim();

            // STEP 2: Resolve module path to its pom.xml file
            File modulePom = resolveModulePomFile(modulePathStr, parentDir);
            if (modulePom == null) {
                return; // Warning already logged in resolveModulePomFile
            }

            // STEP 3: Canonicalize and check for cycles
            File canonicalModulePom = canonicalizeFile(modulePom);
            String modulePomPathKey = getCanonicalPathSafe(canonicalModulePom);

            if (context.getVisitedModulePomPaths().contains(modulePomPathKey)) {
                logger.debug("Skipping already-visited module POM: {}", modulePomPathKey);
                return;
            }
            context.getVisitedModulePomPaths().add(modulePomPathKey);

            logger.info("Processing module POM: {}", canonicalModulePom.getAbsolutePath());

            // STEP 4: Build the effective Maven project model for this module
            MavenProject moduleProject = buildModuleProject(canonicalModulePom, modulePomPathKey, context);
            if (moduleProject == null) {
                return; // Error already logged and error file written
            }

            // STEP 5: Resolve dependencies for both compile and test scopes
            CollectResult moduleCollectCompile = resolveCompileDependencies(
                canonicalModulePom, moduleProject, modulePomPathKey, context);
            if (moduleCollectCompile == null) {
                return; // Error already logged and error file written
            }

            CollectResult moduleCollectTest = resolveTestDependencies(
                canonicalModulePom, moduleProject, modulePomPathKey, context);
            // Test scope failure is not critical - continue processing

            // STEP 6: Write dependency trees to files for debugging
            writeDependencyTreeFiles(moduleProject, moduleCollectCompile, moduleCollectTest, modulePomPathKey, context);

            // STEP 7: Transform graphs and create CodeLocations
            createCodeLocationsForModule(
                moduleProject, moduleCollectCompile, moduleCollectTest, canonicalModulePom, context);

            // STEP 8: Recursively process nested modules
            processNestedModules(moduleProject, canonicalModulePom, context);

        } catch (Exception e) {
            // Catch-all for unexpected errors - log and continue processing other modules
            logger.warn("Failed processing module '{}' due to: {}", moduleEntry, e.getMessage());
        }
    }

    /**
     * Resolves a module entry to its pom.xml file.
     *
     * <p>Maven module entries can be:
     * <ul>
     *   <li>A directory name (we append pom.xml)</li>
     *   <li>A path to a pom.xml file directly</li>
     * </ul>
     *
     * @param modulePathStr The module path string
     * @param parentDir The parent directory
     * @return The resolved POM file, or null if not found
     */
    private File resolveModulePomFile(String modulePathStr, File parentDir) {
        File modulePom = new File(parentDir, modulePathStr);

        if (modulePom.isDirectory()) {
            modulePom = new File(modulePom, POM_XML_FILENAME);
        }

        // Fallback: try appending pom.xml if direct path doesn't exist
        if (!modulePom.exists()) {
            File alternative = new File(parentDir, modulePathStr + File.separator + POM_XML_FILENAME);
            if (alternative.exists()) {
                modulePom = alternative;
            }
        }

        // Canonicalize for validation
        File canonicalPom = canonicalizeFile(modulePom);

        if (!canonicalPom.exists() || !canonicalPom.isFile()) {
            logger.warn("Module POM not found for module '{}' at expected path '{}'. Skipping module.",
                modulePathStr, canonicalPom.getAbsolutePath());
            return null;
        }

        return modulePom;
    }

    /**
     * Canonicalizes a file path to normalize it.
     *
     * <p>This prevents processing the same module twice due to symlinks or relative path references.
     *
     * @param file The file to canonicalize
     * @return The canonical file, or the original if canonicalization fails
     */
    private File canonicalizeFile(File file) {
        try {
            return file.getCanonicalFile();
        } catch (Exception e) {
            logger.debug("Could not canonicalize path {}, continuing with absolute path.", file.getAbsolutePath());
            return file;
        }
    }

    /**
     * Safely gets the canonical path of a file, falling back to absolute path.
     *
     * @param file The file
     * @return The canonical path if available, otherwise absolute path
     */
    private String getCanonicalPathSafe(File file) {
        try {
            return file.getCanonicalPath();
        } catch (Exception e) {
            return file.getAbsolutePath();
        }
    }

    /**
     * Builds the effective Maven project model for a module.
     *
     * @param modulePom The module's POM file
     * @param modulePomPathKey The module's path key (for logging)
     * @param context Processing context
     * @return The MavenProject, or null if building fails
     */
    private MavenProject buildModuleProject(File modulePom, String modulePomPathKey, MavenModuleProcessingContext context) {
        try {
            MavenProject moduleProject = context.getProjectBuilder().buildProject(modulePom);
            logger.info("Module '{}' dependencies: {}", modulePomPathKey,
                moduleProject.getDependencies() == null ? "[]" :
                    moduleProject.getDependencies().stream()
                        .map(d -> d.getCoordinates().getArtifactId() + ":" + d.getCoordinates().getVersion())
                        .map(s -> "\"" + s + "\"")
                        .collect(java.util.stream.Collectors.joining(",", "[", "]"))
            );
            return moduleProject;
        } catch (Exception e) {
            logger.warn("Failed to build effective project for module '{}': {}", modulePomPathKey, e.getMessage());
            context.getTreeWriter().writeErrorTreeFile(
                context.getOutputDir(),
                context.getCompileScope(),
                "BUILD-PROJECT-ERROR",
                modulePomPathKey,
                e.getMessage()
            );
            return null;
        }
    }

    /**
     * Resolves compile-scope dependencies for a module.
     *
     * @param modulePom The module's POM file
     * @param moduleProject The module's project model
     * @param modulePomPathKey The module's path key (for logging)
     * @param context Processing context
     * @return The CollectResult, or null if resolution fails
     */
    private CollectResult resolveCompileDependencies(
        File modulePom, MavenProject moduleProject, String modulePomPathKey, MavenModuleProcessingContext context
    ) {
        try {
            return context.getDependencyResolver().resolveDependencies(
                modulePom, moduleProject, context.getLocalRepoPath().toFile(), context.getCompileScope());
        } catch (org.eclipse.aether.collection.DependencyCollectionException e) {
            logger.warn("Compile dependency resolution failed for module '{}': {}", modulePomPathKey, e.getMessage());
            context.getTreeWriter().writeErrorTreeFile(
                context.getOutputDir(),
                context.getCompileScope(),
                context.getCoordinateFormatter().formatGAV(moduleProject),
                modulePomPathKey,
                e.getMessage()
            );
            return null;
        }
    }

    /**
     * Resolves test-scope dependencies for a module (if enabled).
     *
     * @param modulePom The module's POM file
     * @param moduleProject The module's project model
     * @param modulePomPathKey The module's path key (for logging)
     * @param context Processing context
     * @return The CollectResult, or null if resolution fails or test scope is disabled
     */
    private CollectResult resolveTestDependencies(
        File modulePom, MavenProject moduleProject, String modulePomPathKey, MavenModuleProcessingContext context
    ) {
        if (!context.isIncludeTestScope()) {
            return null;
        }

        try {
            return context.getDependencyResolver().resolveDependencies(
                modulePom, moduleProject, context.getLocalRepoPath().toFile(), context.getTestScope());
        } catch (org.eclipse.aether.collection.DependencyCollectionException e) {
            logger.warn("Test dependency resolution failed for module '{}': {}", modulePomPathKey, e.getMessage());
            context.getTreeWriter().writeErrorTreeFile(
                context.getOutputDir(),
                context.getTestScope(),
                context.getCoordinateFormatter().formatGAV(moduleProject),
                modulePomPathKey,
                e.getMessage()
            );
            // Test scope failure is not critical - return null but continue processing
            return null;
        }
    }

    /**
     * Writes dependency tree files for a module.
     *
     * @param moduleProject The module's project model
     * @param compileResult The compile-scope CollectResult
     * @param testResult The test-scope CollectResult (may be null)
     * @param modulePomPathKey The module's path key
     * @param context Processing context
     */
    private void writeDependencyTreeFiles(
        MavenProject moduleProject,
        CollectResult compileResult,
        CollectResult testResult,
        String modulePomPathKey,
        MavenModuleProcessingContext context
    ) {
        try {
            String gav = context.getCoordinateFormatter().formatGAV(moduleProject);
            String hash = Integer.toHexString(modulePomPathKey.hashCode());

            // Write compile tree
            String compileFileName = context.getTreeWriter().buildDependencyTreeFileName("compile", gav, hash);
            File compileOut = new File(context.getOutputDir(), compileFileName);
            context.getTreeWriter().writeDependencyTree(compileResult, compileOut, context.getCompileScope());

            // Write test tree if available
            if (testResult != null) {
                String testFileName = context.getTreeWriter().buildDependencyTreeFileName("test", gav, hash);
                File testOut = new File(context.getOutputDir(), testFileName);
                context.getTreeWriter().writeDependencyTree(testResult, testOut, context.getTestScope());
            }
        } catch (Exception e) {
            logger.debug("Failed to write module dependency tree files for '{}': {}", modulePomPathKey, e.getMessage());
        }
    }

    /**
     * Creates CodeLocations for a module and adds them to the accumulator.
     *
     * @param moduleProject The module's project model
     * @param compileResult The compile-scope CollectResult
     * @param testResult The test-scope CollectResult (may be null)
     * @param modulePom The module's POM file
     * @param context Processing context
     */
    private void createCodeLocationsForModule(
        MavenProject moduleProject,
        CollectResult compileResult,
        CollectResult testResult,
        File modulePom,
        MavenModuleProcessingContext context
    ) {
        // Transform compile graph
        MavenParseResult compileParseResult = context.getGraphParser().parse(compileResult);
        DependencyGraph compileGraph = context.getGraphTransformer().transform(compileParseResult);

        // Transform test graph if available
        DependencyGraph testGraph = null;
        if (testResult != null) {
            MavenParseResult testParseResult = context.getGraphParser().parse(testResult);
            testGraph = context.getGraphTransformer().transform(testParseResult);
        }

        // Process shaded dependencies if enabled
        MavenResolverOptions options = context.getMavenResolverOptions();
        if (options != null && options.isIncludeShadedDependenciesV2Enabled()) {
            processModuleShadedDependencies(
                moduleProject, compileResult, testResult, compileGraph, testGraph, context);
        }

        // Create and add CodeLocations
        File moduleSourcePath = modulePom.getParentFile();

        context.getCodeLocations().add(
            context.getCodeLocationFactory().createCodeLocation(compileGraph, moduleProject, moduleSourcePath)
        );

        if (testGraph != null) {
            context.getCodeLocations().add(
                context.getCodeLocationFactory().createCodeLocation(testGraph, moduleProject, moduleSourcePath)
            );
        }
    }

    /**
     * Processes shaded dependencies for a module.
     *
     * <p>This method performs the complete shaded dependency detection workflow:
     * <ol>
     *   <li>Extracts Aether dependencies from compile (and optionally test) scopes</li>
     *   <li>Downloads artifact JARs</li>
     *   <li>Scans JARs for shaded dependencies</li>
     *   <li>Grafts shaded dependency sub-trees into the module's dependency graphs</li>
     * </ol>
     *
     * @param moduleProject The module's project model
     * @param compileResult The compile-scope CollectResult
     * @param testResult The test-scope CollectResult (may be null)
     * @param compileGraph The compile-scope dependency graph
     * @param testGraph The test-scope dependency graph (may be null)
     * @param context Processing context
     */
    private void processModuleShadedDependencies(
        MavenProject moduleProject,
        CollectResult compileResult,
        CollectResult testResult,
        DependencyGraph compileGraph,
        DependencyGraph testGraph,
        MavenModuleProcessingContext context
    ) {
        String moduleGav = context.getCoordinateFormatter().formatGAV(moduleProject);
        logger.info("Processing shaded dependencies for module: {}", moduleGav);

        try {
            // Step 1: Extract Aether dependencies from compile scope
            List<org.eclipse.aether.graph.Dependency> aetherDependencies = new ArrayList<>();
            Set<String> seenGavs = new HashSet<>();
            if (compileResult != null && compileResult.getRoot() != null) {
                extractAetherDependenciesFromNode(compileResult.getRoot(), aetherDependencies, seenGavs);
                logger.debug("Module {} - Extracted {} dependencies from compile scope", moduleGav, aetherDependencies.size());
            }

            if (aetherDependencies.isEmpty()) {
                logger.debug("Module {} - No dependencies to process for shaded detection", moduleGav);
                return;
            }

            // Step 1.5: Pre-filter — only keep deps whose POM declares maven-shade-plugin
            aetherDependencies = filterToShadedCandidates(aetherDependencies, context.getLocalRepoPath());
            if (aetherDependencies.isEmpty()) {
                logger.info("Module {} - No dependencies use maven-shade-plugin, skipping JAR downloads", moduleGav);
                return;
            }

            // Step 2: Set up download paths
            Path downloadDir = context.getDownloadDir();
            if (downloadDir == null) {
                logger.warn("Module {} - Download directory not configured, skipping shaded detection", moduleGav);
                return;
            }

            Path moduleDownloadDir = downloadDir.resolve("modules").resolve(sanitizeForPath(moduleGav));
            Files.createDirectories(moduleDownloadDir);

            // Step 3: Create artifact downloader for this module
            List<JavaRepository> moduleRepos = moduleProject.getRepositories();
            if (moduleRepos == null) {
                moduleRepos = context.getRootRepositories();
            }

            MavenResolverOptions options = context.getMavenResolverOptions();
            ArtifactDownloader downloader = new ArtifactDownloader(
                options.getJarRepositoryPath(),
                moduleDownloadDir,
                moduleRepos,
                options
            );

            // Step 4: Download JARs
            logger.info("Module {} - Starting JAR downloads...", moduleGav);
            Map<Artifact, Path> downloadedJars = downloader.downloadArtifacts(aetherDependencies);
            logger.info("Module {} - Downloaded {} JARs", moduleGav, downloadedJars.size());

            if (downloadedJars.isEmpty()) {
                logger.debug("Module {} - No JARs downloaded, skipping shaded detection", moduleGav);
                return;
            }

            // Step 5: Scan JARs for shaded dependencies
            ShadedDependencyScanner scanner = new ShadedDependencyScanner();
            Map<Artifact, List<DiscoveredDependency>> shadedDepsMap = scanner.scanJarsForShadedDependencies(
                downloadedJars,
                compileResult,
                testResult,
                context.getProjectBuilder()
            );

            logger.info("Module {} - Found {} JARs with shaded dependencies", moduleGav, shadedDepsMap.size());

            if (shadedDepsMap.isEmpty()) {
                return;
            }

            // Step 6: Graft shaded dependencies into graphs
            int addedToCompile = addShadedDependenciesToGraph(
                compileGraph, shadedDepsMap, context, moduleDownloadDir);
            logger.info("Module {} - Grafted {} shaded dependency nodes into compile graph", moduleGav, addedToCompile);

            if (testGraph != null) {
                int addedToTest = addShadedDependenciesToGraph(
                    testGraph, shadedDepsMap, context, moduleDownloadDir);
                logger.info("Module {} - Grafted {} shaded dependency nodes into test graph", moduleGav, addedToTest);
            }

        } catch (Exception e) {
            logger.warn("Module {} - Failed to process shaded dependencies: {}", moduleGav, e.getMessage());
            logger.debug("Module shaded dependency processing error:", e);
        }
    }

    /**
     * Filters dependencies to only those whose POM declares maven-shade-plugin.
     * Reads POMs from Aether's local repo cache (zero network calls).
     * Fails open: if a POM is missing or unparseable, the dep is kept to avoid false negatives.
     */
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
                // POM missing — shouldn't happen after Aether resolution, but fail open
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
                // Parse failure — fail open
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

    /**
     * Recursively extracts Aether dependencies from a dependency node tree.
     * Uses a Set for O(1) duplicate detection instead of linear search.
     *
     * @param node The root dependency node to traverse
     * @param dependencies The list to collect Aether dependencies into
     * @param seenGavs Set of already-seen GAV keys for O(1) deduplication
     */
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

    /**
     * Adds discovered shaded dependencies to a dependency graph.
     */
    private int addShadedDependenciesToGraph(
        DependencyGraph graph,
        Map<Artifact, List<DiscoveredDependency>> shadedDepsMap,
        MavenModuleProcessingContext context,
        Path downloadDir
    ) {
        int count = 0;

        for (Map.Entry<Artifact, List<DiscoveredDependency>> entry : shadedDepsMap.entrySet()) {
            Artifact parentArtifact = entry.getKey();
            List<DiscoveredDependency> shadedDeps = entry.getValue();

            // Find parent in graph
            ExternalId parentExternalId = context.getExternalIdFactory().createMavenExternalId(
                parentArtifact.getGroupId(), parentArtifact.getArtifactId(), parentArtifact.getVersion());
            Dependency parentDependency = graph.getDependency(parentExternalId);

            if (parentDependency == null) {
                logger.debug("Parent artifact {} not found in graph, skipping shaded deps",
                    formatArtifact(parentArtifact));
                continue;
            }

            // Add each shaded dependency under the parent
            for (DiscoveredDependency shadedDep : shadedDeps) {
                try {
                    String[] gavParts = shadedDep.getIdentifier().split(":");
                    if (gavParts.length < 3) {
                        continue;
                    }

                    String groupId = gavParts[0];
                    String artifactId = gavParts[1];
                    String version = gavParts[2];

                    // Create and add the shaded dependency node
                    ExternalId shadedExternalId = context.getExternalIdFactory().createMavenExternalId(
                        groupId, artifactId, version);
                    Dependency shadedNode = new Dependency(artifactId, version, shadedExternalId);

                    graph.addChildWithParent(shadedNode, parentDependency);
                    count++;

                    // Resolve and graft the shaded dependency's transitive tree
                    count += resolveAndGraftShadedSubTree(
                        shadedNode, groupId, artifactId, version, graph, context, downloadDir);

                } catch (Exception e) {
                    logger.debug("Failed to add shaded dependency {}: {}",
                        shadedDep.getIdentifier(), e.getMessage());
                }
            }
        }

        return count;
    }

    /**
     * Resolves the transitive dependency tree for a shaded dependency and grafts it onto the main graph.
     * Uses a shared cache to avoid re-resolving the same GAV across modules.
     */
    private int resolveAndGraftShadedSubTree(
        Dependency parentNode,
        String groupId,
        String artifactId,
        String version,
        DependencyGraph mainGraph,
        MavenModuleProcessingContext context,
        Path downloadDir
    ) {
        try {
            String gavKey = groupId + ":" + artifactId + ":" + version;
            Map<String, DependencyGraph> cache = context.getShadedSubTreeCache();
            DependencyGraph shadedGraph = cache.get(gavKey);

            if (shadedGraph == null) {
                // Cache miss — resolve the full sub-tree
                // Download the shaded dependency's POM
                JavaCoordinates coords = new JavaCoordinates(groupId, artifactId, version, "pom");
                MavenDownloader downloader = new MavenDownloader(context.getRootRepositories(), downloadDir);
                File shadedPomFile = downloader.downloadPom(coords);

                if (shadedPomFile == null || !shadedPomFile.exists()) {
                    logger.debug("Could not download POM for shaded dependency {}:{}:{}", groupId, artifactId, version);
                    return 0;
                }

                // Build the shaded project model
                MavenProject shadedProject = context.getProjectBuilder().buildProject(shadedPomFile);

                // Resolve the shaded dependency's transitive tree
                CollectResult shadedCollectResult = context.getDependencyResolver().resolveDependencies(
                    shadedPomFile, shadedProject, context.getLocalRepoPath().toFile(), context.getCompileScope());

                if (shadedCollectResult == null || shadedCollectResult.getRoot() == null) {
                    return 0;
                }

                // Transform to BDIO graph
                MavenParseResult parseResult = context.getGraphParser().parse(shadedCollectResult);
                shadedGraph = context.getGraphTransformer().transform(parseResult);

                cache.put(gavKey, shadedGraph);
                logger.debug("Cached shaded sub-tree for: {}", gavKey);
            } else {
                logger.debug("Cache hit for shaded dependency sub-tree: {}", gavKey);
            }

            // Graft the shaded graph under the parent node (always executes, even on cache hit)
            return graftSubGraph(shadedGraph, mainGraph, parentNode, context);

        } catch (Exception e) {
            logger.debug("Failed to resolve sub-tree for {}:{}:{}: {}", groupId, artifactId, version, e.getMessage());
            return 0;
        }
    }

    /**
     * Grafts a source graph onto a destination graph under a specified parent.
     */
    private int graftSubGraph(
        DependencyGraph sourceGraph,
        DependencyGraph destGraph,
        Dependency destParent,
        MavenModuleProcessingContext context
    ) {
        int count = 0;
        Set<Dependency> sourceRoots = sourceGraph.getRootDependencies();

        if (sourceRoots == null || sourceRoots.isEmpty()) {
            return count;
        }

        Set<ExternalId> visited = new HashSet<>();

        for (Dependency sourceRoot : sourceRoots) {
            count += copyGraphEdges(sourceGraph, destGraph, sourceRoot, destParent, visited);
        }

        return count;
    }

    /**
     * Recursively copies edges from source to destination graph.
     */
    private int copyGraphEdges(
        DependencyGraph sourceGraph,
        DependencyGraph destGraph,
        Dependency sourceDep,
        Dependency destParent,
        Set<ExternalId> visited
    ) {
        if (sourceDep == null || sourceDep.getExternalId() == null) {
            return 0;
        }

        ExternalId externalId = sourceDep.getExternalId();

        // Cycle detection
        if (visited.contains(externalId)) {
            return 0;
        }
        visited.add(externalId);

        // Add this node under the destination parent
        destGraph.addChildWithParent(sourceDep, destParent);
        int count = 1;

        // Recurse for children
        Set<Dependency> children = sourceGraph.getChildrenForParent(sourceDep);
        if (children != null) {
            for (Dependency child : children) {
                count += copyGraphEdges(sourceGraph, destGraph, child, sourceDep, visited);
            }
        }

        return count;
    }

    /**
     * Formats an artifact for logging.
     */
    private String formatArtifact(Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
    }

    /**
     * Sanitizes a string for use in file paths.
     */
    private String sanitizeForPath(String s) {
        if (s == null) {
            return "unknown";
        }
        return s.replace(':', '_').replace('/', '_').replace('\\', '_');
    }

    /**
     * Recursively processes nested modules declared by a module.
     *
     * @param moduleProject The parent module's project model
     * @param modulePom The parent module's POM file
     * @param context Processing context
     */
    private void processNestedModules(MavenProject moduleProject, File modulePom, MavenModuleProcessingContext context) {
        List<String> nestedModules = moduleProject.getModules();
        if (nestedModules == null || nestedModules.isEmpty()) {
            return;
        }

        File childParentDir = modulePom.getParentFile();
        for (String childModule : nestedModules) {
            processModuleRecursive(childModule, childParentDir, context);
        }
    }
}

