package com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.methods;

import com.blackduck.integration.detectable.detectables.maven.resolver.MavenProject;
import com.blackduck.integration.detectable.detectables.maven.resolver.ProjectBuilder;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.JavaCoordinates;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.JavaDependency;
import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.ShadedDependencyInspector;
import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.model.DiscoveredDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    /**
     * Initializes the inspector with the Aether graph context and ProjectBuilder.
     *
     * @param aetherDirectChildrenByGa A map of Artifact GA to a Set of its direct children GAVs from Aether resolution
     * @param projectBuilder The ProjectBuilder instance used to parse and resolve POMs with full BOM/Parent support
     */
    public DeltaAnalysisInspector(Map<String, Set<String>> aetherDirectChildrenByGa, ProjectBuilder projectBuilder) {
        this.aetherDirectChildrenByGa = aetherDirectChildrenByGa != null ? aetherDirectChildrenByGa : new HashMap<String, Set<String>>();
        this.projectBuilder = projectBuilder;
        logger.debug("[Method 1] DeltaAnalysisInspector initialized with {} GA entries in Aether map.", this.aetherDirectChildrenByGa.size());
    }

    @Override
    public List<DiscoveredDependency> detectShadedDependencies(JarFile jarFile) {
        List<DiscoveredDependency> discoveredDependencies = new ArrayList<DiscoveredDependency>();
        logger.debug("[Method 1] Starting Delta Analysis for JAR: {}", jarFile.getName());

        JarEntry originalPomEntry = null;

        // Step 1: Locate the Original POM inside the JAR
        logger.debug("[Method 1] Step 1 - Scanning JAR entries for Original pom.xml...");
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();

            // The original POM is tucked inside META-INF/maven/<groupId>/<artifactId>/pom.xml
            if (name.startsWith("META-INF/maven/") && name.endsWith("pom.xml")) {
                originalPomEntry = entry;
                logger.debug("[Method 1] Step 1 - Found original POM: {}", name);
                break;
            }
        }

        if (originalPomEntry == null) {
            logger.debug("[Method 1] Step 1 - Original pom.xml not found in JAR. Delta analysis skipped.");
            return discoveredDependencies;
        }

        File tempPomFile = null;
        try {
            // Step 2: Extract the POM to a temporary file so ProjectBuilder can consume it
            // Use GAV-based naming for better debugging and potential caching
            logger.debug("[Method 1] Step 2 - Extracting original POM to temporary file...");
            String tempFileName = buildGavBasedTempFileName(originalPomEntry.getName());
            tempPomFile = Files.createTempFile(tempFileName, ".xml").toFile();
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

            // Step 4: Extract and format the resolved dependencies from MavenProject
            logger.debug("[Method 1] Step 4 - Extracting resolved dependencies from MavenProject...");
            Set<String> originalDependencies = new HashSet<String>();
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
                        String gav = coords.getGroupId() + ":" + coords.getArtifactId() + ":" + coords.getVersion();
                        originalDependencies.add(gav);
                        logger.trace("[Method 1] Step 4 - Added original dependency: {}", gav);
                    } else {
                        logger.trace("[Method 1] Step 4 - Skipping dependency with incomplete coordinates: {}", coords);
                    }
                }
            } else {
                logger.debug("[Method 1] Step 4 - MavenProject has no dependencies.");
            }

            logger.debug("[Method 1] Step 4 - Original POM has {} dependencies after scope filtering.", originalDependencies.size());

            // Step 5: Compare with Aether and calculate the strict delta
            logger.debug("[Method 1] Step 5 - Retrieving Aether graph dependencies for host: {}", hostGa);
            Set<String> aetherChildren = aetherDirectChildrenByGa.containsKey(hostGa)
                    ? aetherDirectChildrenByGa.get(hostGa)
                    : Collections.<String>emptySet();

            logger.debug("[Method 1] Step 5 - Aether graph lists {} direct dependencies for host {}.", aetherChildren.size(), hostGa);
            for (String aetherDep : aetherChildren) {
                logger.trace("[Method 1] Step 5 - Aether dependency: {}", aetherDep);
            }

            // Set Difference: Original POM GAVs - Aether Graph GAVs = Shaded dependencies
            int originalSize = originalDependencies.size();
            originalDependencies.removeAll(aetherChildren);
            int shadedCount = originalDependencies.size();

            logger.debug("[Method 1] Step 5 - Delta calculation: {} original - {} aether = {} shaded dependency(ies).", originalSize, aetherChildren.size(), shadedCount);

            for (String shadedGav : originalDependencies) {
                logger.info("[Method 1] Step 5 - Shaded dependency detected: {}", shadedGav);
                discoveredDependencies.add(new DiscoveredDependency(shadedGav, "Delta Analysis (ProjectBuilder)"));
            }

        } catch (Exception e) {
            logger.error("[Method 1] Failed to process ProjectBuilder delta math for JAR {}: {}", jarFile.getName(), e.getMessage(), e);
        } finally {
            // Step 6: Graceful cleanup to prevent disk leaks
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
        }

        logger.debug("[Method 1] Delta Analysis completed for JAR: {}", jarFile.getName());
        return discoveredDependencies;
    }

    /**
     * Builds a GAV-based temp file name from the POM entry path.
     *
     * <p>The POM path follows Maven convention: META-INF/maven/{groupId}/{artifactId}/pom.xml
     * This method extracts the groupId and artifactId to create a meaningful temp file name.
     *
     * <p>Example:
     *   Input:  "META-INF/maven/org.springframework/spring-core/pom.xml"
     *   Output: "extracted-pom-org.springframework_spring-core-"
     *
     * @param pomEntryPath the path to the POM entry inside the JAR
     * @return a sanitized prefix for the temp file name
     */
    private String buildGavBasedTempFileName(String pomEntryPath) {
        // Default fallback
        String prefix = "extracted-pom-";

        if (pomEntryPath == null || pomEntryPath.isEmpty()) {
            return prefix;
        }

        try {
            // Expected format: META-INF/maven/<groupId>/<artifactId>/pom.xml
            // Split by "/" and extract groupId and artifactId
            String[] parts = pomEntryPath.split("/");

            // Validate structure: should have at least 5 parts
            // [META-INF, maven, groupId, artifactId, pom.xml]
            if (parts.length >= 5 && "META-INF".equals(parts[0]) && "maven".equals(parts[1])) {
                String groupId = parts[2];
                String artifactId = parts[3];

                // Sanitize for filesystem: replace dots with underscores, remove special chars
                String sanitizedGroupId = sanitizeForFileName(groupId);
                String sanitizedArtifactId = sanitizeForFileName(artifactId);

                prefix = "extracted-pom-" + sanitizedGroupId + "_" + sanitizedArtifactId + "-";
                logger.trace("[Method 1] Built GAV-based temp file prefix: {}", prefix);
            }
        } catch (Exception e) {
            logger.trace("[Method 1] Could not parse GAV from POM path '{}', using default prefix", pomEntryPath);
        }

        return prefix;
    }

    /**
     * Sanitizes a string for use in file names.
     * Replaces characters that are problematic in file paths with underscores.
     */
    private String sanitizeForFileName(String input) {
        if (input == null) {
            return "unknown";
        }
        // Replace problematic characters: colons, slashes, backslashes, spaces
        return input.replace(':', '_')
                    .replace('/', '_')
                    .replace('\\', '_')
                    .replace(' ', '_')
                    .replace('<', '_')
                    .replace('>', '_')
                    .replace('"', '_')
                    .replace('|', '_')
                    .replace('?', '_')
                    .replace('*', '_');
    }

}