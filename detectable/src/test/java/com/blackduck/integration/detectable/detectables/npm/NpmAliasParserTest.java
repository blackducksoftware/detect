package com.blackduck.integration.detectable.detectables.npm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NpmAliasParserTest {

    @Test
    void testParseNonScopedAlias() {
        String aliasValue = "npm:actualpackage@^1.0.0";
        String[] result = NpmAliasParser.parseNpmAlias(aliasValue);

        assertArrayEquals(new String[] { "actualpackage", "^1.0.0" }, result);
    }

    @Test
    void testParseScopedAlias() {
        String aliasValue = "npm:@scope/package@^2.0.0";
        String[] result = NpmAliasParser.parseNpmAlias(aliasValue);

        assertArrayEquals(new String[] { "@scope/package", "^2.0.0" }, result);
    }

    @Test
    void testParseAliasWithoutVersion() {
        String aliasValue = "npm:package";
        String[] result = NpmAliasParser.parseNpmAlias(aliasValue);

        assertArrayEquals(new String[] { "package", "package" }, result);
    }

    @Test
    void testParseScopedAliasWithoutVersion() {
        String aliasValue = "npm:@scope/package";
        String[] result = NpmAliasParser.parseNpmAlias(aliasValue);

        assertArrayEquals(new String[] { "@scope/package", "@scope/package" }, result);
    }

    @Test
    void testParseNonAlias() {
        String regularValue = "^1.0.0";
        String[] result = NpmAliasParser.parseNpmAlias(regularValue);

        assertNull(result);
    }

    @Test
    void testParseNullValue() {
        String[] result = NpmAliasParser.parseNpmAlias(null);

        assertNull(result);
    }

    @Test
    void testIsNpmAlias() {
        assertTrue(NpmAliasParser.isNpmAlias("npm:package@^1.0.0"));
        assertTrue(NpmAliasParser.isNpmAlias("npm:@scope/package@^2.0.0"));
        assertTrue(NpmAliasParser.isNpmAlias("npm:package"));
    }

    @Test
    void testIsNotNpmAlias() {
        assertFalse(NpmAliasParser.isNpmAlias("^1.0.0"));
        assertFalse(NpmAliasParser.isNpmAlias("package"));
        assertFalse(NpmAliasParser.isNpmAlias(null));
        assertFalse(NpmAliasParser.isNpmAlias(""));
    }

    @Test
    void testExtractPackageName() {
        assertEquals("actualpackage", NpmAliasParser.extractPackageName("npm:actualpackage@^1.0.0"));
        assertEquals("@scope/package", NpmAliasParser.extractPackageName("npm:@scope/package@^2.0.0"));
        assertEquals("package", NpmAliasParser.extractPackageName("npm:package"));
    }

    @Test
    void testExtractPackageNameFromNonAlias() {
        assertNull(NpmAliasParser.extractPackageName("^1.0.0"));
        assertNull(NpmAliasParser.extractPackageName(null));
    }

    @Test
    void testParseComplexVersionSpecifiers() {
        // Test with various version specifiers
        String[] result1 = NpmAliasParser.parseNpmAlias("npm:package@~1.2.3");
        assertArrayEquals(new String[] { "package", "~1.2.3" }, result1);

        String[] result2 = NpmAliasParser.parseNpmAlias("npm:package@>=1.0.0");
        assertArrayEquals(new String[] { "package", ">=1.0.0" }, result2);

        String[] result3 = NpmAliasParser.parseNpmAlias("npm:@scope/package@1.2.3-beta.1");
        assertArrayEquals(new String[] { "@scope/package", "1.2.3-beta.1" }, result3);
    }
}
