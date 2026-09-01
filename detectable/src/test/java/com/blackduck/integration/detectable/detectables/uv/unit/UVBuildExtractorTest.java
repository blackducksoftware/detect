package com.blackduck.integration.detectable.detectables.uv.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );

        Extraction extraction = extractor.extract(uvExe, options, tomlParser);
        assertExtractionSuccess(extraction);

        List<String> arguments = captureArguments();
        assertTrue(arguments.contains("tree"), "Expected 'tree' command in arguments");
        assertTrue(arguments.contains("--no-dedupe"), "Expected '--no-dedupe' flag in arguments");
        assertTrue(arguments.contains("--all-groups"), "Expected '--all-groups' flag when no onlyGroups specified");
    }

    // ==================== Excluded Groups Tests ====================

    @Test
    void extractAddsNoGroupFlagsForExcludedGroups() throws Exception {
        UVBuildExtractor extractor = new UVBuildExtractor(executableRunner, tempDir, transformer);
        UVDetectorOptions options = new UVDetectorOptions(
            Arrays.asList("dev", "test"),
            Collections.emptyList(),
            Collections.emptyList()
        );

        Extraction extraction = extractor.extract(uvExe, options, tomlParser);
        assertExtractionSuccess(extraction);

        List<String> arguments = captureArguments();
        assertTrue(arguments.contains("tree"), "Expected 'tree' command in arguments");
        assertTrue(arguments.contains("--no-dedupe"), "Expected '--no-dedupe' flag in arguments");
        assertTrue(arguments.contains("--all-groups"), "Expected '--all-groups' flag when only excludedGroups specified");
        assertTrue(arguments.contains("--no-group"), "Expected '--no-group' flag for excluded groups");

        int devIndex = arguments.indexOf("dev");
        int testIndex = arguments.indexOf("test");
        assertTrue(devIndex > 0, "Expected 'dev' to be present in arguments");
        assertTrue(testIndex > 0, "Expected 'test' to be present in arguments");
        assertEquals("--no-group", arguments.get(devIndex - 1), "'dev' should be preceded by '--no-group' flag");
        assertEquals("--no-group", arguments.get(testIndex - 1), "'test' should be preceded by '--no-group' flag");
    }

    @Test
    void extractWithEmptyExcludedGroupsAddsNoNoGroupFlags() throws Exception {
        UVBuildExtractor extractor = new UVBuildExtractor(executableRunner, tempDir, transformer);
        UVDetectorOptions options = new UVDetectorOptions(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );

        Extraction extraction = extractor.extract(uvExe, options, tomlParser);
        assertExtractionSuccess(extraction);

        List<String> arguments = captureArguments();
        assertTrue(arguments.contains("--all-groups"), "Expected '--all-groups' flag with no exclusions");
        long noGroupCount = arguments.stream().filter(arg -> arg.equals("--no-group")).count();
        assertEquals(0, noGroupCount, "Expected zero '--no-group' flags when no groups are excluded");
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

        List<String> arguments = captureArguments();
        assertTrue(arguments.contains("--all-groups"), "Expected '--all-groups' flag with excluded groups");
        long noGroupCount = arguments.stream().filter(arg -> arg.equals("--no-group")).count();
        assertEquals(1, noGroupCount, "Expected exactly one '--no-group' flag for single excluded group");
        assertTrue(arguments.contains("dev"), "Expected 'dev' in arguments as the excluded group");
    }

    // ==================== Only Groups Tests ====================

    @Test
    void extractWithOnlyGroupsDoesNotIncludeAllGroupsFlag() throws Exception {
        UVBuildExtractor extractor = new UVBuildExtractor(executableRunner, tempDir, transformer);
        UVDetectorOptions options = new UVDetectorOptions(
            Collections.emptyList(),
            Arrays.asList("dev", "lint"),
            Collections.emptyList(),
            Collections.emptyList()
        );

        Extraction extraction = extractor.extract(uvExe, options, tomlParser);
        assertExtractionSuccess(extraction);

        List<String> arguments = captureArguments();
        assertFalse(arguments.contains("--all-groups"), "'--all-groups' should be absent when onlyGroups is set");
    }

    @Test
    void extractWithOnlyGroupsAddsOnlyGroupFlagForEachGroup() throws Exception {
        UVBuildExtractor extractor = new UVBuildExtractor(executableRunner, tempDir, transformer);
        UVDetectorOptions options = new UVDetectorOptions(
            Collections.emptyList(),
            Arrays.asList("dev", "lint"),
            Collections.emptyList(),
            Collections.emptyList()
        );

        Extraction extraction = extractor.extract(uvExe, options, tomlParser);
        assertExtractionSuccess(extraction);

        List<String> arguments = captureArguments();
        long onlyGroupCount = arguments.stream().filter(arg -> arg.equals("--only-group")).count();
        assertEquals(2, onlyGroupCount, "Expected two '--only-group' flags for [dev, lint]");
        assertTrue(arguments.contains("dev"), "Expected 'dev' group in arguments");
        assertTrue(arguments.contains("lint"), "Expected 'lint' group in arguments");
    }

    @Test
    void extractWithOnlyGroupsEachGroupPrecededByFlag() throws Exception {
        UVBuildExtractor extractor = new UVBuildExtractor(executableRunner, tempDir, transformer);
        UVDetectorOptions options = new UVDetectorOptions(
            Collections.emptyList(),
            Arrays.asList("dev", "lint"),
            Collections.emptyList(),
            Collections.emptyList()
        );

        Extraction extraction = extractor.extract(uvExe, options, tomlParser);
        assertExtractionSuccess(extraction);

        List<String> arguments = captureArguments();
        int devIndex = arguments.indexOf("dev");
        int lintIndex = arguments.indexOf("lint");
        assertTrue(devIndex > 0, "Expected 'dev' to be present in arguments");
        assertTrue(lintIndex > 0, "Expected 'lint' to be present in arguments");
        assertEquals("--only-group", arguments.get(devIndex - 1), "'dev' should be immediately preceded by '--only-group'");
        assertEquals("--only-group", arguments.get(lintIndex - 1), "'lint' should be immediately preceded by '--only-group'");
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

        List<String> arguments = captureArguments();
        assertFalse(arguments.contains("--all-groups"), "'--all-groups' should be absent when onlyGroups is set");
        long onlyGroupCount = arguments.stream().filter(arg -> arg.equals("--only-group")).count();
        assertEquals(1, onlyGroupCount, "Expected exactly one '--only-group' flag for single only-group");
        assertTrue(arguments.contains("dev"), "Expected 'dev' group in arguments");
    }

    // ==================== Conflict Handling Tests ====================

    @Test
    void extractExclusionTakesPrecedenceOverOnlyGroup() throws Exception {
        UVBuildExtractor extractor = new UVBuildExtractor(executableRunner, tempDir, transformer);
        UVDetectorOptions options = new UVDetectorOptions(
            Arrays.asList("dev"),
            Arrays.asList("dev", "lint"),
            Collections.emptyList(),
            Collections.emptyList()
        );

        Extraction extraction = extractor.extract(uvExe, options, tomlParser);
        assertExtractionSuccess(extraction);

        List<String> arguments = captureArguments();
        assertFalse(arguments.contains("--all-groups"), "'--all-groups' should be absent when onlyGroups is set");

        long onlyGroupCount = arguments.stream().filter(arg -> arg.equals("--only-group")).count();
        assertEquals(1, onlyGroupCount, "Only 'lint' should remain after 'dev' is excluded");
        assertTrue(arguments.contains("lint"), "Expected 'lint' to remain as --only-group");
        assertFalse(arguments.contains("dev"), "'dev' should be excluded from --only-group flags");
    }

    @Test
    void extractAllOnlyGroupsExcludedResultsInEmptyBom() throws Exception {
        UVBuildExtractor extractor = new UVBuildExtractor(executableRunner, tempDir, transformer);
        UVDetectorOptions options = new UVDetectorOptions(
            Arrays.asList("dev", "lint"),
            Arrays.asList("dev", "lint"),
            Collections.emptyList(),
            Collections.emptyList()
        );

        Extraction extraction = extractor.extract(uvExe, options, tomlParser);
        assertExtractionSuccess(extraction);

        verify(executableRunner, never()).executeSuccessfully(any(Executable.class));

        assertEquals(1, extraction.getCodeLocations().size(), "Expected one code location for empty BOM");
        assertEquals(0, extraction.getCodeLocations().get(0).getDependencyGraph().getRootDependencies().size(),
                "Expected zero dependencies when all only-groups are excluded");
    }

    // ==================== Helper Methods ====================

    private void assertExtractionSuccess(Extraction extraction) {
        assertTrue(extraction.isSuccess(), "Extraction should succeed but got: " + extraction.getError());
    }

    private List<String> captureArguments() throws Exception {
        ArgumentCaptor<Executable> captor = ArgumentCaptor.forClass(Executable.class);
        verify(executableRunner).executeSuccessfully(captor.capture());
        return captor.getValue().getCommandWithArguments();
    }
}
