package com.blackduck.integration.detect.tool.detector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.common.util.finder.SimpleFileFinder;
import com.blackduck.integration.detect.tool.detector.factory.DetectDetectableFactory;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.inspector.ProjectInspectorResolver;
import com.blackduck.integration.detectable.detectable.inspector.nuget.NugetInspectorResolver;
import com.blackduck.integration.detectable.detectables.nuget.NugetInspectorExtractor;
import com.blackduck.integration.detectable.detectables.nuget.NugetInspectorOptions;
import com.blackduck.integration.detectable.detectables.nuget.NugetProjectDetectable;
import com.blackduck.integration.detectable.detectables.nuget.NugetProjectInspectorDetectable;
import com.blackduck.integration.detectable.detectables.nuget.NugetSolutionDetectable;
import com.blackduck.integration.detectable.detectables.projectinspector.ProjectInspectorExtractor;
import com.blackduck.integration.detectable.detectables.projectinspector.ProjectInspectorOptions;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;
import com.blackduck.integration.detector.accuracy.detectable.DetectableEvaluationResult;
import com.blackduck.integration.detector.accuracy.directory.DirectoryEvaluation;
import com.blackduck.integration.detector.accuracy.entrypoint.DetectorRuleEvaluation;
import com.blackduck.integration.detector.accuracy.search.SearchOptions;
import com.blackduck.integration.detector.base.DetectorType;
import com.blackduck.integration.detector.finder.DirectoryFindResult;
import com.blackduck.integration.detector.rule.DetectorRuleSet;

class NugetSolutionFilterDirectoryEvaluatorTest {
    private static final String NUGET_INSPECTOR = "NuGet Solution Native Inspector";
    private static final String PROJECT_INSPECTOR = "NuGet Project Inspector";

    @TempDir
    Path sourceDirectory;

    private final FileFinder fileFinder = new SimpleFileFinder();
    private final NugetInspectorResolver nugetResolver = mock(NugetInspectorResolver.class);
    private final NugetInspectorExtractor nugetExtractor = mock(NugetInspectorExtractor.class);
    private final ProjectInspectorResolver projectResolver = mock(ProjectInspectorResolver.class);
    private final ProjectInspectorExtractor projectExtractor = mock(ProjectInspectorExtractor.class);
    private final ExecutableTarget nugetInspector = ExecutableTarget.forCommand("nuget-inspector");
    private final ExecutableTarget projectInspector = ExecutableTarget.forCommand("project-inspector");

    private NugetInspectorOptions options;
    private DetectorRuleSet nugetRules;

    @BeforeEach
    void setUp() throws Exception {
        Path solutionFilter = Files.write(sourceDirectory.resolve("filtered.slnf"), "{}".getBytes(StandardCharsets.UTF_8));
        options = new NugetInspectorOptions.Builder()
            .excludedModules(Collections.emptyList())
            .includedModules(Collections.emptyList())
            .nugetExcludedDependencyTypes(Collections.emptySet())
            .solutionFilterPath(solutionFilter)
            .sourceDirectory(sourceDirectory.toFile())
            .build();
        when(projectResolver.resolveProjectInspector()).thenReturn(projectInspector);
        when(projectExtractor.extract(any(), anyList(), any(), any(), any())).thenReturn(Extraction.success(Collections.emptyList()));
        when(nugetResolver.resolveNugetInspector(options)).thenReturn(nugetInspector);
        when(nugetExtractor.extract(anyList(), any(), any(), eq(options))).thenReturn(Extraction.failure("invalid solution filter"));

        DetectDetectableFactory factory = mock(DetectDetectableFactory.class);
        when(factory.createNugetSolutionDetectable(any(DetectableEnvironment.class)))
            .thenAnswer(invocation -> new NugetSolutionDetectable(invocation.getArgument(0), fileFinder, nugetResolver, nugetExtractor, options));
        when(factory.createNugetProjectDetectable(any(DetectableEnvironment.class)))
            .thenAnswer(invocation -> new NugetProjectDetectable(invocation.getArgument(0), fileFinder, options, nugetResolver, nugetExtractor));
        when(factory.createNugetParseDetectable(any(DetectableEnvironment.class)))
            .thenAnswer(invocation -> new NugetProjectInspectorDetectable(
                invocation.getArgument(0), fileFinder, projectResolver, projectExtractor, new ProjectInspectorOptions(null, null, null)
            ));
        nugetRules = new DetectorRuleSet(new DetectorRuleFactory().createRules(factory).getDetectorRules().stream()
            .filter(rule -> rule.getDetectorType() == DetectorType.NUGET)
            .collect(Collectors.toList()));
    }

    @Test
    void projectInspectorFallsBackToTheDirectoriesFoundWithoutASolutionFilter() throws Exception {
        Path nested = project("nested", "nested.sln");

        DirectoryEvaluation evaluation = evaluate(false, tree(nested));

        assertTrue(attempts(evaluation).isEmpty());
        assertEquals(Arrays.asList(NUGET_INSPECTOR + ":failed", PROJECT_INSPECTOR + ":extracted"), attempts(evaluation.getChildren().get(0)));
        verify(nugetExtractor, times(1)).extract(anyList(), any(), any(), eq(options));
        verify(projectExtractor).extract(any(), eq(Collections.emptyList()), eq(nested.toFile()), any(File.class), eq(projectInspector));
    }

    @Test
    void everyDirectoryTheProjectInspectorRunsInShowsTheFailedFilterRun() throws Exception {
        Path first = project("first", "first.csproj");
        Path second = project("second", "second.sln");

        DirectoryEvaluation evaluation = evaluate(false, tree(first, second));

        assertTrue(attempts(evaluation).isEmpty());
        assertEquals(Arrays.asList(NUGET_INSPECTOR + ":failed", PROJECT_INSPECTOR + ":extracted"), attempts(evaluation.getChildren().get(0)));
        assertEquals(Arrays.asList(NUGET_INSPECTOR + ":failed", PROJECT_INSPECTOR + ":extracted"), attempts(evaluation.getChildren().get(1)));
    }

    @Test
    void keepsTheFailedFilterRunWhenTheProjectInspectorAppliesNowhere() throws Exception {
        DirectoryEvaluation evaluation = evaluate(false, tree());

        assertEquals(Collections.singletonList(NUGET_INSPECTOR + ":failed"), attempts(evaluation));
        verify(projectExtractor, never()).extract(any(), anyList(), any(), any(), any());
    }

    @Test
    void sourceDirectoryKeepsTheFailedFilterRunBeforeTheProjectInspector() throws Exception {
        Files.createFile(sourceDirectory.resolve("project.csproj"));

        DirectoryEvaluation evaluation = evaluate(false, tree());

        assertEquals(Arrays.asList(NUGET_INSPECTOR + ":failed", PROJECT_INSPECTOR + ":extracted"), attempts(evaluation));
        verify(projectExtractor, times(1)).extract(any(), eq(Collections.emptyList()), eq(sourceDirectory.toFile()), any(File.class), eq(projectInspector));
    }

    @ParameterizedTest
    @CsvSource({ "true", "false" })
    void nestedDirectoriesFollowTheNormalNestingRulesAfterTheSourceDirectory(boolean searchContinue) throws Exception {
        Files.createFile(sourceDirectory.resolve("project.csproj"));
        Path nested = project("nested", "nested.csproj");

        DirectoryEvaluation evaluation = evaluate(searchContinue, tree(nested));

        List<String> expected = searchContinue ? Arrays.asList(NUGET_INSPECTOR + ":failed", PROJECT_INSPECTOR + ":extracted") : Collections.emptyList();
        assertEquals(expected, attempts(evaluation.getChildren().get(0)));
        verify(projectExtractor, times(searchContinue ? 1 : 0)).extract(any(), anyList(), eq(nested.toFile()), any(File.class), any());
    }

    @Test
    void doesNotEvaluateNugetAgainWhenTheFilterSucceeds() throws Exception {
        when(nugetExtractor.extract(anyList(), any(), any(), eq(options))).thenReturn(Extraction.success(Collections.emptyList()));
        Path nested = project("nested", "nested.csproj");

        DirectoryEvaluation evaluation = evaluate(true, tree(nested));

        assertEquals(Collections.singletonList(NUGET_INSPECTOR + ":extracted"), attempts(evaluation));
        assertTrue(nugetEvaluation(evaluation.getChildren().get(0)).getNotFoundEntryPoints().size() > 0);
        assertTrue(attempts(evaluation.getChildren().get(0)).isEmpty());
        verify(projectExtractor, never()).extract(any(), anyList(), any(), any(), any());
    }

    @Test
    void doesNotRunAProjectInspectorThatWasExcluded() throws Exception {
        Files.createFile(sourceDirectory.resolve("project.csproj"));

        DirectoryEvaluation evaluation = evaluate(false, tree(), PROJECT_INSPECTOR);

        assertEquals(Collections.singletonList(NUGET_INSPECTOR + ":failed"), attempts(evaluation));
        verify(projectExtractor, never()).extract(any(), anyList(), any(), any(), any());
    }

    @Test
    void runsOnlyTheProjectInspectorWhenTheNugetInspectorWasExcluded() throws Exception {
        Files.createFile(sourceDirectory.resolve("project.csproj"));

        DirectoryEvaluation evaluation = evaluate(false, tree(), NUGET_INSPECTOR);

        assertEquals(Collections.singletonList(PROJECT_INSPECTOR + ":extracted"), attempts(evaluation));
        verify(nugetExtractor, never()).extract(anyList(), any(), any(), any());
    }

    private Path project(String directoryName, String projectFileName) throws Exception {
        Path directory = Files.createDirectory(sourceDirectory.resolve(directoryName));
        Files.createFile(directory.resolve(projectFileName));
        return directory;
    }

    private DirectoryFindResult tree(Path... nestedDirectories) {
        Set<DirectoryFindResult> children = Arrays.stream(nestedDirectories)
            .map(directory -> new DirectoryFindResult(directory.toFile(), 1, Collections.emptySet()))
            .collect(Collectors.toCollection(LinkedHashSet::new));
        return new DirectoryFindResult(sourceDirectory.toFile(), 0, children);
    }

    private DirectoryEvaluation evaluate(boolean searchContinue, DirectoryFindResult tree, String... excludedDetectors) {
        return new NugetSolutionFilterDirectoryEvaluator(
            new SearchOptions(type -> true, searchContinue),
            Arrays.asList(excludedDetectors),
            type -> new ExtractionEnvironment(sourceDirectory.resolve("output").toFile())
        ).evaluate(tree, nugetRules);
    }

    private DetectorRuleEvaluation nugetEvaluation(DirectoryEvaluation directory) {
        return directory.getEvaluations().stream()
            .filter(evaluation -> evaluation.getRule().getDetectorType() == DetectorType.NUGET)
            .findFirst()
            .orElseThrow(AssertionError::new);
    }

    private List<String> attempts(DirectoryEvaluation directory) {
        return nugetEvaluation(directory).getFoundEntryPoint()
            .map(found -> found.getEntryPointEvaluation().getEvaluatedDetectables().stream()
                .map(this::describe)
                .collect(Collectors.toList()))
            .orElse(Collections.emptyList());
    }

    private String describe(DetectableEvaluationResult result) {
        return result.getDetectableDefinition().getName() + (result.wasExtractionSuccessful() ? ":extracted" : ":failed");
    }
}
