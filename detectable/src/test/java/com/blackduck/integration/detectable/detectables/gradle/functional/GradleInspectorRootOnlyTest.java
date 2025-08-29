package com.blackduck.integration.detectable.detectables.gradle.functional;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.gradle.inspection.GradleInspectorOptions;
import com.blackduck.integration.detectable.detectables.gradle.inspection.inspector.GradleInspectorScriptOptions;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.functional.DetectableFunctionalTest;
import com.blackduck.integration.detectable.util.ExtractionUtil;
import com.blackduck.integration.detectable.util.FunctionalTestFiles;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;
import com.blackduck.integration.executable.ExecutableOutput;
import com.blackduck.integration.rest.proxy.ProxyInfo;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class GradleInspectorRootOnlyTest extends DetectableFunctionalTest {
    protected GradleInspectorRootOnlyTest() throws IOException {
        super("gradle-inspector-root-only-test");
    }

    @Override
    protected void setup() throws IOException {
        addFile(Paths.get("build.gradle"), "no content");

        ExecutableOutput gradleDependenciesOutput = createStandardOutput("no content");

        addExecutableOutput(
                gradleDependenciesOutput,
                new File("gradle").getCanonicalPath(),
                "gatherDependencies",
                "--init-script=gradle-inspector",
                "-DGRADLEEXTRACTIONDIR=" + getOutputDirectory().toFile().getCanonicalPath(),
                "--no-configuration-cache",
                "--info"
        );

        List<String> rootProjectMetadataLines = FunctionalTestFiles.asListOfStrings("/gradle/root-only-tests/rootProjectMetadata.txt");
        addOutputFile(Paths.get("rootProjectMetadata.txt"), rootProjectMetadataLines);

        List<String> dependenciesCommandOutputLines = FunctionalTestFiles.asListOfStrings("/gradle/root-only-tests/root_project__simple__depth0_dependencyGraph.txt");
        addOutputFile(Paths.get("root_dependencyGraph.txt"), dependenciesCommandOutputLines);
    }

    @NotNull
    @Override
    public Detectable create(@NotNull DetectableEnvironment detectableEnvironment) {
        GradleInspectorOptions gradleInspectorOptions = new GradleInspectorOptions("",
                new GradleInspectorScriptOptions(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        true
                ),
                ProxyInfo.NO_PROXY_INFO, EnumListFilter.excludeNone()
        );
        return detectableFactory.createGradleDetectable(
                detectableEnvironment,
                gradleInspectorOptions,
                () -> new File("gradle-inspector"),
                (environment) -> ExecutableTarget.forFile(new File("gradle"))
        );
    }

    @Override
    public void assertExtraction(@NotNull Extraction extraction) {
        Assertions.assertEquals(5, extraction.getCodeLocations().size());

        CodeLocation root = ExtractionUtil.assertAndGetCodeLocationNamed("simple", extraction);
        CodeLocation subProjectA = ExtractionUtil.assertAndGetCodeLocationNamed("subProjectA", extraction);
        CodeLocation subProjectB = ExtractionUtil.assertAndGetCodeLocationNamed("subProjectB", extraction);
        CodeLocation subProjectC = ExtractionUtil.assertAndGetCodeLocationNamed("subProjectC", extraction);
        CodeLocation subSubProjectAA = ExtractionUtil.assertAndGetCodeLocationNamed("subSubProjectAA", extraction);

        ExternalIdFactory externalIdFactory = new ExternalIdFactory();
        // Root dependencies
        ExternalId logback_classic = externalIdFactory.createMavenExternalId("ch.qos.logback", "logback-classic", "1.2.13");
        ExternalId logback_core = externalIdFactory.createMavenExternalId("ch.qos.logback", "logback-core", "1.2.13");
        ExternalId commons_text = externalIdFactory.createMavenExternalId("org.apache.commons", "commons-text", "1.10.0");
        ExternalId failure_access = externalIdFactory.createMavenExternalId("com.google.guava", "failureaccess", "1.0.1");
        // SubProjectA dependencies
        ExternalId blackduck_common = externalIdFactory.createMavenExternalId("com.blackduck.integration", "blackduck-common", "67.0.2");
        ExternalId blackduck_common_api = externalIdFactory.createMavenExternalId("com.blackduck.integration", "blackduck-common-api", "2023.4.2.7");
        // SubProjectC dependencies
        ExternalId antlr4_runtime = externalIdFactory.createMavenExternalId("org.antlr", "antlr4-runtime", "4.7.2");
        // Overlapping dependencies
        ExternalId guava = externalIdFactory.createMavenExternalId("com.google.guava", "guava", "32.1.2-jre");
        ExternalId digraph_parser = externalIdFactory.createMavenExternalId("com.paypal.digraph", "digraph-parser", "1.0");
        // subSubProjectAA dependency
        ExternalId slf4j = externalIdFactory.createMavenExternalId("org.slf4j", "slf4j-api", "1.7.30");


        NameVersionGraphAssert rootGraphAssert = new NameVersionGraphAssert(Forge.MAVEN, root.getDependencyGraph());
        NameVersionGraphAssert subProjectAGraphAssert = new NameVersionGraphAssert(Forge.MAVEN, subProjectA.getDependencyGraph());
        NameVersionGraphAssert subProjectCGraphAssert = new NameVersionGraphAssert(Forge.MAVEN, subProjectC.getDependencyGraph());
        NameVersionGraphAssert subSubProjectAAGraphAssert = new NameVersionGraphAssert(Forge.MAVEN, subSubProjectAA.getDependencyGraph());


        // Root has 3 direct and 2 transitive
        Set<Dependency> rootProjectDirectDependencies = root.getDependencyGraph().getDirectDependencies();
        Assertions.assertEquals(3, rootProjectDirectDependencies.size());
        rootGraphAssert.hasRootDependency(logback_classic);
        rootGraphAssert.hasRootDependency(commons_text);
        rootGraphAssert.hasRootDependency(guava);
        rootGraphAssert.hasRelationshipCount(guava, 1);
        rootGraphAssert.hasParentChildRelationship(logback_classic, logback_core);
        rootGraphAssert.hasParentChildRelationship(guava, failure_access);

        // SubProjectA has 4 direct and 2 transitive
        Set<Dependency> subProjectADirectDependencies = subProjectA.getDependencyGraph().getDirectDependencies();
        Assertions.assertEquals(3, subProjectADirectDependencies.size());
        subProjectAGraphAssert.hasRootDependency(blackduck_common);
        subProjectAGraphAssert.hasRootDependency(digraph_parser);
        subProjectAGraphAssert.hasRootDependency(guava);
        // guava transitives are only at the root code location (BDIO unaffected since transitives are aggregated under
        // a direct dependency regardless of which projects (root or subproject) they stem from)
        subProjectAGraphAssert.hasRelationshipCount(guava, 0);
        subProjectAGraphAssert.hasParentChildRelationship(blackduck_common, blackduck_common_api);
        // Confirms code location relationships for subProjectA got updated when processing a second configuration
        subProjectAGraphAssert.hasRelationshipCount(digraph_parser, 1);
        subProjectAGraphAssert.hasParentChildRelationship(digraph_parser, antlr4_runtime);

        // Nested SubProjectC has 1 direct and no transitives
        Set<Dependency> subProjectCDirectDependencies = subProjectC.getDependencyGraph().getDirectDependencies();
        Assertions.assertEquals(1, subProjectCDirectDependencies.size());
        subProjectCGraphAssert.hasRootDependency(antlr4_runtime);
        subProjectCGraphAssert.hasRelationshipCount(antlr4_runtime, 0);

        // Empty subProjectB has no dependencies
        Set<Dependency> subProjectBDirectDependencies = subProjectB.getDependencyGraph().getDirectDependencies();
        Assertions.assertEquals(0, subProjectBDirectDependencies.size());

        // Nested subproject subProjectA:subSubProjectAA has 1 dependency
        subSubProjectAAGraphAssert.hasRootDependency(slf4j);

    }
}
