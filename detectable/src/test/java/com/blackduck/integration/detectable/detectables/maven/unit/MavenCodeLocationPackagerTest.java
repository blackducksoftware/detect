package com.blackduck.integration.detectable.detectables.maven.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.blackduck.integration.bdio.model.Forge;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.annotations.UnitTest;
import com.blackduck.integration.detectable.detectables.maven.cli.MavenCodeLocationPackager;
import com.blackduck.integration.detectable.detectables.maven.cli.MavenParseResult;
import com.blackduck.integration.detectable.detectables.maven.cli.ScopedDependency;

@UnitTest
public class MavenCodeLocationPackagerTest {
    @Test
    public void testParseProject() {
        MavenCodeLocationPackager mavenCodeLocationPackager = new MavenCodeLocationPackager(new ExternalIdFactory());

        Dependency dependency = mavenCodeLocationPackager.textToProject("stuff:things:jar:0.0.1");
        assertNotNull(dependency);

        dependency = mavenCodeLocationPackager.textToProject("stuff:things:jar:classifier:0.0.1");
        assertNotNull(dependency);

        dependency = mavenCodeLocationPackager.textToProject("stuff:things:jar");
        assertNull(dependency);

        dependency = mavenCodeLocationPackager.textToProject("stuff:things:jar:classifier:0.0.1:monkey");
        assertNull(dependency);
    }

    @Test
    public void testParseDependency() {
        MavenCodeLocationPackager mavenCodeLocationPackager = new MavenCodeLocationPackager(new ExternalIdFactory());

        ScopedDependency dependency = mavenCodeLocationPackager.textToDependency("stuff:things:jar:0.0.1:compile");
        assertNotNull(dependency);

        dependency = mavenCodeLocationPackager.textToDependency("stuff:things:jar:classifier:0.0.1:test");
        assertNotNull(dependency);

        dependency = mavenCodeLocationPackager.textToDependency("stuff:things:jar");
        assertNull(dependency);

        dependency = mavenCodeLocationPackager.textToDependency("stuff:things:jar:classifier:0.0.1");
        assertNotNull(dependency);
    }

    @Test
    public void testIsLineRelevant() {
        MavenCodeLocationPackager mavenCodeLocationPackager = new MavenCodeLocationPackager(null);

        assertTrue(mavenCodeLocationPackager.isLineRelevant("weird garbage 3525356 [thingsINFO 346534623465] stuff"));

        assertTrue(mavenCodeLocationPackager.isLineRelevant("[thingsINFO 346534623465]stuff"));

        assertTrue(mavenCodeLocationPackager.isLineRelevant("[thingsINFO]  stuff"));

        assertFalse(mavenCodeLocationPackager.isLineRelevant(" [INFO]     "));

        assertFalse(mavenCodeLocationPackager.isLineRelevant("weird garbage 3525356 [thingsINFO 346534623465]"));

        assertFalse(mavenCodeLocationPackager.isLineRelevant("[thingsINFO 346534623465]"));

        assertFalse(mavenCodeLocationPackager.isLineRelevant("[thingsINFO]"));

        assertFalse(mavenCodeLocationPackager.isLineRelevant(" [INFO]"));

        assertFalse(mavenCodeLocationPackager.isLineRelevant(" "));

        assertFalse(mavenCodeLocationPackager.isLineRelevant("[INFO] Downloaded"));

        assertFalse(mavenCodeLocationPackager.isLineRelevant("[INFO] stuff and thingsDownloaded stuff and things"));

        assertFalse(mavenCodeLocationPackager.isLineRelevant("[INFO] Downloading"));

        assertFalse(mavenCodeLocationPackager.isLineRelevant("[INFO] stuff and things Downloadingstuff and things"));
    }

    @Test
    public void testTrimLogLevel() {
        MavenCodeLocationPackager mavenCodeLocationPackager = new MavenCodeLocationPackager(null);

        String actualLine = "";
        final String expectedValue = "thing";

        actualLine = mavenCodeLocationPackager.trimLogLevel("weird garbage 3525356 [thingsINFO 346534623465]" + expectedValue);
        assertEquals(expectedValue, actualLine);

        actualLine = mavenCodeLocationPackager.trimLogLevel("[thingsINFO 346534623465]" + expectedValue);
        assertEquals(expectedValue, actualLine);

        actualLine = mavenCodeLocationPackager.trimLogLevel("[thingsINFO]" + expectedValue);
        assertEquals(expectedValue, actualLine);

        actualLine = mavenCodeLocationPackager.trimLogLevel(" [INFO] " + expectedValue);
        assertEquals(expectedValue, actualLine);
    }

    @Test
    public void testIsProjectSection() {
        MavenCodeLocationPackager mavenCodeLocationPackager = new MavenCodeLocationPackager(null);

        assertFalse(mavenCodeLocationPackager.isProjectSection(" "));

        assertFalse(mavenCodeLocationPackager.isProjectSection("       "));

        assertFalse(mavenCodeLocationPackager.isProjectSection("---maven-dependency-plugin:"));

        assertFalse(mavenCodeLocationPackager.isProjectSection("---maven-dependency-plugin:other      stuff"));

        assertFalse(mavenCodeLocationPackager.isProjectSection("maven-dependency-plugin:tree      stuff"));

        assertTrue(mavenCodeLocationPackager.isProjectSection("---maven-dependency-plugin:tree      stuff"));

        assertTrue(mavenCodeLocationPackager.isProjectSection("things --- stuff maven-dependency-plugin garbage:tree      stuff"));

        assertTrue(mavenCodeLocationPackager.isProjectSection("things --- stuff maven-dependency-plugin:tree      stuff"));

        assertTrue(mavenCodeLocationPackager.isProjectSection("---maven-dependency-plugin:tree"));

        assertTrue(mavenCodeLocationPackager.isProjectSection("      ---       maven-dependency-plugin      :       tree"));
    }

    @Test
    public void testIsDependencyTreeUpdates() {
        MavenCodeLocationPackager mavenCodeLocationPackager = new MavenCodeLocationPackager(null);

        assertTrue(mavenCodeLocationPackager.isDependencyTreeUpdates("artifact com.google.guava:guava:jar:15.0:compile checking for updates from"));

        assertTrue(mavenCodeLocationPackager.isDependencyTreeUpdates("         artifact       com.google.guava:guava:         checking for updates"));

        assertTrue(mavenCodeLocationPackager.isDependencyTreeUpdates("      checking for updates   artifact       com.google.guava:guava:      "));

        assertTrue(mavenCodeLocationPackager.isDependencyTreeUpdates("checking for updates"));

        assertFalse(mavenCodeLocationPackager.isDependencyTreeUpdates("com.google.guava:guava:jar:15.0:compile"));

        assertFalse(mavenCodeLocationPackager.isDependencyTreeUpdates("+- com.google.guava:guava:jar:15.0:compile"));

        assertFalse(mavenCodeLocationPackager.isDependencyTreeUpdates("|  \\- com.google.guava:guava:jar:15.0:compile"));
    }

    @Test
    public void testIsGav() {
        MavenCodeLocationPackager mavenCodeLocationPackager = new MavenCodeLocationPackager(null);

        assertFalse(mavenCodeLocationPackager.isGav(" "));

        assertFalse(mavenCodeLocationPackager.isGav("       "));

        assertFalse(mavenCodeLocationPackager.isGav("::::"));

        assertFalse(mavenCodeLocationPackager.isGav(" : : : : "));

        assertFalse(mavenCodeLocationPackager.isGav("group"));

        assertFalse(mavenCodeLocationPackager.isGav("group:artifact"));

        assertFalse(mavenCodeLocationPackager.isGav("group:artifact:version"));

        assertFalse(mavenCodeLocationPackager.isGav("group-artifact:type-classifier-version:scope-garbage"));

        assertFalse(mavenCodeLocationPackager.isGav("group:artifact::classifier:version: :garbage"));

        assertTrue(mavenCodeLocationPackager.isGav("group:artifact:type:version"));

        assertTrue(mavenCodeLocationPackager.isGav("group:artifact:type:classifier:version"));

        assertTrue(mavenCodeLocationPackager.isGav("group:artifact:type:classifier:version:scope"));

        assertTrue(mavenCodeLocationPackager.isGav("group:artifact:type:classifier:version:scope:garbage"));
    }

    @Test
    public void testIsEclipsePackage() {
        MavenCodeLocationPackager mavenCodeLocationPackager = new MavenCodeLocationPackager(new ExternalIdFactory());

        String line = "[INFO] +- p2.eclipse-plugin:org.eclipse.core.runtime:jar:3.17.100.v20200203-0917:system";
        line = mavenCodeLocationPackager.trimLogLevel(line);
        String cleanedLine = mavenCodeLocationPackager.calculateCurrentLevelAndCleanLine(line);
        Dependency dependency = mavenCodeLocationPackager.textToDependency(cleanedLine);
        assertEquals("org.eclipse.core.runtime/3.17.100.v20200203-0917", dependency.getExternalId().createExternalId());
        assertEquals(Forge.ECLIPSE, dependency.getExternalId().getForge());
        assertEquals(1, mavenCodeLocationPackager.getEclipsePackageExternalIdModifiedCounter());
    }

    @Test
    public void testIndexOfEndOfSegments() {
        MavenCodeLocationPackager mavenCodeLocationPackager = new MavenCodeLocationPackager(null);

        assertEquals(-1, mavenCodeLocationPackager.indexOfEndOfSegments(""));

        assertEquals(-1, mavenCodeLocationPackager.indexOfEndOfSegments("stuff and things"));

        assertEquals(-1, mavenCodeLocationPackager.indexOfEndOfSegments("stuff and things", "things", "and"));

        assertEquals(-1, mavenCodeLocationPackager.indexOfEndOfSegments("stuff and things", "things", "and", "stuff"));

        assertEquals(5, mavenCodeLocationPackager.indexOfEndOfSegments("stuff and things", "stuff"));

        assertEquals(9, mavenCodeLocationPackager.indexOfEndOfSegments("stuff and things", "stuff", "and"));

        assertEquals(16, mavenCodeLocationPackager.indexOfEndOfSegments("stuff and things", "stuff", "and", "things"));

        assertEquals(9, mavenCodeLocationPackager.indexOfEndOfSegments("stuff and things", "and"));

        assertEquals(16, mavenCodeLocationPackager.indexOfEndOfSegments("stuff and things", "things"));
    }

    @Test
    public void testDoesLineContainSegmentsInOrder() {
        MavenCodeLocationPackager mavenCodeLocationPackager = new MavenCodeLocationPackager(null);

        assertFalse(mavenCodeLocationPackager.doesLineContainSegmentsInOrder(""));

        assertFalse(mavenCodeLocationPackager.doesLineContainSegmentsInOrder("stuff and things"));

        assertFalse(mavenCodeLocationPackager.doesLineContainSegmentsInOrder("stuff and things", "things", "and"));

        assertFalse(mavenCodeLocationPackager.doesLineContainSegmentsInOrder("stuff and things", "things", "and", "stuff"));

        assertTrue(mavenCodeLocationPackager.doesLineContainSegmentsInOrder("stuff and things", "stuff"));

        assertTrue(mavenCodeLocationPackager.doesLineContainSegmentsInOrder("stuff and things", "stuff", "and"));

        assertTrue(mavenCodeLocationPackager.doesLineContainSegmentsInOrder("stuff and things", "stuff", "and", "things"));

        assertTrue(mavenCodeLocationPackager.doesLineContainSegmentsInOrder("stuff and things", "and"));

        assertTrue(mavenCodeLocationPackager.doesLineContainSegmentsInOrder("stuff and things", "things"));
    }

    @Test
    public void testLineWithExtraTextAfterScope() {
        MavenCodeLocationPackager mavenCodeLocationPackager = new MavenCodeLocationPackager(new ExternalIdFactory());

        String line = "[INFO] |  |  |  \\- org.eclipse.scout.sdk.deps:org.eclipse.core.jobs:jar:3.8.0.v20160509-0411:compile (version selected from constraint [3.8.0,3.8.1))";
        line = mavenCodeLocationPackager.trimLogLevel(line);
        String cleanedLine = mavenCodeLocationPackager.calculateCurrentLevelAndCleanLine(line);
        Dependency dependency = mavenCodeLocationPackager.textToDependency(cleanedLine);
        assertEquals("org.eclipse.scout.sdk.deps:org.eclipse.core.jobs:3.8.0.v20160509-0411", dependency.getExternalId().createExternalId());
    }

    @Test
    public void testLineWithUnknownScope() {
        MavenCodeLocationPackager mavenCodeLocationPackager = new MavenCodeLocationPackager(new ExternalIdFactory());

        String line = "[INFO] |  |  |  \\- org.eclipse.scout.sdk.deps:org.eclipse.core.jobs:jar:3.8.0.v20160509-0411:pants (version selected from constraint [3.8.0,3.8.1))";
        line = mavenCodeLocationPackager.trimLogLevel(line);
        String cleanedLine = mavenCodeLocationPackager.calculateCurrentLevelAndCleanLine(line);
        ScopedDependency scopedDependency = mavenCodeLocationPackager.textToDependency(cleanedLine);
        assertEquals("org.eclipse.scout.sdk.deps:org.eclipse.core.jobs:3.8.0.v20160509-0411", scopedDependency.getExternalId().createExternalId());
    }

    @Test
    public void testLineWithBadColonPlacement() {
        MavenCodeLocationPackager mavenCodeLocationPackager = new MavenCodeLocationPackager(new ExternalIdFactory());

        String line = "[INFO] |  |  |  \\- org.eclipse.scout.sdk.deps:org.eclipse.core.jobs:jar:3.8.0.v20160509-0411:pants (version selected from: [3.8.0,3.8.1))";
        line = mavenCodeLocationPackager.trimLogLevel(line);
        String cleanedLine = mavenCodeLocationPackager.calculateCurrentLevelAndCleanLine(line);
        Dependency dependency = mavenCodeLocationPackager.textToDependency(cleanedLine);
        assertEquals("org.eclipse.scout.sdk.deps:org.eclipse.core.jobs:pants (version selected from", dependency.getExternalId().createExternalId());
    }

    // Provenance INFO line with a 5-part colon split that would previously
    // have been latched as a fake project header. Real project GAV should
    // still be anchored on the next line.
    @Test
    public void testProvenanceInfoLineWithFewColons_realProjectStillAnchored() {
        MavenCodeLocationPackager packager = new MavenCodeLocationPackager(new ExternalIdFactory());
        List<String> mavenOutput = Arrays.asList(
            "[INFO] --- maven-dependency-plugin:2.8:tree (default-cli) @ demo ---",
            "[INFO] Artifact org.codehaus.plexus:plexus-utils:pom:4.0.1 is present in the local repository, but cached from a remote repository ID that is unavailable in current build context, verifying that is downloadable from [internal (https://repo.maven.apache.org/maven2, default, releases+snapshots)]",
            "[INFO] com.example:demo:jar:1.0",
            "[INFO] +- com.google.guava:guava:jar:32.1.3-jre:compile"
        );

        List<MavenParseResult> results = packager.extractCodeLocations(
            "", mavenOutput,
            Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(),
            Collections.emptyMap()
        );

        assertEquals(1, results.size());
        assertEquals("demo", results.get(0).getProjectName());
        assertEquals("1.0", results.get(0).getProjectVersion());
    }

    // Provenance INFO line with a many-part colon split (URL contains ":443")
    // that would previously have disarmed the parser and produced an empty
    // BOM. Real project GAV should still be anchored on the next line.
    @Test
    public void testProvenanceInfoLineWithManyColons_realProjectStillAnchored() {
        MavenCodeLocationPackager packager = new MavenCodeLocationPackager(new ExternalIdFactory());
        List<String> mavenOutput = Arrays.asList(
            "[INFO] --- maven-dependency-plugin:2.8:tree (default-cli) @ demo ---",
            "[INFO] Artifact org.codehaus.plexus:plexus-utils:pom:4.0.1 is present in the local repository, but cached from a remote repository ID that is unavailable in current build context, verifying that is downloadable from [internal (https://repo.maven.apache.org:443/maven2, default, releases+snapshots)]",
            "[INFO] com.example:demo:jar:1.0",
            "[INFO] +- com.google.guava:guava:jar:32.1.3-jre:compile"
        );

        List<MavenParseResult> results = packager.extractCodeLocations(
            "", mavenOutput,
            Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(),
            Collections.emptyMap()
        );

        assertEquals(1, results.size());
        assertEquals("demo", results.get(0).getProjectName());
        assertEquals("1.0", results.get(0).getProjectVersion());
    }

    // Tree goal header seen but no valid project GAV ever recognized in the
    // output. Extraction must return an empty list (not a fabricated one).
    @Test
    public void testTreeHeaderButNoValidProjectGav_returnsEmpty() {
        MavenCodeLocationPackager packager = new MavenCodeLocationPackager(new ExternalIdFactory());
        List<String> mavenOutput = Arrays.asList(
            "[INFO] --- maven-dependency-plugin:2.8:tree (default-cli) @ demo ---",
            "[INFO] Artifact org.codehaus.plexus:plexus-utils:pom:4.0.1 is present in the local repository, but cached from a remote repository ID that is unavailable in current build context, verifying that is downloadable from [internal (https://repo.maven.apache.org/maven2, default, releases+snapshots)]",
            "[INFO] BUILD SUCCESS"
        );

        List<MavenParseResult> results = packager.extractCodeLocations(
            "", mavenOutput,
            Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(),
            Collections.emptyMap()
        );

        assertTrue(results.isEmpty());
    }

    // Reactor with two modules where the user excludes one. Only the
    // included module should be returned; the excluded module's tree body
    // must not leak into the included module.
    @Test
    public void testExcludedModuleFilter_onlyIncludedModuleReturned() {
        MavenCodeLocationPackager packager = new MavenCodeLocationPackager(new ExternalIdFactory());
        List<String> mavenOutput = Arrays.asList(
            "[INFO] --- maven-dependency-plugin:2.8:tree (default-cli) @ moduleA ---",
            "[INFO] com.example:moduleA:jar:1.0",
            "[INFO] +- com.google.guava:guava:jar:32.1.3-jre:compile",
            "[INFO] ------------------------------------------------------------------------",
            "[INFO] --- maven-dependency-plugin:2.8:tree (default-cli) @ moduleB ---",
            "[INFO] com.example:moduleB:jar:1.0",
            "[INFO] +- org.apache.commons:commons-lang3:jar:3.14.0:compile",
            "[INFO] ------------------------------------------------------------------------"
        );

        List<MavenParseResult> results = packager.extractCodeLocations(
            "", mavenOutput,
            Collections.emptyList(), Collections.emptyList(),
            Collections.singletonList("moduleB"), Collections.emptyList(),
            Collections.emptyMap()
        );

        assertEquals(1, results.size());
        assertEquals("moduleA", results.get(0).getProjectName());
    }
}
