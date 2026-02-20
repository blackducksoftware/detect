package com.blackduck.integration.detectable.detectables.bazel.functional.bazel.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.blackduck.integration.detectable.detectables.bazel.v2.BazelEnvironmentAnalyzer;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.gson.Gson;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.detectables.bazel.DependencySource;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.Pipeline;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.Pipelines;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelVariableSubstitutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.HaskellCabalLibraryJsonProtoParser;
import com.blackduck.integration.exception.IntegrationException;

class PipelinesTest {
    private static final List<String> MAVEN_INSTALL_STANDARD_BAZEL_COMMAND_ARGS = Arrays.asList(
        "cquery",
        "--noimplicit_deps",
        "kind(j.*import, deps(/:testTarget))",
        "--output",
        "build"
    );
    private static final List<String> HASKELL_CABAL_LIBRARY_STANDARD_BAZEL_COMMAND_ARGS = Arrays.asList(
        "cquery",
        "--noimplicit_deps",
        "kind(haskell_cabal_library, deps(/:testTarget))",
        "--output",
        "jsonproto"
    );
    private static final String MAVEN_INSTALL_CQUERY_OUTPUT_SIMPLE = createStandardOutput(
        "# /root/.cache/bazel/_bazel_root/896e039de1e50d7e2de0b14a9acf4028/external/exclusion_testing/BUILD:41:1",
        "jvm_import(",
        "  name = \"com_google_guava_guava\",",
        "  tags = [\"maven_coordinates=com.google.guava:guava:27.0-jre\"], ",
        "  jars = [\"@exclusion_testing//:v1/https/repo1.maven.org/maven2/com/google/guava/guava/27.0-jre/guava-27.0-jre.jar\"],",
        "  deps = [\"@exclusion_testing//:com_google_guava_listenablefuture\", \"@exclusion_testing//:com_google_code_findbugs_jsr305\", \"@exclusion_testing//:com_google_guava_failureaccess\", \"@exclusion_testing//:com_google_errorprone_error_prone_annotations\", \"@exclusion_testing//:org_checkerframework_checker_qual\"],",
        ")",
        "",
        "# /root/.cache/bazel/_bazel_root/896e039de1e50d7e2de0b14a9acf4028/external/maven/BUILD:103:1",
        "jvm_import(",
        "  name = \"org_hamcrest_hamcrest_core\",",
        "  tags = [\"maven_coordinates=org.hamcrest:hamcrest-core:2.1\"],  ",
        "  jars = [\"@maven//:v1/https/jcenter.bintray.com/org/hamcrest/hamcrest-core/2.1/hamcrest-core-2.1.jar\"],",
        "  deps = [\"@maven//:org_hamcrest_hamcrest\"],",
        ")",
        "",
        "# /root/.cache/bazel/_bazel_root/896e039de1e50d7e2de0b14a9acf4028/external/maven/BUILD:115:1",
        "jvm_import(",
        "  name = \"org_hamcrest_hamcrest\",",
        "  tags = [\"maven_coordinates=org.hamcrest:hamcrest:2.1\"], ",
        "  jars = [\"@maven//:v1/https/jcenter.bintray.com/org/hamcrest/hamcrest/2.1/hamcrest-2.1.jar\"],",
        "  deps = [],",
        ")",
        "",
        "# /root/.cache/bazel/_bazel_root/896e039de1e50d7e2de0b14a9acf4028/external/exclusion_testing/BUILD:30:1",
        "jvm_import(",
        "  name = \"com_google_guava_failureaccess\",",
        "  tags = [\"maven_coordinates=com.google.guava:failureaccess:1.0\"],",
        "  jars = [\"@exclusion_testing//:v1/https/repo1.maven.org/maven2/com/google/guava/failureaccess/1.0/failureaccess-1.0.jar\"],",
        "  deps = [],",
        ")",
        "",
        "# /root/.cache/bazel/_bazel_root/896e039de1e50d7e2de0b14a9acf4028/external/exclusion_testing/BUILD:68:1",
        "jvm_import(",
        "  name = \"org_checkerframework_checker_qual\",",
        "  tags = [\"maven_coordinates=org.checkerframework:checker-qual:2.5.2\"],",
        "  jars = [\"@exclusion_testing//:v1/https/repo1.maven.org/maven2/org/checkerframework/checker-qual/2.5.2/checker-qual-2.5.2.jar\"],",
        "  deps = [],",
        ")",
        "",
        "# /root/.cache/bazel/_bazel_root/896e039de1e50d7e2de0b14a9acf4028/external/exclusion_testing/BUILD:19:1",
        "jvm_import(",
        "  name = \"com_google_errorprone_error_prone_annotations\", ",
        "  tags = [\"maven_coordinates=com.google.errorprone:error_prone_annotations:2.2.0\"],",
        "  jars = [\"@exclusion_testing//:v1/https/repo1.maven.org/maven2/com/google/errorprone/error_prone_annotations/2.2.0/error_prone_annotations-2.2.0.jar\"],",
        "  deps = [],",
        ")",
        "",
        "# /root/.cache/bazel/_bazel_root/896e039de1e50d7e2de0b14a9acf4028/external/exclusion_testing/BUILD:57:1",
        "jvm_import(",
        "  name = \"com_google_guava_listenablefuture\",",
        "  tags = [\"maven_coordinates=com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava\"],",
        "  jars = [\"@exclusion_testing//:v1/https/repo1.maven.org/maven2/com/google/guava/listenablefuture/9999.0-empty-to-avoid-conflict-with-guava/listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar\"],",
        "  deps = [],",
        ")",
        "# /root/.cache/bazel/_bazel_root/896e039de1e50d7e2de0b14a9acf4028/external/exclusion_testing/BUILD:8:1",
        "jvm_import(",
        "  name = \"com_google_code_findbugs_jsr305\",",
        "  tags = [\"maven_coordinates=com.google.code.findbugs:jsr305:3.0.2\"],",
        "  jars = [\"@exclusion_testing//:v1/https/repo1.maven.org/maven2/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar\"],",
        "  deps = [],",
        ")"
    );

    private static final String MAVEN_INSTALL_OUTPUT_MIXED_TAGS = createStandardOutput(
        "java_import(",
        "  name = \"sso-adminsdk\",",
        "  tags = [\"__SOME_OTHER_TAG__\"],",
        "  generator_name = \"sso-adminsdk\",",
        "  generator_function = \"java_import\",",
        "  generator_location = \"/home/dail/test/b-cls-bazel/2e7559aa3afc160ade8ae4cb99d56da9/external/rd_platform_services/BUILD.bazel:238\",",
        "  jars = [\"@rd_platform_services//:lin64/jars/sso-adminsdk.jar\"],",
        ")",
        "",
        "# /home/dail/test/b-cls-bazel/2e7559aa3afc160ade8ae4cb99d56da9/external/thing_cis/BUILD.bazel:20:5",
        "java_import(",
        "  name = \"thing-common-client\",",
        "  tags = [\"__SOME_OTHER_TAG__\", \"maven_coordinates=com.company.thing:thing-common-client:2.100.0\"],",
        "  generator_name = \"thing-common-client\",",
        "  generator_function = \"java_import\",",
        "  generator_location = \"/home/dail/test/b-cls-bazel/2e7559aa3afc160ade8ae4cb99d56da9/external/thing_cis/BUILD.bazel:20\",",
        "  jars = [\"@thing_cis//:java-toolkit/runtime/thing-common-client-2.100.0.jar\"],",
        "  srcjar = \"@thing_cis//:java-toolkit/runtime/thing-common-client-2.100.0-sources.jar\",",
        ")",
        "",
        "# /home/dail/test/b-cls-bazel/2e7559aa3afc160ade8ae4cb99d56da9/external/cls_maven_stubs/BUILD:41:1",
        "jvm_import(",
        "  name = \"javax_servlet_javax_servlet_api\",",
        "  tags = [\"maven_coordinates=javax.servlet:javax.servlet-api:3.0.1\"],",
        "  jars = [\"@cls_maven_stubs//:v1/https/build-artifactory.eng.company.com/artifactory/maven/javax/servlet/javax.servlet-api/3.0.1/javax.servlet-api-3.0.1.jar\"],",
        "  srcjar = \"@cls_maven_stubs//:v1/https/build-artifactory.eng.company.com/artifactory/maven/javax/servlet/javax.servlet-api/3.0.1/javax.servlet-api-3.0.1-sources.jar\",",
        "  deps = [],",
        "  neverlink = True,",
        ")"
    );

    private static final String MAVEN_INSTALL_OUTPUT_MIXED_TAGS_REVERSED_ORDER = createStandardOutput(
        "java_import(",
        "  name = \"thing-common-client\",",
        "  tags = [\"maven_coordinates=com.company.thing:thing-common-client:2.100.0\", \"__SOME_OTHER_TAG__\"],",
        "  generator_name = \"thing-common-client\",",
        "  generator_function = \"java_import\",",
        "  generator_location = \"/home/dail/test/b-cls-bazel/2e7559aa3afc160ade8ae4cb99d56da9/external/thing_cis/BUILD.bazel:20\",",
        "  jars = [\"@thing_cis//:java-toolkit/runtime/thing-common-client-2.100.0.jar\"],",
        "  srcjar = \"@thing_cis//:java-toolkit/runtime/thing-common-client-2.100.0-sources.jar\",",
        ")"
    );

    private static final String HASKELL_CABAL_LIBRARY_JSONPROTO = createStandardOutput(
        "{",
        "  \"results\": [{",
        "    \"target\": {",
        "      \"type\": \"RULE\",",
        "      \"rule\": {",
        "        \"name\": \"@stackage//:optparse-applicative\",",
        "        \"ruleClass\": \"haskell_cabal_library\",",
        "        \"location\": \"/root/.cache/bazel/_bazel_root/cc59a4f96db0d7083a7d7596a883ccd0/external/stackage/BUILD.bazel:528:1\",",
        "        \"attribute\": [{",
        "          \"name\": \"generator_location\",",
        "          \"type\": \"STRING\",",
        "          \"stringValue\": \"\",",
        "          \"explicitlySpecified\": false,",
        "          \"nodep\": false",
        "        }, {",
        "          \"name\": \"name\",",
        "          \"type\": \"STRING\",",
        "          \"stringValue\": \"optparse-applicative\",",
        "          \"explicitlySpecified\": true,",
        "          \"nodep\": false",
        "        }, {",
        "          \"name\": \"version\",",
        "          \"type\": \"STRING\",",
        "          \"stringValue\": \"0.14.3.0\",",
        "          \"explicitlySpecified\": true,",
        "          \"nodep\": false",
        "        }, {",
        "          \"name\": \"$rule_implementation_hash\",",
        "          \"type\": \"STRING\",",
        "          \"stringValue\": \"1b75caf6fa81d9febe4bf63485fc3b7e3a1c6bef5637ec66e6ffbf9590a8c41f\"",
        "        }],",
        "        \"ruleInput\": [\"@stackage//:ansi-wl-pprint\", \"@stackage//:base\", \"@stackage//:transformers\", \"@stackage//:transformers-compat\"]",
        "      }",
        "    },",
        "    \"configuration\": {",
        "      \"checksum\": \"113f3ed6a7eba369dbe1453fe1da149ce5b6faa1129ed584fd4ad044389cc463\"",
        "    }",
        "  }]",
        "}"
    );

    @Test
    void testMavenInstall() throws IntegrationException, ExecutableFailedException {
        Assumptions.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        List<Dependency> dependencies = doTest(DependencySource.MAVEN_INSTALL, MAVEN_INSTALL_STANDARD_BAZEL_COMMAND_ARGS, null, MAVEN_INSTALL_CQUERY_OUTPUT_SIMPLE);
        assertEquals(8, dependencies.size());
        int foundCount = 0;
        for (Dependency dependency : dependencies) {
            if ("com.google.guava".equals(dependency.getExternalId().getGroup()) &&
                "guava".equals(dependency.getExternalId().getName()) &&
                "27.0-jre".equals(dependency.getExternalId().getVersion())) {
                foundCount++;
            }
            if ("com.google.code.findbugs".equals(dependency.getExternalId().getGroup()) &&
                "jsr305".equals(dependency.getExternalId().getName()) &&
                "3.0.2".equals(dependency.getExternalId().getVersion())) {
                foundCount++;
            }
        }
        assertEquals(2, foundCount);
    }

    @Test
    void testMavenInstallMixedTags() throws IntegrationException, ExecutableFailedException {
        Assumptions.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        List<Dependency> dependencies = doTest(DependencySource.MAVEN_INSTALL, MAVEN_INSTALL_STANDARD_BAZEL_COMMAND_ARGS, null, MAVEN_INSTALL_OUTPUT_MIXED_TAGS);
        assertEquals(2, dependencies.size());
        int foundCount = 0;
        for (Dependency dependency : dependencies) { //TODO: Factor out into a assertDependency();
            if ("com.company.thing".equals(dependency.getExternalId().getGroup()) &&
                "thing-common-client".equals(dependency.getExternalId().getName()) &&
                "2.100.0".equals(dependency.getExternalId().getVersion())) {
                foundCount++;
            }
            if ("javax.servlet".equals(dependency.getExternalId().getGroup()) &&
                "javax.servlet-api".equals(dependency.getExternalId().getName()) &&
                "3.0.1".equals(dependency.getExternalId().getVersion())) {
                foundCount++;
            }
        }
        assertEquals(2, foundCount);
    }

    @Test
    void testMavenInstallMixedTagsReversedOrder() throws IntegrationException, ExecutableFailedException {
        Assumptions.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        List<Dependency> dependencies = doTest(DependencySource.MAVEN_INSTALL, MAVEN_INSTALL_STANDARD_BAZEL_COMMAND_ARGS, null, MAVEN_INSTALL_OUTPUT_MIXED_TAGS_REVERSED_ORDER);
        assertEquals(1, dependencies.size());
        int foundCount = 0;
        for (Dependency dependency : dependencies) {
            if ("com.company.thing".equals(dependency.getExternalId().getGroup()) &&
                "thing-common-client".equals(dependency.getExternalId().getName()) &&
                "2.100.0".equals(dependency.getExternalId().getVersion())) {
                foundCount++;
            }
        }
        assertEquals(1, foundCount);
    }

    @Test
    void testMavenInstallCqueryAdditionalOptions() throws IntegrationException, ExecutableFailedException {
        Assumptions.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        List<String> userProvidedCqueryAdditionalOptions = Arrays.asList("--option1=a", "--option2=b");
        List<String> expectedBazelCommandArgs = Arrays.asList(
            "cquery",
            "--noimplicit_deps",
            "--option1=a",
            "--option2=b",
            "kind(j.*import, deps(/:testTarget))",
            "--output",
            "build"
        );

        List<Dependency> dependencies = doTest(DependencySource.MAVEN_INSTALL, expectedBazelCommandArgs, userProvidedCqueryAdditionalOptions, MAVEN_INSTALL_CQUERY_OUTPUT_SIMPLE);
        assertEquals(8, dependencies.size());
        int foundCount = 0;
        for (Dependency dependency : dependencies) {
            if ("com.google.guava".equals(dependency.getExternalId().getGroup()) &&
                "guava".equals(dependency.getExternalId().getName()) &&
                "27.0-jre".equals(dependency.getExternalId().getVersion())) {
                foundCount++;
            }
            if ("com.google.code.findbugs".equals(dependency.getExternalId().getGroup()) &&
                "jsr305".equals(dependency.getExternalId().getName()) &&
                "3.0.2".equals(dependency.getExternalId().getVersion())) {
                foundCount++;
            }
        }
        assertEquals(2, foundCount);
    }

    @Test
    void haskellCabalLibraryTest() throws IntegrationException, ExecutableFailedException {
        Assumptions.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        List<Dependency> dependencies = doTest(
            DependencySource.HASKELL_CABAL_LIBRARY,
            HASKELL_CABAL_LIBRARY_STANDARD_BAZEL_COMMAND_ARGS,
            null,
            HASKELL_CABAL_LIBRARY_JSONPROTO
        );
        assertEquals(1, dependencies.size());
        int foundCount = 0;
        for (Dependency dependency : dependencies) {
            if ("optparse-applicative".equals(dependency.getExternalId().getName()) &&
                "0.14.3.0".equals(dependency.getExternalId().getVersion())) {
                foundCount++;
            }
        }
        assertEquals(1, foundCount);
    }

    private List<Dependency> doTest(DependencySource dependencySource, List<String> expectedBazelCommandArgs, List<String> userProvidedCqueryAdditionalOptions, String input)
        throws IntegrationException, ExecutableFailedException {
        BazelCommandExecutor bazelCommandExecutor = Mockito.mock(BazelCommandExecutor.class);
        Mockito.when(bazelCommandExecutor.executeToString(expectedBazelCommandArgs)).thenReturn(Optional.of(input));
        BazelVariableSubstitutor bazelVariableSubstitutor = new BazelVariableSubstitutor("/:testTarget", userProvidedCqueryAdditionalOptions, Collections.emptyList());

        ExternalIdFactory externalIdFactory = new ExternalIdFactory();
        HaskellCabalLibraryJsonProtoParser haskellCabalLibraryJsonProtoParser = new HaskellCabalLibraryJsonProtoParser(new Gson());
        Pipelines pipelines = new Pipelines(
            bazelCommandExecutor,
            bazelVariableSubstitutor,
            externalIdFactory,
            haskellCabalLibraryJsonProtoParser,
            BazelEnvironmentAnalyzer.Mode.WORKSPACE
        );
        Pipeline pipeline = pipelines.get(dependencySource);
        return pipeline.run();
    }

    private static String createStandardOutput(String... outputLines) {
        return String.join(System.lineSeparator(), outputLines);
    }
}
