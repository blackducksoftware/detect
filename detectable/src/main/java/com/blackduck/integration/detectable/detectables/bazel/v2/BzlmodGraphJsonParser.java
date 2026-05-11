package com.blackduck.integration.detectable.detectables.bazel.v2;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Parses the JSON output of `bazel mod graph --output json` (Bazel 7.1+) to discover
 * all BCR module dependencies and their coordinates (name@version).
 *
 * <p>The JSON structure looks like:
 * <pre>
 * {
 *   "key": "&lt;root&gt;",
 *   "dependencies": [
 *     {
 *       "key": "protobuf@29.3",
 *       "dependencies": [...]
 *     }
 *   ]
 * }
 * </pre>
 *
 * <p>Each {@code key} field follows the format {@code name@version} for BCR modules.
 * The root node uses the special key {@code "<root>"} and is excluded.
 */
public class BzlmodGraphJsonParser {
    private static final Logger logger = LoggerFactory.getLogger(BzlmodGraphJsonParser.class);

    private static final String JSON_KEY_KEY = "key";
    private static final String JSON_KEY_DEPENDENCIES = "dependencies";
    private static final String ROOT_KEY = "<root>";
    private static final String MODULE_KEY_SEPARATOR = "@";

    /**
     * Parses the JSON output and returns all unique module keys (name@version) found in the tree.
     * Returns an empty set if the JSON is malformed, null, or empty.
     *
     * @param json Raw JSON string from `bazel mod graph --output json`
     * @return Set of module keys (e.g., "protobuf@29.3", "rules_java@8.6.4")
     */
    public Set<String> parseModuleKeys(String json) {
        if (json == null || json.trim().isEmpty()) {
            logger.debug("mod graph JSON output is null or empty");
            return Collections.emptySet();
        }
        try {
            JsonElement rootElement = JsonParser.parseString(json);
            if (!rootElement.isJsonObject()) {
                logger.warn("mod graph JSON output is not a JSON object");
                return Collections.emptySet();
            }
            Set<String> keys = new LinkedHashSet<>();
            collectKeys(rootElement.getAsJsonObject(), keys);
            logger.debug("Parsed {} module keys from mod graph JSON", keys.size());
            return keys;
        } catch (Exception e) {
            logger.warn("Failed to parse mod graph JSON output: {}", e.getMessage());
            logger.debug("JSON parse exception", e);
            return Collections.emptySet();
        }
    }

    /**
     * Recursively walks the JSON tree and collects all non-root "key" values.
     */
    private void collectKeys(JsonObject node, Set<String> keys) {
        if (node.has(JSON_KEY_KEY)) {
            String key = node.get(JSON_KEY_KEY).getAsString();
            if (!ROOT_KEY.equals(key) && key.contains(MODULE_KEY_SEPARATOR)) {
                keys.add(key);
            }
        }
        if (node.has(JSON_KEY_DEPENDENCIES)) {
            JsonElement depsElement = node.get(JSON_KEY_DEPENDENCIES);
            if (depsElement.isJsonArray()) {
                JsonArray deps = depsElement.getAsJsonArray();
                for (JsonElement dep : deps) {
                    if (dep.isJsonObject()) {
                        collectKeys(dep.getAsJsonObject(), keys);
                    }
                }
            }
        }
    }

    /**
     * Extracts the module name from a module key (e.g., "protobuf" from "protobuf@29.3").
     * Returns the full key if no '@' separator is found.
     *
     * @param moduleKey Module key string (name@version)
     * @return Module name
     */
    public static String extractName(String moduleKey) {
        int atIdx = moduleKey.indexOf(MODULE_KEY_SEPARATOR);
        return atIdx > 0 ? moduleKey.substring(0, atIdx) : moduleKey;
    }

    /**
     * Extracts the version from a module key (e.g., "29.3" from "protobuf@29.3").
     * Returns null if no '@' separator is found.
     *
     * @param moduleKey Module key string (name@version)
     * @return Module version, or null if not present
     */
    public static String extractVersion(String moduleKey) {
        int atIdx = moduleKey.indexOf(MODULE_KEY_SEPARATOR);
        return atIdx > 0 && atIdx < moduleKey.length() - 1 ? moduleKey.substring(atIdx + 1) : null;
    }
}

