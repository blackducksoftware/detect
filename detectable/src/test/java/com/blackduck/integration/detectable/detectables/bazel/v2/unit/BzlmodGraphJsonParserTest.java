package com.blackduck.integration.detectable.detectables.bazel.v2.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.detectables.bazel.v2.BzlmodGraphJsonParser;

public class BzlmodGraphJsonParserTest {

    private final BzlmodGraphJsonParser parser = new BzlmodGraphJsonParser();

    // --- parseModuleKeys ---

    @Test
    public void parseModuleKeys_typicalGraph_returnsAllModuleKeys() {
        String json = "{\n"
                + "  \"key\": \"<root>\",\n"
                + "  \"dependencies\": [\n"
                + "    {\n"
                + "      \"key\": \"protobuf@29.3\",\n"
                + "      \"dependencies\": [\n"
                + "        { \"key\": \"rules_java@8.6.4\", \"dependencies\": [] }\n"
                + "      ]\n"
                + "    },\n"
                + "    { \"key\": \"abseil-cpp@20240722.0\", \"dependencies\": [] }\n"
                + "  ]\n"
                + "}";

        Set<String> keys = parser.parseModuleKeys(json);

        assertEquals(3, keys.size());
        assertTrue(keys.contains("protobuf@29.3"));
        assertTrue(keys.contains("rules_java@8.6.4"));
        assertTrue(keys.contains("abseil-cpp@20240722.0"));
    }

    @Test
    public void parseModuleKeys_rootKeyIsExcluded() {
        String json = "{ \"key\": \"<root>\", \"dependencies\": [] }";
        Set<String> keys = parser.parseModuleKeys(json);
        assertTrue(keys.isEmpty());
    }

    @Test
    public void parseModuleKeys_duplicateDependency_deduplicates() {
        String json = "{\n"
                + "  \"key\": \"<root>\",\n"
                + "  \"dependencies\": [\n"
                + "    { \"key\": \"protobuf@29.3\", \"dependencies\": [] },\n"
                + "    { \"key\": \"protobuf@29.3\", \"dependencies\": [] }\n"
                + "  ]\n"
                + "}";
        Set<String> keys = parser.parseModuleKeys(json);
        assertEquals(1, keys.size());
    }

    @Test
    public void parseModuleKeys_nullInput_returnsEmptySet() {
        assertTrue(parser.parseModuleKeys(null).isEmpty());
    }

    @Test
    public void parseModuleKeys_emptyInput_returnsEmptySet() {
        assertTrue(parser.parseModuleKeys("").isEmpty());
    }

    @Test
    public void parseModuleKeys_malformedJson_returnsEmptySet() {
        assertTrue(parser.parseModuleKeys("{ this is not json }").isEmpty());
    }

    @Test
    public void parseModuleKeys_jsonArray_returnsEmptySet() {
        // Root must be a JSON object, not array
        assertTrue(parser.parseModuleKeys("[\"protobuf@29.3\"]").isEmpty());
    }

    // --- extractName ---

    @Test
    public void extractName_standardKey_returnsName() {
        assertEquals("protobuf", BzlmodGraphJsonParser.extractName("protobuf@29.3"));
    }

    @Test
    public void extractName_hyphenatedName_returnsName() {
        assertEquals("abseil-cpp", BzlmodGraphJsonParser.extractName("abseil-cpp@20240722.0"));
    }

    @Test
    public void extractName_noAtSign_returnsFullKey() {
        assertEquals("protobuf", BzlmodGraphJsonParser.extractName("protobuf"));
    }

    /**
     * The '@@' prefix belongs to Bazel canonical label syntax (BUILD file labels),
     * not to mod graph JSON keys. This test documents how the method behaves
     * defensively if such a string were passed unexpectedly.
     */
    @Test
    public void extractName_doubleAtPrefix_returnsFullKey() {
        // '@@' → indexOf('@') == 0 which is NOT > 0, so full key is returned
        assertEquals("@@protobuf~29.3", BzlmodGraphJsonParser.extractName("@@protobuf~29.3"));
    }

    // --- extractVersion ---

    @Test
    public void extractVersion_standardKey_returnsVersion() {
        assertEquals("29.3", BzlmodGraphJsonParser.extractVersion("protobuf@29.3"));
    }

    @Test
    public void extractVersion_multiSegmentVersion_returnsVersion() {
        assertEquals("20240722.0", BzlmodGraphJsonParser.extractVersion("abseil-cpp@20240722.0"));
    }

    @Test
    public void extractVersion_noAtSign_returnsNull() {
        assertNull(BzlmodGraphJsonParser.extractVersion("protobuf"));
    }

    @Test
    public void extractVersion_atSignAtEnd_returnsNull() {
        assertNull(BzlmodGraphJsonParser.extractVersion("protobuf@"));
    }
}
