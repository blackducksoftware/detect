package com.blackduck.integration.detectable.detectables.bazel.query;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BazelQueryBuilderTest {

    @Test
    void testQueryFactoryMethod() {
        QueryBuilder builder = BazelQueryBuilder.query();
        assertNotNull(builder);
    }

    @Test
    void testCqueryFactoryMethod() {
        CqueryBuilder builder = BazelQueryBuilder.cquery();
        assertNotNull(builder);
    }

    @Test
    void testModFactoryMethod() {
        ModCommandBuilder builder = BazelQueryBuilder.mod();
        assertNotNull(builder);
    }

    @Test
    void testDepsHelperMethod() {
        String deps = BazelQueryBuilder.deps("//:test");
        assertEquals("deps(//:test)", deps);
    }

    @Test
    void testQueryIntegration() {
        List<String> result = BazelQueryBuilder.query()
            .kind(".*library", BazelQueryBuilder.deps("//:test"))
            .build();

        assertEquals(2, result.size());
        assertEquals("query", result.get(0));
        assertEquals("kind(.*library, deps(//:test))", result.get(1));
    }

    @Test
    void testCqueryIntegration() {
        List<String> result = BazelQueryBuilder.cquery()
            .kind("j.*import", BazelQueryBuilder.deps("//:test"))
            .withNoImplicitDeps()
            .withOutput(OutputFormat.BUILD)
            .build();

        assertEquals(5, result.size());
        assertEquals("cquery", result.get(0));
        assertEquals("--noimplicit_deps", result.get(1));
        assertEquals("kind(j.*import, deps(//:test))", result.get(2));
        assertEquals("--output", result.get(3));
        assertEquals("build", result.get(4));
    }

    @Test
    void testModIntegration() {
        List<String> result = BazelQueryBuilder.mod()
            .showRepo("my_repo", false)
            .build();

        assertEquals(3, result.size());
        assertEquals("mod", result.get(0));
        assertEquals("show_repo", result.get(1));
        assertEquals("@my_repo", result.get(2));
    }

    @Test
    void testCannotInstantiate() throws Exception {
        // Verify utility class pattern - constructor throws IllegalStateException
        Constructor<BazelQueryBuilder> constructor =
            BazelQueryBuilder.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException thrown = assertThrows(
            InvocationTargetException.class,
            constructor::newInstance
        );
        assertTrue(thrown.getCause() instanceof IllegalStateException);
    }

    // ===== Real-world usage examples from the codebase =====

    @Test
    void testMavenInstallDetectionQuery() {
        // From BazelGraphProber.detectMavenInstall()
        String target = "//:test";
        List<String> result = BazelQueryBuilder.cquery()
            .kind("j.*import", BazelQueryBuilder.deps(target))
            .withNoImplicitDeps()
            .withOutput(OutputFormat.BUILD)
            .build();

        assertEquals(5, result.size());
        assertEquals("cquery", result.get(0));
        assertEquals("--noimplicit_deps", result.get(1));
        assertEquals("kind(j.*import, deps(//:test))", result.get(2));
        assertEquals("--output", result.get(3));
        assertEquals("build", result.get(4));
    }

    @Test
    void testMavenJarDetectionQuery() {
        // From BazelGraphProber.detectMavenJar()
        String target = "//:test";
        List<String> result = BazelQueryBuilder.cquery()
            .filter("'@.*:jar'", BazelQueryBuilder.deps(target))
            .build();

        assertEquals(2, result.size());
        assertEquals("cquery", result.get(0));
        assertEquals("filter('@.*:jar', deps(//:test))", result.get(1));
    }

    @Test
    void testHaskellCabalDetectionQuery() {
        // From BazelGraphProber.detectHaskellCabal()
        String target = "//:test";
        List<String> result = BazelQueryBuilder.cquery()
            .kind("haskell_cabal_library", BazelQueryBuilder.deps(target))
            .withNoImplicitDeps()
            .withOutput(OutputFormat.LABEL_KIND)
            .build();

        assertEquals(5, result.size());
        assertEquals("cquery", result.get(0));
        assertEquals("--noimplicit_deps", result.get(1));
        assertEquals("kind(haskell_cabal_library, deps(//:test))", result.get(2));
        assertEquals("--output", result.get(3));
        assertEquals("label_kind", result.get(4));
    }

    @Test
    void testHttpArchiveLibraryQuery() {
        // From HttpFamilyProber.detect()
        String target = "//:test";
        List<String> result = BazelQueryBuilder.query()
            .kind(".*library", BazelQueryBuilder.deps(target))
            .build();

        assertEquals(2, result.size());
        assertEquals("query", result.get(0));
        assertEquals("kind(.*library, deps(//:test))", result.get(1));
    }

    @Test
    void testLabelKindProbeQuery() {
        // From HttpFamilyProber.probeRepoWithLabelKind()
        String queryTarget = "@my_repo//:all";
        List<String> result = BazelQueryBuilder.query()
            .kind("'rule'", queryTarget)
            .withOutput(OutputFormat.LABEL_KIND)
            .build();

        assertEquals(4, result.size());
        assertEquals("query", result.get(0));
        assertEquals("kind('rule', @my_repo//:all)", result.get(1));
        assertEquals("--output", result.get(2));
        assertEquals("label_kind", result.get(3));
    }

    @Test
    void testModShowRepoCommand() {
        // From HttpFamilyProber.classifyRepoByModShowRepo()
        String repo = "my_repo";
        List<String> result = BazelQueryBuilder.mod()
            .showRepo(repo, false)
            .build();

        assertEquals(3, result.size());
        assertEquals("mod", result.get(0));
        assertEquals("show_repo", result.get(1));
        assertEquals("@my_repo", result.get(2));
    }

    @Test
    void testModShowRepoCanonical() {
        // From HttpFamilyProber.classifyRepoByModShowRepo() with canonical=true
        String repo = "my_repo";
        List<String> result = BazelQueryBuilder.mod()
            .showRepo(repo, true)
            .build();

        assertEquals(3, result.size());
        assertEquals("mod", result.get(0));
        assertEquals("show_repo", result.get(1));
        assertEquals("@@my_repo", result.get(2));
    }

    @Test
    void testModGraphCommand() {
        // From BazelEnvironmentAnalyzer.detectMode()
        List<String> result = BazelQueryBuilder.mod()
            .graph()
            .build();

        assertEquals(2, result.size());
        assertEquals("mod", result.get(0));
        assertEquals("graph", result.get(1));
    }

    @Test
    void testMavenJarPipelineQuery() {
        // From Pipelines - maven_jar pipeline
        List<String> result = BazelQueryBuilder.cquery()
            .filter("'@.*:jar'", BazelQueryBuilder.deps("${detect.bazel.target}"))
            .withOptions("${detect.bazel.cquery.options}")
            .build();

        assertEquals(3, result.size());
        assertEquals("cquery", result.get(0));
        assertEquals("${detect.bazel.cquery.options}", result.get(1));
        assertEquals("filter('@.*:jar', deps(${detect.bazel.target}))", result.get(2));
    }

    @Test
    void testMavenInstallPipelineQuery() {
        // From Pipelines - maven_install pipeline
        List<String> result = BazelQueryBuilder.cquery()
            .kind("j.*import", BazelQueryBuilder.deps("${detect.bazel.target}"))
            .withNoImplicitDeps()
            .withOptions("${detect.bazel.cquery.options}")
            .withOutput(OutputFormat.BUILD)
            .build();

        assertEquals(6, result.size());
        assertEquals("cquery", result.get(0));
        assertEquals("--noimplicit_deps", result.get(1));
        assertEquals("${detect.bazel.cquery.options}", result.get(2));
        assertEquals("kind(j.*import, deps(${detect.bazel.target}))", result.get(3));
        assertEquals("--output", result.get(4));
        assertEquals("build", result.get(5));
    }

    @Test
    void testHaskellPipelineQuery() {
        // From Pipelines - haskell_cabal_library pipeline
        List<String> result = BazelQueryBuilder.cquery()
            .kind("haskell_cabal_library", BazelQueryBuilder.deps("${detect.bazel.target}"))
            .withNoImplicitDeps()
            .withOptions("${detect.bazel.cquery.options}")
            .withOutput(OutputFormat.JSONPROTO)
            .build();

        assertEquals(6, result.size());
        assertEquals("cquery", result.get(0));
        assertEquals("--noimplicit_deps", result.get(1));
        assertEquals("${detect.bazel.cquery.options}", result.get(2));
        assertEquals("kind(haskell_cabal_library, deps(${detect.bazel.target}))", result.get(3));
        assertEquals("--output", result.get(4));
        assertEquals("jsonproto", result.get(5));
    }

    @Test
    void testHttpArchivePipelineQuery() {
        // From Pipelines - HTTP_ARCHIVE pipeline
        List<String> result = BazelQueryBuilder.query()
            .kind(".*library", BazelQueryBuilder.deps("${detect.bazel.target}"))
            .build();

        assertEquals(2, result.size());
        assertEquals("query", result.get(0));
        assertEquals("kind(.*library, deps(${detect.bazel.target}))", result.get(1));
    }

    @Test
    void testMavenJarXmlQuery() {
        // From Pipelines - maven_jar second step
        List<String> result = BazelQueryBuilder.query()
            .kind("maven_jar", "${input.item}")
            .withOutput(OutputFormat.XML)
            .build();

        assertEquals(4, result.size());
        assertEquals("query", result.get(0));
        assertEquals("kind(maven_jar, ${input.item})", result.get(1));
        assertEquals("--output", result.get(2));
        assertEquals("xml", result.get(3));
    }
}

