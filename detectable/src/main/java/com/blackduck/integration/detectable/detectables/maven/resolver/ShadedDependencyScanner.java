package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.ShadedDependencyInspector;
import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.methods.DeltaAnalysisInspector;
import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.methods.RecursiveMetadataInspector;
import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.model.DiscoveredDependency;
import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

/**
 * Scans JAR files to detect shaded (embedded) dependencies.
 *
 * <p>This scanner analyzes downloaded artifact JARs using multiple inspection methods:
 * <ul>
 *   <li><strong>Delta Analysis:</strong> Compares original pom.xml with dependency-reduced-pom.xml</li>
 *   <li><strong>Recursive Metadata:</strong> Scans for embedded pom.properties files</li>
 * </ul>
 *
 * <p>Results from all inspectors are combined and deduplicated based on GAV identifier.
 * The scanner is designed for integration with Maven Resolver Detectable and processes
 * all provided JARs, gracefully handling failures for individual JARs.
 *
 * @see DeltaAnalysisInspector
 * @see RecursiveMetadataInspector
 */
public class ShadedDependencyScanner {

    private static final Logger logger = LoggerFactory.getLogger(ShadedDependencyScanner.class);

    /**
     * Scans all provided JAR files for shaded dependencies.
     *
     * <p>For each JAR file:
     * <ol>
     *   <li>Opens the JAR file</li>
     *   <li>Runs all registered inspectors (Delta Analysis, Recursive Metadata)</li>
     *   <li>Combines and deduplicates results</li>
     *   <li>Properly closes the JAR file</li>
     * </ol>
     *
     * <p>Error handling: If scanning fails for one JAR, the error is logged and
     * processing continues with the remaining JARs. Partial results are returned.
     *
     * @param artifactJarPaths Map of artifact coordinates to their JAR file paths
     * @return Map of artifacts to their discovered shaded dependencies (only includes artifacts with findings)
     */
    public Map<Artifact, List<DiscoveredDependency>> scanJarsForShadedDependencies(
            Map<Artifact, Path> artifactJarPaths,
            org.eclipse.aether.collection.CollectResult compileResult,
            org.eclipse.aether.collection.CollectResult testResult) {

        Map<Artifact, List<DiscoveredDependency>> results = new LinkedHashMap<Artifact, List<DiscoveredDependency>>();

        if (artifactJarPaths == null || artifactJarPaths.isEmpty()) {
            logger.info("No JAR files provided for shaded dependency scanning.");
            return results;
        }

        int totalJars = artifactJarPaths.size();
        logger.info("Starting shaded dependency scan for {} JAR file(s)...", totalJars);

        //Build the aether Direct Children map to pass to the inspectors
        Map<String, java.util.Set<String>> aetherDirectChildrenByGa = new java.util.HashMap<String, java.util.Set<String>>();
        if(compileResult != null && compileResult.getRoot() != null) {
            buildAetherDirectChildrenMap(compileResult.getRoot(), aetherDirectChildrenByGa);
        }
        if(testResult != null && testResult.getRoot() != null) {
            buildAetherDirectChildrenMap(testResult.getRoot(), aetherDirectChildrenByGa);
        }

        // Initialize inspectors
        List<ShadedDependencyInspector> inspectors = createInspectors(aetherDirectChildrenByGa);
        logger.debug("Registered {} inspector(s) for shaded dependency detection.", inspectors.size());

        int processedCount = 0;
        int jarsWithShadedDeps = 0;
        int totalShadedDepsFound = 0;

        for (Map.Entry<Artifact, Path> entry : artifactJarPaths.entrySet()) {
            Artifact artifact = entry.getKey();
            Path jarPath = entry.getValue();
            processedCount++;

            String artifactGav = formatArtifactGav(artifact);
            logger.debug("Scanning JAR {}/{}: {}", processedCount, totalJars, artifactGav);

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
                // Continue processing other JARs
            }
        }

        // Log final summary
        logger.info("Shaded dependency scan complete.");
        logger.info("  - JARs scanned: {}", totalJars);
        logger.info("  - JARs with shaded dependencies: {}", jarsWithShadedDeps);
        logger.info("  - Total shaded dependencies found: {}", totalShadedDepsFound);

        return results;
    }

    /**
     * Creates and returns the list of inspectors to use for shaded dependency detection.
     *
     * @return List of initialized inspectors
     */
    private List<ShadedDependencyInspector> createInspectors(Map<String, java.util.Set<String>> aetherDirectChildrenByGa) {
        List<ShadedDependencyInspector> inspectors = new ArrayList<ShadedDependencyInspector>();
//        inspectors.add(new DeltaAnalysisInspector());        // Method 1: Delta analysis of POM files
        inspectors.add(new DeltaAnalysisInspector(aetherDirectChildrenByGa));        // Method 1: Delta analysis of POM files
        inspectors.add(new RecursiveMetadataInspector());    // Method 2: Recursive metadata scanning
        return inspectors;
    }

    /**
     * Scans a single JAR file using all registered inspectors.
     *
     * @param jarPath      Path to the JAR file
     * @param inspectors   List of inspectors to run
     * @param artifactGav  GAV string for logging purposes
     * @return Deduplicated list of discovered shaded dependencies
     * @throws Exception If the JAR cannot be opened or processed
     */
    private List<DiscoveredDependency> scanSingleJar(Path jarPath, List<ShadedDependencyInspector> inspectors,
                                                      String artifactGav) throws Exception {
        // Map for deduplication: identifier -> DiscoveredDependency
        // Using LinkedHashMap to preserve insertion order
        Map<String, DiscoveredDependency> deduplicatedDeps = new LinkedHashMap<String, DiscoveredDependency>();

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
                                // Dependency already found by another inspector
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
                    // Continue with other inspectors
                }
            }
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (Exception ignored) {
                    // Ignore close exceptions
                }
            }
        }

        return new ArrayList<DiscoveredDependency>(deduplicatedDeps.values());
    }

    /**
     * Formats an artifact's coordinates as a GAV string for logging.
     *
     * @param artifact The artifact to format
     * @return Formatted string in "groupId:artifactId:version" format
     */
    private String formatArtifactGav(Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
    }

    /**
     * Recursively flattens the Aether DependencyNode tree into a Map for lightning-fast querying.
     * Key: parent GA (groupId:artifactId). Value: Set of direct children GAVs (groupId:artifactId:version).
     */
    private void buildAetherDirectChildrenMap(org.eclipse.aether.graph.DependencyNode node, Map<String, java.util.Set<String>> map) {
        if (node == null || node.getDependency() == null || node.getDependency().getArtifact() == null) {
            return;
        }

        Artifact artifact = node.getDependency().getArtifact();
        String ga = artifact.getGroupId() + ":" + artifact.getArtifactId();

        java.util.Set<String> childrenGavs = new java.util.HashSet<String>();
        if (node.getChildren() != null) {
            for (org.eclipse.aether.graph.DependencyNode child : node.getChildren()) {
                if (child.getDependency() != null && child.getDependency().getArtifact() != null) {
                    Artifact childArtifact = child.getDependency().getArtifact();
                    childrenGavs.add(childArtifact.getGroupId() + ":" +
                            childArtifact.getArtifactId() + ":" +
                            childArtifact.getVersion());
                }
            }
        }

        // Add if not present. If multiple versions exist in the graph (rare but possible), we keep the first resolved one.
        if (!map.containsKey(ga)) {
            map.put(ga, childrenGavs);
        }

        // Recurse down the tree
        if (node.getChildren() != null) {
            for (org.eclipse.aether.graph.DependencyNode child : node.getChildren()) {
                buildAetherDirectChildrenMap(child, map);
            }
        }
    }
}
