package com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.methods;

import com.blackduck.integration.detectable.detectables.maven.resolver.pom.MavenProject;
import com.blackduck.integration.detectable.detectables.maven.resolver.pom.ProjectBuilder;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.JavaCoordinates;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.JavaDependency;
import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.ShadedDependencyInspector;
import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.model.DiscoveredDependency;
import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.model.ShadePluginConfig;
import org.eclipse.aether.artifact.Artifact;
import org.jetbrains.annotations.Nullable;
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
 * <h2>Ghost Filter (relocation-aware)</h2>
 * <p>A dependency found by delta math is a "ghost" — not actually bundled — only if it is
 * listed in the shade plugin's {@code <artifactSet><excludes>}. This replaces the old
 * class-path prefix heuristic, which incorrectly dropped relocated dependencies because
 * their classes live under the {@code <shadedPattern>} path, not the original groupId path.
 *
 * <p>If no {@link ShadePluginConfig} is available (e.g., the embedded POM could not be parsed),
 * the inspector falls back to the legacy class-path prefix heuristic so detection is not lost.
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
     * Parsed shade plugin configuration for the host artifact's JAR.
     * Provides explicit exclude list and relocation patterns for an accurate ghost filter.
     * May be null — in that case the legacy class-path heuristic is used as fallback.
     */
    @Nullable
    private final ShadePluginConfig shadePluginConfig;

    /**
     * Initializes the inspector with the Aether graph context, ProjectBuilder, host artifact,
     * and the parsed shade plugin configuration.
     *
     * @param aetherDirectChildrenByGa A map of Artifact GA to a Set of its direct children GAVs from Aether resolution
     * @param projectBuilder           The ProjectBuilder instance used to parse and resolve POMs
     * @param hostArtifact             The host artifact whose JAR is being inspected
     * @param shadePluginConfig        Parsed shade config (excludes + relocations); may be null → fallback to legacy heuristic
     */
    public DeltaAnalysisInspector(
            Map<String, Set<String>> aetherDirectChildrenByGa,
            ProjectBuilder projectBuilder,
            Artifact hostArtifact,
            @Nullable ShadePluginConfig shadePluginConfig
    ) {
        this.aetherDirectChildrenByGa = aetherDirectChildrenByGa != null ? aetherDirectChildrenByGa : new HashMap<String, Set<String>>();
        this.projectBuilder = projectBuilder;
        this.hostArtifact = hostArtifact;
        this.shadePluginConfig = shadePluginConfig;
        logger.debug("[Method 1] DeltaAnalysisInspector initialized with {} GA entries in Aether map. ShadeConfig available: {}",
                this.aetherDirectChildrenByGa.size(), shadePluginConfig != null);
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

        // Legacy fallback: pre-scan .class file path prefixes only if we have no shade config.
        // When shade config IS available, we use the explicit <artifactSet><excludes> list instead,
        // which correctly handles relocated deps (their classes live at the shadedPattern path,
        // not the original groupId path, so class-path scanning would give wrong results).
        NavigableSet<String> classPathPrefixes = null;
        if (shadePluginConfig == null) {
            logger.debug("[Method 1] No ShadePluginConfig available — falling back to legacy class-path ghost filter.");
            classPathPrefixes = new TreeSet<String>();
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
            logger.debug("[Method 1] Pre-scanned {} unique class path prefixes for legacy ghost detection.", classPathPrefixes.size());
        }

        JarEntry originalPomEntry = null;

        // Step 1: Locate the Original POM inside the JAR using targeted path lookup
        logger.debug("[Method 1] Step 1 - Scanning JAR entries for Original pom.xml...");
        String targetPath = "META-INF/maven/" + hostArtifact.getGroupId()
                + "/" + hostArtifact.getArtifactId() + "/pom.xml";
        logger.debug("[Method 1] Step 1 - Looking for POM at exact path: {}", targetPath);

        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().equals(targetPath)) {
                originalPomEntry = entry;
                logger.debug("[Method 1] Step 1 - Found original POM: {}", entry.getName());
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
            // Step 2: Extract the POM to a temp directory so ProjectBuilder can process it
            logger.debug("[Method 1] Step 2 - Extracting original POM to temporary file...");
            tempDir = Files.createTempDirectory(
                    "extracted-pom-" + hostArtifact.getGroupId()
                    + "_" + hostArtifact.getArtifactId()
                    + "_" + hostArtifact.getVersion());
            tempPomFile = tempDir.resolve("pom.xml").toFile();

            try (InputStream is = jarFile.getInputStream(originalPomEntry)) {
                Files.copy(is, tempPomFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            logger.debug("[Method 1] Step 2 - Successfully extracted POM to temporary file.");

            // Step 3: Delegate to ProjectBuilder to resolve properties, BOMs, and Parents
            logger.debug("[Method 1] Step 3 - Passing extracted POM to ProjectBuilder for full resolution...");
            MavenProject mavenProject = projectBuilder.buildProject(tempPomFile);
            logger.debug("[Method 1] Step 3 - ProjectBuilder successfully built MavenProject model.");

            JavaCoordinates hostCoords = mavenProject.getCoordinates();
            if (hostCoords.getGroupId() == null || hostCoords.getArtifactId() == null) {
                logger.warn("[Method 1] Step 3 - ProjectBuilder could not resolve host GA. Aborting delta math.");
                return discoveredDependencies;
            }

            String hostGa = hostCoords.getGroupId() + ":" + hostCoords.getArtifactId();
            logger.debug("[Method 1] Step 3 - Host GA: {}", hostGa);

            // Step 4: Extract resolved dependencies from MavenProject (GA-keyed map)
            logger.debug("[Method 1] Step 4 - Extracting resolved dependencies from MavenProject...");
            Map<String, String> originalDepsByGa = new HashMap<String, String>();
            if (mavenProject.getDependencies() != null) {
                for (JavaDependency dep : mavenProject.getDependencies()) {
                    String scope = dep.getScope() != null ? dep.getScope().toLowerCase() : "compile";
                    if (EXCLUDED_SCOPES.contains(scope)) {
                        continue;
                    }
                    JavaCoordinates coords = dep.getCoordinates();
                    if (coords != null && coords.getGroupId() != null && coords.getArtifactId() != null && coords.getVersion() != null) {
                        String ga = coords.getGroupId() + ":" + coords.getArtifactId();
                        String gav = coords.getGroupId() + ":" + coords.getArtifactId() + ":" + coords.getVersion();
                        originalDepsByGa.put(ga, gav);
                    }
                }
            }
            logger.debug("[Method 1] Step 4 - Original POM has {} dependencies (GA-keyed) after scope filtering.", originalDepsByGa.size());

            // Step 5: Compare with Aether (GA-based) to find shaded candidates
            logger.debug("[Method 1] Step 5 - Retrieving Aether graph dependencies for host: {}", hostGa);
            Set<String> aetherChildren = aetherDirectChildrenByGa.containsKey(hostGa)
                    ? aetherDirectChildrenByGa.get(hostGa)
                    : Collections.<String>emptySet();

            Set<String> aetherGaKeys = new HashSet<String>();
            for (String aetherGav : aetherChildren) {
                String[] parts = aetherGav.split(":");
                if (parts.length >= 2) {
                    aetherGaKeys.add(parts[0] + ":" + parts[1]);
                }
            }

            // Step 6: Ghost filter + reporting
            // A candidate is a genuine ghost ONLY if it is explicitly excluded via <artifactSet><excludes>.
            // Relocated deps are NOT excluded — their classes are in the JAR under the shadedPattern path.
            int originalSize = originalDepsByGa.size();
            int shadedCount = 0;
            int ghostCount = 0;

            for (Map.Entry<String, String> depEntry : originalDepsByGa.entrySet()) {
                String gaKey = depEntry.getKey();
                String gavValue = depEntry.getValue();

                if (aetherGaKeys.contains(gaKey)) {
                    logger.trace("[Method 1] Step 6 - Dependency resolved by Aether (GA match): {}", gaKey);
                    continue;
                }

                // Candidate: in original POM but not in Aether → potentially shaded
                String[] gaParts = gaKey.split(":", 2);
                String candidateGroupId = gaParts.length > 0 ? gaParts[0] : "";
                String candidateArtifactId = gaParts.length > 1 ? gaParts[1] : "";

                if (isGhost(candidateGroupId, candidateArtifactId, gavValue, classPathPrefixes)) {
                    ghostCount++;
                    continue;
                }

                logger.info("[Method 1] Step 6 - Shaded dependency detected: {}", gavValue);
                discoveredDependencies.add(new DiscoveredDependency(gavValue, "Delta Analysis (ProjectBuilder)"));
                shadedCount++;
            }

            logger.debug("[Method 1] Step 6 - Delta: {} original, {} aether GA keys, {} shaded, {} ghosts filtered.",
                    originalSize, aetherGaKeys.size(), shadedCount, ghostCount);

        } catch (Exception e) {
            logger.error("[Method 1] Failed to process ProjectBuilder delta math for JAR {}: {}", jarFile.getName(), e.getMessage(), e);
        } finally {
            if (tempPomFile != null && tempPomFile.exists()) {
                try { Files.delete(tempPomFile.toPath()); } catch (Exception ignored) { tempPomFile.deleteOnExit(); }
            }
            if (tempDir != null) {
                try { Files.delete(tempDir); } catch (Exception ignored) { tempDir.toFile().deleteOnExit(); }
            }
        }

        logger.debug("[Method 1] Delta Analysis completed for JAR: {}", jarFile.getName());
        return discoveredDependencies;
    }

    /**
     * Determines whether a delta candidate is a ghost (not actually bundled in the JAR).
     *
     * <p><strong>Primary path (ShadePluginConfig available):</strong>
     * A dep is a ghost if and only if it is explicitly listed in {@code <artifactSet><excludes>}.
     * This correctly handles relocated deps — their groupId is NOT in the excludes list, so they pass.
     *
     * <p><strong>Fallback path (ShadePluginConfig null):</strong>
     * Falls back to the legacy class-path prefix heuristic: if no {@code .class} files exist under
     * the dep's groupId package path, treat it as a ghost.
     * <em>Note:</em> this fallback incorrectly drops relocated deps (known limitation when shade config
     * is unavailable).
     */
    private boolean isGhost(
            String groupId,
            String artifactId,
            String gavValue,
            @Nullable NavigableSet<String> classPathPrefixesFallback
    ) {
        if (shadePluginConfig != null) {
            // Primary: use explicit exclude list — relocation-aware and intent-driven
            if (shadePluginConfig.isExcluded(groupId, artifactId)) {
                logger.debug("[Method 1] Ghost (explicitly excluded in shade config): {}", gavValue);
                return true;
            }
            if (shadePluginConfig.isRelocated(groupId)) {
                logger.debug("[Method 1] Dep was relocated, confirming as shaded: {}", gavValue);
            }
            return false;
        }

        // Fallback: legacy class-path heuristic
        if (classPathPrefixesFallback != null) {
            String expectedClassPrefix = groupId.replace('.', '/') + "/";
            String ceiling = classPathPrefixesFallback.ceiling(expectedClassPrefix);
            boolean hasClasses = ceiling != null && ceiling.startsWith(expectedClassPrefix);
            if (!hasClasses) {
                logger.debug("[Method 1] Ghost (legacy fallback — no .class files for '{}'): {}", expectedClassPrefix, gavValue);
                return true;
            }
        }

        return false;
    }
}
