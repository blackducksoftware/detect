package com.blackduck.integration.detect.tool.detector;

import com.blackduck.integration.detect.tool.detector.factory.DetectDetectableFactory;
import com.blackduck.integration.detector.rule.DetectorRule;
import com.blackduck.integration.detector.rule.DetectorRuleSet;
import com.blackduck.integration.detector.rule.DetectableDefinition;
import com.blackduck.integration.detector.rule.EntryPoint;
import com.blackduck.integration.detector.base.DetectorType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class DetectorRuleFactoryTest {

    @Test
    void testCreateRules() {
        // Arrange
        DetectDetectableFactory mockDetectableFactory = Mockito.mock(DetectDetectableFactory.class);
        DetectorRuleFactory detectorRuleFactory = new DetectorRuleFactory();

        // Act
        DetectorRuleSet detectorRuleSet = detectorRuleFactory.createRules(mockDetectableFactory);

        // Assert
        assertNotNull(detectorRuleSet, "DetectorRuleSet should not be null");

        List<DetectorRule> detectorRules = detectorRuleSet.getDetectorRules();
        assertNotNull(detectorRules, "DetectorRules should not be null");

        Set<DetectorType> declaredTypes = new HashSet<>();
        for (DetectorRule rule : detectorRules) {
            DetectorType currentType = rule.getDetectorType();
            assertNotNull(currentType, "DetectorType should not be null");

            rule.getEntryPoints().forEach(entryPoint -> {
                entryPoint.getSearchRule().getYieldsTo().forEach(yieldedType -> {
                    assertTrue(declaredTypes.contains(yieldedType),
                        String.format("DetectorType %s yields to %s, which is not declared earlier.", currentType, yieldedType));
                });
            });
            declaredTypes.add(currentType);
        }
    }

    @Test
    void testDetectorNamesMatchDocumentation() throws Exception {
        /**
         * Ideally the docs table would be generated based on the code and we should explore why it wasn't done so far.
         * In the meantime, we have this test that ensures that there isn't a drift between docs and code.
         * This is especially important now that there is the ability to exclude individual detectables based on their names.
        */

        // Arrange
        DetectDetectableFactory mockDetectableFactory = Mockito.mock(DetectDetectableFactory.class);
        DetectorRuleFactory detectorRuleFactory = new DetectorRuleFactory();

        // Act - Get detector names from code
        DetectorRuleSet detectorRuleSet = detectorRuleFactory.createRules(mockDetectableFactory);
        Set<String> codeDetectorNames = new HashSet<>();

        for (DetectorRule rule : detectorRuleSet.getDetectorRules()) {
            for (EntryPoint entryPoint : rule.getEntryPoints()) {
                for (DetectableDefinition detectable : entryPoint.allDetectables()) {
                    codeDetectorNames.add(detectable.getName());
                }
            }
        }

        // Act - Get detector names from documentation
        Set<String> docDetectorNames = parseDetectorNamesFromDocumentation();

        // Assert with detailed diff on failure
        if (!docDetectorNames.equals(codeDetectorNames)) {
            Set<String> missingInCode = new HashSet<>(docDetectorNames);
            missingInCode.removeAll(codeDetectorNames);

            Set<String> missingInDocs = new HashSet<>(codeDetectorNames);
            missingInDocs.removeAll(docDetectorNames);

            StringBuilder errorMessage = new StringBuilder();
            errorMessage.append("Detector names mismatch between code and documentation:\n");

            if (!missingInCode.isEmpty()) {
                errorMessage.append("Missing in code (found in docs): ").append(missingInCode).append("\n");
            }

            if (!missingInDocs.isEmpty()) {
                errorMessage.append("Missing in docs (found in code): ").append(missingInDocs).append("\n");
            }

            errorMessage.append("Total in code: ").append(codeDetectorNames.size());
            errorMessage.append(", Total in docs: ").append(docDetectorNames.size());

            assertEquals(docDetectorNames, codeDetectorNames, errorMessage.toString());
        }
    }

    private Set<String> parseDetectorNamesFromDocumentation() throws Exception {
        Set<String> detectorNames = new HashSet<>();

        // Parse the DITA XML file
        File docFile = new File("documentation/src/main/markdown/components/detectors.dita");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

        // Disable DTD validation and external entity resolution
        dbFactory.setValidating(false);
        dbFactory.setFeature("http://xml.org/sax/features/validation", false);
        dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(docFile);
        doc.getDocumentElement().normalize();

        // Find the table with id "detector-types-and-detectors"
        NodeList topics = doc.getElementsByTagName("topic");
        Element targetTable = null;

        for (int i = 0; i < topics.getLength(); i++) {
            Element topic = (Element) topics.item(i);
            if ("detector-types-and-detectors".equals(topic.getAttribute("id"))) {
                NodeList tables = topic.getElementsByTagName("table");
                if (tables.getLength() > 0) {
                    targetTable = (Element) tables.item(0);
                    break;
                }
            }
        }

        if (targetTable != null) {
            // Only process rows from tbody, skip thead
            NodeList tbodies = targetTable.getElementsByTagName("tbody");
            if (tbodies.getLength() > 0) {
                Element tbody = (Element) tbodies.item(0);
                NodeList rows = tbody.getElementsByTagName("row");

                for (int i = 0; i < rows.getLength(); i++) {
                    Element row = (Element) rows.item(i);
                    NodeList entries = row.getElementsByTagName("entry");

                    if (entries.getLength() > 1) {
                        Element secondEntry = (Element) entries.item(1);
                        String detectorName = secondEntry.getTextContent().trim();

                        // Only add non-empty detector names (skip type rows and empty entries)
                        if (!detectorName.isEmpty()) {
                            detectorNames.add(detectorName);
                        }
                    }
                }
            }
        }

        return detectorNames;
    }
}
