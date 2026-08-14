package com.blackduck.integration.detectable.detectables.uv.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
        assertTrue(arguments.contains("--all-groups"));
    }

    // ==================== Excluded Groups Tests ====================

    @Test
    void extractAddsNoGroupFlagsForExcludedGroups() throws Exception {
        UVBuildExtractor extractor = new UVBuildExtractor(executableRunner, tempDir, transformer);
        UVDetectorOptions options = new UVDetectorOptions(
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
        assertTrue(arguments.contains("--all-groups"));
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
            Collections.emptyList()
        );

        Extraction extraction = extractor.extract(uvExe, options, tomlParser);
        assertExtractionSuccess(extraction);

        ArgumentCaptor<Executable> captor = ArgumentCaptor.forClass(Executable.class);
        verify(executableRunner).executeSuccessfully(captor.capture());

        List<String> arguments = captor.getValue().getCommandWithArguments();

        assertTrue(arguments.contains("--all-groups"));
        long noGroupCount = arguments.stream().filter(arg -> arg.equals("--no-group")).count();
        assertEquals(0, noGroupCount);
    }

    @Test
    void extractWithSingleExcludedGroup() throws Exception {
        UVBuildExtractor extractor = new UVBuildExtractor(executableRunner, tempDir, transformer);
        UVDetectorOptions options = new UVDetectorOptions(
            Arrays.asList("dev"),
            Collections.emptyList(),
            Collections.emptyList()
        );

        Extraction extraction = extractor.extract(uvExe, options, tomlParser);
        assertExtractionSuccess(extraction);

        ArgumentCaptor<Executable> captor = ArgumentCaptor.forClass(Executable.class);
        verify(executableRunner).executeSuccessfully(captor.capture());

        List<String> arguments = captor.getValue().getCommandWithArguments();

        assertTrue(arguments.contains("--all-groups"));
        long noGroupCount = arguments.stream().filter(arg -> arg.equals("--no-group")).count();
        assertEquals(1, noGroupCount);
        assertTrue(arguments.contains("dev"));
    }

    // ==================== Only Groups Tests ====================

    @Test
    void extractAddsOnlyGroupFlagsAndSkipsAllExtrasAllGroups() throws Exception {
        UVBuildExtractor extractor = new UVBuildExtractor(executableRunner, tempDir, transformer);
        UVDetectorOptions options = new UVDetectorOptions(
            Collections.emptyList(),           // excludedGroups
            Arrays.asList("dev", "lint"),       // onlyGroups
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

        // --all-groups should NOT be present when onlyGroups is set
        assertTrue(!arguments.contains("--all-groups"), "Expected --all-groups to be absent when onlyGroups is set");

        // --only-group flags should be present for each group
        long onlyGroupCount = arguments.stream().filter(arg -> arg.equals("--only-group")).count();
        assertEquals(2, onlyGroupCount);
        assertTrue(arguments.contains("dev"));
        assertTrue(arguments.contains("lint"));

        // Each group should be preceded by --only-group
        int devIndex = arguments.indexOf("dev");
        int lintIndex = arguments.indexOf("lint");
        assertTrue(devIndex > 0);
        assertTrue(lintIndex > 0);
        assertEquals("--only-group", arguments.get(devIndex - 1));
        assertEquals("--only-group", arguments.get(lintIndex - 1));
    }

    @Test
    void extractWithSingleOnlyGroup() throws Exception {
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

        assertTrue(!arguments.contains("--all-groups"));
        long onlyGroupCount = arguments.stream().filter(arg -> arg.equals("--only-group")).count();
        assertEquals(1, onlyGroupCount);
        assertTrue(arguments.contains("dev"));
    }

    // ==================== Conflict Handling Tests ====================

    @Test
    void extractExclusionTakesPrecedenceOverOnlyGroup() throws Exception {
        UVBuildExtractor extractor = new UVBuildExtractor(executableRunner, tempDir, transformer);
        // "dev" is in both only and excluded — exclusion should take precedence
        UVDetectorOptions options = new UVDetectorOptions(
            Arrays.asList("dev"),              // excludedGroups
            Arrays.asList("dev", "lint"),       // onlyGroups
            Collections.emptyList(),
            Collections.emptyList()
        );

        Extraction extraction = extractor.extract(uvExe, options, tomlParser);
        assertExtractionSuccess(extraction);

        ArgumentCaptor<Executable> captor = ArgumentCaptor.forClass(Executable.class);
        verify(executableRunner).executeSuccessfully(captor.capture());

        List<String> arguments = captor.getValue().getCommandWithArguments();

        // --all-groups should NOT be present (onlyGroups path)
        assertTrue(!arguments.contains("--all-groups"));

        // "dev" should be excluded — only "lint" should remain as --only-group
        long onlyGroupCount = arguments.stream().filter(arg -> arg.equals("--only-group")).count();
        assertEquals(1, onlyGroupCount, "Only 'lint' should remain after 'dev' is excluded");
        assertTrue(arguments.contains("lint"));
        assertTrue(!arguments.contains("dev"), "'dev' should be excluded from --only-group flags");
    }

    @Test
    void extractAllOnlyGroupsExcludedResultsInEmptyBom() throws Exception {
        UVBuildExtractor extractor = new UVBuildExtractor(executableRunner, tempDir, transformer);
        // All only-groups are also excluded
        UVDetectorOptions options = new UVDetectorOptions(
            Arrays.asList("dev", "lint"),       // excludedGroups
            Arrays.asList("dev", "lint"),       // onlyGroups
            Collections.emptyList(),
            Collections.emptyList()
        );

        Extraction extraction = extractor.extract(uvExe, options, tomlParser);
        assertExtractionSuccess(extraction);

        // uv tree should never be executed since all groups are excluded
        verify(executableRunner, never()).executeSuccessfully(any(Executable.class));

        // BOM should exist but contain zero dependencies
        assertEquals(1, extraction.getCodeLocations().size(), "Expected one code location for empty BOM");
        assertEquals(0, extraction.getCodeLocations().get(0).getDependencyGraph().getRootDependencies().size(),
                "Expected zero dependencies when all only-groups are excluded");
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
