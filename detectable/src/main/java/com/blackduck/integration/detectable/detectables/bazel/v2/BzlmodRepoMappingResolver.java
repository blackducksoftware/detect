package com.blackduck.integration.detectable.detectables.bazel.v2;

import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves Bazel external repository labels (as returned by {@code bazel query})
 * to plain BCR module names (as returned by {@code bazel mod graph}).
 *
 * <h3>The problem</h3>
 * {@code bazel query} returns labels in two different formats:
 * <ul>
 *   <li><b>Canonical form</b> ({@code @@abseil-cpp~//...}, double {@code @}): the globally unique
 *       internal name Bazel assigns to a repo. It carries a version-specific suffix —
 *       {@code +} on Bazel &lt; 7.5, {@code ~} on Bazel 7.5+. The suffix must be stripped
 *       to recover the plain module name ({@code abseil-cpp}).</li>
 *   <li><b>Apparent form</b> ({@code @com_google_protobuf//...}, single {@code @}): the nickname
 *       the root module gave the dep. May differ from the module name when the project uses
 *       {@code repo_name = "com_google_protobuf"} in its {@code bazel_dep} declaration.
 *       Must be resolved through Bazel's repo mapping to get the real module name ({@code protobuf}).</li>
 * </ul>
 *
 * <h3>The solution</h3>
 * This class loads {@code bazel mod dump_repo_mapping ""} once per scan. The output is a flat JSON
 * map of {@code apparent_name → canonical_name_with_suffix} for the root module's entire namespace.
 * From that map we:
 * <ol>
 *   <li>Detect the suffix character being used by this Bazel version (no hardcoding).</li>
 *   <li>Build a reverse map ({@code module_name → canonical_with_suffix}) for constructing
 *       {@code show_repo} arguments.</li>
 * </ol>
 *
 * <p>If {@code dump_repo_mapping} fails or produces no output, the resolver degrades gracefully:
 * canonical labels ({@code @@name~}) are handled by stripping any known suffix ({@code +} or {@code ~}),
 * and apparent-name aliases are passed through unchanged (same behaviour as before this fix).
 */
public class BzlmodRepoMappingResolver {
    private static final Logger logger = LoggerFactory.getLogger(BzlmodRepoMappingResolver.class);

    // Label prefix constants
    private static final String CANONICAL_PREFIX = "@@";
    private static final String APPARENT_PREFIX  = "@";
    // Canonical names for module extension sub-repos contain "++" (e.g., rules_jvm_external++maven+guava).
    // These never appear as module keys in bazel mod graph and must be excluded.
    private static final String MODULE_EXTENSION_MARKER = "++";
    // Separator between repo name and path in a fully-qualified label
    private static final String LABEL_PATH_SEPARATOR = "//";
    // Regex that strips any known canonical suffix when the mapping is unavailable
    private static final String KNOWN_SUFFIXES_REGEX = "[+~]$";

    // apparent_name → canonical_with_suffix  (e.g. "com_google_protobuf" → "protobuf~")
    private final Map<String, String> apparentToCanonical;
    // module_name → canonical_with_suffix  (e.g. "protobuf" → "protobuf~")
    // derived by stripping the detected suffix from every value in apparentToCanonical
    private final Map<String, String> moduleNameToCanonical;
    // the suffix character detected from mapping values (e.g. "~" for Bazel 7.5+, "+" for older)
    private final String detectedSuffix;
    // false if dump_repo_mapping failed; resolver degrades gracefully
    private final boolean available;

    private BzlmodRepoMappingResolver(Map<String, String> apparentToCanonical,
                                       Map<String, String> moduleNameToCanonical,
                                       String detectedSuffix,
                                       boolean available) {
        this.apparentToCanonical  = apparentToCanonical;
        this.moduleNameToCanonical = moduleNameToCanonical;
        this.detectedSuffix        = detectedSuffix;
        this.available             = available;
    }

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /**
     * Runs {@code bazel mod dump_repo_mapping ""} (the root module's mapping), parses the
     * JSON output, and returns a fully-initialised resolver.
     *
     * <p>Returns an unavailable resolver (with graceful fallback behaviour) if the command
     * fails or produces no output.
     */
    public static BzlmodRepoMappingResolver load(BazelCommandExecutor bazelCmd) {
        // "mod dump_repo_mapping <repo>" — passing "" selects the root module.
        // This is a metadata-only command; Bazel reads from its resolved graph cache instantly.
        List<String> cmd = Arrays.asList("mod", "dump_repo_mapping", "");
        logger.info("BZLMOD BCR: loading repo mapping via 'bazel mod dump_repo_mapping \"\"'");
        Optional<String> output;
        try {
            output = bazelCmd.executeModCommandToString(cmd);
        } catch (Exception e) {
            logger.warn("BZLMOD BCR: dump_repo_mapping command failed ({}); " +
                "apparent-name aliases (repo_name overrides) will not be resolved", e.getMessage());
            return unavailable();
        }
        if (!output.isPresent() || output.get().trim().isEmpty()) {
            logger.warn("BZLMOD BCR: dump_repo_mapping produced no output; " +
                "apparent-name aliases will not be resolved");
            return unavailable();
        }
        return parse(output.get());
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Given a raw label line from {@code bazel query} output (e.g. {@code @@abseil-cpp~//absl:lib}
     * or {@code @com_google_protobuf//google/protobuf:lib}), returns the plain BCR module name
     * that matches the key format in {@code bazel mod graph} (e.g. {@code abseil-cpp},
     * {@code protobuf}).
     *
     * <p>Returns {@link Optional#empty()} for:
     * <ul>
     *   <li>Local build targets (no leading {@code @})</li>
     *   <li>Module extension sub-repos (contain {@code ++})</li>
     *   <li>Empty or null input</li>
     * </ul>
     */
    public Optional<String> resolveLabel(String label) {
        if (label == null || label.isEmpty() || !label.startsWith(APPARENT_PREFIX)) {
            return Optional.empty();
        }

        // Strip the //path:target suffix — we only care about the repo name part
        String repoName;
        int pathIdx = label.indexOf(LABEL_PATH_SEPARATOR);
        repoName = pathIdx >= 0 ? label.substring(0, pathIdx) : label;

        String moduleName;
        if (repoName.startsWith(CANONICAL_PREFIX)) {
            // ──────────────────────────────────────────────────────────────────
            // Canonical form: @@abseil-cpp~  or  @@abseil-cpp+
            // The suffix carries Bazel's version-specific mangling and must be stripped.
            // No mapping lookup needed — the name is already unambiguous.
            // ──────────────────────────────────────────────────────────────────
            String raw = repoName.substring(CANONICAL_PREFIX.length()); // "abseil-cpp~"
            if (raw.contains(MODULE_EXTENSION_MARKER)) {
                return Optional.empty(); // e.g. rules_jvm_external++maven+guava
            }
            moduleName = stripKnownSuffix(raw);
        } else {
            // ──────────────────────────────────────────────────────────────────
            // Apparent form: @com_google_protobuf
            // Look up in the forward map to resolve any repo_name alias.
            // If not found, the apparent name equals the module name (no override).
            // ──────────────────────────────────────────────────────────────────
            String apparent = repoName.substring(APPARENT_PREFIX.length()); // "com_google_protobuf"
            if (apparent.contains(MODULE_EXTENSION_MARKER)) {
                return Optional.empty();
            }
            if (available && apparentToCanonical.containsKey(apparent)) {
                String canonical = apparentToCanonical.get(apparent); // "protobuf~"
                moduleName = stripKnownSuffix(canonical);             // "protobuf"
            } else {
                // No alias in the mapping — apparent name IS the module name
                moduleName = apparent;
            }
        }

        return moduleName.isEmpty() ? Optional.empty() : Optional.of(moduleName);
    }

    /**
     * Given a plain module name extracted from a {@code bazel mod graph} key
     * (e.g. {@code protobuf} from {@code protobuf@31.0}), returns the canonical repo argument
     * to pass to {@code bazel mod show_repo} (e.g. {@code @@protobuf~}).
     *
     * <p>Uses the reverse map for modules that appeared in the mapping (i.e., direct deps of the
     * root module). Falls back to appending the detected suffix for pure transitive deps that
     * the root module never explicitly declared and are therefore absent from the mapping.
     */
    public String canonicalRepoArg(String moduleName) {
        if (available && moduleNameToCanonical.containsKey(moduleName)) {
            // Exact canonical form from the mapping — e.g. "protobuf~" → "@@protobuf~"
            return CANONICAL_PREFIX + moduleNameToCanonical.get(moduleName);
        }
        // Transitive dep not in root mapping — construct canonical form using the detected suffix
        return CANONICAL_PREFIX + moduleName + detectedSuffix;
    }

    /**
     * Strips the detected canonical suffix from {@code rawName}.
     * Used when parsing {@code ## @@name~:} block headers in batched show_repo output.
     *
     * <p>Example: {@code "protobuf~"} → {@code "protobuf"}
     */
    public String stripCanonicalSuffix(String rawName) {
        return stripSuffix(rawName, detectedSuffix);
    }

    /** Returns whether the mapping was successfully loaded (for diagnostic logging). */
    public boolean isAvailable() {
        return available;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static BzlmodRepoMappingResolver unavailable() {
        return new BzlmodRepoMappingResolver(
            Collections.<String, String>emptyMap(),
            Collections.<String, String>emptyMap(),
            "+",   // default to "+" as a safe fallback
            false
        );
    }

    private static BzlmodRepoMappingResolver parse(String json) {
        try {
            JsonElement element = JsonParser.parseString(json);
            if (!element.isJsonObject()) {
                logger.warn("BZLMOD BCR: dump_repo_mapping output is not a JSON object; degrading gracefully");
                return unavailable();
            }
            JsonObject obj = element.getAsJsonObject();

            // Build the forward map (apparent → canonical)
            Map<String, String> apparentToCanonical = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String apparent  = entry.getKey();
                String canonical = entry.getValue().getAsString();
                if (apparent != null && !apparent.isEmpty() && canonical != null && !canonical.isEmpty()) {
                    apparentToCanonical.put(apparent, canonical);
                }
            }
            if (apparentToCanonical.isEmpty()) {
                logger.warn("BZLMOD BCR: dump_repo_mapping JSON had no usable entries; degrading gracefully");
                return unavailable();
            }

            // Detect the canonical suffix from the first mapping value that ends with a known character
            String detectedSuffix = "+"; // safe default
            for (String canonical : apparentToCanonical.values()) {
                if (canonical.endsWith("~")) { detectedSuffix = "~"; break; }
                if (canonical.endsWith("+")) { detectedSuffix = "+"; break; }
            }
            logger.info("BZLMOD BCR: repo mapping loaded ({} entries), detected canonical suffix: '{}'",
                apparentToCanonical.size(), detectedSuffix);

            // Build the reverse map: strip suffix from each canonical value to get the module name
            Map<String, String> moduleNameToCanonical = new LinkedHashMap<>();
            for (String canonical : apparentToCanonical.values()) {
                String moduleName = stripSuffix(canonical, detectedSuffix);
                if (!moduleName.isEmpty()) {
                    moduleNameToCanonical.putIfAbsent(moduleName, canonical);
                }
            }

            return new BzlmodRepoMappingResolver(apparentToCanonical, moduleNameToCanonical, detectedSuffix, true);

        } catch (Exception e) {
            logger.warn("BZLMOD BCR: failed to parse dump_repo_mapping output ({}); degrading gracefully", e.getMessage());
            return unavailable();
        }
    }

    /**
     * Strips {@code suffix} from the end of {@code rawName} if it ends with it.
     * Returns {@code rawName} unchanged otherwise.
     */
    private static String stripSuffix(String rawName, String suffix) {
        if (suffix != null && !suffix.isEmpty() && rawName.endsWith(suffix)) {
            return rawName.substring(0, rawName.length() - suffix.length());
        }
        return rawName;
    }

    /**
     * Strips a canonical suffix from {@code raw}. When the mapping is available, only the
     * detected suffix is stripped (precise). When unavailable, a regex strips any known
     * suffix ({@code +} or {@code ~}) as a best-effort fallback.
     */
    private String stripKnownSuffix(String raw) {
        if (available) {
            return stripSuffix(raw, detectedSuffix);
        }
        return raw.replaceAll(KNOWN_SUFFIXES_REGEX, "");
    }
}
