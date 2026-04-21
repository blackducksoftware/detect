package com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.methods;

import com.blackduck.integration.detectable.detectables.maven.resolver.pom.MavenProject;
import com.blackduck.integration.detectable.detectables.maven.resolver.pom.ProjectBuilder;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.JavaCoordinates;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.JavaDependency;
import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.ShadedDependencyInspector;
import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.model.DiscoveredDependency;
import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Detects shaded dependencies by performing a delta (set difference) between:
 * - The Original pom.xml hidden inside the JAR (parsed and fully resolved via ProjectBuilder with BOM/Parent support)
 * - The Eclipse Aether Dependency Graph (contains only unshaded dependencies)
 *
 * <p>This inspector leverages the core ProjectBuilder infrastructure to handle:
 * <ul>
 *   <li>Property placeholder resolution (${project.version}, custom properties, etc.)</li>
 *   <li>Parent POM inheritance and property override chains</li>
 *   <li>BOM imports and dependencyManagement version resolution</li>
 *   <li>Multi-level dependency management merging</li>
 * </ul>
 *
 * The difference = the shaded dependencies.
 */
public class DeltaAnalysisInspector implements ShadedDependencyInspector {

    private static final Logger logger = LoggerFactory.getLogger(DeltaAnalysisInspector.class);

    private static final Set<String> EXCLUDED_SCOPES = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("test", "provided", "system"))
    );

    // A mapping of GA -> Set of exact child GAVs extracted from the Aether tree
    private final Map<String, Set<String>> aetherDirectChildrenByGa;
    private final ProjectBuilder projectBuilder;
    private final Artifact hostArtifact;

    /**
     * Initializes the inspector with the Aether graph context, ProjectBuilder, and host artifact.
     *
     * @param aetherDirectChildrenByGa A map of Artifact GA to a Set of its direct children GAVs from Aether resolution
     * @param projectBuilder The ProjectBuilder instance used to parse and resolve POMs with full BOM/Parent support
     * @param hostArtifact The host artifact whose JAR is being inspected (used to locate the correct POM)
     */
    public DeltaAnalysisInspector(Map<String, Set<String>> aetherDirectChildrenByGa, ProjectBuilder projectBuilder, Artifact hostArtifact) {
        this.aetherDirectChildrenByGa = aetherDirectChildrenByGa != null ? aetherDirectChildrenByGa : new HashMap<String, Set<String>>();
        this.projectBuilder = projectBuilder;
        this.hostArtifact = hostArtifact;
        logger.debug("[Method 1] DeltaAnalysisInspector initialized with {} GA entries in Aether map.", this.aetherDirectChildrenByGa.size());
    }

    @Override
    public List<DiscoveredDependency> detectShadedDependencies(JarFile jarFile) {
        List<DiscoveredDependency> discoveredDependencies = new ArrayList<DiscoveredDependency>();
        logger.debug("[Method 1] Starting Delta Analysis for JAR: {}", jarFile.getName());

        // Guard: host artifact is required for targeted POM lookup
        if (hostArtifact == null) {
            logger.warn("[Method 1] Host artifact not provided. Delta analysis skipped.");
            return discoveredDependencies;
        }

        // Pre-scan JAR for .class file directory prefixes to filter ghost deps (POM lists them but classes weren't bundled)
        NavigableSet<String> classPathPrefixes = new TreeSet<String>();
        Enumeration<JarEntry> classEntries = jarFile.entries();
        while (classEntries.hasMoreElements()) {
            String entryName = classEntries.nextElement().getName();
            if (entryName.endsWith(".class")) {
                int lastSlash = entryName.lastIndexOf('/');
                if (lastSlash > 0) {
                    classPathPrefixes.add(entryName.substring(0, lastSlash + 1));
                }
            }
        }
        logger.debug("[Method 1] Pre-scanned {} unique class path prefixes for ghost detection.", classPathPrefixes.size());

        JarEntry originalPomEntry = null;

        // Step 1: Locate the Original POM inside the JAR using targeted path lookup
        logger.debug("[Method 1] Step 1 - Scanning JAR entries for Original pom.xml...");
        String targetPath = "META-INF/maven/" + hostArtifact.getGroupId()
                + "/" + hostArtifact.getArtifactId() + "/pom.xml";
        logger.debug("[Method 1] Step 1 - Looking for POM at exact path: {}", targetPath);

        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();

            // Match only the exact expected path for the host artifact's POM
            if (name.equals(targetPath)) {
                originalPomEntry = entry;
                logger.debug("[Method 1] Step 1 - Found original POM: {}", name);
                break;
            }
        }

        if (originalPomEntry == null) {
            logger.debug("[Method 1] Step 1 - Original pom.xml not found at expected path '{}'. Delta analysis skipped.", targetPath);
            return discoveredDependencies;
        }

        File tempPomFile = null;
        Path tempDir = null;
        try {
            // Step 2: Extract the POM to a deterministic temporary directory so ProjectBuilder cache works
            // Use GAV-based naming for cache hits and better debugging
            logger.debug("[Method 1] Step 2 - Extracting original POM to temporary file...");
            tempDir = Files.createTempDirectory(
                "extracted-pom-" + hostArtifact.getGroupId()
                + "_" + hostArtifact.getArtifactId()
                + "_" + hostArtifact.getVersion());
            tempPomFile = tempDir.resolve("pom.xml").toFile();
            logger.trace("[Method 1] Step 2 - Temporary POM file: {}", tempPomFile.getAbsolutePath());

            try (InputStream is = jarFile.getInputStream(originalPomEntry)) {
                Files.copy(is, tempPomFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            logger.debug("[Method 1] Step 2 - Successfully extracted POM to temporary file.");

            // Step 3: Delegate to ProjectBuilder to resolve properties, BOMs, and Parents
            logger.debug("[Method 1] Step 3 - Passing extracted POM to ProjectBuilder for full resolution (BOMs, Parents, Properties)...");
            MavenProject mavenProject = projectBuilder.buildProject(tempPomFile);
            logger.debug("[Method 1] Step 3 - ProjectBuilder successfully built MavenProject model.");

            JavaCoordinates hostCoords = mavenProject.getCoordinates();
            if (hostCoords.getGroupId() == null || hostCoords.getArtifactId() == null) {
                logger.warn("[Method 1] Step 3 - ProjectBuilder could not resolve host GA. Aborting delta math.");
                return discoveredDependencies;
            }

            String hostGa = hostCoords.getGroupId() + ":" + hostCoords.getArtifactId();
            logger.debug("[Method 1] Step 3 - Host artifact coordinates - GroupId: {}, ArtifactId: {}", hostCoords.getGroupId(), hostCoords.getArtifactId());
            logger.debug("[Method 1] Step 3 - Host GA: {}", hostGa);

            // Step 4: Extract and format the resolved dependencies from MavenProject using GA-based map
            logger.debug("[Method 1] Step 4 - Extracting resolved dependencies from MavenProject (GA-based)...");
            Map<String, String> originalDepsByGa = new HashMap<String, String>();
            if (mavenProject.getDependencies() != null) {
                logger.debug("[Method 1] Step 4 - MavenProject has {} total dependencies.", mavenProject.getDependencies().size());

                for (JavaDependency dep : mavenProject.getDependencies()) {
                    String scope = dep.getScope() != null ? dep.getScope().toLowerCase() : "compile";

                    if (EXCLUDED_SCOPES.contains(scope)) {
                        logger.trace("[Method 1] Step 4 - Skipping dependency with excluded scope '{}': {}", scope, dep.getCoordinates());
                        continue;
                    }

                    JavaCoordinates coords = dep.getCoordinates();
                    if (coords != null && coords.getGroupId() != null && coords.getArtifactId() != null && coords.getVersion() != null) {
                        String ga = coords.getGroupId() + ":" + coords.getArtifactId();
                        String gav = coords.getGroupId() + ":" + coords.getArtifactId() + ":" + coords.getVersion();
                        originalDepsByGa.put(ga, gav);
                        logger.trace("[Method 1] Step 4 - Added original dependency GA '{}' -> GAV '{}'", ga, gav);
                    } else {
                        logger.trace("[Method 1] Step 4 - Skipping dependency with incomplete coordinates: {}", coords);
                    }
                }
            } else {
                logger.debug("[Method 1] Step 4 - MavenProject has no dependencies.");
            }

            logger.debug("[Method 1] Step 4 - Original POM has {} dependencies (GA-keyed) after scope filtering.", originalDepsByGa.size());

            // Step 5: Compare with Aether using GA-based lookup to handle version bumps
            logger.debug("[Method 1] Step 5 - Retrieving Aether graph dependencies for host: {}", hostGa);
            Set<String> aetherChildren = aetherDirectChildrenByGa.containsKey(hostGa)
                    ? aetherDirectChildrenByGa.get(hostGa)
                    : Collections.<String>emptySet();

            logger.debug("[Method 1] Step 5 - Aether graph lists {} direct dependencies for host {}.", aetherChildren.size(), hostGa);
            for (String aetherDep : aetherChildren) {
                logger.trace("[Method 1] Step 5 - Aether dependency: {}", aetherDep);
            }

            // Build GA key set from Aether children for GA-based comparison
            Set<String> aetherGaKeys = new HashSet<String>();
            for (String aetherGav : aetherChildren) {
                String[] parts = aetherGav.split(":");
                if (parts.length >= 2) {
                    aetherGaKeys.add(parts[0] + ":" + parts[1]);
                }
            }
            logger.debug("[Method 1] Step 5 - Built {} GA keys from Aether children for comparison.", aetherGaKeys.size());

            // GA-based Delta: If original GA is NOT in Aether GA keys, it's a shaded candidate.
            // Ghost filter: verify .class files actually exist in the JAR for each candidate's groupId.
            // Without this, deps listed in the POM but excluded by shade plugin config are false positives.
            int originalSize = originalDepsByGa.size();
            int shadedCount = 0;
            int ghostCount = 0;

            for (Map.Entry<String, String> depEntry : originalDepsByGa.entrySet()) {
                String gaKey = depEntry.getKey();
                String gavValue = depEntry.getValue();

                if (!aetherGaKeys.contains(gaKey)) {
                    // Candidate shaded dep — verify classes are actually bundled in the JAR
                    String groupId = gaKey.split(":")[0];
                    String expectedClassPrefix = groupId.replace('.', '/') + "/";
                    String ceiling = classPathPrefixes.ceiling(expectedClassPrefix);
                    boolean hasClasses = ceiling != null && ceiling.startsWith(expectedClassPrefix);

                    if (!hasClasses) {
                        logger.debug("[Method 1] Step 5 - Ghost dependency filtered (no .class files for '{}'): {}", expectedClassPrefix, gavValue);
                        ghostCount++;
                        continue;
                    }

                    logger.info("[Method 1] Step 5 - Shaded dependency detected: {}", gavValue);
                    discoveredDependencies.add(new DiscoveredDependency(gavValue, "Delta Analysis (ProjectBuilder)"));
                    shadedCount++;
                } else {
                    logger.trace("[Method 1] Step 5 - Dependency resolved by Aether (GA match): {}", gaKey);
                }
            }

            logger.debug("[Method 1] Step 5 - Delta: {} original, {} aether GA keys, {} shaded, {} ghosts filtered.",
                    originalSize, aetherGaKeys.size(), shadedCount, ghostCount);

        } catch (Exception e) {
            logger.error("[Method 1] Failed to process ProjectBuilder delta math for JAR {}: {}", jarFile.getName(), e.getMessage(), e);
        } finally {
            // Step 6: Graceful cleanup to prevent disk leaks (delete file and directory)
            if (tempPomFile != null && tempPomFile.exists()) {
                logger.trace("[Method 1] Step 6 - Cleaning up temporary POM file: {}", tempPomFile.getAbsolutePath());
                try {
                    Files.delete(tempPomFile.toPath());
                    logger.trace("[Method 1] Step 6 - Successfully deleted temporary POM file.");
                } catch (Exception e) {
                    logger.warn("[Method 1] Step 6 - Failed to delete temporary POM file: {}", tempPomFile.getAbsolutePath());
                    tempPomFile.deleteOnExit();
                }
            }
            if (tempDir != null) {
                try {
                    Files.delete(tempDir);
                    logger.trace("[Method 1] Step 6 - Successfully deleted temporary directory.");
                } catch (Exception e) {
                    logger.warn("[Method 1] Step 6 - Failed to delete temporary directory: {}", tempDir);
                    tempDir.toFile().deleteOnExit();
                }
            }
        }

        logger.debug("[Method 1] Delta Analysis completed for JAR: {}", jarFile.getName());
        return discoveredDependencies;
    }

}

