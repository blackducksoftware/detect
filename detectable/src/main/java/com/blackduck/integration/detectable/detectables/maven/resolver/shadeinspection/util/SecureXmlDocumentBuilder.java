package com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.util;

import org.slf4j.Logger;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.StringReader;

/**
 * Factory for creating secure DocumentBuilder instances with fail-safe XXE protection.
 *
 * <p>This utility centralizes XML parser configuration to ensure consistent security
 * settings across all parsing operations. It applies security hardening on a best-effort
 * basis - if a particular security feature or attribute is not supported by the runtime's
 * XML parser implementation, it logs a debug message and continues without failing.
 *
 * <p><strong>Security hardening applied (in order):</strong>
 * <ol>
 *   <li>XMLConstants.FEATURE_SECURE_PROCESSING - General security hardening</li>
 *   <li>Disable DOCTYPE declarations to prevent XXE attacks</li>
 *   <li>Disable external general entities</li>
 *   <li>Disable external parameter entities</li>
 *   <li>Disable loading external DTDs</li>
 *   <li>Block external DTD access via attribute</li>
 *   <li>Block external schema access via attribute</li>
 *   <li>Install EntityResolver that returns empty content for any external entity</li>
 * </ol>
 *
 * <p><strong>Compatibility:</strong> This class is designed to work across different
 * XML parser implementations (Xerces, JDK built-in, etc.) without failing. The
 * error "Property 'http://javax.xml.XMLConstants/property/accessExternalDTD' is not recognized"
 * will no longer cause failures - it will simply be logged at debug level and skipped.
 *
 * <p><strong>Thread Safety:</strong> Each call to {@link #newDocumentBuilder(Logger)}
 * creates a new DocumentBuilder instance. DocumentBuilder itself is not thread-safe,
 * so callers should not share instances across threads.
 *
 * <p><strong>Java 8 Compatibility:</strong> This class uses only Java 8 compatible APIs.
 */
public final class SecureXmlDocumentBuilder {

    // Feature constants for XXE protection
    private static final String FEATURE_DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String FEATURE_EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
    private static final String FEATURE_EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";
    private static final String FEATURE_LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    /**
     * Private constructor to prevent instantiation.
     */
    private SecureXmlDocumentBuilder() {
        // Utility class - no instantiation
    }

    /**
     * Creates a new DocumentBuilder with best-effort XXE protection.
     *
     * <p>This method applies multiple layers of security hardening to prevent
     * XML External Entity (XXE) attacks. Each security feature is applied
     * independently - if one fails (e.g., not supported by the parser), the
     * method continues applying other features and does not throw an exception.
     *
     * <p>As a final safety net, an EntityResolver is installed that returns
     * empty content for any external entity reference, ensuring that even if
     * some security features are not supported, external entities cannot be
     * loaded.
     *
     * @param logger The logger to use for debug messages about unsupported features.
     *               Must not be null.
     * @return A configured DocumentBuilder instance, never null.
     * @throws ParserConfigurationException If the DocumentBuilder cannot be created
     *                                       at all (extremely rare - indicates JVM issue).
     */
    public static DocumentBuilder newDocumentBuilder(Logger logger) throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // Apply security features - each wrapped to handle unsupported features gracefully
        applyFeatureSafely(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true, logger,
                "XMLConstants.FEATURE_SECURE_PROCESSING");

        applyFeatureSafely(factory, FEATURE_DISALLOW_DOCTYPE, true, logger,
                "disallow-doctype-decl");

        applyFeatureSafely(factory, FEATURE_EXTERNAL_GENERAL_ENTITIES, false, logger,
                "external-general-entities");

        applyFeatureSafely(factory, FEATURE_EXTERNAL_PARAMETER_ENTITIES, false, logger,
                "external-parameter-entities");

        applyFeatureSafely(factory, FEATURE_LOAD_EXTERNAL_DTD, false, logger,
                "load-external-dtd");

        // Apply attributes - these are the ones that commonly fail on some parsers
        applyAttributeSafely(factory, XMLConstants.ACCESS_EXTERNAL_DTD, "", logger,
                "ACCESS_EXTERNAL_DTD");

        applyAttributeSafely(factory, XMLConstants.ACCESS_EXTERNAL_SCHEMA, "", logger,
                "ACCESS_EXTERNAL_SCHEMA");

        // Create the builder
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Install EntityResolver as final safety net
        // This ensures external entities return empty content even if parser ignores features
        builder.setEntityResolver(new SafeEntityResolver(logger));

        return builder;
    }

    /**
     * Safely applies a feature to the DocumentBuilderFactory.
     * If the feature is not supported, logs a debug message and continues.
     *
     * @param factory     The factory to configure.
     * @param feature     The feature URI.
     * @param value       The feature value (true/false).
     * @param logger      The logger for debug messages.
     * @param featureName Human-readable name for logging.
     */
    private static void applyFeatureSafely(DocumentBuilderFactory factory, String feature,
                                           boolean value, Logger logger, String featureName) {
        try {
            factory.setFeature(feature, value);
            logger.debug("XML security feature '{}' set to {}", featureName, value);
        } catch (Exception e) {
            logger.debug("XML security feature '{}' not supported by this parser: {}",
                    featureName, e.getMessage());
        }
    }

    /**
     * Safely applies an attribute to the DocumentBuilderFactory.
     * If the attribute is not supported, logs a debug message and continues.
     *
     * @param factory       The factory to configure.
     * @param attribute     The attribute name.
     * @param value         The attribute value.
     * @param logger        The logger for debug messages.
     * @param attributeName Human-readable name for logging.
     */
    private static void applyAttributeSafely(DocumentBuilderFactory factory, String attribute,
                                             String value, Logger logger, String attributeName) {
        try {
            factory.setAttribute(attribute, value);
            logger.debug("XML security attribute '{}' set to '{}'", attributeName, value);
        } catch (Exception e) {
            logger.debug("XML security attribute '{}' not supported by this parser: {}",
                    attributeName, e.getMessage());
        }
    }

    /**
     * EntityResolver that returns empty content for any external entity.
     * This is the final safety net against XXE attacks - even if the parser
     * ignores security features, this resolver ensures no external content
     * is loaded.
     */
    private static class SafeEntityResolver implements EntityResolver {

        private final Logger logger;

        SafeEntityResolver(Logger logger) {
            this.logger = logger;
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId) {
            // Log the blocked entity resolution attempt
            if (logger.isDebugEnabled()) {
                logger.debug("Blocked external entity resolution - publicId: '{}', systemId: '{}'",
                        publicId != null ? publicId : "null",
                        systemId != null ? systemId : "null");
            }

            // Return empty InputSource to prevent any external content loading
            return new InputSource(new StringReader(""));
        }
    }
}

