package com.blackduck.integration.detect.lifecycle.run.step;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detect.configuration.enumeration.DetectTool;
import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.lifecycle.boot.decision.CorrelatedScanningDecision;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.data.DockerTargetData;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.detect.lifecycle.run.step.utility.OperationWrapper;
import com.blackduck.integration.detect.lifecycle.run.step.utility.StepHelper;
import com.blackduck.integration.detect.util.filter.DetectToolFilter;
import com.blackduck.integration.detect.workflow.bdio.BdioResult;
import com.blackduck.integration.detect.workflow.blackduck.codelocation.CodeLocationAccumulator;
import com.blackduck.integration.detect.workflow.blackduck.integratedmatching.ScanCountsPayloadCreator;
import com.blackduck.integration.detect.workflow.blackduck.integratedmatching.model.ScanCounts;
import com.blackduck.integration.detect.workflow.blackduck.integratedmatching.model.ScanCountsPayload;
import com.blackduck.integration.detect.workflow.codelocation.DetectCodeLocationNamesResult;
import com.blackduck.integration.util.NameVersion;

public class IntelligentModeStepRunnerTest {

    private OperationRunner operationRunner;
    private StepHelper stepHelper;
    private ScanCountsPayloadCreator scanCountsPayloadCreator;
    private IntelligentModeStepRunner intelligentModeStepRunner;

    @BeforeEach
    public void setUp() {
        operationRunner = mock(OperationRunner.class);
        stepHelper = mock(StepHelper.class);
        scanCountsPayloadCreator = mock(ScanCountsPayloadCreator.class);
        CorrelatedScanningDecision correlatedScanningDecision = CorrelatedScanningDecision.userEnabled();
        intelligentModeStepRunner = new IntelligentModeStepRunner(operationRunner, stepHelper, null, scanCountsPayloadCreator, correlatedScanningDecision);
    }

    @Test
    public void testUploadCorrelatedScanCountsWhenNoScanCounts() throws Exception {
        BlackDuckRunData blackDuckRunData = mock(BlackDuckRunData.class);
        CodeLocationAccumulator codeLocationAccumulator = mock(CodeLocationAccumulator.class);
        
        ScanCounts scanCounts = new ScanCounts(0, 0, 0);
        ScanCountsPayload scanCountsPayload = new ScanCountsPayload(scanCounts);

        when(scanCountsPayloadCreator.createPayloadFromCountsByTool(any(), any(), any())).thenReturn(scanCountsPayload);

        intelligentModeStepRunner.uploadCorrelatedScanCounts(blackDuckRunData, codeLocationAccumulator);

        verify(operationRunner, never()).uploadCorrelatedScanCounts(any(), any(), any());
    }
    
    @Test
    public void testUploadCorrelatedScanCountsWhenScanCounts() throws OperationException {
        BlackDuckRunData blackDuckRunData = mock(BlackDuckRunData.class);
        CodeLocationAccumulator codeLocationAccumulator = mock(CodeLocationAccumulator.class);
        
        ScanCounts scanCounts = new ScanCounts(1, 0, 0);
        ScanCountsPayload scanCountsPayload = new ScanCountsPayload(scanCounts);

        when(scanCountsPayloadCreator.createPayloadFromCountsByTool(any(), any(), any())).thenReturn(scanCountsPayload);

        intelligentModeStepRunner.uploadCorrelatedScanCounts(blackDuckRunData, codeLocationAccumulator);

        verify(operationRunner, times(1)).uploadCorrelatedScanCounts(any(), any(), any());
    }

    @Test
    public void projectVersionCreationSkippedWhenPropertyFalseAndNoComponentsAndNoScanTools() throws OperationException {
        when(operationRunner.shouldCreateProjectVersionWhenNoComponents()).thenReturn(false);

        BdioResult bdioResult = new BdioResult(
            Collections.emptyList(),
            new DetectCodeLocationNamesResult(Collections.emptyMap()),
            Collections.emptySet(),
            false
        );

        DetectToolFilter detectToolFilter = mock(DetectToolFilter.class);
        when(detectToolFilter.shouldInclude(any(DetectTool.class))).thenReturn(false);

        intelligentModeStepRunner.runOnline(
            mock(BlackDuckRunData.class),
            bdioResult,
            new NameVersion("test-project", "1.0.0"),
            detectToolFilter,
            mock(DockerTargetData.class),
            Collections.emptySet()
        );

        verify(stepHelper, never()).runAsGroup(any(String.class), any(), any(OperationWrapper.OperationSupplier.class));
    }
}