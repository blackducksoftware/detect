package com.blackduck.integration.detectable.detectables.bazel.v2.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import com.blackduck.integration.detectable.detectables.bazel.v2.BazelVersion;
import com.blackduck.integration.detectable.detectables.bazel.v2.BzlmodBcrExtractor;

/**
 * Unit tests for {@link BzlmodBcrExtractor}.
 *
 * <p>Uses a {@link StubBazelCommandExecutor} to inject canned command responses
 * without invoking any real Bazel process. All tests exercise the full
 * {@link BzlmodBcrExtractor#extractGraph()} path, which covers:
 * <ul>
 *   <li>mod graph JSON parsing and direct/transitive classification</li>
 *   <li>Repo mapping and suffix detection</li>
 *   <li>Target-scoped module filtering</li>
 *   <li>Batched and per-module show_repo parsing</li>
 *   <li>GitHub URL extraction and version normalization</li>
 *   <li>ExternalId dedup set population</li>
 *   <li>Diamond dependency handling</li>
 * </ul>
 *
 * <p>All stub responses are organised in {@link Fixtures}, grouped by scenario.
 * Each scenario class holds the four command responses that {@link BzlmodBcrExtractor#extractGraph()}
 * issues in order: mod graph, dump_repo_mapping, target-scope query, batched show_repo.
 */
public class BzlmodBcrExtractorTest {

    // -------------------------------------------------------------------------
    // Shared test constants
    // -------------------------------------------------------------------------

    /** Bazel 7.1.0 — gates the BCR extraction path in BzlmodBcrExtractor. */
    private static final BazelVersion VERSION_7_1 = new BazelVersion(7, 1, 0);

    /** The Bazel target used across all tests. */
    private static final String TEST_TARGET = "//src/main:example";

    // -------------------------------------------------------------------------
    // Fixture catalogue — grouped by scenario
    // -------------------------------------------------------------------------

    /**
     * Canned command responses for each test scenario.
     *
     * <p>Each nested class corresponds to one scenario and holds the responses
     * that {@link BzlmodBcrExtractor#extractGraph()} issues in order:
     * <ol>
     *   <li>{@code MOD_GRAPH} — output of {@code bazel mod graph --output json}</li>
     *   <li>{@code REPO_MAPPING} — output of {@code bazel mod dump_repo_mapping ""}</li>
     *   <li>{@code TARGET_QUERY} — output of {@code bazel query kind(.*library, deps(target))}</li>
     *   <li>{@code SHOW_REPO_BATCH} — output of {@code bazel mod show_repo @@name~ ...}</li>
     * </ol>
     */
    static final class Fixtures {

        /**
         * One direct dep (protobuf@31.0) with one transitive dep (abseil-cpp@20240722.0).
         * Used to verify basic direct/transitive classification and ExternalId dedup set population.
         */
        static final class BasicDirectTransitive {
            static final String MOD_GRAPH =
                "{\n"
                + "  \"key\": \"<root>\",\n"
                + "  \"dependencies\": [\n"
                + "    {\n"
                + "      \"key\": \"protobuf@31.0\",\n"
                + "      \"dependencies\": [\n"
                + "        { \"key\": \"abseil-cpp@20240722.0\", \"dependencies\": [] }\n"
                + "      ]\n"
                + "    }\n"
                + "  ]\n"
                + "}";

            static final String REPO_MAPPING =
                "{\n"
                + "  \"protobuf\": \"protobuf~\",\n"
                + "  \"abseil-cpp\": \"abseil-cpp~\"\n"
                + "}";

            static final String TARGET_QUERY =
                "@@protobuf~//google/protobuf:lib\n"
                + "@@abseil-cpp~//absl/base:base\n";

            /** Block format: {@code ## @@name~:\n<rule body>} — split by BzlmodBcrExtractor on {@code ## @@}. */
            static final String SHOW_REPO_BATCH =
                "## @@protobuf~:\n"
                + "http_archive(\n"
                + "    name = \"protobuf~\",\n"
                + "    urls = [\"https://github.com/protocolbuffers/protobuf/archive/v31.0.tar.gz\"],\n"
                + ")\n"
                + "## @@abseil-cpp~:\n"
                + "http_archive(\n"
                + "    name = \"abseil-cpp~\",\n"
                + "    urls = [\"https://github.com/abseil/abseil-cpp/archive/20240722.0.tar.gz\"],\n"
                + ")\n";
        }

        /**
         * One direct dep (gflags@2.2.2) whose archive URL uses a {@code refs/tags/} path.
         * Used to verify that BzlmodBcrExtractor strips the prefix so the BOM version
         * is {@code v2.2.2}, not {@code refs/tags/v2.2.2}.
         */
        static final class RefsTagsVersion {
            static final String MOD_GRAPH =
                "{\n"
                + "  \"key\": \"<root>\",\n"
                + "  \"dependencies\": [\n"
                + "    { \"key\": \"gflags@2.2.2\", \"dependencies\": [] }\n"
                + "  ]\n"
                + "}";

            static final String REPO_MAPPING = "{ \"gflags\": \"gflags~\" }";

            static final String TARGET_QUERY = "@@gflags~//gflags:gflags\n";

            static final String SHOW_REPO_BATCH =
                "## @@gflags~:\n"
                + "http_archive(\n"
                + "    name = \"gflags~\",\n"
                + "    urls = [\"https://github.com/gflags/gflags/archive/refs/tags/v2.2.2.tar.gz\"],\n"
                + ")\n";
        }

        /**
         * One direct dep whose show_repo output contains a private (non-GitHub) URL.
         * Used to verify that deps with non-matchable URLs are excluded from the BOM.
         */
        static final class NonGithubUrl {
            static final String MOD_GRAPH =
                "{\n"
                + "  \"key\": \"<root>\",\n"
                + "  \"dependencies\": [\n"
                + "    { \"key\": \"internal-lib@1.0\", \"dependencies\": [] }\n"
                + "  ]\n"
                + "}";

            static final String REPO_MAPPING = "{ \"internal-lib\": \"internal-lib~\" }";

            static final String TARGET_QUERY = "@@internal-lib~//src:lib\n";

            static final String SHOW_REPO_BATCH =
                "## @@internal-lib~:\n"
                + "http_archive(\n"
                + "    name = \"internal-lib~\",\n"
                + "    urls = [\"https://registry.internal.corp/libs/internal-lib-1.0.tar.gz\"],\n"
                + ")\n";
        }

        /**
         * One direct dep whose show_repo output contains no URLs at all (e.g. local_path_repository).
         * Used to verify that deps with no URL are excluded from the BOM.
         */
        static final class NoUrl {
            static final String MOD_GRAPH =
                "{\n"
                + "  \"key\": \"<root>\",\n"
                + "  \"dependencies\": [\n"
                + "    { \"key\": \"local-dep@0.1\", \"dependencies\": [] }\n"
                + "  ]\n"
                + "}";

            static final String REPO_MAPPING = "{ \"local-dep\": \"local-dep~\" }";

            static final String TARGET_QUERY = "@@local-dep~//src:lib\n";

            static final String SHOW_REPO_BATCH =
                "## @@local-dep~:\n"
                + "local_path_repository(\n"
                + "    name = \"local-dep~\",\n"
                + "    path = \"/workspace/local-dep\",\n"
                + ")\n";
        }

        /**
         * Two direct deps (moduleA, moduleB) that both depend on a shared transitive dep.
         * Used to verify diamond-dependency handling: both parent edges recorded, subtree
         * traversed only once, shared dep not promoted to root.
         */
        static final class Diamond {
            static final String MOD_GRAPH =
                "{\n"
                + "  \"key\": \"<root>\",\n"
                + "  \"dependencies\": [\n"
                + "    {\n"
                + "      \"key\": \"moduleA@1.0\",\n"
                + "      \"dependencies\": [\n"
                + "        { \"key\": \"shared@3.0\", \"dependencies\": [] }\n"
                + "      ]\n"
                + "    },\n"
                + "    {\n"
                + "      \"key\": \"moduleB@2.0\",\n"
                + "      \"dependencies\": [\n"
                + "        { \"key\": \"shared@3.0\", \"dependencies\": [] }\n"
                + "      ]\n"
                + "    }\n"
                + "  ]\n"
                + "}";

            static final String REPO_MAPPING =
                "{\n"
                + "  \"moduleA\": \"moduleA~\",\n"
                + "  \"moduleB\": \"moduleB~\",\n"
                + "  \"shared\": \"shared~\"\n"
                + "}";

            static final String TARGET_QUERY =
                "@@moduleA~//src:lib\n"
                + "@@moduleB~//src:lib\n"
                + "@@shared~//src:lib\n";

            static final String SHOW_REPO_BATCH =
                "## @@moduleA~:\n"
                + "http_archive(\n"
                + "    urls = [\"https://github.com/example/moduleA/archive/v1.0.tar.gz\"],\n"
                + ")\n"
                + "## @@moduleB~:\n"
                + "http_archive(\n"
                + "    urls = [\"https://github.com/example/moduleB/archive/v2.0.tar.gz\"],\n"
                + ")\n"
                + "## @@shared~:\n"
                + "http_archive(\n"
                + "    urls = [\"https://github.com/example/shared/archive/v3.0.tar.gz\"],\n"
                + ")\n";
        }

        /**
         * One direct dep (glog@0.6.0) used to verify the per-module show_repo fallback path.
         * The batched call returns empty; the per-module call returns the show_repo block.
         */
        static final class BatchFallback {
            static final String MOD_GRAPH =
                "{\n"
                + "  \"key\": \"<root>\",\n"
                + "  \"dependencies\": [\n"
                + "    { \"key\": \"glog@0.6.0\", \"dependencies\": [] }\n"
                + "  ]\n"
                + "}";

            static final String REPO_MAPPING = "{ \"glog\": \"glog~\" }";

            static final String TARGET_QUERY = "@@glog~//glog:glog\n";

            /** Returned by the per-module fallback call (batch returns empty for this scenario). */
            static final String SHOW_REPO_PER_MODULE =
                "## @@glog~:\n"
                + "http_archive(\n"
                + "    name = \"glog~\",\n"
                + "    urls = [\"https://github.com/google/glog/archive/v0.6.0.tar.gz\"],\n"
                + ")\n";
        }
    }

    // -------------------------------------------------------------------------
    // Tests — basic direct/transitive graph
    // -------------------------------------------------------------------------

    @Test
    public void extractGraph_basicDirectAndTransitive_graphHasCorrectStructure() {
        StubBazelCommandExecutor stub = new StubBazelCommandExecutor();
        stub.addModResponse(Fixtures.BasicDirectTransitive.MOD_GRAPH);
        stub.addModResponse(Fixtures.BasicDirectTransitive.REPO_MAPPING);
        stub.addQueryResponse(Fixtures.BasicDirectTransitive.TARGET_QUERY);
        stub.addModResponse(Fixtures.BasicDirectTransitive.SHOW_REPO_BATCH);

        DependencyGraph graph = new BzlmodBcrExtractor(stub, VERSION_7_1, TEST_TARGET).extractGraph();

        Set<Dependency> rootDeps = graph.getRootDependencies();
        assertEquals(1, rootDeps.size(), "Only protobuf should be a root (direct) dependency");

        Dependency protobuf = rootDeps.iterator().next();
        assertEquals("protocolbuffers/protobuf", protobuf.getExternalId().getName());
        assertEquals("v31.0", protobuf.getExternalId().getVersion());

        Set<Dependency> protobufChildren = graph.getChildrenForParent(protobuf);
        assertEquals(1, protobufChildren.size(), "abseil-cpp should be the sole transitive dep under protobuf");

        Dependency abseil = protobufChildren.iterator().next();
        assertEquals("abseil/abseil-cpp", abseil.getExternalId().getName());
        assertEquals("20240722.0", abseil.getExternalId().getVersion());
    }

    @Test
    public void extractGraph_basicDirectAndTransitive_externalIdSetPopulatedForBothDeps() {
        StubBazelCommandExecutor stub = new StubBazelCommandExecutor();
        stub.addModResponse(Fixtures.BasicDirectTransitive.MOD_GRAPH);
        stub.addModResponse(Fixtures.BasicDirectTransitive.REPO_MAPPING);
        stub.addQueryResponse(Fixtures.BasicDirectTransitive.TARGET_QUERY);
        stub.addModResponse(Fixtures.BasicDirectTransitive.SHOW_REPO_BATCH);

        BzlmodBcrExtractor extractor = new BzlmodBcrExtractor(stub, VERSION_7_1, TEST_TARGET);
        extractor.extractGraph();

        Set<ExternalId> resolved = extractor.getResolvedExternalIds();
        assertEquals(2, resolved.size(), "Both protobuf and abseil-cpp should be in the resolved set");

        assertTrue(resolved.stream().anyMatch(id ->
            "protocolbuffers/protobuf".equals(id.getName()) && "v31.0".equals(id.getVersion())),
            "Resolved set must contain protobuf ExternalId");
        assertTrue(resolved.stream().anyMatch(id ->
            "abseil/abseil-cpp".equals(id.getName()) && "20240722.0".equals(id.getVersion())),
            "Resolved set must contain abseil-cpp ExternalId");
    }

    // -------------------------------------------------------------------------
    // Tests — version normalization
    // -------------------------------------------------------------------------

    @Test
    public void extractGraph_refsTagsVersionInUrl_versionStrippedToTagName() {
        StubBazelCommandExecutor stub = new StubBazelCommandExecutor();
        stub.addModResponse(Fixtures.RefsTagsVersion.MOD_GRAPH);
        stub.addModResponse(Fixtures.RefsTagsVersion.REPO_MAPPING);
        stub.addQueryResponse(Fixtures.RefsTagsVersion.TARGET_QUERY);
        stub.addModResponse(Fixtures.RefsTagsVersion.SHOW_REPO_BATCH);

        DependencyGraph graph = new BzlmodBcrExtractor(stub, VERSION_7_1, TEST_TARGET).extractGraph();

        Set<Dependency> rootDeps = graph.getRootDependencies();
        assertEquals(1, rootDeps.size());

        Dependency gflags = rootDeps.iterator().next();
        assertEquals("gflags/gflags", gflags.getExternalId().getName());
        assertEquals("v2.2.2", gflags.getExternalId().getVersion(),
            "refs/tags/ prefix must be stripped — version should be 'v2.2.2' not 'refs/tags/v2.2.2'");
    }

    // -------------------------------------------------------------------------
    // Tests — non-GitHub URL / no URL → dep excluded
    // -------------------------------------------------------------------------

    @Test
    public void extractGraph_nonGithubUrl_depExcludedFromGraph() {
        StubBazelCommandExecutor stub = new StubBazelCommandExecutor();
        stub.addModResponse(Fixtures.NonGithubUrl.MOD_GRAPH);
        stub.addModResponse(Fixtures.NonGithubUrl.REPO_MAPPING);
        stub.addQueryResponse(Fixtures.NonGithubUrl.TARGET_QUERY);
        stub.addModResponse(Fixtures.NonGithubUrl.SHOW_REPO_BATCH);

        DependencyGraph graph = new BzlmodBcrExtractor(stub, VERSION_7_1, TEST_TARGET).extractGraph();

        assertTrue(graph.getRootDependencies().isEmpty(),
            "A dep with a non-GitHub URL must be excluded from the BOM");
    }

    @Test
    public void extractGraph_noUrlInShowRepo_depExcludedFromGraph() {
        StubBazelCommandExecutor stub = new StubBazelCommandExecutor();
        stub.addModResponse(Fixtures.NoUrl.MOD_GRAPH);
        stub.addModResponse(Fixtures.NoUrl.REPO_MAPPING);
        stub.addQueryResponse(Fixtures.NoUrl.TARGET_QUERY);
        stub.addModResponse(Fixtures.NoUrl.SHOW_REPO_BATCH);

        DependencyGraph graph = new BzlmodBcrExtractor(stub, VERSION_7_1, TEST_TARGET).extractGraph();

        assertTrue(graph.getRootDependencies().isEmpty(),
            "A dep with no URL in show_repo output must be excluded from the BOM");
    }

    // -------------------------------------------------------------------------
    // Tests — empty / degenerate mod graph
    // -------------------------------------------------------------------------

    @Test
    public void extractGraph_emptyModGraphOutput_returnsEmptyGraph() {
        StubBazelCommandExecutor stub = new StubBazelCommandExecutor();
        stub.addEmptyModResponse();

        DependencyGraph graph = new BzlmodBcrExtractor(stub, VERSION_7_1, TEST_TARGET).extractGraph();

        assertTrue(graph.getRootDependencies().isEmpty(),
            "Empty mod graph output must yield an empty dependency graph");
    }

    @Test
    public void extractGraph_rootOnlyModGraph_returnsEmptyGraph() {
        StubBazelCommandExecutor stub = new StubBazelCommandExecutor();
        stub.addModResponse("{ \"key\": \"<root>\", \"dependencies\": [] }");

        DependencyGraph graph = new BzlmodBcrExtractor(stub, VERSION_7_1, TEST_TARGET).extractGraph();

        assertTrue(graph.getRootDependencies().isEmpty());
    }

    // -------------------------------------------------------------------------
    // Tests — diamond dependency
    // -------------------------------------------------------------------------

    @Test
    public void extractGraph_diamondDependency_sharedDepHasBothParentEdges() {
        StubBazelCommandExecutor stub = new StubBazelCommandExecutor();
        stub.addModResponse(Fixtures.Diamond.MOD_GRAPH);
        stub.addModResponse(Fixtures.Diamond.REPO_MAPPING);
        stub.addQueryResponse(Fixtures.Diamond.TARGET_QUERY);
        stub.addModResponse(Fixtures.Diamond.SHOW_REPO_BATCH);

        DependencyGraph graph = new BzlmodBcrExtractor(stub, VERSION_7_1, TEST_TARGET).extractGraph();

        Set<Dependency> rootDeps = graph.getRootDependencies();
        assertEquals(2, rootDeps.size(), "moduleA and moduleB must be direct (root) dependencies");

        boolean sharedAtRoot = rootDeps.stream()
            .anyMatch(d -> "example/shared".equals(d.getExternalId().getName()));
        assertFalse(sharedAtRoot, "shared@3.0 is transitive — it must not be a root dependency");

        for (Dependency direct : rootDeps) {
            Set<Dependency> children = graph.getChildrenForParent(direct);
            assertEquals(1, children.size(),
                "Each direct dep must have exactly one child (shared@3.0)");
            assertEquals("example/shared", children.iterator().next().getExternalId().getName());
        }
    }

    // -------------------------------------------------------------------------
    // Tests — batched show_repo fallback to per-module
    // -------------------------------------------------------------------------

    @Test
    public void extractGraph_batchedShowRepoFails_perModuleFallbackResolvesAllDeps() {
        StubBazelCommandExecutor stub = new StubBazelCommandExecutor();
        stub.addModResponse(Fixtures.BatchFallback.MOD_GRAPH);
        stub.addModResponse(Fixtures.BatchFallback.REPO_MAPPING);
        stub.addQueryResponse(Fixtures.BatchFallback.TARGET_QUERY);
        stub.addEmptyModResponse();                                    // batched show_repo → empty → triggers fallback
        stub.addModResponse(Fixtures.BatchFallback.SHOW_REPO_PER_MODULE); // per-module show_repo for glog

        DependencyGraph graph = new BzlmodBcrExtractor(stub, VERSION_7_1, TEST_TARGET).extractGraph();

        Set<Dependency> rootDeps = graph.getRootDependencies();
        assertEquals(1, rootDeps.size(), "glog must be resolved via the per-module fallback path");

        Dependency glog = rootDeps.iterator().next();
        assertEquals("google/glog", glog.getExternalId().getName());
        assertEquals("v0.6.0", glog.getExternalId().getVersion());
    }

    // -------------------------------------------------------------------------
    // Stub executor
    // -------------------------------------------------------------------------

    /**
     * A test double for {@link BazelCommandExecutor} that returns pre-loaded responses
     * from a queue, one per call, without invoking any real Bazel process.
     *
     * <p>Mod commands ({@code mod graph}, {@code mod dump_repo_mapping}, {@code mod show_repo})
     * consume from {@code modResponses}. Query commands consume from {@code queryResponses}.
     *
     * <p>Passing {@code null} to the super constructor is safe because none of the overridden
     * methods call {@code super} — they return pre-loaded values directly.
     */
    private static class StubBazelCommandExecutor extends BazelCommandExecutor {

        private final Queue<Optional<String>> modResponses   = new LinkedList<>();
        private final Queue<Optional<String>> queryResponses = new LinkedList<>();

        StubBazelCommandExecutor() {
            super(null, null, null);
        }

        void addModResponse(String response) {
            modResponses.add(Optional.of(response));
        }

        void addEmptyModResponse() {
            modResponses.add(Optional.empty());
        }

        void addQueryResponse(String response) {
            queryResponses.add(Optional.of(response));
        }

        @Override
        public Optional<String> executeModCommandToString(List<String> args) {
            return modResponses.isEmpty() ? Optional.empty() : modResponses.poll();
        }

        @Override
        public Optional<String> executeQueryToString(List<String> args) throws ExecutableFailedException {
            return queryResponses.isEmpty() ? Optional.empty() : queryResponses.poll();
        }
    }
}

