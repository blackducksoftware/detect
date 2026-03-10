package com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.methods;

import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.ShadedDependencyInspector;
import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.model.DiscoveredDependency;
import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.util.SecureXmlDocumentBuilder;
import javax.xml.parsers.DocumentBuilder;
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
 * Detects shaded dependencies by performing a delta (set difference) between:
 * - The Original pom.xml hidden inside the JAR (contains ALL declared dependencies)
 * - The Eclipse Aether Dependency Graph (contains only unshaded dependencies)
 *
 * The difference = the shaded dependencies.
 */
public class DeltaAnalysisInspector implements ShadedDependencyInspector {

    private static final Logger logger = LoggerFactory.getLogger(DeltaAnalysisInspector.class);

    private static final Set<String> EXCLUDED_SCOPES = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("test", "provided"))
    );

    private static final Pattern PROPERTY_PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final int MAX_PARENT_DEPTH = 5;
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;

    private final Map<String, Document> parentPomCache = new HashMap<String, Document>();

    // A mapping of GA -> Set of exact child GAVs extracted from the Aether tree
    private final Map<String, Set<String>> aetherDirectChildrenByGa;

    /**
     * Initializes the inspector with the Aether graph context.
     *
     * @param aetherDirectChildrenByGa A map of Artifact GA to a Set of its direct children GAVs.
     */
    public DeltaAnalysisInspector(Map<String, Set<String>> aetherDirectChildrenByGa) {
        this.aetherDirectChildrenByGa = aetherDirectChildrenByGa != null ? aetherDirectChildrenByGa : new HashMap<String, Set<String>>();
        logger.debug("[Method 1] DeltaAnalysisInspector initialized with {} GA entries in Aether map.", this.aetherDirectChildrenByGa.size());
    }

    @Override
    public List<DiscoveredDependency> detectShadedDependencies(JarFile jarFile) {
        parentPomCache.clear();
        logger.debug("[Method 1] Cleared parent POM cache.");
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

        // Step 2: Parse the original POM to get its full list of dependencies and its own coordinates
        logger.debug("[Method 1] Step 2 - Parsing original POM XML tree and resolving GAVs...");
        OriginalPomResult pomResult = parseDependencies(jarFile, originalPomEntry);
        Set<String> originalDependencies = pomResult.dependencies;

        logger.debug("[Method 1] Step 2 - Original POM has {} direct dependencies (after scope/optional filtering).", originalDependencies.size());
        for (String dep : originalDependencies) {
            logger.trace("[Method 1] Step 2 - Original POM dependency: {}", dep);
        }

        // Step 3: Extract the host's own GA from the parsed POM to query the Aether map
        String hostGroupId = pomResult.properties.get("project.groupId");
        String hostArtifactId = pomResult.properties.get("project.artifactId");

        logger.debug("[Method 1] Step 3 - Host artifact coordinates - GroupId: {}, ArtifactId: {}", hostGroupId, hostArtifactId);

        if (hostGroupId == null || hostArtifactId == null) {
            logger.warn("[Method 1] Step 3 - Could not extract Host GA from pom.xml. Aether delta math aborted.");
            return discoveredDependencies;
        }

        String hostGa = hostGroupId + ":" + hostArtifactId;
        logger.debug("[Method 1] Step 3 - Host GA: {}", hostGa);

        // Step 4: Compare with Aether and calculate the strict delta
        Set<String> aetherChildren = aetherDirectChildrenByGa.containsKey(hostGa)
                ? aetherDirectChildrenByGa.get(hostGa)
                : Collections.<String>emptySet();

        logger.debug("[Method 1] Step 4 - Aether graph lists {} direct dependencies for host {}.", aetherChildren.size(), hostGa);
        for (String aetherDep : aetherChildren) {
            logger.trace("[Method 1] Step 4 - Aether dependency: {}", aetherDep);
        }

        // Set Difference: Original POM GAVs - Aether Graph GAVs = Shaded dependencies
        int originalSize = originalDependencies.size();
        originalDependencies.removeAll(aetherChildren);
        int shadedCount = originalDependencies.size();

        logger.debug("[Method 1] Step 4 - Delta calculation: {} original - {} aether = {} shaded dependency(ies).", originalSize, aetherChildren.size(), shadedCount);

        for (String shadedGav : originalDependencies) {
            logger.info("[Method 1] Step 4 - Shaded dependency detected: {}", shadedGav);
            discoveredDependencies.add(new DiscoveredDependency(shadedGav, "Delta Analysis (Aether Graph)"));
        }

        logger.debug("[Method 1] Delta Analysis completed for JAR: {}", jarFile.getName());
        return discoveredDependencies;
    }

    private DocumentBuilder createSecureDocumentBuilder() throws Exception {
        return SecureXmlDocumentBuilder.newDocumentBuilder(logger);
    }

    /**
     * Container to return both the extracted dependencies and the root properties.
     */
    private static class OriginalPomResult {
        Set<String> dependencies = new HashSet<String>();
        Map<String, String> properties = new HashMap<String, String>();
    }

    private OriginalPomResult parseDependencies(JarFile jarFile, JarEntry entry) {
        OriginalPomResult result = new OriginalPomResult();
        InputStream is = null;

        try {
            is = jarFile.getInputStream(entry);
            DocumentBuilder builder = createSecureDocumentBuilder();
            Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();
            logger.debug("[Method 1] Successfully parsed POM XML document.");

            // Extract all properties, including project.groupId and project.artifactId
            Map<String, String> properties = extractProperties(doc);
            result.properties = properties;

            logger.debug("[Method 1] Extracted {} properties for placeholder resolution.", properties.size());

            NodeList dependencyNodes = doc.getElementsByTagName("dependency");
            logger.debug("[Method 1] Found {} <dependency> elements in POM.", dependencyNodes.getLength());

            for (int i = 0; i < dependencyNodes.getLength(); i++) {
                Node node = dependencyNodes.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    Node parent = element.getParentNode();
                    if (parent != null && "dependencies".equals(parent.getNodeName())) {
                        Node grandParent = parent.getParentNode();
                        if (grandParent != null && "project".equals(grandParent.getNodeName())) {

                            // Check for optional flag - Aether ignores these, so we must ignore them too to prevent false positives
                            String optional = getTagValue(element, "optional");
                            if ("true".equalsIgnoreCase(optional)) {
                                logger.trace("[Method 1] Skipping optional dependency at index {}", i);
                                continue;
                            }

                            String groupId = getTagValue(element, "groupId");
                            String artifactId = getTagValue(element, "artifactId");
                            String version = getTagValue(element, "version");

                            logger.trace("[Method 1] Processing dependency {}: groupId={}, artifactId={}, version={}", i, groupId, artifactId, version);

                            groupId = resolvePropertyPlaceholders(groupId, properties);
                            artifactId = resolvePropertyPlaceholders(artifactId, properties);
                            version = resolvePropertyPlaceholders(version, properties);

                            logger.trace("[Method 1] After placeholder resolution: groupId={}, artifactId={}, version={}", groupId, artifactId, version);

                            if (version == null || version.isEmpty() || "inherited".equals(version) || version.startsWith("${")) {
                                logger.debug("[Method 1] Version needs resolution for {}:{} (current: {})", groupId, artifactId, version);

                                String managed = findManagedVersion(doc, groupId, artifactId, properties);
                                if (managed != null && !managed.isEmpty() && !managed.startsWith("${")) {
                                    version = managed;
                                    logger.debug("[Method 1] Resolved version from dependencyManagement for {}:{} -> {}", groupId, artifactId, version);
                                } else if ((managed != null && managed.startsWith("${")) || (version != null && version.startsWith("${"))) {
                                    String placeholderToResolve = (managed != null && managed.startsWith("${")) ? managed : version;

                                    logger.debug("[Method 1] Attempting parent POM walking for: {}", placeholderToResolve);
                                    String walked = resolveVersionWithParentWalking(placeholderToResolve, doc, groupId, artifactId, MAX_PARENT_DEPTH);
                                    if (walked != null && !walked.equals("UNKNOWN") && !walked.startsWith("${")) {
                                        version = walked;
                                        logger.debug("[Method 1] Resolved version via parent walking for {}:{} -> {}", groupId, artifactId, version);
                                    } else {
                                        version = "UNKNOWN";
                                        logger.warn("[Method 1] Could not resolve version for {}:{}, setting to UNKNOWN", groupId, artifactId);
                                    }
                                } else {
                                    version = "UNKNOWN";
                                    logger.warn("[Method 1] No managed version found for {}:{}, setting to UNKNOWN", groupId, artifactId);
                                }
                            }

                            String scope = getTagValue(element, "scope");
                            if ("inherited".equals(scope) || scope == null || scope.isEmpty()) {
                                scope = "compile";
                            }

                            logger.trace("[Method 1] Dependency scope: {}", scope);

                            if (EXCLUDED_SCOPES.contains(scope.toLowerCase())) {
                                logger.trace("[Method 1] Skipping dependency with excluded scope '{}': {}:{}:{}", scope, groupId, artifactId, version);
                                continue;
                            }

                            String gav = groupId + ":" + artifactId + ":" + version;
                            result.dependencies.add(gav);
                            logger.trace("[Method 1] Added dependency: {}", gav);
                        }
                    }
                }
            }
            logger.debug("[Method 1] Finished processing dependencies. Total valid dependencies: {}", result.dependencies.size());
        } catch (Exception e) {
            logger.error("[Method 1] Failed to parse XML for {} - {}", entry.getName(), e.getMessage(), e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignored) {}
            }
        }

        return result;
    }

    private String resolveVersionWithParentWalking(String placeholder, Document currentPomDoc, String depGroupId, String depArtifactId, int maxDepth) {
        logger.debug("[Method 1] Parent walking for {}:{} at depth {} (max: {})", depGroupId, depArtifactId, MAX_PARENT_DEPTH - maxDepth + 1, MAX_PARENT_DEPTH);
        if (maxDepth <= 0) {
            logger.debug("[Method 1] Max parent depth reached. Returning UNKNOWN.");
            return "UNKNOWN";
        }

        String propertyKey = null;
        if (placeholder != null && placeholder.startsWith("${") && placeholder.endsWith("}")) {
            propertyKey = placeholder.substring(2, placeholder.length() - 1);
            logger.debug("[Method 1] Extracted property key: {}", propertyKey);
        }

        Element root = currentPomDoc.getDocumentElement();
        NodeList parentNodes = root.getElementsByTagName("parent");
        if (parentNodes.getLength() == 0) {
            logger.debug("[Method 1] No <parent> element found. Returning UNKNOWN.");
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
            logger.debug("[Method 1] Parent node is not an Element. Returning UNKNOWN.");
            return "UNKNOWN";
        }

        Element parentElement = (Element) parentNode;
        String parentGroupId = getTagValue(parentElement, "groupId");
        String parentArtifactId = getTagValue(parentElement, "artifactId");
        String parentVersion = getTagValue(parentElement, "version");

        logger.debug("[Method 1] Found parent: {}:{}:{}", parentGroupId, parentArtifactId, parentVersion);

        Map<String, String> currentProperties = extractProperties(currentPomDoc);
        parentVersion = resolvePropertyPlaceholders(parentVersion, currentProperties);

        if (parentGroupId == null || parentGroupId.isEmpty() || "inherited".equals(parentGroupId) ||
                parentArtifactId == null || parentArtifactId.isEmpty() || "inherited".equals(parentArtifactId) ||
                parentVersion == null || parentVersion.isEmpty() || "inherited".equals(parentVersion) ||
                parentVersion.startsWith("${")) {
            logger.debug("[Method 1] Parent coordinates incomplete or unresolved. Returning UNKNOWN.");
            return "UNKNOWN";
        }

        logger.debug("[Method 1] Fetching parent POM: {}:{}:{}", parentGroupId, parentArtifactId, parentVersion);
        Document parentDoc = fetchPom(parentGroupId, parentArtifactId, parentVersion);
        if (parentDoc == null) {
            logger.debug("[Method 1] Could not fetch parent POM. Returning UNKNOWN.");
            return "UNKNOWN";
        }

        Map<String, String> parentProperties = extractProperties(parentDoc);

        if (propertyKey != null && parentProperties.containsKey(propertyKey)) {
            String resolvedValue = parentProperties.get(propertyKey);
            logger.debug("[Method 1] Found property '{}' in parent with value: {}", propertyKey, resolvedValue);
            if (resolvedValue != null && resolvedValue.startsWith("${")) {
                logger.debug("[Method 1] Property value is still a placeholder. Recursing...");
                return resolveVersionWithParentWalking(resolvedValue, parentDoc, depGroupId, depArtifactId, maxDepth - 1);
            }
            return resolvedValue;
        }

        String managedInParent = findManagedVersion(parentDoc, depGroupId, depArtifactId, parentProperties);
        if (managedInParent != null && !managedInParent.isEmpty() && !managedInParent.startsWith("${")) {
            logger.debug("[Method 1] Found managed version in parent: {}", managedInParent);
            return managedInParent;
        }

        if (managedInParent != null && managedInParent.startsWith("${")) {
            logger.debug("[Method 1] Managed version in parent is a placeholder. Recursing...");
            return resolveVersionWithParentWalking(managedInParent, parentDoc, depGroupId, depArtifactId, maxDepth - 1);
        }

        logger.debug("[Method 1] No resolution found in parent. Recursing to grandparent...");
        return resolveVersionWithParentWalking(placeholder, parentDoc, depGroupId, depArtifactId, maxDepth - 1);
    }

    private Document fetchPom(String groupId, String artifactId, String version) {
        String cacheKey = groupId + ":" + artifactId + ":" + version;
        if (parentPomCache.containsKey(cacheKey)) {
            logger.debug("[Method 1] Parent POM cache hit for: {}", cacheKey);
            return parentPomCache.get(cacheKey);
        }

        logger.debug("[Method 1] Fetching POM for: {}", cacheKey);
        Document doc = fetchPomFromLocalCache(groupId, artifactId, version);
        if (doc == null) {
            logger.debug("[Method 1] Not found in local cache. Trying Maven Central...");
            doc = fetchPomFromMavenCentral(groupId, artifactId, version);
        } else {
            logger.debug("[Method 1] Found in local cache: {}", cacheKey);
        }

        parentPomCache.put(cacheKey, doc);
        return doc;
    }

    private Document fetchPomFromLocalCache(String groupId, String artifactId, String version) {
        String userHome = System.getProperty("user.home");
        String groupPath = groupId.replace('.', '/');
        String pomFileName = artifactId + "-" + version + ".pom";
        String localPath = userHome + "/.m2/repository/" + groupPath + "/" + artifactId + "/" + version + "/" + pomFileName;

        File pomFile = new File(localPath);
        if (!pomFile.exists()) {
            logger.trace("[Method 1] Local POM not found: {}", localPath);
            return null;
        }

        logger.debug("[Method 1] Reading local POM: {}", localPath);
        FileInputStream is = null;
        try {
            is = new FileInputStream(pomFile);
            DocumentBuilder builder = createSecureDocumentBuilder();
            Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();
            return doc;
        } catch (Exception e) {
            logger.warn("[Method 1] Failed to parse local POM {}: {}", localPath, e.getMessage());
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignored) {}
            }
        }
    }

    private Document fetchPomFromMavenCentral(String groupId, String artifactId, String version) {
        String groupPath = groupId.replace('.', '/');
        String pomFileName = artifactId + "-" + version + ".pom";
        String urlString = "https://repo1.maven.org/maven2/" + groupPath + "/" + artifactId + "/" + version + "/" + pomFileName;

        logger.debug("[Method 1] Fetching from Maven Central: {}", urlString);
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
                logger.debug("[Method 1] Maven Central returned HTTP {}", responseCode);
                return null;
            }

            is = connection.getInputStream();
            DocumentBuilder builder = createSecureDocumentBuilder();
            Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();
            logger.debug("[Method 1] Successfully fetched and parsed POM from Maven Central.");
            return doc;
        } catch (Exception e) {
            logger.warn("[Method 1] Failed to fetch from Maven Central {}: {}", urlString, e.getMessage());
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignored) {}
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private Map<String, String> extractProperties(Document doc) {
        Map<String, String> properties = new HashMap<String, String>();
        Element root = doc.getDocumentElement();

        String projectVersion = getDirectChildTagValue(root, "version");
        String projectGroupId = getDirectChildTagValue(root, "groupId");
        String projectArtifactId = getDirectChildTagValue(root, "artifactId");

        NodeList parentNodes = root.getElementsByTagName("parent");
        if (parentNodes.getLength() > 0) {
            Element parentElement = (Element) parentNodes.item(0);
            if (projectGroupId == null || projectGroupId.isEmpty() || "inherited".equals(projectGroupId)) {
                projectGroupId = getTagValue(parentElement, "groupId");
            }
            if (projectVersion == null || projectVersion.isEmpty() || "inherited".equals(projectVersion)) {
                projectVersion = getTagValue(parentElement, "version");
            }
        }

        if (projectVersion != null && !"inherited".equals(projectVersion)) {
            properties.put("project.version", projectVersion);
            properties.put("pom.version", projectVersion);
            properties.put("version", projectVersion);
        }
        if (projectGroupId != null && !"inherited".equals(projectGroupId)) {
            properties.put("project.groupId", projectGroupId);
            properties.put("pom.groupId", projectGroupId);
            properties.put("groupId", projectGroupId);
        }
        if (projectArtifactId != null && !"inherited".equals(projectArtifactId)) {
            properties.put("project.artifactId", projectArtifactId);
            properties.put("pom.artifactId", projectArtifactId);
            properties.put("artifactId", projectArtifactId);
        }

        NodeList propertiesNodes = doc.getElementsByTagName("properties");
        if (propertiesNodes.getLength() > 0) {
            Element propertiesElement = (Element) propertiesNodes.item(0);
            NodeList children = propertiesElement.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    properties.put(child.getNodeName(), child.getTextContent().trim());
                }
            }
        }
        return properties;
    }

    private String resolvePropertyPlaceholders(String value, Map<String, String> properties) {
        if (value == null || value.isEmpty() || "inherited".equals(value)) return value;

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
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
                }
            }
            matcher.appendTail(sb);
            resolved = sb.toString();
            if (!foundAny) break;
            iteration++;
        }
        return resolved;
    }

    private String findManagedVersion(Document doc, String groupId, String artifactId, Map<String, String> properties) {
        NodeList depMgmtNodes = doc.getElementsByTagName("dependencyManagement");
        if (depMgmtNodes.getLength() == 0) return null;

        Element depMgmtElement = (Element) depMgmtNodes.item(0);
        NodeList dependencyNodes = depMgmtElement.getElementsByTagName("dependency");

        for (int i = 0; i < dependencyNodes.getLength(); i++) {
            Node node = dependencyNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String managedGroupId = resolvePropertyPlaceholders(getTagValue(element, "groupId"), properties);
                String managedArtifactId = resolvePropertyPlaceholders(getTagValue(element, "artifactId"), properties);

                if (groupId.equals(managedGroupId) && artifactId.equals(managedArtifactId)) {
                    String managedVersion = getTagValue(element, "version");
                    return resolvePropertyPlaceholders(managedVersion, properties);
                }
            }
        }
        return null;
    }

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

    private String getTagValue(Element parentElement, String tagName) {
        NodeList list = parentElement.getElementsByTagName(tagName);
        if (list != null && list.getLength() > 0) {
            return list.item(0).getTextContent().trim();
        }
        return "inherited";
    }
}

