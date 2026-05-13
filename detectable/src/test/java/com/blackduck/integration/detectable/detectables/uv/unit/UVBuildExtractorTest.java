package com.blackduck.integration.detectable.detectables.uv.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectables.uv.UVDetectorOptions;
import com.blackduck.integration.detectable.detectables.uv.buildexe.UVBuildExtractor;
import com.blackduck.integration.detectable.detectables.uv.parse.UVTomlParser;
import com.blackduck.integration.detectable.detectables.uv.transform.UVTreeDependencyGraphTransformer;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.executable.Executable;
import com.blackduck.integration.executable.ExecutableOutput;

class UVBuildExtractorTest {

    @TempDir
    public File tempDir;

    private DetectableExecutableRunner executableRunner;
    private UVTreeDependencyGraphTransformer transformer;
    private UVTomlParser tomlParser;
    private ExecutableTarget uvExe;

    @BeforeEach
    void setUp() throws Exception {
        executableRunner = mock(DetectableExecutableRunner.class);
        transformer = new UVTreeDependencyGraphTransformer(new ExternalIdFactory());

        File tomlFile = new File(tempDir, "pyproject.toml");
        tomlFile.createNewFile();
        tomlParser = mock(UVTomlParser.class);
        // Stub parseNameVersion() — without this Mockito returns null, causing a NullPointerException
        // inside UVBuildExtractor.extract() and silently routing all tests through the exception path.
        when(tomlParser.parseNameVersion()).thenReturn(Optional.empty());

        uvExe = ExecutableTarget.forFile(new File("/usr/bin/uv"));

        ExecutableOutput mockOutput = mock(ExecutableOutput.class);
        when(mockOutput.getStandardOutputAsList()).thenReturn(Arrays.asList(
            "my-project v1.0.0",
            "├── requests v2.31.0"
        ));
        when(executableRunner.executeSuccessfully(any(Executable.class))).thenReturn(mockOutput);
    }

    // ==================== Basic Arguments Tests ====================

    @Test
    void extractBuildsBasicArguments() throws Exception {
        UVBuildExtractor extractor = new UVBuildExtractor(executableRunner, tempDir, transformer);
        UVDetectorOptions options = new UVDetectorOptions(
            Collections.emptyList(),  // includedGroups
            Collections.emptyList(),  // excludedGroups
            Collections.emptyList(),  // includedWorkspaceMembers
            Collections.emptyList()   // excludedWorkspaceMembers
        );

        Extraction extraction = extractor.extract(uvExe, options, tomlParser);
        assertExtractionSuccess(extraction);

        ArgumentCaptor<Executable> captor = ArgumentCaptor.forClass(Executable.class);
        verify(executableRunner).executeSuccessfully(captor.capture());

        List<String> arguments = captor.getValue().getCommandWithArguments();
        assertTrue(arguments.contains("tree"));
        assertTrue(arguments.contains("--no-dedupe"));
    }

    // ==================== Excluded Groups Tests ====================

    @Test
    void extractAddsNoGroupFlagsForExcludedGroups() throws Exception {
        UVBuildExtractor extractor = new UVBuildExtractor(executableRunner, tempDir, transformer);
        UVDetectorOptions options = new UVDetectorOptions(
            Collections.emptyList(),        // includedGroups
            Arrays.asList("dev", "test"),   // excludedGroups
            Collections.emptyList(),
            Collections.emptyList()
        );

        Extraction extraction = extractor.extract(uvExe, options, tomlParser);
        assertExtractionSuccess(extraction);

        ArgumentCaptor<Executable> captor = ArgumentCaptor.forClass(Executable.class);
        verify(executableRunner).executeSuccessfully(captor.capture());

        List<String> arguments = captor.getValue().getCommandWithArguments();
        assertTrue(arguments.contains("tree"));
        assertTrue(arguments.contains("--no-dedupe"));
        assertTrue(arguments.contains("--no-group"));

        int devIndex = arguments.indexOf("dev");
        int testIndex = arguments.indexOf("test");
        assertTrue(devIndex > 0);
        assertTrue(testIndex > 0);
        assertEquals("--no-group", arguments.get(devIndex - 1));
        assertEquals("--no-group", arguments.get(testIndex - 1));
    }

    @Test
    void extractWithEmptyExcludedGroupsAddsNoFlags() throws Exception {
        UVBuildExtractor extractor = new UVBuildExtractor(executableRunner, tempDir, transformer);
        UVDetectorOptions options = new UVDetectorOptions(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );

        Extraction extraction = extractor.extract(uvExe, options, tomlParser);
        assertExtractionSuccess(extraction);

        ArgumentCaptor<Executable> captor = ArgumentCaptor.forClass(Executable.class);
        verify(executableRunner).executeSuccessfully(captor.capture());

        List<String> arguments = captor.getValue().getCommandWithArguments();

        long noGroupCount = arguments.stream().filter(arg -> arg.equals("--no-group")).count();
        assertEquals(0, noGroupCount);
    }

    @Test
    void extractWithSingleExcludedGroup() throws Exception {
        UVBuildExtractor extractor = new UVBuildExtractor(executableRunner, tempDir, transformer);
        UVDetectorOptions options = new UVDetectorOptions(
            Collections.emptyList(),
            Arrays.asList("dev"),
            Collections.emptyList(),
            Collections.emptyList()
        );

        Extraction extraction = extractor.extract(uvExe, options, tomlParser);
        assertExtractionSuccess(extraction);

        ArgumentCaptor<Executable> captor = ArgumentCaptor.forClass(Executable.class);
        verify(executableRunner).executeSuccessfully(captor.capture());

        List<String> arguments = captor.getValue().getCommandWithArguments();

        long noGroupCount = arguments.stream().filter(arg -> arg.equals("--no-group")).count();
        assertEquals(1, noGroupCount);
        assertTrue(arguments.contains("dev"));
    }

    // ==================== Included Groups Tests ====================

    @Test
    void extractAddsGroupFlagsForIncludedGroups() throws Exception {
        UVBuildExtractor extractor = new UVBuildExtractor(executableRunner, tempDir, transformer);
        UVDetectorOptions options = new UVDetectorOptions(
            Arrays.asList("dev", "test"),   // includedGroups
            Collections.emptyList(),        // excludedGroups
            Collections.emptyList(),
            Collections.emptyList()
        );

        Extraction extraction = extractor.extract(uvExe, options, tomlParser);
        assertExtractionSuccess(extraction);

        ArgumentCaptor<Executable> captor = ArgumentCaptor.forClass(Executable.class);
        verify(executableRunner).executeSuccessfully(captor.capture());

        List<String> arguments = captor.getValue().getCommandWithArguments();
        assertTrue(arguments.contains("tree"));
        assertTrue(arguments.contains("--no-dedupe"));
        assertTrue(arguments.contains("--group"));

        int devIndex = arguments.indexOf("dev");
        int testIndex = arguments.indexOf("test");
        assertTrue(devIndex > 0, "dev group should be in arguments");
        assertTrue(testIndex > 0, "test group should be in arguments");
        assertEquals("--group", arguments.get(devIndex - 1));
        assertEquals("--group", arguments.get(testIndex - 1));
    }

    @Test
    void extractWithSingleIncludedGroup() throws Exception {
        UVBuildExtractor extractor = new UVBuildExtractor(executableRunner, tempDir, transformer);
        UVDetectorOptions options = new UVDetectorOptions(
            Arrays.asList("docs"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );

        Extraction extraction = extractor.extract(uvExe, options, tomlParser);
        assertExtractionSuccess(extraction);

        ArgumentCaptor<Executable> captor = ArgumentCaptor.forClass(Executable.class);
        verify(executableRunner).executeSuccessfully(captor.capture());

        List<String> arguments = captor.getValue().getCommandWithArguments();

        long groupCount = arguments.stream().filter(arg -> arg.equals("--group")).count();
        assertEquals(1, groupCount);
        assertTrue(arguments.contains("docs"));
    }

    @Test
    void extractWithAllKeywordAddsAllGroupsFlag() throws Exception {
        UVBuildExtractor extractor = new UVBuildExtractor(executableRunner, tempDir, transformer);
        UVDetectorOptions options = new UVDetectorOptions(
            Arrays.asList("all"),           // "all" keyword
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );

        Extraction extraction = extractor.extract(uvExe, options, tomlParser);
        assertExtractionSuccess(extraction);

        ArgumentCaptor<Executable> captor = ArgumentCaptor.forClass(Executable.class);
        verify(executableRunner).executeSuccessfully(captor.capture());

        List<String> arguments = captor.getValue().getCommandWithArguments();
        assertTrue(arguments.contains("--all-groups"), "Should contain --all-groups flag when 'all' keyword is used");
        assertFalse(arguments.contains("--group"), "Should not contain --group flag when using --all-groups");
    }

    @Test
    void extractWithAllKeywordCaseInsensitive() throws Exception {
        UVBuildExtractor extractor = new UVBuildExtractor(executableRunner, tempDir, transformer);
        UVDetectorOptions options = new UVDetectorOptions(
            Arrays.asList("ALL"),           // uppercase "ALL"
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );

        Extraction extraction = extractor.extract(uvExe, options, tomlParser);
        assertExtractionSuccess(extraction);

        ArgumentCaptor<Executable> captor = ArgumentCaptor.forClass(Executable.class);
        verify(executableRunner).executeSuccessfully(captor.capture());

        List<String> arguments = captor.getValue().getCommandWithArguments();
        assertTrue(arguments.contains("--all-groups"), "Should handle 'ALL' keyword case-insensitively");
    }

    @Test
    void extractWithAllKeywordAndExclusions() throws Exception {
        // When using "all" keyword to include all groups, but also excluding specific groups,
        // both --all-groups and --no-group flags should be present
        UVBuildExtractor extractor = new UVBuildExtractor(executableRunner, tempDir, transformer);
        UVDetectorOptions options = new UVDetectorOptions(
            Arrays.asList("all"),           // include all groups
            Arrays.asList("dev", "test"),   // but exclude dev and test
            Collections.emptyList(),
            Collections.emptyList()
        );

        Extraction extraction = extractor.extract(uvExe, options, tomlParser);
        assertExtractionSuccess(extraction);

        ArgumentCaptor<Executable> captor = ArgumentCaptor.forClass(Executable.class);
        verify(executableRunner).executeSuccessfully(captor.capture());

        List<String> arguments = captor.getValue().getCommandWithArguments();

        // Should have --all-groups flag
        assertTrue(arguments.contains("--all-groups"), "Should contain --all-groups when 'all' keyword is used");

        // Should also have --no-group flags for excluded groups
        assertTrue(arguments.contains("--no-group"), "Should have --no-group for excluded groups even with --all-groups");

        // Verify excluded groups are present
        long noGroupCount = arguments.stream().filter(arg -> arg.equals("--no-group")).count();
        assertEquals(2, noGroupCount, "Should have --no-group for each excluded group");
        assertTrue(arguments.contains("dev"), "dev should be in arguments as excluded");
        assertTrue(arguments.contains("test"), "test should be in arguments as excluded");
    }

    // ==================== Conflict Detection Tests ====================

    @Test
    void extractExcludesGroupWhenInBothIncludedAndExcluded() throws Exception {
        UVBuildExtractor extractor = new UVBuildExtractor(executableRunner, tempDir, transformer);
        UVDetectorOptions options = new UVDetectorOptions(
            Arrays.asList("dev", "test"),   // includedGroups
            Arrays.asList("dev"),           // excludedGroups - "dev" is in both!
            Collections.emptyList(),
            Collections.emptyList()
        );

        Extraction extraction = extractor.extract(uvExe, options, tomlParser);
        assertExtractionSuccess(extraction);

        ArgumentCaptor<Executable> captor = ArgumentCaptor.forClass(Executable.class);
        verify(executableRunner).executeSuccessfully(captor.capture());

        List<String> arguments = captor.getValue().getCommandWithArguments();

        // "dev" should be excluded (--no-group dev) since excluded takes precedence
        assertTrue(arguments.contains("--no-group"), "Excluded groups should have --no-group flag");

        // "test" should be included (--group test) since it's not in excluded
        assertTrue(arguments.contains("--group"), "Non-conflicting included groups should have --group flag");
        assertTrue(arguments.contains("test"), "test group should be included");

        // Find indices and verify correct flags
        int noGroupDevIndex = -1;
        int groupTestIndex = -1;
        for (int i = 0; i < arguments.size() - 1; i++) {
            if (arguments.get(i).equals("--no-group") && arguments.get(i + 1).equals("dev")) {
                noGroupDevIndex = i;
            }
            if (arguments.get(i).equals("--group") && arguments.get(i + 1).equals("test")) {
                groupTestIndex = i;
            }
        }
        assertTrue(noGroupDevIndex >= 0, "dev should be excluded with --no-group");
        assertTrue(groupTestIndex >= 0, "test should be included with --group");
    }

    @Test
    void extractWithMultipleConflictingGroups() throws Exception {
        UVBuildExtractor extractor = new UVBuildExtractor(executableRunner, tempDir, transformer);
        UVDetectorOptions options = new UVDetectorOptions(
            Arrays.asList("dev", "test", "docs"),   // includedGroups
            Arrays.asList("dev", "test"),           // excludedGroups - both dev and test conflict
            Collections.emptyList(),
            Collections.emptyList()
        );

        Extraction extraction = extractor.extract(uvExe, options, tomlParser);
        assertExtractionSuccess(extraction);

        ArgumentCaptor<Executable> captor = ArgumentCaptor.forClass(Executable.class);
        verify(executableRunner).executeSuccessfully(captor.capture());

        List<String> arguments = captor.getValue().getCommandWithArguments();

        // Only "docs" should be included with --group
        long groupCount = arguments.stream().filter(arg -> arg.equals("--group")).count();
        assertEquals(1, groupCount, "Only non-conflicting group should have --group flag");

        // "dev" and "test" should both be excluded
        long noGroupCount = arguments.stream().filter(arg -> arg.equals("--no-group")).count();
        assertEquals(2, noGroupCount, "Conflicting groups should have --no-group flags");
    }

    @Test
    void extractWithEmptyIncludedGroupsAddsNoGroupFlags() throws Exception {
        UVBuildExtractor extractor = new UVBuildExtractor(executableRunner, tempDir, transformer);
        UVDetectorOptions options = new UVDetectorOptions(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );

        Extraction extraction = extractor.extract(uvExe, options, tomlParser);
        assertExtractionSuccess(extraction);

        ArgumentCaptor<Executable> captor = ArgumentCaptor.forClass(Executable.class);
        verify(executableRunner).executeSuccessfully(captor.capture());

        List<String> arguments = captor.getValue().getCommandWithArguments();

        long groupCount = arguments.stream().filter(arg -> arg.equals("--group")).count();
        assertEquals(0, groupCount, "No --group flags should be added when includedGroups is empty");

        long allGroupsCount = arguments.stream().filter(arg -> arg.equals("--all-groups")).count();
        assertEquals(0, allGroupsCount, "No --all-groups flag should be added when includedGroups is empty");
    }

    /**
     * Asserts that the extraction completed on the success path.
     * Without this check, a NullPointerException or other failure inside extract() would be silently
     * swallowed by the catch block, causing all argument-verification assertions to still pass
     * (via Mockito's verify) while the extractor never actually reached the success branch.
     */
    private void assertExtractionSuccess(Extraction extraction) {
        assertTrue(extraction.isSuccess(), "Extraction should succeed but got: " + extraction.getError());
    }
}
