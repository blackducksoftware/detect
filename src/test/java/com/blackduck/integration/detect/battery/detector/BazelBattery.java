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
}
