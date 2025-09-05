package com.blackduck.integration.detectable.detectables.gradle.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.detectables.gradle.inspection.model.GradleGav;
import com.blackduck.integration.detectable.detectables.gradle.inspection.model.GradleTreeNode;
import com.blackduck.integration.detectable.detectables.gradle.inspection.parse.GradleReportLineParser;

import java.util.Collections;

public class GradleReportParserTest {
    //private final TestUtil testUtil = new TestUtil();

    /*

------------------------------------------------------------
Project :complex-test - Tests different project positions, components and opening characters
------------------------------------------------------------

archives - test that we can handle no dependencies
No dependencies

projectTest - test that this project dependency does not show up
\--- project :child-project

junitTest - test an only component under a config that opens with indicator
\--- solo:component:4.12

dependency - Compile classpath for source set 'main'.
+--- project :nested-parent
|    \--- project :nested-child
\--- non-project:with-nested:1.0.0

compile
+--- project :spring-webflux
|    +--- project :spring-beans (*)
|    +--- project :spring-core (*)
|    +--- project :spring-web (*)
|    \--- should-suppress:project-child: -> 6
+--- some.group:parent:5.0.0
|    \--- some.group:child:2.2.2
\--- terminal:child:6.2.3ok

     */
    @Test
    public void parseSuffixIgnored() {
        assertParsedGavDoesNotContain("+--- com.fasterxml.jackson.core:jackson-databind:2.9.5 (n)", "(n)");
        assertParsedGavDoesNotContain("+--- com.fasterxml.jackson.core:jackson-databind:2.9.5 (c)", "(c)");
        assertParsedGavDoesNotContain("+--- com.fasterxml.jackson.core:jackson-databind:2.9.5 (*)", "(*)");
    }

    private void assertParsedGavDoesNotContain(String line, String contains) {
        GradleReportLineParser gradleReportLineParser = new GradleReportLineParser();
        GradleTreeNode node = gradleReportLineParser.parseLine(line, Collections.emptyMap());
        assertTrue(node.getGav().isPresent());

        GradleGav gav = node.getGav().get();
        assertFalse(gav.getVersion().contains(contains));
    }

    @Test
    public void getLineLevelTest() {
        GradleReportLineParser gradleReportLineParser = new GradleReportLineParser();
        assertEquals(5, gradleReportLineParser.parseLine(("|    |         |    |    \\--- org.springframework:spring-core:4.3.5.RELEASE"), Collections.emptyMap()).getLevel());
        assertEquals(3, gradleReportLineParser.parseLine(("|    |         \\--- com.squareup.okhttp3:okhttp:3.4.2 (*)"), Collections.emptyMap()).getLevel());
        assertEquals(4, gradleReportLineParser.parseLine(("     |    |         \\--- org.ow2.asm:asm:5.0.3"), Collections.emptyMap()).getLevel());
        assertEquals(1, gradleReportLineParser.parseLine(("     +--- org.hamcrest:hamcrest-core:1.3"), Collections.emptyMap()).getLevel());
        assertEquals(0, gradleReportLineParser.parseLine(("+--- org.springframework.boot:spring-boot-starter: -> 1.4.3.RELEASE"), Collections.emptyMap()).getLevel());
        assertEquals(0, gradleReportLineParser.parseLine(("\\--- org.apache.commons:commons-compress:1.13"), Collections.emptyMap()).getLevel());
    }

    @Test
    void failedSuffixIsStrippedFromVersion() {
        GradleReportLineParser gradleReportLineParser = new GradleReportLineParser();
        String line = "+--- org.apache.commons:commons-email:1.2 FAILED";
        GradleTreeNode node = gradleReportLineParser.parseLine(line, Collections.emptyMap());

        assertTrue(node.getGav().isPresent());

        GradleGav gav = node.getGav().get();
        assertEquals("org.apache.commons", gav.getGroup());
        assertEquals("commons-email", gav.getName());
        assertEquals("1.2", gav.getVersion(), "Expected version to exclude 'FAILED'");
    }

    @Test
    void missingVersionHandledAsUnknownNode() {
        GradleReportLineParser parser = new GradleReportLineParser();
        String line = "+--- org.apache.commons:commons-lang3:";
        GradleTreeNode node = parser.parseLine(line, Collections.emptyMap());

        assertEquals(GradleTreeNode.NodeType.UNKNOWN, node.getNodeType());
        assertFalse(node.getGav().isPresent());
    }
}
