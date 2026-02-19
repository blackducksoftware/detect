package com.blackduck.integration.detectable.detectables.ivy.parse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Optional;

import javax.xml.parsers.SAXParser;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parses build.xml to find targets that contain ivy:dependencytree task.
 * Returns the name of the first target found with the task, or empty if none found.
 */
public class IvyDependencyTreeTargetParser {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SAXParser saxParser;

    public IvyDependencyTreeTargetParser(SAXParser saxParser) {
        this.saxParser = saxParser;
    }

    public Optional<String> parseTargetWithDependencyTree(@Nullable File buildXmlFile) throws IOException {
        if (buildXmlFile == null) {
            logger.debug("No build.xml file provided");
            return Optional.empty();
        }

        if (!buildXmlFile.exists()) {
            logger.debug("build.xml file does not exist: {}", buildXmlFile.getAbsolutePath());
            return Optional.empty();
        }

        DependencyTreeTargetHandler handler = new DependencyTreeTargetHandler();
        try (InputStream buildXmlInputStream = Files.newInputStream(buildXmlFile.toPath())) {
            saxParser.parse(buildXmlInputStream, handler);
        } catch (SAXException e) {
            // Check if this was our early exit marker (not an actual error)
            if (!"Found target".equals(e.getMessage())) {
                logger.debug("Failed to parse build.xml for ivy:dependencytree task: {}", e.getMessage());
                return Optional.empty();
            }
            // If it was our marker, continue - handler already has the target name
        }

        // Check if we found a target (either through normal parsing or early exit)
        if (handler.getTargetName() != null) {
            logger.info("Found <ivy:dependencytree> task in target '{}'", handler.getTargetName());
            logger.debug("Will execute: ant {}", handler.getTargetName());
            return Optional.of(handler.getTargetName());
        } else {
            logger.debug("No <ivy:dependencytree> task found in any target in build.xml");
            return Optional.empty();
        }
    }

    private static class DependencyTreeTargetHandler extends DefaultHandler {
        private String currentTargetName = null;
        private String foundTargetName = null;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            // When we enter a <target> element, store its name
            // Example: <target name="tree">
            if ("target".equalsIgnoreCase(qName)) {
                currentTargetName = attributes.getValue("name");
            }

            // When we find <ivy:dependencytree> or <dependencytree> element,
            // capture the name of the parent <target> that contains it
            // The parent target name is stored in currentTargetName
            if (foundTargetName == null && currentTargetName != null) {
                if ("ivy:dependencytree".equalsIgnoreCase(qName) ||
                    "dependencytree".equalsIgnoreCase(qName) ||
                    qName.endsWith(":dependencytree")) {
                    // Found ivy:dependencytree inside a target - save the target's name
                    foundTargetName = currentTargetName;
                    throw new SAXException("Found target"); // Early exit optimization
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            // When we exit a <target> element, clear the current target name
            // This ensures we only capture tasks within the current target scope
            if ("target".equalsIgnoreCase(qName)) {
                currentTargetName = null;
            }
        }

        public String getTargetName() {
            return foundTargetName;
        }
    }
}