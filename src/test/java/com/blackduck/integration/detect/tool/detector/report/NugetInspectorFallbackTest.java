package com.blackduck.integration.detect.tool.detector.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.common.util.finder.SimpleFileFinder;
import com.blackduck.integration.detect.tool.detector.DetectorRuleFactory;
import com.blackduck.integration.detect.tool.detector.factory.DetectDetectableFactory;
import com.blackduck.integration.detect.tool.detector.report.util.DetectorReporter;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
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
import com.blackduck.integration.detector.accuracy.detectable.DetectableEvaluator;
import com.blackduck.integration.detector.accuracy.detectable.DetectableExclusionEvaluator;
import com.blackduck.integration.detector.accuracy.directory.DirectoryEvaluation;
import com.blackduck.integration.detector.accuracy.entrypoint.DetectorRuleEvaluation;
import com.blackduck.integration.detector.accuracy.entrypoint.DetectorRuleEvaluator;
import com.blackduck.integration.detector.accuracy.search.SearchEnvironment;
import com.blackduck.integration.detector.accuracy.search.SearchEvaluator;
import com.blackduck.integration.detector.accuracy.search.SearchOptions;
import com.blackduck.integration.detector.base.DetectorType;
import com.blackduck.integration.detector.rule.DetectorRule;

class NugetInspectorFallbackTest {
    @TempDir
    Path sourceDirectory;

    private final FileFinder fileFinder = new SimpleFileFinder();
    private final NugetInspectorResolver nugetResolver = mock(NugetInspectorResolver.class);
    private final NugetInspectorExtractor nugetExtractor = mock(NugetInspectorExtractor.class);
    private final ProjectInspectorResolver projectResolver = mock(ProjectInspectorResolver.class);
    private final ProjectInspectorExtractor projectExtractor = mock(ProjectInspectorExtractor.class);
    private final ExecutableTarget nugetInspector = ExecutableTarget.forCommand("nuget-inspector");
    private final ExecutableTarget projectInspector = ExecutableTarget.forCommand("project-inspector");
    private final ProjectInspectorOptions projectOptions = new ProjectInspectorOptions(null, null, null);

    private Path solutionFilter;
    private Logger reporterLogger;
    private ListAppender<ILoggingEvent> logAppender;
    private boolean solutionFilterUsed;

    @BeforeEach
    void setUp() throws Exception {
        solutionFilter = Files.write(sourceDirectory.resolve("filtered.slnf"), "{}".getBytes(StandardCharsets.UTF_8));
        Files.createFile(sourceDirectory.resolve("project.csproj"));
        when(projectResolver.resolveProjectInspector()).thenReturn(projectInspector);
        when(projectExtractor.extract(any(), anyList(), any(), any(), any()))
            .thenReturn(Extraction.success(Collections.emptyList()));

        reporterLogger = (Logger) LoggerFactory.getLogger(DetectorReporter.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        reporterLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        reporterLogger.detachAppender(logAppender);
        logAppender.stop();
    }

    @Test
    void warnsWhenNugetInspectorCannotBeFoundAndProjectInspectorSucceeds() throws Exception {
        DetectorDirectoryReport report = runDetection(options(true), Collections.emptyList());

        assertProjectInspectorExtracted(report);
        assertFallbackWarnings("NuGet Solution Native Inspector failed", "No nuget inspector was found.");
        verifyProjectInspectorScannedSourceDirectory();
    }

    @Test
    void warnsWhenNugetInspectorResolutionThrows() throws Exception {
        when(nugetResolver.resolveNugetInspector(any())).thenThrow(new DetectableException("inspector download failed"));

        DetectorDirectoryReport report = runDetection(options(true), Collections.emptyList());

        assertProjectInspectorExtracted(report);
        assertFallbackWarnings("NuGet Solution Native Inspector failed", "inspector download failed");
        verifyProjectInspectorScannedSourceDirectory();
    }

    @ParameterizedTest
    @MethodSource("nugetExtractionFailures")
    void warnsWhenNugetInspectorExtractionFails(Extraction failure, String expectedReason) throws Exception {
        NugetInspectorOptions options = options(true);
        when(nugetResolver.resolveNugetInspector(options)).thenReturn(nugetInspector);
        when(nugetExtractor.extract(anyList(), any(), any(), eq(options))).thenReturn(failure);

        DetectorDirectoryReport report = runDetection(options, Collections.emptyList());

        assertProjectInspectorExtracted(report);
        assertFallbackWarnings("NuGet Solution Native Inspector failed", expectedReason);
        verify(nugetExtractor).extract(eq(Collections.singletonList(solutionFilter.toFile())), any(), eq(nugetInspector), eq(options));
        verifyProjectInspectorScannedSourceDirectory();
    }

    private static Stream<Arguments> nugetExtractionFailures() {
        return Stream.of(
            Arguments.of(Extraction.failure("invalid inspector output"), "invalid inspector output"),
            Arguments.of(new Extraction.Builder().exception(new DetectableException("inspector exited with code 23")).build(), "inspector exited with code 23")
        );
    }

    @Test
    void warnsWhenProjectInspectorAlsoFails() throws Exception {
        when(projectExtractor.extract(any(), anyList(), any(), any(), any()))
            .thenReturn(Extraction.failure("project inspector failed"));

        DetectorDirectoryReport report = runDetection(options(true), Collections.emptyList());

        assertEquals(1, report.getNotExtractedDetectors().size());
        assertFallbackWarnings("NuGet Solution Native Inspector failed", "No nuget inspector was found.");
        verifyProjectInspectorScannedSourceDirectory();
    }

    @Test
    void warnsWhenProjectInspectorCannotBeResolved() throws Exception {
        when(projectResolver.resolveProjectInspector()).thenReturn(null);

        DetectorDirectoryReport report = runDetection(options(true), Collections.emptyList());

        assertEquals(1, report.getNotExtractedDetectors().size());
        assertFallbackWarnings("NuGet Solution Native Inspector failed", "No nuget inspector was found.");
        verify(projectExtractor, never()).extract(any(), anyList(), any(), any(), any());
    }

    @Test
    void reportsAnExcludedNativeInspectorWithoutCallingItAFailure() throws Exception {
        DetectorDirectoryReport report = runDetection(options(true), Collections.singletonList("NuGet Solution Native Inspector"));

        assertProjectInspectorExtracted(report);
        assertFallbackWarnings("NuGet Solution Native Inspector was excluded", sourceDirectory.toString());
        verify(nugetResolver, never()).resolveNugetInspector(any());
        verifyProjectInspectorScannedSourceDirectory();
    }

    @Test
    void warnsForNestedFallbackWhenContinueSearchIsEnabled() throws Exception {
        Path nestedDirectory = Files.createDirectory(sourceDirectory.resolve("nested"));
        Files.createFile(nestedDirectory.resolve("nested.csproj"));

        DetectorDirectoryReport report = runDetectionAt(
            options(true), Collections.singletonList("NuGet Solution Native Inspector"), nestedDirectory, 1
        );

        assertProjectInspectorExtracted(report);
        assertFallbackWarnings("NuGet Solution Native Inspector was excluded", nestedDirectory.toString());
        verify(projectExtractor).extract(any(), eq(Collections.emptyList()), eq(nestedDirectory.toFile()), any(File.class), eq(projectInspector));
    }

    @Test
    void warnsForProjectNativeInspectorFailureWithoutASolutionFilter() throws Exception {
        DetectorDirectoryReport report = runDetection(options(false), Collections.emptyList());

        assertProjectInspectorExtracted(report);
        assertFallbackWarnings("NuGet Project Native Inspector failed", "No nuget inspector was found.");
        verifyProjectInspectorScannedSourceDirectory();
    }

    @Test
    void keepsTheExcludedWordingWithoutASolutionFilter() throws Exception {
        DetectorDirectoryReport report = runDetection(options(false), Collections.singletonList("NuGet Project Native Inspector"));

        assertProjectInspectorExtracted(report);
        assertFallbackWarnings("NuGet Project Native Inspector was excluded", sourceDirectory.toString());
    }

    @Test
    void doesNotWarnOrRunFallbackWhenNugetInspectorSucceeds() throws Exception {
        NugetInspectorOptions options = options(true);
        when(nugetResolver.resolveNugetInspector(options)).thenReturn(nugetInspector);
        when(nugetExtractor.extract(anyList(), any(), any(), eq(options)))
            .thenReturn(Extraction.success(Collections.emptyList()));

        DetectorDirectoryReport report = runDetection(options, Collections.emptyList());

        assertEquals("NuGet Solution Native Inspector", report.getExtractedDetectors().get(0).getExtractedDetectable().getDetectable().getName());
        assertTrue(warnings().isEmpty());
        verify(projectResolver, never()).resolveProjectInspector();
    }

    @Test
    void warnsWhenFallbackIsAttemptedButNotApplicable() throws Exception {
        Files.delete(sourceDirectory.resolve("project.csproj"));

        DetectorDirectoryReport report = runDetection(options(true), Collections.emptyList());

        assertEquals(1, report.getNotExtractedDetectors().size());
        assertFallbackWarnings("NuGet Solution Native Inspector failed", "No nuget inspector was found.");
        verify(projectResolver, never()).resolveProjectInspector();
    }

    @Test
    void doesNotWarnWhenProjectInspectorIsExcluded() throws Exception {
        DetectorDirectoryReport report = runDetection(options(true), Collections.singletonList("NuGet Project Inspector"));

        assertEquals(1, report.getNotExtractedDetectors().size());
        assertTrue(warnings().isEmpty());
        verify(projectResolver, never()).resolveProjectInspector();
    }

    @ParameterizedTest
    @CsvSource({ "true, true, 1", "true, false, 0", "false, true, 0" })
    void logsOnceWhenSolutionFilterSkipsNestedNugetDirectories(boolean filterEnabled, boolean searchContinue, int expectedLines) throws Exception {
        NugetInspectorOptions options = options(filterEnabled);
        Path nestedSolution = Files.createDirectory(sourceDirectory.resolve("nested-solution"));
        Files.createFile(nestedSolution.resolve("nested.sln"));
        Path nestedProject = Files.createDirectory(sourceDirectory.resolve("nested-project"));
        Files.createFile(nestedProject.resolve("nested.csproj"));
        List<DirectoryEvaluation> nested = Stream.of(nestedSolution, nestedProject)
            .map(directory -> new DirectoryEvaluation(
                directory.toFile(), 1, Collections.singletonList(evaluate(options, Collections.emptyList(), directory, 1, searchContinue)), Collections.emptyList()
            ))
            .collect(Collectors.toList());
        DirectoryEvaluation root = new DirectoryEvaluation(
            sourceDirectory.toFile(), 0, Collections.singletonList(evaluate(options, Collections.emptyList(), sourceDirectory, 0, searchContinue)), nested
        );

        new DetectorReporter().generateReport(root);

        List<String> skipLines = logAppender.list.stream()
            .filter(event -> event.getLevel() == Level.INFO)
            .map(ILoggingEvent::getFormattedMessage)
            .filter(message -> message.equals(
                "NuGet detection is limited to the solution filter configured in detect.nuget.solution.filter.path; other NuGet solutions and projects are skipped."))
            .collect(Collectors.toList());
        assertEquals(expectedLines, skipLines.size(), logAppender.list.toString());
    }

    private NugetInspectorOptions options(boolean filterEnabled) {
        return new NugetInspectorOptions.Builder()
            .excludedModules(Collections.emptyList())
            .includedModules(Collections.emptyList())
            .nugetExcludedDependencyTypes(Collections.emptySet())
            .solutionFilterPath(filterEnabled ? solutionFilter : null)
            .sourceDirectory(sourceDirectory.toFile())
            .build();
    }

    private DetectorDirectoryReport runDetection(NugetInspectorOptions options, List<String> excludedDetectables) {
        return runDetectionAt(options, excludedDetectables, sourceDirectory, 0);
    }

    private DetectorDirectoryReport runDetectionAt(NugetInspectorOptions options, List<String> excludedDetectables, Path directory, int depth) {
        DirectoryEvaluation directoryEvaluation = new DirectoryEvaluation(
            directory.toFile(), depth, Collections.singletonList(evaluate(options, excludedDetectables, directory, depth, true)), Collections.emptyList()
        );
        solutionFilterUsed = options.getSolutionFilterPath().isPresent();
        return new DetectorReporter(solutionFilterUsed).generateReport(directoryEvaluation).get(0);
    }

    private DetectorRuleEvaluation evaluate(NugetInspectorOptions options, List<String> excludedDetectables, Path directory, int depth, boolean searchContinue) {
        DetectDetectableFactory factory = mock(DetectDetectableFactory.class);
        when(factory.createNugetSolutionDetectable(any(DetectableEnvironment.class)))
            .thenAnswer(invocation -> new NugetSolutionDetectable(invocation.getArgument(0), fileFinder, nugetResolver, nugetExtractor, options));
        when(factory.createNugetProjectDetectable(any(DetectableEnvironment.class)))
            .thenAnswer(invocation -> new NugetProjectDetectable(invocation.getArgument(0), fileFinder, options, nugetResolver, nugetExtractor));
        when(factory.createNugetParseDetectable(any(DetectableEnvironment.class)))
            .thenAnswer(invocation -> new NugetProjectInspectorDetectable(invocation.getArgument(0), fileFinder, projectResolver, projectExtractor, projectOptions));

        DetectorRule nugetRule = new DetectorRuleFactory().createRules(factory).getDetectorRules().stream()
            .filter(rule -> rule.getDetectorType() == DetectorType.NUGET)
            .findFirst()
            .orElseThrow(AssertionError::new);
        DetectorRuleEvaluator evaluator = new DetectorRuleEvaluator(
            new SearchEvaluator(new SearchOptions(type -> true, searchContinue)),
            new DetectableEvaluator(),
            new DetectableExclusionEvaluator(excludedDetectables)
        );
        return evaluator.evaluate(
            new DetectableEnvironment(directory.toFile()),
            new SearchEnvironment(depth, Collections.emptySet(), depth == 0 ? Collections.emptySet() : Collections.singleton(DetectorType.NUGET), Collections.emptySet()),
            nugetRule,
            () -> new ExtractionEnvironment(sourceDirectory.resolve("output").toFile())
        );
    }

    private void assertProjectInspectorExtracted(DetectorDirectoryReport report) {
        assertEquals(1, report.getExtractedDetectors().size());
        assertEquals("NuGet Project Inspector", report.getExtractedDetectors().get(0).getExtractedDetectable().getDetectable().getName());
    }

    private void assertFallbackWarnings(String expectedStatus, String expectedReason) {
        List<String> warnings = warnings();
        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains(expectedStatus), warnings.toString());
        assertTrue(warnings.get(0).contains(expectedReason), warnings.toString());
        assertTrue(warnings.get(0).contains("NuGet Project Inspector fallback"), warnings.toString());
        assertEquals(solutionFilterUsed ? 1 : 0, projectInspectorNotices().size(), logAppender.list.toString());
    }

    private List<String> projectInspectorNotices() {
        return logAppender.list.stream()
            .filter(event -> event.getLevel() == Level.INFO)
            .map(ILoggingEvent::getFormattedMessage)
            .filter(message -> message.equals(
                "NuGet Project Inspector does not support solution filters (detect.nuget.solution.filter.path) or other NuGet-specific Detect properties (detect.nuget.*); they are ignored during fallback."))
            .collect(Collectors.toList());
    }

    private List<String> warnings() {
        return logAppender.list.stream()
            .filter(event -> event.getLevel() == Level.WARN)
            .map(ILoggingEvent::getFormattedMessage)
            .collect(Collectors.toList());
    }

    private void verifyProjectInspectorScannedSourceDirectory() throws Exception {
        verify(projectExtractor).extract(any(), eq(Collections.emptyList()), eq(sourceDirectory.toFile()), any(File.class), eq(projectInspector));
    }
}
