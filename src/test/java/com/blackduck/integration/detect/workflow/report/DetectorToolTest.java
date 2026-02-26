package com.blackduck.integration.detect.workflow.report;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import com.blackduck.integration.detect.tool.detector.DetectorTool;
import com.blackduck.integration.detect.tool.detector.DetectorToolResult;
import com.blackduck.integration.detect.tool.detector.report.DetectorDirectoryReport;
import com.blackduck.integration.detect.tool.detector.report.rule.ExtractedDetectorRuleReport;
import com.blackduck.integration.detect.tool.detector.report.detectable.ExtractedDetectableReport;
import com.blackduck.integration.detect.workflow.file.DirectoryManager;
import com.blackduck.integration.detect.workflow.file.DirectoryOptions;
import com.blackduck.integration.detect.workflow.DetectRunId;

import static com.blackduck.integration.detect.workflow.componentlocationanalysis.GenerateComponentLocationAnalysisOperation.INVOKED_DETECTORS_AND_RELEVANT_FILES_JSON;
import static com.blackduck.integration.detect.workflow.componentlocationanalysis.GenerateComponentLocationAnalysisOperation.QUACKPATCH_SUBDIRECTORY_NAME;
import static org.junit.jupiter.api.Assertions.*;
import com.blackduck.integration.detector.rule.DetectableDefinition;
import com.blackduck.integration.detectable.detectables.gradle.inspection.GradleInspectorDetectable;
import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.detectable.executable.resolver.GradleResolver;
import com.blackduck.integration.detectable.detectable.inspector.GradleInspectorResolver;
import com.blackduck.integration.detectable.detectables.gradle.inspection.GradleInspectorExtractor;
import com.blackduck.integration.detectable.detectables.gradle.inspection.GradleInspectorOptions;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import java.lang.reflect.Field;


class DetectorToolTest {
    @Test
    void testSaveExtractedDetectorsAndTheirRelevantFilePaths_gradle() throws Exception {
        Path tempDir = Files.createTempDirectory("detect-tool-test");
        DirectoryManager directoryManager = new DirectoryManager(
            new DirectoryOptions(tempDir, tempDir, tempDir, tempDir, tempDir, tempDir, tempDir),
            DetectRunId.createDefault()
        );
        File relevantFile = File.createTempFile("build", ".gradle", tempDir.toFile());

        FileFinder fileFinder = (dir, filter, followSymLinks, depth, findInsideMatchingDirectories) -> Collections.singletonList(relevantFile);
        GradleResolver gradleResolver = env -> null;
        GradleInspectorResolver gradleInspectorResolver = () -> null;
        GradleInspectorExtractor gradleInspectorExtractor = new GradleInspectorExtractor(fileFinder, null, null, null, null, null);
        GradleInspectorOptions gradleInspectorOptions = new GradleInspectorOptions(null, null, null, null);

        DetectableInfo info = GradleInspectorDetectable.class.getAnnotation(DetectableInfo.class);
        DetectableDefinition gradleInspectorDefinition = new DetectableDefinition(
            env -> new GradleInspectorDetectable(
                env, fileFinder, gradleResolver, gradleInspectorResolver, gradleInspectorExtractor, gradleInspectorOptions
            ),
            info.name(), info.forge(), info.language(), info.requirementsMarkdown(), info.accuracy()
        );

        ExtractedDetectableReport extractedDetectableReport = new ExtractedDetectableReport(
            gradleInspectorDefinition, Collections.emptyList(), Collections.singletonList(relevantFile), null, null
        );
        ExtractedDetectorRuleReport extractedDetectorRuleReport = new ExtractedDetectorRuleReport(
            null, 0, Collections.emptyList(), Collections.emptyList(), extractedDetectableReport
        );
        DetectorDirectoryReport directoryReport = new DetectorDirectoryReport(
            tempDir.toFile(), 0, Collections.emptyList(), Collections.singletonList(extractedDetectorRuleReport), Collections.emptyList()
        );
        ArrayList<DetectorDirectoryReport> reports = new ArrayList<>();
        reports.add(directoryReport);
        DetectorToolResult toolResult = new DetectorToolResult();
        Field reportsField = DetectorToolResult.class.getDeclaredField("reports");
        reportsField.setAccessible(true);
        reportsField.set(toolResult, reports);

        new DetectorTool(null, null, null, null, null, null, null)
            .saveExtractedDetectorsAndTheirRelevantFilePaths(directoryManager, toolResult);

        File outputFile = new File(
            new File(directoryManager.getScanOutputDirectory(), QUACKPATCH_SUBDIRECTORY_NAME),
            INVOKED_DETECTORS_AND_RELEVANT_FILES_JSON
        );
        assertTrue(outputFile.exists());
        String content = new String(Files.readAllBytes(outputFile.toPath()));
        assertTrue(content.contains(info.name()));
        assertTrue(content.contains(relevantFile.getName()));
    }
}
