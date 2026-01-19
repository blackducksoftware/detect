package com.blackduck.integration.detect.battery.detector;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detect.battery.util.DetectorBatteryTestRunner;
import com.blackduck.integration.detect.configuration.DetectProperties;

@Tag("battery")
class BazelBattery {
    private static final String BAZEL_MAVEN_INSTALL_OUTPUT_RESOURCE = "bazel-maven-install-query.xout";
    private static final String BAZEL_HASKELL_CABAL_LIBRARY_OUTPUT_RESOURCE = "bazel-haskell-cabal-library-query.xout";
    private static final String BAZEL_MAVEN_JAR_OUTPUT1_RESOURCE = "bazel-maven-jar-query1.xout";
    private static final String BAZEL_MAVEN_JAR_OUTPUT2_RESOURCE = "bazel-maven-jar-query2.xout";
    private static final String BAZEL_MAVEN_JAR_OUTPUT3_RESOURCE = "bazel-maven-jar-query3.xout";
    private static final String BAZEL_HTTP_ARCHIVE_GITHUB_OUTPUT1_RESOURCE = "bazel-http-archive-query1.xout";
    private static final String BAZEL_HTTP_ARCHIVE_GITHUB_OUTPUT2_RESOURCE = "bazel-http-archive-query2_and_3.xout";
    private static final String BAZEL_HTTP_ARCHIVE_GITHUB_OUTPUT3_RESOURCE = "bazel-http-archive-query4.xout";
    private static final String BAZEL_HTTP_ARCHIVE_GITHUB_OUTPUT4_RESOURCE = "bazel-http-archive-query5.xout";
    private static final String EMPTY_OUTPUT_RESOURCE = "empty.xout";

    // BZLMOD V2 Graph Probing + HTTP Pipeline Test Resources
    private static final String BAZEL_V2_BZLMOD_PROBE_MAVEN_INSTALL_RESOURCE = "probe-maven-install.xout";
    private static final String BAZEL_V2_BZLMOD_PROBE_MAVEN_JAR_RESOURCE = "probe-maven-jar.xout";
    private static final String BAZEL_V2_BZLMOD_PROBE_HASKELL_RESOURCE = "probe-haskell-cabal.xout";
    private static final String BAZEL_V2_BZLMOD_PROBE_HTTP_RESOURCE = "probe-http-libraries.xout";
    private static final String BAZEL_V2_BZLMOD_PROBE_CLASSIFY_GFLAGS_RESOURCE = "probe-classify-gflags.xout";
    private static final String BAZEL_V2_BZLMOD_HTTP_INITIAL_QUERY_RESOURCE = "http-initial-query.xout";
    private static final String BAZEL_V2_BZLMOD_HTTP_SHOW_REPO_GFLAGS_RESOURCE = "http-show-repo-gflags.xout";
    private static final String BAZEL_V2_BZLMOD_HTTP_SHOW_REPO_GLOG_RESOURCE = "http-show-repo-glog.xout";

    @Test
    void bazelMavenInstall() {
        DetectorBatteryTestRunner test = new DetectorBatteryTestRunner("bazel-maven-install", "bazel/maven-install");
        test.withToolsValue("BAZEL");
        test.property("detect.bazel.target", "//tests/integration:ArtifactExclusionsTest");
        test.property("detect.bazel.workspace.rules", "MAVEN_INSTALL");
        test.property("detect.bazel.era", "LEGACY");
        test.executableFromResourceFiles(DetectProperties.DETECT_BAZEL_PATH, BAZEL_MAVEN_INSTALL_OUTPUT_RESOURCE);
        test.sourceDirectoryNamed("bazel-maven-install");
        test.sourceFileNamed("WORKSPACE");
        test.expectBdioResources();
        test.run();
    }

    @Test
    void bazelMavenInstallComplexCoordinates() {
        DetectorBatteryTestRunner test = new DetectorBatteryTestRunner("bazel-maven-install-complex", "bazel/maven-install-complex");
        test.withToolsValue("BAZEL");
        test.property("detect.bazel.target", "//tests/integration:ArtifactExclusionsTest");
        test.property("detect.bazel.workspace.rules", "MAVEN_INSTALL");
        test.property("detect.bazel.era", "LEGACY");
        test.executableFromResourceFiles(DetectProperties.DETECT_BAZEL_PATH, BAZEL_MAVEN_INSTALL_OUTPUT_RESOURCE);
        test.sourceDirectoryNamed("bazel-maven-install-complex");
        test.sourceFileNamed("WORKSPACE");
        test.expectBdioResources();
        test.run();
    }

    @Test
    void bazelHaskellCabalLibrary() {
        DetectorBatteryTestRunner test = new DetectorBatteryTestRunner("bazel-haskell-cabal-library", "bazel/haskell-cabal-library");
        test.withToolsValue("BAZEL");
        test.property("detect.bazel.target", "//cat_hs/lib/args:args");
        test.property("detect.bazel.workspace.rules", "HASKELL_CABAL_LIBRARY");
        test.property("detect.bazel.era", "LEGACY");
        test.executableFromResourceFiles(DetectProperties.DETECT_BAZEL_PATH, BAZEL_HASKELL_CABAL_LIBRARY_OUTPUT_RESOURCE);
        test.sourceDirectoryNamed("bazel-haskell-cabal-library");
        test.sourceFileNamed("WORKSPACE");
        test.expectBdioResources();
        test.run();
    }

    @Test
    void bazelMavenJar() {
        DetectorBatteryTestRunner test = new DetectorBatteryTestRunner("bazel-maven-jar", "bazel/maven-jar");
        test.withToolsValue("BAZEL");
        test.property("detect.bazel.target", "//:ProjectRunner");
        test.property("detect.bazel.workspace.rules", "MAVEN_JAR");
        test.property("detect.bazel.era", "LEGACY");
        test.executableFromResourceFiles(DetectProperties.DETECT_BAZEL_PATH, BAZEL_MAVEN_JAR_OUTPUT1_RESOURCE, BAZEL_MAVEN_JAR_OUTPUT2_RESOURCE, BAZEL_MAVEN_JAR_OUTPUT3_RESOURCE);
        test.sourceDirectoryNamed("bazel-maven-jar");
        test.sourceFileNamed("WORKSPACE");
        test.expectBdioResources();
        test.run();
    }

    @Test
    void bazelHaskellCabalLibraryAll() {
        DetectorBatteryTestRunner test = new DetectorBatteryTestRunner("bazel-haskell-cabal-library-all", "bazel/haskell-cabal-library-all");
        test.withToolsValue("BAZEL");
        test.property("detect.bazel.target", "//cat_hs/lib/args:args");
        test.property("detect.bazel.workspace.rules", "ALL");
        test.property("detect.bazel.era", "LEGACY");
        test.executableFromResourceFiles(
            DetectProperties.DETECT_BAZEL_PATH,
            EMPTY_OUTPUT_RESOURCE,
            EMPTY_OUTPUT_RESOURCE,
            BAZEL_HASKELL_CABAL_LIBRARY_OUTPUT_RESOURCE,
            EMPTY_OUTPUT_RESOURCE
        );
        test.sourceDirectoryNamed("bazel-haskell-cabal-library-all");
        test.sourceFileNamed("WORKSPACE");
        test.expectBdioResources();
        test.run();
    }

    @Test
    void bazelHttpArchiveGithubUrl() {
        DetectorBatteryTestRunner test = new DetectorBatteryTestRunner("bazel-http-archive-github", "bazel/http-archive-github");
        test.withToolsValue("BAZEL");
        test.property("detect.bazel.target", "//:bd_bazel");
        test.property("detect.bazel.workspace.rules", "HTTP_ARCHIVE");
        test.property("detect.bazel.era", "LEGACY");
        test.executableFromResourceFiles(
            DetectProperties.DETECT_BAZEL_PATH,
            BAZEL_HTTP_ARCHIVE_GITHUB_OUTPUT1_RESOURCE,  // query1.xout - Initial library query
            BAZEL_HTTP_ARCHIVE_GITHUB_OUTPUT2_RESOURCE,  // query2_and_3.xout - bazel_tools
            BAZEL_HTTP_ARCHIVE_GITHUB_OUTPUT4_RESOURCE,  // query5.xout - gflags (comes first in LinkedHashSet order, ref: IntermediateStepDeDupLines)
            BAZEL_HTTP_ARCHIVE_GITHUB_OUTPUT3_RESOURCE   // query4.xout - glog
        );
        test.sourceDirectoryNamed("bazel-http-archive-github");
        test.sourceFileNamed("WORKSPACE");
        test.expectBdioResources();
        test.run();
    }

    @Test
    void bazelV2GraphProbingHttpBzlmod() {
        DetectorBatteryTestRunner test = new DetectorBatteryTestRunner("bazel-v2-graph-probing-http-bzlmod", "bazel/v2-graph-probing-http-bzmod");
        test.withToolsValue("BAZEL");
        test.property("detect.bazel.target", "//:bd_bazel_bzlmod");
        // NO workspace.rules property! This forces V2 to use graph probing
        test.property("detect.bazel.era", "BZLMOD");
        test.executableFromResourceFiles(
            DetectProperties.DETECT_BAZEL_PATH,
            // Graph Probing Phase (5 queries to determine which pipelines to run)
            BAZEL_V2_BZLMOD_PROBE_MAVEN_INSTALL_RESOURCE,  // Probe for maven_install -> empty
            BAZEL_V2_BZLMOD_PROBE_MAVEN_JAR_RESOURCE,      // Probe for maven_jar -> empty
            BAZEL_V2_BZLMOD_PROBE_HASKELL_RESOURCE,        // Probe for haskell_cabal_library -> empty
            BAZEL_V2_BZLMOD_PROBE_HTTP_RESOURCE,           // Probe for HTTP archives -> finds gflags & glog
            BAZEL_V2_BZLMOD_PROBE_CLASSIFY_GFLAGS_RESOURCE, // mod show_repo to classify gflags as HTTP family
            // HTTP Pipeline Execution Phase (3 queries to extract dependencies)
            BAZEL_V2_BZLMOD_HTTP_INITIAL_QUERY_RESOURCE,   // Initial library query
            BAZEL_V2_BZLMOD_HTTP_SHOW_REPO_GFLAGS_RESOURCE, // bazel mod show_repo com_github_gflags_gflags
            BAZEL_V2_BZLMOD_HTTP_SHOW_REPO_GLOG_RESOURCE    // bazel mod show_repo glog
        );
        test.sourceDirectoryNamed("bazel-v2-graph-probing-http-bzlmod");
        test.sourceFileNamed("MODULE.bazel");  // BZLMOD uses MODULE.bazel instead of WORKSPACE
        test.expectBdioResources();
        test.run();
    }
}
