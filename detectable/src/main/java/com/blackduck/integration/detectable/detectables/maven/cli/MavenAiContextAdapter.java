package com.blackduck.integration.detectable.detectables.maven.cli;

import com.blackduck.integration.detectable.detectable.ai.AiContext;
import com.blackduck.integration.detectable.detectable.ai.AiContextAdapter;
import com.blackduck.integration.detectable.detectable.ai.AiQuestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * AI context adapter for the Maven detector.
 * Co-located with {@link MavenPomDetectable} in the {@code detectables/maven/cli} package.
 *
 * <p>Can be instantiated without any injected dependencies — only requires a source directory.
 * Mirrors the applicable/extractable checks of the real detectable without depending on it.</p>
 *
 * <p>Extracts three signals from {@code pom.xml}:</p>
 * <ul>
 *   <li>Test-scoped dependencies ({@code <scope>test</scope>})</li>
 *   <li>Profile IDs ({@code <profiles><profile><id>})</li>
 *   <li>Sub-module names ({@code <modules><module>})</li>
 * </ul>
 */
public class MavenAiContextAdapter implements AiContextAdapter {

    private static final String POM_XML_SUFFIX = "pom.xml";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Applicable if any file matching {@code *pom.xml} exists in the source directory.
     * Mirrors {@link MavenPomDetectable#POM_FILENAME} ({@code "*pom.xml"}).
     */
    @Override
    public boolean isApplicable(File sourceDirectory) {
        File[] files = sourceDirectory.listFiles();
        if (files == null) return false;
        for (File f : files) {
            if (f.isFile() && f.getName().endsWith(POM_XML_SUFFIX)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extractable if {@code mvnw} wrapper exists in the source directory,
     * or {@code mvn} is found on the system PATH.
     * Mirrors the executable resolution logic used by the Maven detectable.
     */
    @Override
    public boolean isExtractable(File sourceDirectory) {
        // Prefer the Maven wrapper
        if (new File(sourceDirectory, "mvnw").exists()) {
            return true;
        }
        // Fall back to checking the system PATH
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"mvn", "--version"});
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.debug("Interrupted while checking for mvn on PATH");
            return false;
        } catch (Exception e) {
            logger.debug("mvn not found on PATH: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Parses {@code pom.xml} using the standard Java XML parser and extracts
     * the three signals needed for AI-assisted configuration Q&A.
     */
    @Override
    public MavenAiContext extractContext(File sourceDirectory) {
        File pomFile = findPomFile(sourceDirectory);
        if (pomFile == null) {
            logger.warn("Could not find pom.xml in {} for AI context extraction", sourceDirectory);
            return new MavenAiContext(false, new ArrayList<>(), new ArrayList<>());
        }
        try {
            Document doc = parsePomXml(pomFile);
            boolean hasTestDependencies = detectTestDependencies(doc);
            List<String> profiles = extractProfileIds(doc);
            List<String> modules = extractModuleNames(doc);
            return new MavenAiContext(hasTestDependencies, profiles, modules);
        } catch (Exception e) {
            logger.warn("Failed to parse pom.xml for AI context extraction: {}", e.getMessage());
            logger.debug("AI context extraction error details", e);
            return new MavenAiContext(false, new ArrayList<>(), new ArrayList<>());
        }
    }

    private Document parsePomXml(File pomFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(null);
        Document doc = builder.parse(pomFile);
        doc.getDocumentElement().normalize();
        return doc;
    }

    private boolean detectTestDependencies(Document doc) {
        NodeList scopeNodes = doc.getElementsByTagName("scope");
        for (int i = 0; i < scopeNodes.getLength(); i++) {
            if ("test".equalsIgnoreCase(scopeNodes.item(i).getTextContent().trim())) {
                return true;
            }
        }
        return false;
    }

    private List<String> extractProfileIds(Document doc) {
        List<String> profiles = new ArrayList<>();
        NodeList profileNodes = doc.getElementsByTagName("profile");
        for (int i = 0; i < profileNodes.getLength(); i++) {
            NodeList children = profileNodes.item(i).getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                if ("id".equals(children.item(j).getNodeName())) {
                    String profileId = children.item(j).getTextContent().trim();
                    if (!profileId.isEmpty()) {
                        profiles.add(profileId);
                    }
                }
            }
        }
        return profiles;
    }

    private List<String> extractModuleNames(Document doc) {
        List<String> modules = new ArrayList<>();
        NodeList moduleNodes = doc.getElementsByTagName("module");
        for (int i = 0; i < moduleNodes.getLength(); i++) {
            String moduleName = moduleNodes.item(i).getTextContent().trim();
            if (!moduleName.isEmpty()) {
                modules.add(moduleName);
            }
        }
        return modules;
    }

    @Override
    public String getDetectorName() {
        return "MAVEN";
    }

    /**
     * Returns the three Maven-specific questions, with hints populated from the
     * signals found in {@code pom.xml} so the user knows what was auto-detected.
     */
    @Override
    public List<AiQuestion> getQuestions(AiContext context) {
        MavenAiContext ctx = (MavenAiContext) context;
        List<AiQuestion> questions = new ArrayList<>();

        // Q1 — boolean: exclude test dependencies from the BOM?
        String testHint = ctx.hasTestDependencies
            ? "Test-scoped dependencies detected in pom.xml."
            : "No test-scoped dependencies found in pom.xml.";
        questions.add(new AiQuestion(
            "Exclude test dependencies from the scan? (recommended for a production BOM)",
            AiQuestion.Type.YES_NO,
            testHint
        ));

        // Q2 — text: activate a Maven profile?
        String profileHint = ctx.profiles.isEmpty()
            ? "No Maven profiles detected in pom.xml."
            : "Profiles detected in pom.xml: " + String.join(", ", ctx.profiles);
        questions.add(new AiQuestion(
            "Activate a Maven profile? Enter profile name(s) or press Enter to skip:",
            AiQuestion.Type.TEXT,
            profileHint
        ));

        // Q3 — text: exclude sub-modules?
        String moduleHint = ctx.modules.isEmpty()
            ? "No sub-modules detected in pom.xml."
            : "Sub-modules detected in pom.xml: " + String.join(", ", ctx.modules);
        questions.add(new AiQuestion(
            "Exclude any sub-modules from the scan? Enter module name(s) or press Enter to skip:",
            AiQuestion.Type.TEXT,
            moduleHint
        ));

        return questions;
    }

    private File findPomFile(File sourceDirectory) {
        // Prefer exact pom.xml
        File direct = new File(sourceDirectory, POM_XML_SUFFIX);
        if (direct.exists()) return direct;
        // Fall back to any *pom.xml
        File[] files = sourceDirectory.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().endsWith(POM_XML_SUFFIX)) {
                    return f;
                }
            }
        }
        return null;
    }
}











