package com.blackduck.integration.detectable.detectables.gradle.functional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.gradle.inspection.GradleInspectorOptions;
import com.blackduck.integration.detectable.detectables.gradle.inspection.inspector.GradleInspectorScriptOptions;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.functional.DetectableFunctionalTest;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;
import com.blackduck.integration.executable.ExecutableOutput;
import com.blackduck.integration.rest.proxy.ProxyInfo;

public class GradleInspectorDetectableTest extends DetectableFunctionalTest {

    public GradleInspectorDetectableTest() throws IOException {
        super("gradle-inspector");
    }

    @Override
    protected void setup() throws IOException {
        addFile(
            Paths.get("build.gradle"),
            "buildscript {",
            "    repositories {",
            "        jcenter()",
            "        mavenCentral()",
            "        maven { url 'https://plugins.gradle.org/m2/' }",
            "    }",
            "    dependencies { classpath 'com.blackducksoftware.integration:common-gradle-plugin:0.0.+' }",
            "}",
            "",
            "version = '13.1.1-SNAPSHOT'",
            "",
            "apply plugin: 'com.blackducksoftware.integration.library'",
            "",
            "dependencies {",
            "    compile 'org.apache.httpcomponents:httpmime:4.5.6'",
            "    compile group: 'commons-collections', name: 'commons-collections', version: '3.2.2'",
            "    compile 'org.apache.commons:commons-lang3:3.7'",
            "}"
        );

        ExecutableOutput gradleDependenciesOutput = createStandardOutput("");
        addExecutableOutput(
            gradleDependenciesOutput,
            new File("gradle").getCanonicalPath(),
            "gatherDependencies",
            "--init-script=gradle-inspector",
            "-DGRADLEEXTRACTIONDIR=" + getOutputDirectory().toFile().getCanonicalPath(),
            "--info"
        );

        addOutputFile(Paths.get("rootProjectMetadata.txt"), Arrays.asList(
            "DETECT META DATA START",
            "rootProjectDirectory:/Users/ekerwin/Documents/source/integration/hub-detect",
            "rootProjectGroup:com.blackducksoftware.integration",
            "rootProjectName:hub-detect",
            "rootProjectVersion:2.0.0-SNAPSHOT",
            "DETECT META DATA END"
        ));

        addOutputFile(Paths.get("_dependencyGraph.txt"), Arrays.asList(
            "------------------------------------------------------------",
            "Root project",
            "------------------------------------------------------------",
            "",
            "compile - Dependencies for source set 'main'.",
            "+--- com.blackducksoftware.integration:hub-common:11.0.0-SNAPSHOT",
            "|    +--- com.blackducksoftware.integration:hub-common-rest:1.0.0",
            "|    |    +--- com.blackducksoftware.integration:integration-common:5.2.1",
            "|    |    |    +--- org.apache.commons:commons-lang3:3.5",
            "|    |    |    +--- commons-io:commons-io:2.5",
            "|    |    |    +--- commons-codec:commons-codec:1.10",
            "|    |    |    \\--- org.slf4j:slf4j-api:1.7.21 -> 1.7.22"
        ));
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
                "",
                false
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
        Assertions.assertEquals(1, extraction.getCodeLocations().size());

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.MAVEN, extraction.getCodeLocations().get(0).getDependencyGraph());
        graphAssert.hasRootSize(1);

        ExternalIdFactory externalIdFactory = new ExternalIdFactory();

        ExternalId hubCommonExternalId = externalIdFactory.createMavenExternalId("com.blackducksoftware.integration", "hub-common", "11.0.0-SNAPSHOT");
        ExternalId hubCommonRestExternalId = externalIdFactory.createMavenExternalId("com.blackducksoftware.integration", "hub-common-rest", "1.0.0");
        ExternalId integrationCommonExternalId = externalIdFactory.createMavenExternalId("com.blackducksoftware.integration", "integration-common", "5.2.1");
        ExternalId slf4jExternalId = externalIdFactory.createMavenExternalId("org.slf4j", "slf4j-api", "1.7.22");
        ExternalId commonsIoExternalId = externalIdFactory.createMavenExternalId("commons-io", "commons-io", "2.5");
        ExternalId commonsCodecExternalId = externalIdFactory.createMavenExternalId("commons-codec", "commons-codec", "1.10");
        ExternalId commonsLangExternalId = externalIdFactory.createMavenExternalId("org.apache.commons", "commons-lang3", "3.5");

        graphAssert.hasParentChildRelationship(hubCommonExternalId, hubCommonRestExternalId);
        graphAssert.hasParentChildRelationship(hubCommonRestExternalId, integrationCommonExternalId);
        graphAssert.hasParentChildRelationship(integrationCommonExternalId, slf4jExternalId);
        graphAssert.hasParentChildRelationship(integrationCommonExternalId, commonsIoExternalId);
        graphAssert.hasParentChildRelationship(integrationCommonExternalId, commonsCodecExternalId);
        graphAssert.hasParentChildRelationship(integrationCommonExternalId, commonsLangExternalId);

    }
}
