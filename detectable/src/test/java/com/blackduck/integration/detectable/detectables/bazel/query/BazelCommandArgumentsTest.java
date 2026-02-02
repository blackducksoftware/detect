package com.blackduck.integration.detectable.detectables.bazel.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BazelCommandArgumentsTest {

    @Test
    void testCommands() {
        assertEquals("query", BazelCommandArguments.QUERY);
        assertEquals("cquery", BazelCommandArguments.CQUERY);
        assertEquals("mod", BazelCommandArguments.MOD);
    }

    @Test
    void testCommonFlags() {
        assertEquals("--noimplicit_deps", BazelCommandArguments.NO_IMPLICIT_DEPS);
        assertEquals("--output", BazelCommandArguments.OUTPUT_FLAG);
    }

    @Test
    void testQueryFunctions() {
        assertEquals("kind", BazelCommandArguments.KIND_FUNCTION);
        assertEquals("deps", BazelCommandArguments.DEPS_FUNCTION);
        assertEquals("filter", BazelCommandArguments.FILTER_FUNCTION);
    }

    @Test
    void testModSubcommands() {
        assertEquals("show_repo", BazelCommandArguments.MOD_SHOW_REPO);
        assertEquals("graph", BazelCommandArguments.MOD_GRAPH);
    }

    @Test
    void testRepoPrefixes() {
        assertEquals("@", BazelCommandArguments.REPO_PREFIX_SINGLE);
        assertEquals("@@", BazelCommandArguments.REPO_PREFIX_CANONICAL);
    }

    @Test
    void testCannotInstantiate() {
        // Verify utility class pattern - constructor throws IllegalStateException
        try {
            java.lang.reflect.Constructor<BazelCommandArguments> constructor =
                BazelCommandArguments.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
            fail("Expected IllegalStateException to be thrown");
        } catch (Exception e) {
            // Expected - the constructor should throw IllegalStateException
            assertTrue(e.getCause() instanceof IllegalStateException);
        }
    }

    @Test
    void testAllConstantsAreNonNull() {
        assertNotNull(BazelCommandArguments.QUERY);
        assertNotNull(BazelCommandArguments.CQUERY);
        assertNotNull(BazelCommandArguments.MOD);
        assertNotNull(BazelCommandArguments.NO_IMPLICIT_DEPS);
        assertNotNull(BazelCommandArguments.OUTPUT_FLAG);
        assertNotNull(BazelCommandArguments.KIND_FUNCTION);
        assertNotNull(BazelCommandArguments.DEPS_FUNCTION);
        assertNotNull(BazelCommandArguments.FILTER_FUNCTION);
        assertNotNull(BazelCommandArguments.MOD_SHOW_REPO);
        assertNotNull(BazelCommandArguments.MOD_GRAPH);
        assertNotNull(BazelCommandArguments.REPO_PREFIX_SINGLE);
        assertNotNull(BazelCommandArguments.REPO_PREFIX_CANONICAL);
    }

    @Test
    void testConstantsAreCorrectlyFormatted() {
        // Commands should be lowercase
        assertEquals("query", BazelCommandArguments.QUERY);
        assertEquals("cquery", BazelCommandArguments.CQUERY);
        assertEquals("mod", BazelCommandArguments.MOD);

        // Flags should start with --
        assertTrue(BazelCommandArguments.NO_IMPLICIT_DEPS.startsWith("--"));
        assertTrue(BazelCommandArguments.OUTPUT_FLAG.startsWith("--"));

        // Functions should be lowercase
        assertEquals("kind", BazelCommandArguments.KIND_FUNCTION);
        assertEquals("deps", BazelCommandArguments.DEPS_FUNCTION);
        assertEquals("filter", BazelCommandArguments.FILTER_FUNCTION);

        // Subcommands should be lowercase with underscores
        assertEquals("show_repo", BazelCommandArguments.MOD_SHOW_REPO);
        assertEquals("graph", BazelCommandArguments.MOD_GRAPH);
    }
}

