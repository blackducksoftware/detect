package com.blackduck.integration.detect.lifecycle.run.step;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        intelligentModeStepRunner = new IntelligentModeStepRunner(operationRunner, null, null, scanCountsPayloadCreator, "test-run-uuid");
    }

    @Test
    public void testUploadCorrelatedScanCountsWhenNoScanCounts() throws Exception {
        BlackDuckRunData blackDuckRunData = mock(BlackDuckRunData.class);
        CodeLocationAccumulator codeLocationAccumulator = mock(CodeLocationAccumulator.class);
        
        ScanCounts scanCounts = new ScanCounts(0, 0, 0);
        ScanCountsPayload scanCountsPayload = new ScanCountsPayload(scanCounts);

        when(scanCountsPayloadCreator.create(any(), any())).thenReturn(scanCountsPayload);

        intelligentModeStepRunner.uploadCorrelatedScanCounts(blackDuckRunData, codeLocationAccumulator, "test-run-uuid");

        verify(operationRunner, never()).uploadCorrelatedScanCounts(any(), any(), any());
    }
}