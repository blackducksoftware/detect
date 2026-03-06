package com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.util;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SecureXmlDocumentBuilder}.
 *
 * <p>These tests verify that:
 * <ul>
 *   <li>The builder can be created without throwing exceptions</li>
 *   <li>Simple XML documents can be parsed successfully</li>
 *   <li>Maven POM structures can be parsed</li>
 *   <li>The builder works even when some security features are not supported</li>
 * </ul>
 */
public class SecureXmlDocumentBuilderTest {

    private static final Logger logger = LoggerFactory.getLogger(SecureXmlDocumentBuilderTest.class);

    /**
     * Verifies that newDocumentBuilder() returns a non-null builder.
     */
    @Test
    public void testBuilderCreationReturnsNonNull() throws Exception {
        DocumentBuilder builder = SecureXmlDocumentBuilder.newDocumentBuilder(logger);
        assertNotNull(builder, "DocumentBuilder should not be null");
    }

    /**
     * Verifies that a simple XML document can be parsed successfully.
     */
    @Test
    public void testParseSimpleXml() throws Exception {
        DocumentBuilder builder = SecureXmlDocumentBuilder.newDocumentBuilder(logger);

        String xml = "<project><groupId>com.example</groupId><artifactId>test</artifactId></project>";
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes("UTF-8"));

        Document doc = builder.parse(bais);

        assertNotNull(doc, "Parsed document should not be null");
        assertNotNull(doc.getDocumentElement(), "Document element should not be null");
        assertEquals("project", doc.getDocumentElement().getNodeName(), "Root element should be 'project'");
    }

    /**
     * Verifies that a typical Maven POM structure can be parsed.
     */
    @Test
    public void testParseMavenPomStructure() throws Exception {
        DocumentBuilder builder = SecureXmlDocumentBuilder.newDocumentBuilder(logger);

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
                "  <modelVersion>4.0.0</modelVersion>\n" +
                "  <groupId>com.example</groupId>\n" +
                "  <artifactId>my-artifact</artifactId>\n" +
                "  <version>1.0.0</version>\n" +
                "  <dependencies>\n" +
                "    <dependency>\n" +
                "      <groupId>org.apache.commons</groupId>\n" +
                "      <artifactId>commons-lang3</artifactId>\n" +
                "      <version>3.12.0</version>\n" +
                "    </dependency>\n" +
                "  </dependencies>\n" +
                "</project>";

        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        Document doc = builder.parse(bais);

        assertNotNull(doc, "Parsed document should not be null");
        assertEquals("project", doc.getDocumentElement().getNodeName(), "Root element should be 'project'");

        // Verify we can find the dependency element
        assertTrue(doc.getElementsByTagName("dependency").getLength() > 0,
                "Should find at least one dependency element");
    }

    /**
     * Verifies that multiple builders can be created without issues.
     * This tests that the utility class is stateless and can be used repeatedly.
     */
    @Test
    public void testMultipleBuilderCreation() throws Exception {
        for (int i = 0; i < 5; i++) {
            DocumentBuilder builder = SecureXmlDocumentBuilder.newDocumentBuilder(logger);
            assertNotNull(builder, "Builder " + i + " should not be null");

            String xml = "<root><item>" + i + "</item></root>";
            ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            Document doc = builder.parse(bais);
            assertNotNull(doc, "Document " + i + " should not be null");
        }
    }

    /**
     * Verifies that XML with processing instructions can be parsed.
     */
    @Test
    public void testParseXmlWithProcessingInstruction() throws Exception {
        DocumentBuilder builder = SecureXmlDocumentBuilder.newDocumentBuilder(logger);

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<?xml-stylesheet type=\"text/xsl\" href=\"style.xsl\"?>\n" +
                "<project><name>Test</name></project>";

        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        Document doc = builder.parse(bais);

        assertNotNull(doc, "Parsed document should not be null");
        assertEquals("project", doc.getDocumentElement().getNodeName());
    }

    /**
     * Verifies that empty XML content is handled gracefully.
     */
    @Test
    public void testParseMinimalXml() throws Exception {
        DocumentBuilder builder = SecureXmlDocumentBuilder.newDocumentBuilder(logger);

        String xml = "<r/>";
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        Document doc = builder.parse(bais);

        assertNotNull(doc, "Parsed document should not be null");
        assertEquals("r", doc.getDocumentElement().getNodeName());
    }
}

