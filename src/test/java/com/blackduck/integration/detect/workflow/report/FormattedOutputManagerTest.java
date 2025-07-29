package com.blackduck.integration.detect.workflow.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.blackduck.integration.detect.lifecycle.shutdown.ExitCodeRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detect.configuration.DetectInfo;
import com.blackduck.integration.detect.configuration.enumeration.DetectTool;
import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.detect.workflow.event.Event;
import com.blackduck.integration.detect.workflow.event.EventSystem;
import com.blackduck.integration.detect.workflow.report.output.FormattedCodeLocationOutput;
import com.blackduck.integration.detect.workflow.report.output.FormattedOutput;
import com.blackduck.integration.detect.workflow.report.output.FormattedOutputManager;
import com.blackduck.integration.detect.workflow.status.FormattedCodeLocation;

public class FormattedOutputManagerTest {
    @Test
    public void formattedCodeLocationTest() {
        // Setup the reporting infrastructure
        EventSystem eventSystem = new EventSystem();
        FormattedOutputManager formattedOutputManager = new FormattedOutputManager(eventSystem);
        
        // Mock a completed scan to report on
        List<FormattedCodeLocation> codeLocationExpected = new ArrayList<>();
        codeLocationExpected.add(new FormattedCodeLocation(null, UUID.randomUUID(), DetectTool.DETECTOR.name()));
        
        // Publish the event
        eventSystem.publishEvent(Event.CodeLocationsCompleted, codeLocationExpected);
        
        DetectInfo detectInfo = new DetectInfo("", null, "");
        FormattedOutput formattedOutput = formattedOutputManager.createFormattedOutput(detectInfo, new ExitCodeRequest(ExitCodeType.SUCCESS), Optional.empty());
        
        List<FormattedCodeLocationOutput> codeLocationActual = formattedOutput.codeLocations;
        Assertions.assertEquals(1, codeLocationActual.size());
        Assertions.assertEquals(codeLocationExpected.get(0).getScanId(), codeLocationActual.get(0).scanId);
        Assertions.assertEquals(codeLocationExpected.get(0).getScanType(), codeLocationActual.get(0).scanType);
    }
}

/*
// TODO Jordan deleted this test. It would require a re-write
package com.blackduck.integration.detect.workflow.report;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.blackduck.integration.blackduck.bdio2.model.GitInfo;
import com.blackduck.integration.detect.configuration.DetectInfo;
import com.blackduck.integration.detect.tool.detector.DetectorToolResult;
import com.blackduck.integration.detect.workflow.event.Event;
import com.blackduck.integration.detect.workflow.event.EventSystem;
import com.blackduck.integration.detect.workflow.report.output.FormattedDetectorOutput;
import com.blackduck.integration.detect.workflow.report.output.FormattedOutput;
import com.blackduck.integration.detect.workflow.report.output.FormattedOutputManager;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.result.ExecutableNotFoundDetectableResult;
import com.blackduck.integration.detector.base.DetectorEvaluation;
import com.blackduck.integration.detector.base.DetectorEvaluationTree;
import com.blackduck.integration.detector.base.DetectorStatusCode;
import com.blackduck.integration.detector.base.DetectorType;
import com.blackduck.integration.detector.result.DetectorResult;
import com.blackduck.integration.detector.rule.DetectorRule;

public class FormattedOutputManagerTest {

    @Test
    public <T extends Detectable> void detectorOutputStatusDataTest() throws IllegalAccessException {
        EventSystem eventSystem = new EventSystem();
        FormattedOutputManager formattedOutputManager = new FormattedOutputManager(eventSystem);

        DetectorRule rule = Mockito.mock(DetectorRule.class);
        Mockito.when(rule.getDescriptiveName()).thenReturn("");
        Mockito.when(rule.getName()).thenReturn("");
        Mockito.when(rule.getDetectorType()).thenReturn(DetectorType.GO_MOD);

        DetectorEvaluation detectorEvaluation = new DetectorEvaluation(rule);
        ExecutableNotFoundDetectableResult result = new ExecutableNotFoundDetectableResult("go");
        DetectorResult extractableResult = new DetectorResult(result.getPassed(), result.toDescription(), result.getClass(), Collections.emptyList(), Collections.emptyList());
        detectorEvaluation.setExtractable(extractableResult);
        detectorEvaluation.setApplicable(new DetectorResult(true, "", Collections.emptyList(), Collections.emptyList()));
        detectorEvaluation.setSearchable(new DetectorResult(true, "", Collections.emptyList(), Collections.emptyList()));
        detectorEvaluation.setDetectableEnvironment(new DetectableEnvironment(new File("")));

        DetectorToolResult detectorToolResult = new DetectorToolResult(
            null,
            GitInfo.none(),
            null,
            null,
            new HashSet<>(),
            new DetectorEvaluationTree(null, 0, null, Collections.singletonList(detectorEvaluation), new HashSet<>()),
            null
        );
        eventSystem.publishEvent(Event.DetectorsComplete, detectorToolResult);

        DetectInfo detectInfo = new DetectInfo("", null, "");
        FormattedOutput formattedOutput = formattedOutputManager.createFormattedOutput(detectInfo);
        FormattedDetectorOutput detectorOutput = formattedOutput.detectors.get(0);

        Assertions.assertEquals("FAILURE", detectorOutput.status);
        Assertions.assertEquals(DetectorStatusCode.EXECUTABLE_NOT_FOUND, detectorOutput.statusCode);
        Assertions.assertEquals("No go executable was found.", detectorOutput.statusReason);
    }
}
*/