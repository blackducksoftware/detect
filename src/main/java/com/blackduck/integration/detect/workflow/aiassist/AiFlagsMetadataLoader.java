package com.blackduck.integration.detect.workflow.aiassist;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Loads the pre-generated flags metadata JSON for a given detector from the classpath.
 * Resource files live at {@code src/main/resources/aiassist/<detector-lowercase>-flags.json}.
 */
public class AiFlagsMetadataLoader {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Loads the flags metadata JSON for the given detector name.
     *
     * @param detectorName e.g. {@code "MAVEN"}, {@code "BAZEL"}
     * @return the JSON string, or an empty JSON object string if the resource is not found
     */
    public String loadFlagsJson(String detectorName) {
        String resourcePath = "/aiassist/" + detectorName.toLowerCase() + "-flags.json";
        try (InputStream stream = AiFlagsMetadataLoader.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                logger.warn("No flags metadata resource found at {}", resourcePath);
                return "{}";
            }
            return IOUtils.toString(stream, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.warn("Failed to load flags metadata from {}: {}", resourcePath, e.getMessage());
            return "{}";
        }
    }
}

