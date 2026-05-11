package com.blackduck.integration.detectable.detectables.ivy.functional;

import java.io.IOException;
import java.nio.file.Paths;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.resolver.AntResolver;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.functional.DetectableFunctionalTest;
import com.blackduck.integration.detectable.util.graph.MavenGraphAssert;
import com.blackduck.integration.executable.ExecutableOutput;

public class IvyCliDetectableTest extends DetectableFunctionalTest {

    public IvyCliDetectableTest() throws IOException {
        super("ivy");
    }

    @Override
    protected void setup() throws IOException {
        addFile(
            Paths.get("ivy.xml"),
            "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>",
            "<ivy-module version=\"2.0\">",
            "    <info organisation=\"org.apache.ivy\" module=\"ivy\" status=\"integration\"/>",
            "    <dependencies>",
            "        <dependency org=\"org.apache.httpcomponents\" name=\"httpclient\" rev=\"4.5.13\"/>",
            "        <dependency org=\"oro\" name=\"oro\" rev=\"2.0.8\"/>",
            "        <dependency org=\"org.apache.commons\" name=\"commons-vfs2\" rev=\"2.2\"/>",
            "    </dependencies>",
            "</ivy-module>"
        );

        addFile(
            Paths.get("build.xml"),
            "<project name=\"ivy\" default=\"test\" xmlns:ivy=\"antlib:org.apache.ivy.ant\">",
            "    <target name=\"dependencytree\">",
            "        <ivy:resolve />",
            "        <ivy:dependencytree log=\"download-only\" />",
            "    </target>",
            "</project>"
        );

        // Mock ant --version output
        ExecutableOutput antVersionOutput = createStandardOutput("Apache Ant(TM) version 1.10.15 compiled on August 25 2024");
        addExecutableOutput(antVersionOutput, "ant", "--version");

        // Mock ant dependencytree output
        ExecutableOutput antDependencyTreeOutput = createStandardOutput(
            "[ivy:dependencytree] Dependency tree for org.apache.ivy-ivy",
            "[ivy:dependencytree] +- org.apache.httpcomponents#httpclient;4.5.13",
            "[ivy:dependencytree] |  +- commons-codec#commons-codec;1.18.0",
            "[ivy:dependencytree] |  +- org.apache.httpcomponents#httpcore;4.4.13",
            "[ivy:dependencytree] |  +- commons-logging#commons-logging;1.2",
            "[ivy:dependencytree] +- oro#oro;2.0.8",
            "[ivy:dependencytree] +- org.apache.commons#commons-vfs2;2.2",
            "[ivy:dependencytree] |  +- commons-logging#commons-logging;1.2"
        );
        addExecutableOutput(antDependencyTreeOutput, "ant", "dependencytree");
    }

    @NotNull
    @Override
    public Detectable create(@NotNull DetectableEnvironment detectableEnvironment) {
        class AntResolverTest implements AntResolver {
            @Override
            public ExecutableTarget resolveAnt() throws DetectableException {
                return ExecutableTarget.forCommand("ant");
            }
        }
        return detectableFactory.createIvyCliDetectable(detectableEnvironment, new AntResolverTest());
    }

    @Override
    public void assertExtraction(@NotNull Extraction extraction) {
        Assertions.assertEquals(1, extraction.getCodeLocations().size());
        Assertions.assertEquals("ivy", extraction.getProjectName());

        MavenGraphAssert graphAssert = new MavenGraphAssert(extraction.getCodeLocations().get(0).getDependencyGraph());

        // Verify direct dependencies (use GAV format: group:artifact:version)
        graphAssert.hasRootDependency("org.apache.httpcomponents:httpclient:4.5.13");
        graphAssert.hasRootDependency("oro:oro:2.0.8");
        graphAssert.hasRootDependency("org.apache.commons:commons-vfs2:2.2");

        // Verify transitive dependencies exist in graph
        graphAssert.hasDependency("commons-codec:commons-codec:1.18.0");
        graphAssert.hasDependency("org.apache.httpcomponents:httpcore:4.4.13");
        graphAssert.hasDependency("commons-logging:commons-logging:1.2");

        // Verify root size
        graphAssert.hasRootSize(3);
    }
}

