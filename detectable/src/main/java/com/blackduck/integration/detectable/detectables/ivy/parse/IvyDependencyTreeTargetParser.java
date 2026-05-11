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
            logger.debug("No build.xml file provided.");
            return Optional.empty();
        } else if (!buildXmlFile.exists()) {
            logger.debug("build.xml file does not exist: {}", buildXmlFile.getAbsolutePath());
            return Optional.empty();
        }

        DependencyTreeTargetHandler handler = new DependencyTreeTargetHandler();
        try (InputStream buildXmlInputStream = Files.newInputStream(buildXmlFile.toPath())) {
            saxParser.parse(buildXmlInputStream, handler);
        } catch (SAXException e) {
            logger.debug("Failed to parse build.xml for ivy:dependencytree task: {}", e.getMessage());
            return Optional.empty();
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
        private static final String ELEMENT_TARGET = "target";
        private static final String ATTRIBUTE_NAME = "name";
        private static final String ELEMENT_IVY_DEPENDENCYTREE = "ivy:dependencytree";
        private static final String ELEMENT_DEPENDENCYTREE = "dependencytree";
        private static final String ELEMENT_DEPENDENCYTREE_SUFFIX = ":dependencytree";

        private String currentTargetName = null;
        private String foundTargetName = null;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (ELEMENT_TARGET.equalsIgnoreCase(qName)) {
                currentTargetName = attributes.getValue(ATTRIBUTE_NAME);
            }

            if (foundTargetName == null && currentTargetName != null) {
                if (ELEMENT_IVY_DEPENDENCYTREE.equalsIgnoreCase(qName) ||
                    ELEMENT_DEPENDENCYTREE.equalsIgnoreCase(qName) ||
                    qName.endsWith(ELEMENT_DEPENDENCYTREE_SUFFIX)) {
                    foundTargetName = currentTargetName;
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (ELEMENT_TARGET.equalsIgnoreCase(qName)) {
                currentTargetName = null;
            }
        }

        public String getTargetName() {
            return foundTargetName;
        }
    }
}