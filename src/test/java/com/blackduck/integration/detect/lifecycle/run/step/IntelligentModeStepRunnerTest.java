package com.blackduck.integration.detect.lifecycle.run.step;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.lifecycle.boot.decision.CorrelatedScanningDecision;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.detect.workflow.blackduck.codelocation.CodeLocationAccumulator;
import com.blackduck.integration.detect.workflow.blackduck.integratedmatching.ScanCountsPayloadCreator;
import com.blackduck.integration.detect.workflow.blackduck.integratedmatching.model.ScanCounts;
import com.blackduck.integration.detect.workflow.blackduck.integratedmatching.model.ScanCountsPayload;

public class IntelligentModeStepRunnerTest {

    private OperationRunner operationRunner;
    private ScanCountsPayloadCreator scanCountsPayloadCreator;
    private IntelligentModeStepRunner intelligentModeStepRunner;

    @BeforeEach
    public void setUp() {
        operationRunner = mock(OperationRunner.class);
        scanCountsPayloadCreator = mock(ScanCountsPayloadCreator.class);
        CorrelatedScanningDecision correlatedScanningDecision = CorrelatedScanningDecision.userEnabled();
        intelligentModeStepRunner = new IntelligentModeStepRunner(operationRunner, null, null, scanCountsPayloadCreator, correlatedScanningDecision);
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
}