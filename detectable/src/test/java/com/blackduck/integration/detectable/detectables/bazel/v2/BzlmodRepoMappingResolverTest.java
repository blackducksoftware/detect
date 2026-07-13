package com.blackduck.integration.detectable.detectables.bazel.v2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BzlmodRepoMappingResolver}.
 *
 * <p>Uses the package-private {@code parse(String)} factory directly to avoid any dependency on
 * {@link com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor}.
 * The {@code unavailable} resolver is exercised via a dedicated helper that constructs it through
 * the same code path that {@code load()} uses when the command fails.
 */
public class BzlmodRepoMappingResolverTest {

    // -------------------------------------------------------------------------
    // Helpers — mapping JSON fixtures
    // -------------------------------------------------------------------------

    /** Mapping JSON whose canonical values end with '~' (Bazel 7.5+ style). */
    private static final String TILDE_MAPPING_JSON =
            "{\n"
            + "  \"com_google_protobuf\": \"protobuf~\",\n"
            + "  \"rules_java\": \"rules_java~\"\n"
            + "}";

    /** Mapping JSON whose canonical values end with '+' (Bazel < 7.5 style). */
    private static final String PLUS_MAPPING_JSON =
            "{\n"
            + "  \"grpc\": \"grpc+\",\n"
            + "  \"boringssl\": \"boringssl+\"\n"
            + "}";

    /** Returns a resolver built from {@link #TILDE_MAPPING_JSON}. */
    private BzlmodRepoMappingResolver tildeResolver() {
        return BzlmodRepoMappingResolver.parse(TILDE_MAPPING_JSON);
    }

    /** Returns a resolver built from {@link #PLUS_MAPPING_JSON}. */
    private BzlmodRepoMappingResolver plusResolver() {
        return BzlmodRepoMappingResolver.parse(PLUS_MAPPING_JSON);
    }

    /**
     * Returns an unavailable resolver — constructed via {@code parse()} with JSON that contains
     * no usable entries, which triggers the same {@code unavailable()} fallback as a failed command.
     */
    private BzlmodRepoMappingResolver unavailableResolver() {
        // An empty JSON object has no entries → parse() calls unavailable()
        return BzlmodRepoMappingResolver.parse("{}");
    }

    // -------------------------------------------------------------------------
    // Suffix detection
    // -------------------------------------------------------------------------

    @Test
    public void parse_tildeMapping_detectedSuffixIsTilde() {
        BzlmodRepoMappingResolver resolver = tildeResolver();
        assertTrue(resolver.isAvailable());
        // Verify suffix via canonicalRepoArg: a module in the reverse map must use '~'
        assertEquals("@@rules_java~", resolver.canonicalRepoArg("rules_java"));
    }

    @Test
    public void parse_plusMapping_detectedSuffixIsPlus() {
        BzlmodRepoMappingResolver resolver = plusResolver();
        assertTrue(resolver.isAvailable());
        // Verify suffix via canonicalRepoArg: a module in the reverse map must use '+'
        assertEquals("@@grpc+", resolver.canonicalRepoArg("grpc"));
    }

    // -------------------------------------------------------------------------
    // resolveLabel — canonical form (@@name~)
    // -------------------------------------------------------------------------

    @Test
    public void resolveLabel_canonicalTilde_stripsAndReturnsModuleName() {
        // @@abseil-cpp~//absl:lib → "abseil-cpp" (suffix stripped, no mapping lookup)
        BzlmodRepoMappingResolver resolver = tildeResolver();
        assertEquals(Optional.of("abseil-cpp"), resolver.resolveLabel("@@abseil-cpp~//absl:lib"));
    }

    @Test
    public void resolveLabel_canonicalPlus_stripsAndReturnsModuleName() {
        BzlmodRepoMappingResolver resolver = plusResolver();
        assertEquals(Optional.of("boringssl"), resolver.resolveLabel("@@boringssl+//src:lib"));
    }

    // -------------------------------------------------------------------------
    // resolveLabel — apparent form (@alias → module name via forward map)
    // -------------------------------------------------------------------------

    @Test
    public void resolveLabel_apparentAlias_resolvedViaForwardMap() {
        // @com_google_protobuf is the repo_name alias; forward map resolves it to "protobuf~" → "protobuf"
        BzlmodRepoMappingResolver resolver = tildeResolver();
        assertEquals(Optional.of("protobuf"), resolver.resolveLabel("@com_google_protobuf//google/protobuf:lib"));
    }

    // -------------------------------------------------------------------------
    // resolveLabel — skipped cases
    // -------------------------------------------------------------------------

    @Test
    public void resolveLabel_moduleExtensionRepo_returnsEmpty() {
        // Labels containing "++" are sub-repos of module extensions — skip them
        BzlmodRepoMappingResolver resolver = tildeResolver();
        assertEquals(Optional.empty(), resolver.resolveLabel("@@rules_jvm_external++maven+guava//java/guava:lib"));
    }

    @Test
    public void resolveLabel_localTarget_returnsEmpty() {
        // No leading '@' → local target; nothing to resolve
        BzlmodRepoMappingResolver resolver = tildeResolver();
        assertEquals(Optional.empty(), resolver.resolveLabel("//src/main:foo"));
    }

    @Test
    public void resolveLabel_nullInput_returnsEmpty() {
        assertEquals(Optional.empty(), tildeResolver().resolveLabel(null));
    }

    @Test
    public void resolveLabel_emptyInput_returnsEmpty() {
        assertEquals(Optional.empty(), tildeResolver().resolveLabel(""));
    }

    // -------------------------------------------------------------------------
    // canonicalRepoArg
    // -------------------------------------------------------------------------

    @Test
    public void canonicalRepoArg_moduleInReverseMap_returnsExactCanonical() {
        // "protobuf" is in the reverse map (derived from "protobuf~" in the forward map)
        BzlmodRepoMappingResolver resolver = tildeResolver();
        assertEquals("@@protobuf~", resolver.canonicalRepoArg("protobuf"));
    }

    @Test
    public void canonicalRepoArg_pureTransitiveDep_usesSuffixFallback() {
        // "abseil-cpp" is not in the root module's mapping (never directly declared),
        // so the resolver falls back to appending the detected suffix
        BzlmodRepoMappingResolver resolver = tildeResolver();
        assertEquals("@@abseil-cpp~", resolver.canonicalRepoArg("abseil-cpp"));
    }

    // -------------------------------------------------------------------------
    // Unavailable resolver — regex fallback
    // -------------------------------------------------------------------------

    @Test
    public void unavailableResolver_isNotAvailable() {
        assertFalse(unavailableResolver().isAvailable());
    }

    @Test
    public void unavailableResolver_resolveCanonicalTilde_stripsViaRegexFallback() {
        // When mapping is unavailable, stripKnownSuffix uses regex "[+~]$"
        // @@protobuf~//... should still yield "protobuf"
        BzlmodRepoMappingResolver resolver = unavailableResolver();
        assertEquals(Optional.of("protobuf"), resolver.resolveLabel("@@protobuf~//google/protobuf:lib"));
    }

    @Test
    public void unavailableResolver_resolveCanonicalPlus_stripsViaRegexFallback() {
        BzlmodRepoMappingResolver resolver = unavailableResolver();
        assertEquals(Optional.of("grpc"), resolver.resolveLabel("@@grpc+//src:lib"));
    }

    @Test
    public void unavailableResolver_moduleExtensionRepo_stillReturnsEmpty() {
        // "++" check happens before suffix stripping — must still be excluded
        BzlmodRepoMappingResolver resolver = unavailableResolver();
        assertEquals(Optional.empty(), resolver.resolveLabel("@@rules_jvm_external++maven+guava//java:lib"));
    }
}

