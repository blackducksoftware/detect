package com.blackduck.integration.detect.lifecycle.boot.product.version;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.blackduck.integration.util.ResourceUtil;

public class CompatibilityMatrixLoader {
    private static final String COMPATIBILITY_RESOURCE_PATH = "/detect-compatibility.json";
    private static final Gson GSON = new Gson();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public Optional<CompatibilityMatrix> load() {
        try {
            String contents = ResourceUtil.getResourceAsString(this.getClass(), COMPATIBILITY_RESOURCE_PATH, StandardCharsets.UTF_8.toString());
            CompatibilityMatrix matrix = GSON.fromJson(contents, CompatibilityMatrix.class);
            if (matrix == null || matrix.getRows().isEmpty()) {
                logger.debug("Detect compatibility matrix resource {} was empty; compatibility check will be skipped.", COMPATIBILITY_RESOURCE_PATH);
                return Optional.empty();
            }
            return Optional.of(matrix);
        } catch (IOException | JsonSyntaxException e) {
            logger.debug("Unable to load Detect compatibility matrix resource {}: {}", COMPATIBILITY_RESOURCE_PATH, e.getMessage());
            return Optional.empty();
        }
    }
}
