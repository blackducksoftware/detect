package com.blackduck.integration.detectable.detectables.nuget.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;
import com.blackduck.integration.detectable.detectables.nuget.NugetInspectorOptions;
import com.blackduck.integration.detectable.detectable.inspector.nuget.NugetInspectorResolver;
import com.blackduck.integration.detectable.detectables.nuget.NugetInspectorExtractor;
import com.blackduck.integration.detectable.detectables.nuget.NugetSolutionDetectable;
import com.blackduck.integration.detectable.util.MockDetectableEnvironment;
import com.blackduck.integration.detectable.util.MockFileFinder;

public class NugetSolutionDetectableTest {
    @Test
    public void testApplicableForSolution() {
        NugetInspectorResolver nugetInspectorManager = null;
        NugetInspectorExtractor nugetInspectorExtractor = null;
        NugetInspectorOptions nugetInspectorOptions = null;

        DetectableEnvironment environment = MockDetectableEnvironment.empty();
        FileFinder fileFinder = MockFileFinder.withFileNamed("test.sln");

        NugetSolutionDetectable detectable = new NugetSolutionDetectable(environment, fileFinder, nugetInspectorManager, nugetInspectorExtractor, nugetInspectorOptions);

        assertTrue(detectable.applicable().getPassed());
    }

    @Test
    public void testApplicableForSolutionXML() {
        NugetInspectorResolver nugetInspectorManager = null;
        NugetInspectorExtractor nugetInspectorExtractor = null;
        NugetInspectorOptions nugetInspectorOptions = null;

        DetectableEnvironment environment = MockDetectableEnvironment.empty();
        FileFinder fileFinder = MockFileFinder.withFileNamed("test.slnx");

        NugetSolutionDetectable detectable = new NugetSolutionDetectable(environment, fileFinder, nugetInspectorManager, nugetInspectorExtractor, nugetInspectorOptions);

        assertTrue(detectable.applicable().getPassed());
    }

    @Test
    public void notApplicableForPodfile() {
        NugetInspectorResolver nugetInspectorManager = null;
        NugetInspectorExtractor nugetInspectorExtractor = null;
        NugetInspectorOptions nugetInspectorOptions = null;

        DetectableEnvironment environment = MockDetectableEnvironment.empty();
        FileFinder fileFinder = MockFileFinder.withFileNamed("podfile.lock");

        NugetSolutionDetectable detectable = new NugetSolutionDetectable(environment, fileFinder, nugetInspectorManager, nugetInspectorExtractor, nugetInspectorOptions);

        assertFalse(detectable.applicable().getPassed());
    }

    @Test
    public void solutionFilterReturnsOnlySlnfAndPassesItAsTarget(@TempDir Path sourceDirectory) throws Exception {
        Path slnf = createSolutionFilter(sourceDirectory);
        NugetInspectorResolver nugetInspectorResolver = mock(NugetInspectorResolver.class);
        NugetInspectorExtractor nugetInspectorExtractor = mock(NugetInspectorExtractor.class);
        ExecutableTarget inspector = ExecutableTarget.forFile(new File("inspector"));
        when(nugetInspectorResolver.resolveNugetInspector(any())).thenReturn(inspector);

        NugetInspectorOptions options = solutionFilterOptions(slnf, sourceDirectory.toFile());
        DetectableEnvironment environment = new MockDetectableEnvironment(sourceDirectory.toFile());
        FileFinder fileFinder = MockFileFinder.withFileNamed("test.sln");

        NugetSolutionDetectable detectable = new NugetSolutionDetectable(environment, fileFinder, nugetInspectorResolver, nugetInspectorExtractor, options);

        DetectableResult applicable = detectable.applicable();
        assertTrue(applicable.getPassed());
        assertEquals(Collections.singletonList(slnf.toFile()), applicable.getRelevantFiles());

        assertTrue(detectable.extractable().getPassed());
        File outputDirectory = sourceDirectory.resolve("out").toFile();
        detectable.extract(new ExtractionEnvironment(outputDirectory));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<File>> targets = ArgumentCaptor.forClass(List.class);
        verify(nugetInspectorExtractor).extract(targets.capture(), eq(outputDirectory), eq(inspector), eq(options));
        assertEquals(Collections.singletonList(slnf.toFile()), targets.getValue());
    }

    @Test
    public void solutionFilterNotApplicableOutsideSourceDirectory(@TempDir Path sourceDirectory) throws IOException {
        Path slnf = createSolutionFilter(sourceDirectory);
        Path nestedDirectory = Files.createDirectories(sourceDirectory.resolve("nested"));

        NugetInspectorOptions options = solutionFilterOptions(slnf, sourceDirectory.toFile());
        DetectableEnvironment environment = new MockDetectableEnvironment(nestedDirectory.toFile());
        FileFinder fileFinder = MockFileFinder.withFileNamed("test.sln");

        NugetSolutionDetectable detectable = new NugetSolutionDetectable(environment, fileFinder, null, null, options);

        assertFalse(detectable.applicable().getPassed());
    }

    private static Path createSolutionFilter(Path directory) throws IOException {
        return Files.write(directory.resolve("filtered.slnf"), "{}".getBytes());
    }

    private static NugetInspectorOptions solutionFilterOptions(Path slnf, File sourceDirectory) {
        return new NugetInspectorOptions.Builder()
            .excludedModules(Collections.emptyList())
            .includedModules(Collections.emptyList())
            .nugetExcludedDependencyTypes(Collections.emptySet())
            .solutionFilterPath(slnf)
            .sourceDirectory(sourceDirectory)
            .build();
    }
}
