package com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.util;

import com.blackduck.integration.detectable.detectables.maven.resolver.model.pomxml.PomXml;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.pomxml.PomXmlPlugin;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.pomxml.PomXmlShadeArtifactSet;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.pomxml.PomXmlShadeConfig;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.pomxml.PomXmlShadeExecution;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.pomxml.PomXmlShadeRelocation;
import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.model.ShadePluginConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Reads a dependency JAR's embedded {@code pom.xml} and extracts the
 * maven-shade-plugin configuration into a {@link ShadePluginConfig}.
 *
 * <h2>Why the embedded POM?</h2>
 * <p>The POM published to Maven Central is the <em>dependency-reduced</em> version —
 * its {@code <dependencies>} section has already been cleaned up. But the original
 * {@code pom.xml} baked into the JAR at
 * {@code META-INF/maven/<groupId>/<artifactId>/pom.xml} is untouched and retains
 * the full shade plugin {@code <configuration>}, including relocation patterns and
 * artifact exclusions. This is the authoritative source for what was (or was not) bundled.
 *
 * <h2>Two places to look</h2>
 * <p>The shade plugin config may appear in two locations (both are merged):
 * <ol>
 *   <li>Directly on the {@code <plugin>} element ({@code <plugin><configuration>})</li>
 *   <li>Inside an {@code <execution>} ({@code <plugin><executions><execution><configuration>})</li>
 * </ol>
 */
public class ShadePluginConfigExtractor {

    private static final Logger logger = LoggerFactory.getLogger(ShadePluginConfigExtractor.class);
    private static final String MAVEN_SHADE_PLUGIN_ARTIFACT_ID = "maven-shade-plugin";

    private ShadePluginConfigExtractor() {
        // static utility class
    }

    /**
     * Extracts the shade plugin configuration from the given JAR's embedded {@code pom.xml}.
     *
     * @param jarFile      the opened JAR to inspect (caller manages lifecycle — do not close here)
     * @param hostArtifact the Aether artifact representing this JAR (used to find the POM path)
     * @return a {@link ShadePluginConfig} with the parsed excludes and relocation patterns,
     *         or {@code null} if the embedded POM could not be read or contains no shade config
     */
    public static ShadePluginConfig extract(JarFile jarFile, Artifact hostArtifact) {
        if (jarFile == null || hostArtifact == null) {
            return null;
        }

        String pomPath = "META-INF/maven/"
                + hostArtifact.getGroupId()
                + "/" + hostArtifact.getArtifactId()
                + "/pom.xml";

        JarEntry pomEntry = jarFile.getJarEntry(pomPath);
        if (pomEntry == null) {
            logger.debug("[ShadeConfigExtractor] Embedded pom.xml not found at '{}' in JAR: {}",
                    pomPath, jarFile.getName());
            return null;
        }

        try (InputStream is = jarFile.getInputStream(pomEntry)) {
            XmlMapper xmlMapper = new XmlMapper();
            xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            PomXml pomXml = xmlMapper.readValue(is, PomXml.class);

            return buildShadeConfig(pomXml, hostArtifact);

        } catch (Exception e) {
            logger.debug("[ShadeConfigExtractor] Failed to parse embedded pom.xml from JAR '{}': {}",
                    jarFile.getName(), e.getMessage());
            return null;
        }
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private static ShadePluginConfig buildShadeConfig(PomXml pomXml, Artifact hostArtifact) {
        if (pomXml == null || pomXml.getBuild() == null || pomXml.getBuild().getPlugins() == null) {
            return null;
        }

        for (PomXmlPlugin plugin : pomXml.getBuild().getPlugins()) {
            if (!MAVEN_SHADE_PLUGIN_ARTIFACT_ID.equals(plugin.getArtifactId())) {
                continue;
            }

            // Merge plugin-level config + all execution-level configs
            Set<String> excludedGavPatterns = new HashSet<>();
            Set<String> relocatedPrefixes = new HashSet<>();

            // Style A: <plugin><configuration>
            mergeConfig(plugin.getConfiguration(), excludedGavPatterns, relocatedPrefixes);

            // Style B: <plugin><executions><execution><configuration>
            if (plugin.getExecutions() != null) {
                for (PomXmlShadeExecution execution : plugin.getExecutions()) {
                    mergeConfig(execution.getConfiguration(), excludedGavPatterns, relocatedPrefixes);
                }
            }

            logger.debug("[ShadeConfigExtractor] Extracted shade config for {}:{}: {} exclusion(s), {} relocation(s)",
                    hostArtifact.getGroupId(), hostArtifact.getArtifactId(),
                    excludedGavPatterns.size(), relocatedPrefixes.size());

            return new ShadePluginConfig(excludedGavPatterns, relocatedPrefixes);
        }

        logger.debug("[ShadeConfigExtractor] maven-shade-plugin found but no configuration block in: {}:{}",
                hostArtifact.getGroupId(), hostArtifact.getArtifactId());
        // Return empty config (plugin exists, no explicit excludes → nothing excluded)
        return new ShadePluginConfig(new HashSet<String>(), new HashSet<String>());
    }

    private static void mergeConfig(
            PomXmlShadeConfig config,
            Set<String> excludedGavPatterns,
            Set<String> relocatedPrefixes
    ) {
        if (config == null) {
            return;
        }

        // Collect excluded GAV patterns
        PomXmlShadeArtifactSet artifactSet = config.getArtifactSet();
        if (artifactSet != null && artifactSet.getExcludes() != null) {
            for (String exclude : artifactSet.getExcludes()) {
                if (exclude != null && !exclude.trim().isEmpty()) {
                    excludedGavPatterns.add(exclude.trim());
                    logger.debug("[ShadeConfigExtractor]   Exclude pattern: {}", exclude.trim());
                }
            }
        }

        // Collect relocation patterns (convert to class-path prefixes: com.google.guava → com/google/guava/)
        List<PomXmlShadeRelocation> relocations = config.getRelocations();
        if (relocations != null) {
            for (PomXmlShadeRelocation relocation : relocations) {
                String pattern = relocation.getPattern();
                if (pattern != null && !pattern.trim().isEmpty()) {
                    String classPrefix = pattern.trim().replace('.', '/') + "/";
                    relocatedPrefixes.add(classPrefix);
                    logger.debug("[ShadeConfigExtractor]   Relocation pattern: {} → class prefix: {}",
                            pattern.trim(), classPrefix);
                }
            }
        }
    }
}

