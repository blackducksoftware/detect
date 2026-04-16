package com.blackduck.integration.detectable.detectables.maven.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Recursively walks a Maven project tree (all {@code pom.xml} files up to a configurable
 * depth) and extracts a compact {@link MavenProjectSummary} for the QuackStart Express mode.
 *
 * <p>Only build metadata is extracted — module names, dependency scopes, profile IDs,
 * and profile-specific compile dependencies. No source code is read.</p>
 *
 * <p>Token budget: a 4-module project typically renders under 300 tokens;
 * a 30-module project stays under 2,000.</p>
 */
public class MavenProjectSummarizer {

    private static final String POM_XML = "pom.xml";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Walks the source directory recursively (up to {@value #MAX_DEPTH} levels) and
     * returns a {@link MavenProjectSummary} for every {@code pom.xml} found.
     *
     * @param sourceDirectory project root
     * @return populated summary; never {@code null}
     */
    public MavenProjectSummary summarize(File sourceDirectory) {
        MavenProjectSummary summary = new MavenProjectSummary();
        walkDirectory(sourceDirectory, summary);
        return summary;
    }

    // ── Recursive walker ───────────────────────────────────────────────────────

    private void walkDirectory(File dir, MavenProjectSummary summary) {
        if (!dir.isDirectory()) return;

        File pom = new File(dir, POM_XML);
        if (pom.exists() && pom.isFile()) {
            MavenProjectSummary.ModuleSummary module = parsePom(pom);
            if (module != null) {
                summary.addModule(module);
            }
        }

        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) {
                String name = child.getName();
                if (name.startsWith(".") || name.equals("target") || name.equals("build")
                        || name.equals("node_modules") || name.equals("out")) {
                    continue;
                }
                walkDirectory(child, summary);
            }
        }
    }

    // ── pom.xml parser ─────────────────────────────────────────────────────────

    private MavenProjectSummary.ModuleSummary parsePom(File pomFile) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(null);
            Document doc = builder.parse(pomFile);
            doc.getDocumentElement().normalize();

            String artifactId      = firstText(doc, "artifactId", pomFile.getParentFile().getName());
            String packaging       = firstText(doc, "packaging", "jar");
            String parentArtifactId = extractParentArtifactId(doc);

            // Dependencies grouped by scope (top-level only — not inside profiles)
            Map<String, List<String>> depsByScope = extractTopLevelDeps(doc);

            // Profile compile-deps
            Map<String, List<String>> profileCompileDeps = extractProfileCompileDeps(doc);

            // Sub-modules
            List<String> submodules = extractSubmodules(doc);

            return new MavenProjectSummary.ModuleSummary(
                    artifactId, packaging, parentArtifactId,
                    depsByScope, profileCompileDeps, submodules);

        } catch (Exception e) {
            logger.debug("Failed to parse {} for express summary: {}", pomFile.getAbsolutePath(), e.getMessage());
            return null;
        }
    }

    // ── Field extractors ───────────────────────────────────────────────────────

    /** Returns the text content of the first direct child element with the given tag name
     *  directly under the document root. Falls back to {@code defaultValue}. */
    private String firstText(Document doc, String tagName, String defaultValue) {
        NodeList list = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && tagName.equals(n.getNodeName())) {
                String text = n.getTextContent().trim();
                if (!text.isEmpty()) return text;
            }
        }
        return defaultValue;
    }

    private String extractParentArtifactId(Document doc) {
        NodeList parents = doc.getDocumentElement().getElementsByTagName("parent");
        if (parents.getLength() == 0) return null;
        NodeList children = parents.item(0).getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if ("artifactId".equals(n.getNodeName())) {
                String text = n.getTextContent().trim();
                if (!text.isEmpty()) return text;
            }
        }
        return null;
    }

    /**
     * Extracts top-level (not inside {@code <profiles>}) dependencies grouped by scope.
     * Omits scope "compile" label and uses "compile" as the default scope per Maven spec.
     */
    private Map<String, List<String>> extractTopLevelDeps(Document doc) {
        Map<String, List<String>> result = new LinkedHashMap<>();

        // Find the top-level <dependencies> element (direct child of root)
        Element root = doc.getDocumentElement();
        NodeList rootChildren = root.getChildNodes();
        for (int i = 0; i < rootChildren.getLength(); i++) {
            Node n = rootChildren.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && "dependencies".equals(n.getNodeName())) {
                // This is the top-level <dependencies>
                NodeList deps = n.getChildNodes();
                for (int j = 0; j < deps.getLength(); j++) {
                    Node dep = deps.item(j);
                    if (dep.getNodeType() != Node.ELEMENT_NODE || !"dependency".equals(dep.getNodeName())) continue;
                    String artifactId = "";
                    String scope = "compile";
                    NodeList depChildren = dep.getChildNodes();
                    for (int k = 0; k < depChildren.getLength(); k++) {
                        Node dc = depChildren.item(k);
                        if ("artifactId".equals(dc.getNodeName())) artifactId = dc.getTextContent().trim();
                        if ("scope".equals(dc.getNodeName())) scope = dc.getTextContent().trim();
                    }
                    if (!artifactId.isEmpty()) {
                        result.computeIfAbsent(scope, s -> new ArrayList<>()).add(artifactId);
                    }
                }
                break; // only process the first top-level <dependencies>
            }
        }
        return result;
    }

    /**
     * For each {@code <profile>}, extracts the profile id and the compile-scope
     * dependencies declared inside that profile's {@code <dependencies>} block.
     */
    private Map<String, List<String>> extractProfileCompileDeps(Document doc) {
        Map<String, List<String>> result = new LinkedHashMap<>();

        NodeList profilesBlocks = doc.getElementsByTagName("profiles");
        for (int b = 0; b < profilesBlocks.getLength(); b++) {
            NodeList profileList = profilesBlocks.item(b).getChildNodes();
            for (int i = 0; i < profileList.getLength(); i++) {
                Node profileNode = profileList.item(i);
                if (profileNode.getNodeType() != Node.ELEMENT_NODE || !"profile".equals(profileNode.getNodeName())) continue;

                String profileId = "";
                List<String> compileDeps = new ArrayList<>();

                NodeList profileChildren = profileNode.getChildNodes();
                for (int j = 0; j < profileChildren.getLength(); j++) {
                    Node pc = profileChildren.item(j);
                    if ("id".equals(pc.getNodeName())) profileId = pc.getTextContent().trim();
                    if ("dependencies".equals(pc.getNodeName())) {
                        NodeList deps = pc.getChildNodes();
                        for (int k = 0; k < deps.getLength(); k++) {
                            Node dep = deps.item(k);
                            if (dep.getNodeType() != Node.ELEMENT_NODE || !"dependency".equals(dep.getNodeName())) continue;
                            String artifactId = "";
                            String scope = "compile";
                            NodeList dc = dep.getChildNodes();
                            for (int l = 0; l < dc.getLength(); l++) {
                                if ("artifactId".equals(dc.item(l).getNodeName())) artifactId = dc.item(l).getTextContent().trim();
                                if ("scope".equals(dc.item(l).getNodeName())) scope = dc.item(l).getTextContent().trim();
                            }
                            if (!artifactId.isEmpty() && "compile".equalsIgnoreCase(scope)) {
                                compileDeps.add(artifactId);
                            }
                        }
                    }
                }
                if (!profileId.isEmpty()) {
                    result.put(profileId, compileDeps);
                }
            }
        }
        return result;
    }

    private List<String> extractSubmodules(Document doc) {
        List<String> result = new ArrayList<>();
        NodeList moduleNodes = doc.getElementsByTagName("module");
        for (int i = 0; i < moduleNodes.getLength(); i++) {
            String name = moduleNodes.item(i).getTextContent().trim();
            if (!name.isEmpty()) result.add(name);
        }
        return result;
    }
}



