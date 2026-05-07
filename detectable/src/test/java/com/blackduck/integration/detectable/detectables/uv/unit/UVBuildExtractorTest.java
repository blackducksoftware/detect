package com.blackduck.integration.detectable.detectables.uv.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

        uvExe = ExecutableTarget.forFile(new File("/usr/bin/uv"));

        ExecutableOutput mockOutput = mock(ExecutableOutput.class);
        when(mockOutput.getStandardOutputAsList()).thenReturn(Arrays.asList(
            "my-project v1.0.0",
            "├── requests v2.31.0"
        ));
        when(executableRunner.executeSuccessfully(any(Executable.class))).thenReturn(mockOutput);
    }

    @Test
    void extractBuildsBasicArguments() throws Exception {
        UVBuildExtractor extractor = new UVBuildExtractor(executableRunner, tempDir, transformer);
        UVDetectorOptions options = new UVDetectorOptions(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );

        extractor.extract(uvExe, options, tomlParser);

        ArgumentCaptor<Executable> captor = ArgumentCaptor.forClass(Executable.class);
        verify(executableRunner).executeSuccessfully(captor.capture());

        List<String> arguments = captor.getValue().getCommandWithArguments();
        assertTrue(arguments.contains("tree"));
        assertTrue(arguments.contains("--no-dedupe"));
    }

    @Test
    void extractAddsNoGroupFlagsForExcludedGroups() throws Exception {
        UVBuildExtractor extractor = new UVBuildExtractor(executableRunner, tempDir, transformer);
        UVDetectorOptions options = new UVDetectorOptions(
            Arrays.asList("dev", "test"),
            Collections.emptyList(),
            Collections.emptyList()
        );

        extractor.extract(uvExe, options, tomlParser);

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
            Collections.emptyList()
        );

        extractor.extract(uvExe, options, tomlParser);

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
            Arrays.asList("dev"),
            Collections.emptyList(),
            Collections.emptyList()
        );

        extractor.extract(uvExe, options, tomlParser);

        ArgumentCaptor<Executable> captor = ArgumentCaptor.forClass(Executable.class);
        verify(executableRunner).executeSuccessfully(captor.capture());

        List<String> arguments = captor.getValue().getCommandWithArguments();

        long noGroupCount = arguments.stream().filter(arg -> arg.equals("--no-group")).count();
        assertEquals(1, noGroupCount);
        assertTrue(arguments.contains("dev"));
    }
}
