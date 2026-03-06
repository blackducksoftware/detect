package com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.methods;

// Method 1: Detect shaded dependencies by comparing original pom.xml with dependency-reduced-pom.xml.
//
// NOTE: An alternative to manual parent POM walking is to use the Eclipse Aether library
// (maven-resolver-api + maven-resolver-impl). Aether handles property interpolation,
// parent chain traversal, BOM imports, and version ranges automatically via
// RepositorySystem.resolveDependencies(). The trade-off is ~10 additional JAR dependencies
// and significant setup complexity. The manual approach implemented here covers the
// majority of real-world cases (shallow parent chains, simple property inheritance)
// with zero extra dependencies.


import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.ShadedDependencyInspector;
import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.model.DiscoveredDependency;
import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.util.SecureXmlDocumentBuilder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detects shaded dependencies by performing a "delta" (set difference) between:
 *   - The original pom.xml (contains ALL declared dependencies)
 *   - The dependency-reduced-pom.xml (contains only non-shaded dependencies)
 *
 * The difference = the shaded dependencies.
 */
public class DeltaAnalysisInspector implements ShadedDependencyInspector {

    private static final Logger logger = LoggerFactory.getLogger(DeltaAnalysisInspector.class);

    // Scopes that should be excluded from shaded dependency detection
    // "test" scope: only used during testing, not included in final JAR
    // "provided" scope: expected to be provided by the runtime environment (e.g., servlet container)
    // Java 8 compatible: Using Collections.unmodifiableSet instead of Set.of()
    private static final Set<String> EXCLUDED_SCOPES = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("test", "provided"))
    );

    // Regex pattern to match Maven property placeholders like ${property.name}
    private static final Pattern PROPERTY_PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    // Maximum depth for parent POM walking to prevent infinite loops
    private static final int MAX_PARENT_DEPTH = 5;

    // Timeouts for Maven Central HTTP requests (milliseconds)
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;

    // Cache for fetched parent POMs to avoid redundant disk/network access
    // Key format: "groupId:artifactId:version"
    // Java 8 compatible: explicit type parameters
    private final Map<String, Document> parentPomCache = new HashMap<String, Document>();

    /**
     * Main detection logic. Locates both POM files inside the JAR, parses them,
     * and computes the set difference to identify shaded dependencies.
     *
     * @param jarFile The JAR file to analyze.
     * @return List of discovered shaded dependencies.
     */
    @Override
    public List<DiscoveredDependency> detectShadedDependencies(JarFile jarFile) {
        // Clear the cache for each new JAR analysis
        parentPomCache.clear();

        List<DiscoveredDependency> discoveredDependencies = new ArrayList<>();

        logger.debug("[Method 1] Starting Delta Analysis for JAR: {}", jarFile.getName());

        JarEntry originalPomEntry = null;
        JarEntry reducedPomEntry = null;

        // Step 1: Walk through all entries to locate the two POM files we need
        logger.debug("[Method 1] Step 1 - Scanning JAR entries for POM files...");
        Enumeration<JarEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();

            // The original POM is always tucked inside META-INF/maven/<groupId>/<artifactId>/pom.xml
            if (name.startsWith("META-INF/maven/") && name.endsWith("pom.xml")) {
                originalPomEntry = entry;
                logger.debug("[Method 1] Step 1 - Found original POM: {}", name);
            }
            // The reduced POM is often placed at the root of the JAR by the shade plugin
            else if (name.endsWith("dependency-reduced-pom.xml")) {
                reducedPomEntry = entry;
                logger.debug("[Method 1] Step 1 - Found reduced POM: {}", name);
            }
        }

        // If we can't find the original POM, there's nothing to compare — exit early
        if (originalPomEntry == null) {
            logger.debug("[Method 1] Step 1 - Original pom.xml not found in JAR. Delta analysis skipped.");
            return discoveredDependencies;
        }

        // Step 2: Parse the original POM to get its full list of dependencies
        logger.debug("[Method 1] Step 2 - Parsing original POM XML tree...");
        Set<String> originalDependencies = parseDependencies(jarFile, originalPomEntry);
        logger.debug("[Method 1] Step 2 - Original POM has {} direct dependencies (after scope filtering).", originalDependencies.size());

        // Step 3: Compare with reduced POM and calculate the delta
        if (reducedPomEntry != null) {
            logger.debug("[Method 1] Step 3 - Parsing reduced POM and calculating strict delta...");
            Set<String> reducedDependencies = parseDependencies(jarFile, reducedPomEntry);
            logger.debug("[Method 1] Step 3 - Reduced POM has {} dependencies (after scope filtering).", reducedDependencies.size());

            // Set Difference: Original - Reduced = Shaded dependencies
            originalDependencies.removeAll(reducedDependencies);

            logger.debug("[Method 1] Step 3 - Delta = {} shaded dependency(ies) found.", originalDependencies.size());

            for (String shadedGav : originalDependencies) {
                logger.debug("[Method 1] Step 3 - Shaded: {}", shadedGav);
                discoveredDependencies.add(new DiscoveredDependency(shadedGav, "Delta Analysis (Exact)"));
            }
        } else {
            // No reduced POM found — we can only return the full original list for external comparison
            logger.debug("[Method 1] Step 3 - Reduced POM not found. Returning all original dependencies for external comparison.");

            for (String gav : originalDependencies) {
                logger.debug("[Method 1] Step 3 - Candidate: {}", gav);
                discoveredDependencies.add(new DiscoveredDependency(gav, "Original POM (Needs external Delta)"));
            }
        }

        // Step 4: Final summary
        logger.debug("[Method 1] Step 4 - Delta Analysis complete. Total results: {}", discoveredDependencies.size());

        return discoveredDependencies;
    }

    /**
     * Creates a secure DocumentBuilder using the centralized utility.
     * This method delegates to SecureXmlDocumentBuilder which applies
     * fail-safe XXE protection that works across different parser implementations.
     *
     * @return A securely configured DocumentBuilder.
     * @throws Exception If the DocumentBuilder cannot be created.
     */
    private DocumentBuilder createSecureDocumentBuilder() throws Exception {
        return SecureXmlDocumentBuilder.newDocumentBuilder(logger);
    }

    /**
     * Safely parses a POM file from a JAR entry and extracts direct runtime dependencies.
     * Only considers <dependency> elements directly under <project><dependencies>,
     * ignoring the <dependencyManagement> block.
     * Also filters out dependencies with scope "test" or "provided" as they are not shaded.
     * Resolves property placeholders like ${project.version} using the POM's properties section.
     *
     * @param jarFile The JAR containing the POM entry.
     * @param entry   The specific JAR entry pointing to a pom.xml file.
     * @return A set of dependency GAV strings (groupId:artifactId:version).
     */
    private Set<String> parseDependencies(JarFile jarFile, JarEntry entry) {
        Set<String> dependencies = new HashSet<String>();

        InputStream is = null;
        try {
            is = jarFile.getInputStream(entry);

            // Create a secure XML parser with fail-safe XXE protection
            DocumentBuilder builder = createSecureDocumentBuilder();
            Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();

            // Step 2a: Extract all properties from the POM for placeholder resolution
            Map<String, String> properties = extractProperties(doc);
            logger.debug("[Method 1] Step 2a - Extracted {} properties for placeholder resolution.", properties.size());

            // Find all <dependency> elements in the document
            NodeList dependencyNodes = doc.getElementsByTagName("dependency");

            for (int i = 0; i < dependencyNodes.getLength(); i++) {
                Node node = dependencyNodes.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    // Only grab dependencies directly under <project><dependencies>
                    Node parent = element.getParentNode();
                    if (parent != null && "dependencies".equals(parent.getNodeName())) {
                        Node grandParent = parent.getParentNode();
                        if (grandParent != null && "project".equals(grandParent.getNodeName())) {

                            String groupId = getTagValue(element, "groupId");
                            String artifactId = getTagValue(element, "artifactId");
                            String version = getTagValue(element, "version");

                            groupId = resolvePropertyPlaceholders(groupId, properties);
                            artifactId = resolvePropertyPlaceholders(artifactId, properties);
                            version = resolvePropertyPlaceholders(version, properties);

                            if (version == null || version.isEmpty() || "inherited".equals(version) || version.startsWith("${")) {

                                String managed = findManagedVersion(doc, groupId, artifactId, properties);
                                if (managed != null && !managed.isEmpty() && !managed.startsWith("${")) {
                                    version = managed;
                                    logger.debug("[Method 1] Resolved version from dependencyManagement for {}:{} -> {}",
                                        groupId, artifactId, version);

                                } else if ((managed != null && managed.startsWith("${")) ||
                                        (version != null && version.startsWith("${"))) {
                                    String placeholderToResolve = (managed != null && managed.startsWith("${")) ? managed : version;

                                    logger.debug("[Method 1] Attempting parent POM walking for: {}", placeholderToResolve);
                                    String walked = resolveVersionWithParentWalking(placeholderToResolve, doc, groupId, artifactId, MAX_PARENT_DEPTH);
                                    if (walked != null && !walked.equals("UNKNOWN") && !walked.startsWith("${")) {
                                        version = walked;
                                        logger.debug("[Method 1] Resolved '{}' -> '{}' from parent POM.", placeholderToResolve, version);
                                    } else {
                                        logger.warn("[Method 1] Could not resolve '{}' after walking {} parent levels. Marking as UNKNOWN.",
                                            placeholderToResolve, MAX_PARENT_DEPTH);
                                        version = "UNKNOWN";
                                    }

                                } else {
                                    logger.warn("[Method 1] Could not resolve version for {}:{} - marking as UNKNOWN", groupId, artifactId);
                                    version = "UNKNOWN";
                                }
                            }

                            String scope = getTagValue(element, "scope");
                            if ("inherited".equals(scope) || scope == null || scope.isEmpty()) {
                                scope = "compile";
                            }

                            if (EXCLUDED_SCOPES.contains(scope.toLowerCase())) {
                                logger.debug("[Method 1] Skipping dependency with scope '{}': {}:{}:{}",
                                    scope, groupId, artifactId, version);
                                continue;
                            }

                            dependencies.add(groupId + ":" + artifactId + ":" + version);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("[Method 1] Failed to parse XML for {} - {}", entry.getName(), e.getMessage());
            logger.debug("[Method 1] Stack trace for parse failure:", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignored) {
                    // Ignore close exceptions
                }
            }
        }

        return dependencies;
    }

    /**
     * Resolves a version placeholder by walking up the Maven parent POM chain.
     * This method handles cases where a property like ${jsr305.version} is defined
     * in a parent POM that is not embedded in the JAR.
     *
     * Resolution strategy (in priority order, short-circuit on first hit):
     * 1. Local .m2 cache first — instant, no network
     * 2. Maven Central fallback — if not in local cache
     * 3. Recursive parent walking — if property not found in fetched parent
     * 4. Depth cap at 5 — never recurse deeper than 5 levels
     *
     * @param placeholder     The version string (must be a ${placeholder}).
     * @param currentPomDoc   The current POM document to extract <parent> block from.
     * @param depGroupId      The groupId of the dependency being resolved (for dependencyManagement lookup).
     * @param depArtifactId   The artifactId of the dependency being resolved (for dependencyManagement lookup).
     * @param maxDepth        Maximum recursion depth (to prevent infinite loops).
     * @return The resolved version, or "UNKNOWN" if resolution fails.
     */
    private String resolveVersionWithParentWalking(String placeholder, Document currentPomDoc,
                                                   String depGroupId, String depArtifactId, int maxDepth) {
        if (maxDepth <= 0) {
            return "UNKNOWN";
        }

        String propertyKey = null;
        if (placeholder != null && placeholder.startsWith("${") && placeholder.endsWith("}")) {
            propertyKey = placeholder.substring(2, placeholder.length() - 1);
        }

        Element root = currentPomDoc.getDocumentElement();
        NodeList parentNodes = root.getElementsByTagName("parent");

        if (parentNodes.getLength() == 0) {
            return "UNKNOWN";
        }

        Node parentNode = parentNodes.item(0);

        if (parentNode.getParentNode() == null || !"project".equals(parentNode.getParentNode().getNodeName())) {
            for (int i = 0; i < parentNodes.getLength(); i++) {
                Node node = parentNodes.item(i);
                if (node.getParentNode() != null && "project".equals(node.getParentNode().getNodeName())) {
                    parentNode = node;
                    break;
                }
            }
        }

        if (parentNode.getNodeType() != Node.ELEMENT_NODE) {
            return "UNKNOWN";
        }

        Element parentElement = (Element) parentNode;
        String parentGroupId = getTagValue(parentElement, "groupId");
        String parentArtifactId = getTagValue(parentElement, "artifactId");
        String parentVersion = getTagValue(parentElement, "version");

        Map<String, String> currentProperties = extractProperties(currentPomDoc);
        parentVersion = resolvePropertyPlaceholders(parentVersion, currentProperties);

        if (parentGroupId == null || parentGroupId.isEmpty() || "inherited".equals(parentGroupId) ||
                parentArtifactId == null || parentArtifactId.isEmpty() || "inherited".equals(parentArtifactId) ||
                parentVersion == null || parentVersion.isEmpty() || "inherited".equals(parentVersion) ||
                parentVersion.startsWith("${")) {
            return "UNKNOWN";
        }

        logger.debug("[Method 1] Resolving '{}' - checking parent: {}:{}:{}",
            placeholder, parentGroupId, parentArtifactId, parentVersion);

        Document parentDoc = fetchPom(parentGroupId, parentArtifactId, parentVersion);
        if (parentDoc == null) {
            return "UNKNOWN";
        }

        Map<String, String> parentProperties = extractProperties(parentDoc);

        if (propertyKey != null && parentProperties.containsKey(propertyKey)) {
            String resolvedValue = parentProperties.get(propertyKey);

            if (resolvedValue != null && resolvedValue.startsWith("${")) {
                return resolveVersionWithParentWalking(resolvedValue, parentDoc, depGroupId, depArtifactId, maxDepth - 1);
            }

            return resolvedValue;
        }

        String managedInParent = findManagedVersion(parentDoc, depGroupId, depArtifactId, parentProperties);
        if (managedInParent != null && !managedInParent.isEmpty() && !managedInParent.startsWith("${")) {
            logger.debug("[Method 1] Found version in parent's dependencyManagement: {}", managedInParent);
            return managedInParent;
        }

        if (managedInParent != null && managedInParent.startsWith("${")) {
            return resolveVersionWithParentWalking(managedInParent, parentDoc, depGroupId, depArtifactId, maxDepth - 1);
        }

        return resolveVersionWithParentWalking(placeholder, parentDoc, depGroupId, depArtifactId, maxDepth - 1);
    }

    /**
     * Fetches a POM file by Maven coordinates.
     * Tries local .m2 cache first, then falls back to Maven Central.
     *
     * @param groupId    The Maven groupId.
     * @param artifactId The Maven artifactId.
     * @param version    The Maven version.
     * @return The parsed POM Document, or null if not found or on error.
     */
    private Document fetchPom(String groupId, String artifactId, String version) {
        // Check cache first
        String cacheKey = groupId + ":" + artifactId + ":" + version;
        if (parentPomCache.containsKey(cacheKey)) {
            return parentPomCache.get(cacheKey);
        }

        // Try local .m2 cache first
        Document doc = fetchPomFromLocalCache(groupId, artifactId, version);

        // Fall back to Maven Central if not in local cache
        if (doc == null) {
            doc = fetchPomFromMavenCentral(groupId, artifactId, version);
        }

        // Cache the result (including null) to prevent repeated failed lookups.
        // Intentionally caching null: if a POM doesn't exist locally or on Maven Central,
        // we don't want to retry the same failed fetch on every call within this JAR analysis.
        parentPomCache.put(cacheKey, doc);

        return doc;
    }

    /**
     * Fetches a POM from the local Maven cache (~/.m2/repository).
     *
     * @param groupId    The Maven groupId.
     * @param artifactId The Maven artifactId.
     * @param version    The Maven version.
     * @return The parsed POM Document, or null if not found or on error.
     */
    private Document fetchPomFromLocalCache(String groupId, String artifactId, String version) {
        String userHome = System.getProperty("user.home");
        String groupPath = groupId.replace('.', '/');
        String pomFileName = artifactId + "-" + version + ".pom";
        String localPath = userHome + "/.m2/repository/" + groupPath + "/" + artifactId + "/" + version + "/" + pomFileName;

        File pomFile = new File(localPath);
        if (!pomFile.exists()) {
            return null;
        }

        FileInputStream is = null;
        try {
            is = new FileInputStream(pomFile);
            DocumentBuilder builder = createSecureDocumentBuilder();
            Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();

            logger.debug("[Method 1] Parent POM found in local cache: {}", localPath);
            return doc;

        } catch (Exception e) {
            logger.error("[Method 1] Failed to parse local POM {}: {}", localPath, e.getMessage());
            logger.debug("[Method 1] Stack trace for local POM parse failure:", e);
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignored) {
                    // Ignore close exceptions
                }
            }
        }
    }

    /**
     * Fetches a POM from Maven Central repository.
     *
     * @param groupId    The Maven groupId.
     * @param artifactId The Maven artifactId.
     * @param version    The Maven version.
     * @return The parsed POM Document, or null if not found or on error.
     */
    private Document fetchPomFromMavenCentral(String groupId, String artifactId, String version) {
        String groupPath = groupId.replace('.', '/');
        String pomFileName = artifactId + "-" + version + ".pom";
        String urlString = "https://repo1.maven.org/maven2/" + groupPath + "/" + artifactId + "/" + version + "/" + pomFileName;

        logger.debug("[Method 1] Fetching parent POM from Maven Central: {}", urlString);

        HttpURLConnection connection = null;
        InputStream is = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "ShadeInspector/1.0");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                logger.warn("[Method 1] Maven Central returned HTTP {} for {}", responseCode, urlString);
                return null;
            }

            is = connection.getInputStream();
            DocumentBuilder builder = createSecureDocumentBuilder();
            Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();
            return doc;

        } catch (Exception e) {
            logger.error("[Method 1] Failed to fetch POM from Maven Central {}: {}", urlString, e.getMessage());
            logger.debug("[Method 1] Stack trace for Maven Central fetch failure:", e);
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignored) {
                    // Ignore close exceptions
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Extracts all properties from the POM document for placeholder resolution.
     * This includes:
     *   - User-defined properties from <properties> section
     *   - Implicit project properties like project.version, project.groupId, project.artifactId
     *   - Parent project properties (if parent is defined)
     *
     * @param doc The parsed POM document.
     * @return A map of property names to their values.
     */
    private Map<String, String> extractProperties(Document doc) {
        Map<String, String> properties = new HashMap<String, String>();

        // Extract project-level implicit properties (project.version, project.groupId, etc.)
        Element root = doc.getDocumentElement();

        // Get project's own version, groupId, artifactId
        String projectVersion = getDirectChildTagValue(root, "version");
        String projectGroupId = getDirectChildTagValue(root, "groupId");
        String projectArtifactId = getDirectChildTagValue(root, "artifactId");

        // If project doesn't define its own groupId/version, inherit from parent
        NodeList parentNodes = root.getElementsByTagName("parent");
        if (parentNodes.getLength() > 0) {
            Element parentElement = (Element) parentNodes.item(0);

            // If project.groupId is missing, inherit from parent
            if (projectGroupId == null || projectGroupId.isEmpty() || "inherited".equals(projectGroupId)) {
                projectGroupId = getTagValue(parentElement, "groupId");
            }

            // If project.version is missing, inherit from parent
            if (projectVersion == null || projectVersion.isEmpty() || "inherited".equals(projectVersion)) {
                projectVersion = getTagValue(parentElement, "version");
            }
        }

        // Add implicit project properties - these can be referenced as ${project.version}, etc.
        if (projectVersion != null && !"inherited".equals(projectVersion)) {
            properties.put("project.version", projectVersion);
            properties.put("pom.version", projectVersion);  // Legacy alias
            properties.put("version", projectVersion);      // Short form sometimes used
        }
        if (projectGroupId != null && !"inherited".equals(projectGroupId)) {
            properties.put("project.groupId", projectGroupId);
            properties.put("pom.groupId", projectGroupId);  // Legacy alias
            properties.put("groupId", projectGroupId);      // Short form sometimes used
        }
        if (projectArtifactId != null && !"inherited".equals(projectArtifactId)) {
            properties.put("project.artifactId", projectArtifactId);
            properties.put("pom.artifactId", projectArtifactId);  // Legacy alias
            properties.put("artifactId", projectArtifactId);      // Short form sometimes used
        }

        // Extract user-defined properties from <properties> section
        NodeList propertiesNodes = doc.getElementsByTagName("properties");
        if (propertiesNodes.getLength() > 0) {
            Element propertiesElement = (Element) propertiesNodes.item(0);
            NodeList children = propertiesElement.getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    // Each child element is a property: <propertyName>value</propertyName>
                    String propertyName = child.getNodeName();
                    String propertyValue = child.getTextContent().trim();
                    properties.put(propertyName, propertyValue);
                }
            }
        }

        return properties;
    }

    /**
     * Resolves property placeholders in a string value.
     * Handles nested placeholders by resolving iteratively up to a maximum depth.
     * Example: "${project.version}" -> "1.2.3" if project.version is defined.
     *
     * @param value      The string that may contain ${property} placeholders.
     * @param properties The map of property names to resolved values.
     * @return The string with all resolvable placeholders replaced.
     */
    private String resolvePropertyPlaceholders(String value, Map<String, String> properties) {
        if (value == null || value.isEmpty() || "inherited".equals(value)) {
            return value;
        }

        int maxIterations = 10;
        int iteration = 0;
        String resolved = value;

        while (resolved.contains("${") && iteration < maxIterations) {
            Matcher matcher = PROPERTY_PLACEHOLDER_PATTERN.matcher(resolved);
            StringBuffer sb = new StringBuffer();

            boolean foundAny = false;
            while (matcher.find()) {
                String propertyName = matcher.group(1);
                String replacement = properties.get(propertyName);

                if (replacement != null) {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                    foundAny = true;
                } else {
                    logger.debug("[Method 1] Could not resolve property: ${{}}", propertyName);
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
                }
            }
            matcher.appendTail(sb);
            resolved = sb.toString();

            if (!foundAny) {
                break;
            }
            iteration++;
        }

        return resolved;
    }

    /**
     * Finds the version of a dependency from the <dependencyManagement> section.
     * This is used when a dependency doesn't declare its own version.
     *
     * @param doc        The parsed POM document.
     * @param groupId    The groupId of the dependency to look up.
     * @param artifactId The artifactId of the dependency to look up.
     * @param properties The properties map for resolving placeholders.
     * @return The managed version, or null if not found.
     */
    private String findManagedVersion(Document doc, String groupId, String artifactId, Map<String, String> properties) {
        // Find <dependencyManagement><dependencies> section
        NodeList depMgmtNodes = doc.getElementsByTagName("dependencyManagement");
        if (depMgmtNodes.getLength() == 0) {
            return null;
        }

        Element depMgmtElement = (Element) depMgmtNodes.item(0);
        NodeList dependencyNodes = depMgmtElement.getElementsByTagName("dependency");

        for (int i = 0; i < dependencyNodes.getLength(); i++) {
            Node node = dependencyNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;

                String managedGroupId = resolvePropertyPlaceholders(getTagValue(element, "groupId"), properties);
                String managedArtifactId = resolvePropertyPlaceholders(getTagValue(element, "artifactId"), properties);

                // Check if this managed dependency matches the one we're looking for
                if (groupId.equals(managedGroupId) && artifactId.equals(managedArtifactId)) {
                    String managedVersion = getTagValue(element, "version");
                    // Resolve any placeholders in the managed version
                    return resolvePropertyPlaceholders(managedVersion, properties);
                }
            }
        }

        return null;
    }

    /**
     * Gets the value of a direct child element (not nested deeper).
     * This is used for getting project-level properties without accidentally
     * grabbing values from nested elements like <parent>.
     *
     * @param parentElement The parent element to search within.
     * @param tagName       The name of the direct child tag.
     * @return The text content of the tag, or null if not found.
     */
    private String getDirectChildTagValue(Element parentElement, String tagName) {
        NodeList children = parentElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && tagName.equals(child.getNodeName())) {
                return child.getTextContent().trim();
            }
        }
        return null;
    }

    /**
     * Helper method to safely extract text content from a child XML element.
     *
     * @param parentElement The parent XML element containing the tag.
     * @param tagName       The name of the child tag to read.
     * @return The text content of the tag, or "inherited" if the tag is missing
     *         (some values are legitimately inherited from parent POMs).
     */
    private String getTagValue(Element parentElement, String tagName) {
        NodeList list = parentElement.getElementsByTagName(tagName);
        if (list != null && list.getLength() > 0) {
            return list.item(0).getTextContent().trim();
        }
        // Return "inherited" when the tag is absent — this happens when values come from a parent POM
        return "inherited";
    }
}

