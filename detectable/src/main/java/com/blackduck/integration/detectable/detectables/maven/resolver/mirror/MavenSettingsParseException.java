package com.blackduck.integration.detectable.detectables.maven.resolver.mirror;

/**
 * Exception thrown when parsing Maven settings.xml fails.
 *
 * <p>This exception is thrown in the following scenarios:
 * <ul>
 *   <li>The specified settings.xml file does not exist (when explicitly provided)</li>
 *   <li>The settings.xml file cannot be read (permission denied)</li>
 *   <li>The settings.xml file contains malformed XML</li>
 * </ul>
 *
 * <p>This exception should be caught by the extractor and converted to an
 * extraction failure to properly surface the error to the Detect framework.
 */
public class MavenSettingsParseException extends Exception {

    /**
     * Constructs a new exception with the specified message.
     *
     * @param message the detail message
     */
    public MavenSettingsParseException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause
     */
    public MavenSettingsParseException(String message, Throwable cause) {
        super(message, cause);
    }
}

