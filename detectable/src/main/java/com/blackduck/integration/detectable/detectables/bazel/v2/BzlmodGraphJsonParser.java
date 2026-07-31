package com.blackduck.integration.detectable.detectables.bazel.v2;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

    // -------------------------------------------------------------------------
    // Tree-structure parsing (used by BzlmodBcrExtractor for direct/transitive classification)
    // -------------------------------------------------------------------------

    /**
     * Parses the JSON output of {@code bazel mod graph --output json} and returns a
     * {@link ModuleGraph} that preserves the parent-child edges needed for direct/transitive
     * classification. Returns an empty {@code ModuleGraph} on null, empty, or malformed input.
     *
     * <p>The existing {@link #parseModuleKeys(String)} (used by the HTTP-archive probe path)
     * is left completely untouched.
     *
     * @param json Raw JSON string from {@code bazel mod graph --output json}
     * @return ModuleGraph with direct keys and full edge map
     */
    public ModuleGraph parseModuleGraph(String json) {
        if (json == null || json.trim().isEmpty()) {
            logger.debug("mod graph JSON is null/empty; returning empty ModuleGraph");
            return new ModuleGraph(Collections.<String>emptySet(), Collections.<String, List<String>>emptyMap());
        }
        try {
            JsonElement rootElement = JsonParser.parseString(json);
            if (!rootElement.isJsonObject()) {
                logger.warn("mod graph JSON is not a JSON object; returning empty ModuleGraph");
                return new ModuleGraph(Collections.<String>emptySet(), Collections.<String, List<String>>emptyMap());
            }
            JsonObject rootObj = rootElement.getAsJsonObject();

            // Root's immediate children → direct module keys
            Set<String> directKeys = new LinkedHashSet<>();
            if (rootObj.has(JSON_KEY_DEPENDENCIES)) {
                JsonArray deps = rootObj.get(JSON_KEY_DEPENDENCIES).getAsJsonArray();
                for (JsonElement dep : deps) {
                    if (dep.isJsonObject()) {
                        JsonObject depObj = dep.getAsJsonObject();
                        if (depObj.has(JSON_KEY_KEY)) {
                            String key = depObj.get(JSON_KEY_KEY).getAsString();
                            if (key.contains(MODULE_KEY_SEPARATOR)) {
                                directKeys.add(key);
                            }
                        }
                    }
                }
            }

            // Walk the full tree to collect all non-root parent→children edges.
            // Using Set<String> for children values to deduplicate in case the same module
            // appears under the same parent multiple times in the JSON tree (diamond deps).
            Map<String, Set<String>> edgeMap = new LinkedHashMap<>();
            collectEdges(rootObj, edgeMap);

            // Convert Set children → List (insertion-ordered) for the final ModuleGraph
            Map<String, List<String>> childrenByModuleKey = new LinkedHashMap<>();
            for (Map.Entry<String, Set<String>> entry : edgeMap.entrySet()) {
                childrenByModuleKey.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }

            logger.debug("Parsed module graph: {} direct deps, {} modules with children", directKeys.size(), childrenByModuleKey.size());
            return new ModuleGraph(directKeys, childrenByModuleKey);
        } catch (Exception e) {
            logger.warn("Failed to parse mod graph for tree structure: {}; returning empty ModuleGraph", e.getMessage());
            logger.debug("mod graph parse exception", e);
            return new ModuleGraph(Collections.<String>emptySet(), Collections.<String, List<String>>emptyMap());
        }
    }

    /**
     * Recursively walks the JSON tree and populates {@code edgeMap} with non-root
     * parent→children edges. The root node is skipped as a parent (it has no {@code @} in its key).
     * Diamond dependencies appear as repeated subtrees in the JSON; the {@code Set<String>}
     * value type deduplicates children so each edge is recorded only once regardless of
     * how many times a module appears in the tree.
     */
    private void collectEdges(JsonObject node, Map<String, Set<String>> edgeMap) {
        if (!node.has(JSON_KEY_DEPENDENCIES)) {
            return;
        }
        JsonElement depsElement = node.get(JSON_KEY_DEPENDENCIES);
        if (!depsElement.isJsonArray()) {
            return;
        }
        JsonArray deps = depsElement.getAsJsonArray();

        // Determine the parent key; skip root and non-module entries
        String parentKey = node.has(JSON_KEY_KEY) ? node.get(JSON_KEY_KEY).getAsString() : null;
        boolean parentIsModule = parentKey != null
            && !ROOT_KEY.equals(parentKey)
            && parentKey.contains(MODULE_KEY_SEPARATOR);

        for (JsonElement dep : deps) {
            if (!dep.isJsonObject()) {
                continue;
            }
            JsonObject depObj = dep.getAsJsonObject();
            String childKey = depObj.has(JSON_KEY_KEY) ? depObj.get(JSON_KEY_KEY).getAsString() : null;

            // Register the parent→child edge only for real module-to-module relationships
            if (parentIsModule && childKey != null && childKey.contains(MODULE_KEY_SEPARATOR)) {
                edgeMap.computeIfAbsent(parentKey, k -> new LinkedHashSet<String>()).add(childKey);
            }

            // Always recurse to discover deeper edges regardless of whether the current edge was recorded
            collectEdges(depObj, edgeMap);
        }
    }
}

