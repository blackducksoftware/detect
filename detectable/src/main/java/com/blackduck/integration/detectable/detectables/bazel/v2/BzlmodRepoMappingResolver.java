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
 * to plain BCR module names (as returned by {@code bazel mod graph}), and constructs
 * the correct argument form for {@code bazel mod show_repo}.
 *
 * <h3>The label-format problem</h3>
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
 * <h3>The show_repo argument problem</h3>
 * {@code bazel mod show_repo} accepts three argument forms, but their reliability varies by
 * Bazel version:
 * <ul>
 *   <li><b>{@code @@name~} or {@code @@name+}</b> (canonical with suffix): reliable on Bazel 8.x+.
 *       On Bazel 7.x, crashes with a {@code NullPointerException} inside Bazel for some modules
 *       not registered in the root's internal repo map — a Bazel bug in
 *       {@code ModuleArg$CanonicalRepoName.resolveToRepoNames}.</li>
 *   <li><b>{@code @@name}</b> (canonical without suffix): only valid for Bazel-embedded modules
 *       like {@code platforms}. For regular BCR modules on Bazel 7.4, {@code dump_repo_mapping}
 *       may incorrectly report no-suffix values, making {@code @@name} fail with "no such repo"
 *       even though the module exists.</li>
 *   <li><b>{@code name}</b> (bare module name): routes through Bazel's apparent-name resolution
 *       path, which is stable across all Bazel versions (7.4, 7.x, 8.x, 9.x). Safe universal
 *       fallback, but cannot resolve {@code repo_name} aliases on its own.</li>
 * </ul>
 *
 * <h3>The solution</h3>
 * This class loads {@code bazel mod dump_repo_mapping ""} once per scan. The output is a flat JSON
 * map of {@code apparent_name → canonical_name_with_or_without_suffix} for the root module.
 * From that map we:
 * <ol>
 *   <li>Detect whether a suffix was actually observed in the mapping values ({@code hasSuffixEvidence}).</li>
 *   <li>Build a forward map ({@code apparent_name → canonical}) for {@code repo_name} alias resolution.</li>
 *   <li>Build a reverse map ({@code module_name → canonical}) for constructing {@code show_repo} args.</li>
 *   <li>Expose {@link #candidateRepoArgs} which returns an ordered list of argument forms to try,
 *       starting from the most specific and falling back to the bare module name.</li>
 * </ol>
 *
 * <p>If {@code dump_repo_mapping} fails or produces no output, the resolver degrades gracefully:
 * canonical labels ({@code @@name~}) are handled by stripping any known suffix ({@code +} or {@code ~}),
 * apparent-name aliases are passed through unchanged, and {@code candidateRepoArgs} returns both
 * known suffix forms plus the bare name to maximise coverage without hardcoding any specific form.
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
    // The two canonical suffix characters used by Bazel across versions:
    //   SUFFIX_TILDE (~) — introduced in Bazel 7.5+
    //   SUFFIX_PLUS  (+) — used in Bazel 7.x (pre-7.5) and some 8.x builds
    private static final String SUFFIX_TILDE = "~";
    private static final String SUFFIX_PLUS  = "+";

    // -------------------------------------------------------------------------
    // Inner types
    // -------------------------------------------------------------------------

    /**
     * The six resolution cases for {@link #candidateRepoArgs}, classified by map state and
     * what is known about the module's canonical suffix.
     *
     * <p><b>Why some modules are "pure transitive" (not in the map):</b><br>
     * {@code bazel mod dump_repo_mapping ""} only returns entries for modules the root project
     * <em>directly declares</em> via {@code bazel_dep(...)} in its {@code MODULE.bazel}.
     * Transitive dependencies (deps of deps) are never present in this output — this is a
     * structural limitation of the command, not an error.
     * <br>
     * For these pure transitives, we infer the correct suffix from the direct-dep entries in the
     * map. This is safe because the suffix character ({@code ~} vs {@code +}) is determined by
     * the Bazel version, not per-module — all repos in a single Bazel invocation use the same
     * suffix convention.
     *
     * <pre>
     * MAP_UNAVAILABLE                   — dump_repo_mapping failed entirely
     * IN_MAP_SUFFIXED                   — module in map with a well-formed suffixed canonical (e.g. "protobuf~")
     * IN_MAP_NO_SUFFIX_EVIDENCE_KNOWN   — module in map, no-suffix value, but other entries confirmed suffix
     * IN_MAP_NO_SUFFIX_EVIDENCE_UNKNOWN — module in map, no-suffix value, entire map has no suffix evidence
     * PURE_TRANSITIVE_SUFFIX_KNOWN      — transitive dep (not in map by design); suffix confirmed from direct-dep entries
     * PURE_TRANSITIVE_SUFFIX_UNKNOWN    — transitive dep (not in map by design); no suffix evidence in any direct-dep entry
     * </pre>
     */
    private enum RepoResolutionCase {
        MAP_UNAVAILABLE,
        IN_MAP_SUFFIXED,
        IN_MAP_NO_SUFFIX_EVIDENCE_KNOWN,
        IN_MAP_NO_SUFFIX_EVIDENCE_UNKNOWN,
        PURE_TRANSITIVE_SUFFIX_KNOWN,
        PURE_TRANSITIVE_SUFFIX_UNKNOWN
    }

    // -------------------------------------------------------------------------
    // Instance fields
    // -------------------------------------------------------------------------

    // apparent_name → canonical_with_or_without_suffix  (e.g. "com_google_protobuf" → "protobuf~")
    // On Bazel 7.4 some entries may have no suffix (e.g. "bazel_skylib" → "bazel_skylib") even
    // though the module actually needs one — this is a Bazel 7.4 dump_repo_mapping inconsistency.
    private final Map<String, String> apparentToCanonical;
    // module_name → canonical_with_or_without_suffix  (e.g. "protobuf" → "protobuf~")
    // derived by stripping the detected suffix from every value in apparentToCanonical
    private final Map<String, String> moduleNameToCanonical;
    // the suffix character detected from mapping values (e.g. "~" for Bazel 7.5+, "+" for older)
    private final String canonicalSuffix;
    // true only when at least one mapping value was observed to end with a known suffix character.
    // Distinct from 'available' — the mapping can load successfully but contain no suffixed values
    // (e.g. Bazel 7.4 projects where all direct deps happen to have no-suffix canonical names).
    // When false, candidateRepoArgs() tries both suffix forms rather than guessing one.
    private final boolean hasSuffixEvidence;
    // false if dump_repo_mapping failed; resolver degrades gracefully
    private final boolean available;

    private BzlmodRepoMappingResolver(Map<String, String> apparentToCanonical,
                                       Map<String, String> moduleNameToCanonical,
                                       String canonicalSuffix,
                                       boolean available,
                                       boolean hasSuffixEvidence) {
        this.apparentToCanonical  = apparentToCanonical;
        this.moduleNameToCanonical = moduleNameToCanonical;
        this.canonicalSuffix        = canonicalSuffix;
        this.available             = available;
        this.hasSuffixEvidence        = hasSuffixEvidence;
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
     * (e.g. {@code protobuf} from {@code protobuf@31.0}), returns the primary canonical repo
     * argument to pass to {@code bazel mod show_repo} in the batched call.
     *
     * <p>Selection logic:
     * <ul>
     *   <li>Module is in the reverse map <em>and</em> the map value has a suffix (e.g. {@code protobuf~}):
     *       returns {@code @@protobuf~} — exact canonical form, reliable on Bazel 8.x+.</li>
     *   <li>Module is in the reverse map <em>but</em> the map value has no suffix (Bazel 7.4
     *       {@code dump_repo_mapping} inconsistency): returns the bare module name (e.g. {@code bazel_skylib})
     *       rather than {@code @@bazel_skylib}, which would fail with "no such repo".</li>
     *   <li>Module is NOT in the map (pure transitive dep): returns {@code @@name<canonicalSuffix>},
     *       constructed by appending the suffix detected from other mapping values.</li>
     * </ul>
     *
     * <p>Note: {@link #candidateRepoArgs} should be preferred in the per-module fallback loop
     * because it provides a complete ordered candidate list including the bare-name safety net.
     * This method is intended for constructing the initial batch call argument.
     */
    public String canonicalRepoArg(String moduleName) {
        if (available && moduleNameToCanonical.containsKey(moduleName)) {
            String canonical = moduleNameToCanonical.get(moduleName);
            if (hasSuffix(canonical)) {
                // Normal case: canonical value has a suffix — e.g. "protobuf~" → "@@protobuf~"
                return CANONICAL_PREFIX + canonical;
            } else {
                // Bazel 7.4 inconsistency: dump_repo_mapping reports no-suffix value for a BCR module
                // that actually needs a suffix (e.g. "bazel_skylib" → "bazel_skylib").
                // "@@bazel_skylib" would fail; the bare module name routes through Bazel's
                // apparent-name path which handles it correctly across all versions.
                logger.debug("BZLMOD BCR: module '{}' has no-suffix canonical value '{}' in mapping; " +
                    "using bare module name for show_repo (Bazel 7.4 dump_repo_mapping inconsistency)",
                    moduleName, canonical);
                return moduleName;
            }
        }
        // Pure transitive dep not in root mapping — construct canonical form using the detected suffix.
        // Falls back to bare name in candidateRepoArgs() if this form triggers a Bazel crash.
        return CANONICAL_PREFIX + moduleName + canonicalSuffix;
    }

    /**
     * Strips the detected canonical suffix from {@code rawName}.
     * Used when parsing {@code ## @@name<suffix>:} block headers in batched show_repo output.
     *
     * <p>When the mapping is available, only the detected suffix is stripped (precise).
     * When unavailable, a regex strips any known suffix ({@code +} or {@code ~}) as a
     * best-effort fallback — consistent with how {@link #resolveLabel} degrades.
     *
     * <p>Example: {@code "protobuf~"} → {@code "protobuf"}
     */
    public String stripCanonicalSuffix(String rawName) {
        if (available) {
            return stripSuffix(rawName, canonicalSuffix);
        }
        return rawName.replaceAll(KNOWN_SUFFIXES_REGEX, "");
    }

    /**
     * Returns an ordered list of {@code bazel mod show_repo} argument candidates to try for the
     * given module name. The per-module fallback loop tries each candidate in order and uses the
     * first that returns non-empty output.
     *
     * <p>The candidate matrix is designed to handle three classes of Bazel inconsistencies:
     * <ol>
     *   <li><b>Bazel 7.x NPE</b>: {@code @@name~} crashes Bazel internally for some pure transitive
     *       modules. The bare module name routes through a different code path and does not crash.</li>
     *   <li><b>Bazel 7.4 dump_repo_mapping inconsistency</b>: the mapping reports no-suffix values
     *       for some BCR modules that actually need the suffix. {@code @@name} fails; bare name and
     *       {@code @@name~} both work.</li>
     *   <li><b>No suffix evidence</b>: all mapping values have no suffix, so {@code canonicalSuffix}
     *       is the hardcoded default {@code "+"}. Both suffix forms are tried rather than guessing.</li>
     * </ol>
     *
     * <p><b>Candidate matrix:</b>
     * <pre>
     * Module in map, suffixed value (e.g. "googletest~"):
     *   1. @@googletest~   — exact canonical from map (primary, works on Bazel 8.x+)
     *   2. googletest      — bare name fallback (safe if Bazel 7.x NPE hits the canonical form)
     *
     * Module in map, no-suffix value (e.g. "bazel_skylib", Bazel 7.4 inconsistency):
     *   1. bazel_skylib          — bare name first (safe apparent path, avoids @@bazel_skylib failure)
     *   2. @@bazel_skylib~       — try with detected suffix
     *   3. @@bazel_skylib+       — try with the other known suffix
     *
     * Pure transitive dep (not in map — dump_repo_mapping only covers root-declared deps), suffix confirmed:
     *   1. @@googletest~   — canonical with confirmed suffix (primary)
     *   2. googletest      — bare name fallback (handles Bazel 7.x NPE)
     *
     * Pure transitive dep (not in map — dump_repo_mapping only covers root-declared deps), no suffix evidence:
     *   1. @@name~         — try tilde first (more common on modern Bazel 7.5+)
     *   2. @@name+         — try plus (pre-7.5)
     *   3. name            — bare name final fallback
     *
     * Mapping unavailable (dump_repo_mapping failed entirely):
     *   1. @@name~         — try ~ first
     *   2. @@name+         — try +
     *   3. name            — bare name final fallback
     * </pre>
     *
     * <p>The bare module name is safe as a universal last resort because:
     * it routes through Bazel's apparent-name resolution path (stable across all versions),
     * and pure transitives / no-alias modules have their module name equal to their apparent name.
     * The only case where bare name could be wrong is a {@code repo_name} alias, but aliased modules
     * are always in the map with a suffixed canonical value and are handled by the first candidate.
     */
    public List<String> candidateRepoArgs(String moduleName) {
        String otherSuffix = SUFFIX_TILDE.equals(canonicalSuffix) ? SUFFIX_PLUS : SUFFIX_TILDE;

        switch (classify(moduleName)) {
            case MAP_UNAVAILABLE:
            case PURE_TRANSITIVE_SUFFIX_UNKNOWN:
                // No suffix evidence at all — try both suffix forms then bare name
                return suffixFormsThenBareName(moduleName);

            case IN_MAP_SUFFIXED:
                // Exact canonical from map; bare name as safety net for Bazel 7.x NPE
                return Arrays.asList(CANONICAL_PREFIX + moduleNameToCanonical.get(moduleName), moduleName);

            case IN_MAP_NO_SUFFIX_EVIDENCE_KNOWN:
                // Bazel 7.4 inconsistency — bare name first, then suffix forms in detected order
                return Arrays.asList(
                    moduleName,
                    CANONICAL_PREFIX + moduleName + canonicalSuffix,
                    CANONICAL_PREFIX + moduleName + otherSuffix
                );

            case IN_MAP_NO_SUFFIX_EVIDENCE_UNKNOWN:
                // Bazel 7.4 inconsistency, no suffix evidence — bare name first, then tilde-first order
                return bareNameThenSuffixForms(moduleName);

            case PURE_TRANSITIVE_SUFFIX_KNOWN:
                // Canonical form with confirmed suffix; bare name as safety net for Bazel 7.x NPE
                return Arrays.asList(CANONICAL_PREFIX + moduleName + canonicalSuffix, moduleName);

            default:
                return suffixFormsThenBareName(moduleName);
        }
    }

    /**
     * Classifies a module name into one of the {@link RepoResolutionCase} values that drive
     * {@link #candidateRepoArgs}. Encapsulates all boolean state checks ({@code available},
     * map membership, suffix presence, {@code hasSuffixEvidence}) in one place so that
     * {@code candidateRepoArgs} is a pure switch.
     *
     * <p>A module that is absent from {@code moduleNameToCanonical} is a <em>pure transitive</em>
     * dependency — one not directly declared by the root. This is expected: {@code dump_repo_mapping ""}
     * only covers root-declared deps. For these modules we infer the suffix from direct-dep entries,
     * which is valid because all repos in a Bazel invocation share the same suffix convention.
     */
    private RepoResolutionCase classify(String moduleName) {
        if (!available) {
            return RepoResolutionCase.MAP_UNAVAILABLE;
        }
        if (moduleNameToCanonical.containsKey(moduleName)) {
            String canonical = moduleNameToCanonical.get(moduleName);
            if (hasSuffix(canonical)) {
                return RepoResolutionCase.IN_MAP_SUFFIXED;
            }
            return hasSuffixEvidence
                ? RepoResolutionCase.IN_MAP_NO_SUFFIX_EVIDENCE_KNOWN
                : RepoResolutionCase.IN_MAP_NO_SUFFIX_EVIDENCE_UNKNOWN;
        }
        // Module not in map — this is a pure transitive dep, absent by design (see class Javadoc).
        // Use suffix evidence gathered from direct-dep entries to infer the right suffix form.
        return hasSuffixEvidence
            ? RepoResolutionCase.PURE_TRANSITIVE_SUFFIX_KNOWN
            : RepoResolutionCase.PURE_TRANSITIVE_SUFFIX_UNKNOWN;
    }


    /**
     * No-evidence fallback candidate list: try tilde form first (more common on Bazel 7.5+),
     * then plus form, then bare name as universal last resort.
     * Used when there is no positive evidence for which suffix the project uses.
     */
    private List<String> suffixFormsThenBareName(String moduleName) {
        return Arrays.asList(
            CANONICAL_PREFIX + moduleName + SUFFIX_TILDE,
            CANONICAL_PREFIX + moduleName + SUFFIX_PLUS,
            moduleName
        );
    }

    /**
     * Bazel 7.4 no-suffix inconsistency candidate list: bare name first (safe apparent path,
     * avoids the "no such repo" error from {@code @@name} with no suffix), then both suffix forms.
     * Used when the mapping value for a module has no suffix character.
     */
    private List<String> bareNameThenSuffixForms(String moduleName) {
        return Arrays.asList(
            moduleName,
            CANONICAL_PREFIX + moduleName + SUFFIX_TILDE,
            CANONICAL_PREFIX + moduleName + SUFFIX_PLUS
        );
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
            SUFFIX_PLUS, // default to "+" as a safe fallback
            false,       // mapping not available
            false        // no suffix detected
        );
    }

    static BzlmodRepoMappingResolver parse(String json) {
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

            // Detect the canonical suffix from the first mapping value that ends with a known character.
            // hasSuffixEvidence = true only when we have actual evidence — not just the hardcoded default.
            // This distinction matters in candidateRepoArgs(): when no suffix evidence exists, we try
            // both forms rather than guessing, covering projects where the entire mapping has no-suffix values.
            String canonicalSuffix = SUFFIX_PLUS; // safe default if no evidence is found
            boolean hasSuffixEvidence = false;
            for (String canonical : apparentToCanonical.values()) {
                if (canonical.endsWith(SUFFIX_TILDE)) { canonicalSuffix = SUFFIX_TILDE; hasSuffixEvidence = true; break; }
                if (canonical.endsWith(SUFFIX_PLUS))  { canonicalSuffix = SUFFIX_PLUS;  hasSuffixEvidence = true; break; }
            }
            logger.info("BZLMOD BCR: repo mapping loaded ({} entries), detected canonical suffix: '{}' (evidence found: {})",
                apparentToCanonical.size(), canonicalSuffix, hasSuffixEvidence);

            // Build the reverse map: strip suffix from each canonical value to get the module name
            Map<String, String> moduleNameToCanonical = new LinkedHashMap<>();
            for (String canonical : apparentToCanonical.values()) {
                String moduleName = stripSuffix(canonical, canonicalSuffix);
                if (!moduleName.isEmpty()) {
                    moduleNameToCanonical.putIfAbsent(moduleName, canonical);
                }
            }

            return new BzlmodRepoMappingResolver(apparentToCanonical, moduleNameToCanonical, canonicalSuffix, true, hasSuffixEvidence);

        } catch (Exception e) {
            logger.warn("BZLMOD BCR: failed to parse dump_repo_mapping output ({}); degrading gracefully", e.getMessage());
            return unavailable();
        }
    }

    /**
     * Returns true if {@code canonical} ends with a known Bazel suffix character ({@code ~} or {@code +}).
     * Used to distinguish well-formed canonical values from Bazel 7.4 no-suffix anomalies.
     */
    private static boolean hasSuffix(String canonical) {
        return canonical.endsWith(SUFFIX_TILDE) || canonical.endsWith(SUFFIX_PLUS);
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
            return stripSuffix(raw, canonicalSuffix);
        }
        return raw.replaceAll(KNOWN_SUFFIXES_REGEX, "");
    }
}
