package com.blackduck.integration.detect.lifecycle.run.operation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import org.mockito.Mockito;

import com.blackduck.integration.detect.lifecycle.run.DetectFontLoaderFactory;
import com.blackduck.integration.detect.lifecycle.run.singleton.BootSingletons;
import com.blackduck.integration.detect.lifecycle.run.singleton.EventSingletons;
import com.blackduck.integration.detect.lifecycle.run.singleton.UtilitySingletons;
import com.blackduck.integration.detect.tool.detector.factory.DetectDetectableFactory;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.blackduck.api.generated.view.BomStatusScanView;
import com.blackduck.integration.blackduck.api.generated.enumeration.BomStatusScanStatusType;
import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.detect.lifecycle.shutdown.ExitCodePublisher;

class OperationRunnerTest {
    private static OperationRunner operationRunner;
    private static ExitCodePublisher exitCodePublisher;
    
    @BeforeAll
    static void setUp() {
        exitCodePublisher = Mockito.mock(ExitCodePublisher.class);
        EventSingletons eventSingletons = Mockito.mock(EventSingletons.class);
        Mockito.when(eventSingletons.getExitCodePublisher()).thenReturn(exitCodePublisher);
        
        operationRunner = new OperationRunner(
            Mockito.mock(DetectDetectableFactory.class),
            Mockito.mock(DetectFontLoaderFactory.class),
            Mockito.mock(BootSingletons.class),
            Mockito.mock(UtilitySingletons.class),
            eventSingletons
        );
    }

    @Test
    void testCalculateMaxWaitInSeconds() throws IntegrationException {
        // Lower bound edge cases
        Assertions.assertEquals(5, operationRunner.calculateMaxWaitInSeconds(0));
        Assertions.assertEquals(5, operationRunner.calculateMaxWaitInSeconds(-1));
        Assertions.assertEquals(5, operationRunner.calculateMaxWaitInSeconds(2));
        Assertions.assertEquals(5, operationRunner.calculateMaxWaitInSeconds(3));
        Assertions.assertEquals(5, operationRunner.calculateMaxWaitInSeconds(4));

        // Core cases
        Assertions.assertEquals(5, operationRunner.calculateMaxWaitInSeconds(5));
        Assertions.assertEquals(8, operationRunner.calculateMaxWaitInSeconds(6));
        Assertions.assertEquals(13, operationRunner.calculateMaxWaitInSeconds(7));
        Assertions.assertEquals(21, operationRunner.calculateMaxWaitInSeconds(8));
        Assertions.assertEquals(34, operationRunner.calculateMaxWaitInSeconds(9));
        Assertions.assertEquals(55, operationRunner.calculateMaxWaitInSeconds(10));

        // Upper bound edge cases
        Assertions.assertEquals(55, operationRunner.calculateMaxWaitInSeconds(11));
        Assertions.assertEquals(55, operationRunner.calculateMaxWaitInSeconds(100));
    }

    @Test
    void testCheckBomStatusAndHandleFailure() {
        // Test that SUCCESS status does not publish exit code
        BomStatusScanView successView = Mockito.mock(BomStatusScanView.class);
        Mockito.when(successView.getStatus()).thenReturn(BomStatusScanStatusType.SUCCESS);
        
        operationRunner.checkBomStatusAndHandleFailure(successView);
        
        Mockito.verify(exitCodePublisher, Mockito.never()).publishExitCode(Mockito.any(ExitCodeType.class));
        
        // Test that non-SUCCESS statuses publish failure exit code
        BomStatusScanView[] failureViews = {
            createMockBomStatusScanView(BomStatusScanStatusType.BUILDING),
            createMockBomStatusScanView(BomStatusScanStatusType.FAILURE),
            createMockBomStatusScanView(BomStatusScanStatusType.NOT_INCLUDED)
        };
        
        for (BomStatusScanView failureView : failureViews) {
            Mockito.reset(exitCodePublisher);
            operationRunner.checkBomStatusAndHandleFailure(failureView);
            Mockito.verify(exitCodePublisher, Mockito.times(1)).publishExitCode(ExitCodeType.FAILURE_BOM_PREPARATION);
        }
    }
    
    private BomStatusScanView createMockBomStatusScanView(BomStatusScanStatusType status) {
        BomStatusScanView view = Mockito.mock(BomStatusScanView.class);
        Mockito.when(view.getStatus()).thenReturn(status);
        return view;
    }
}
