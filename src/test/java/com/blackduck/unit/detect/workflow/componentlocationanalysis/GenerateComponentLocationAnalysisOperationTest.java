package com.blackduck.unit.detect.workflow.componentlocationanalysis;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.blackduck.integration.detect.configuration.DetectConfigurationFactory;
import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.detect.lifecycle.shutdown.ExitCodePublisher;
import com.blackduck.integration.detect.workflow.componentlocationanalysis.ComponentLocatorException;
import com.blackduck.integration.detect.workflow.componentlocationanalysis.GenerateComponentLocationAnalysisOperation;
import com.blackduck.integration.detect.workflow.status.StatusEventPublisher;

public class GenerateComponentLocationAnalysisOperationTest {
    @Test
    public void testFailComponentLocationAnalysisOperation_AffectsStatus() {
        DetectConfigurationFactory configurationFactory = Mockito.mock(DetectConfigurationFactory.class);
        Mockito.when(configurationFactory.doesComponentLocatorAffectStatus()).thenReturn(true);

        StatusEventPublisher statusEventPublisher = Mockito.mock(StatusEventPublisher.class);
        ExitCodePublisher exitCodePublisher = Mockito.mock(ExitCodePublisher.class);

        GenerateComponentLocationAnalysisOperation op = new GenerateComponentLocationAnalysisOperation(
            configurationFactory,
            statusEventPublisher,
            exitCodePublisher
        );

        try {
            op.failComponentLocationAnalysisOperation();
        } catch (ComponentLocatorException e) {
            Mockito.verify(statusEventPublisher).publishStatusSummary(Mockito.any());
            Mockito.verify(exitCodePublisher).publishExitCode(ExitCodeType.FAILURE_COMPONENT_LOCATION_ANALYSIS);
            Mockito.verifyNoMoreInteractions(statusEventPublisher, exitCodePublisher);
            return;
        }
        assertFalse(true, "An exception should have been thrown");
    }

    @Test
    public void testFailComponentLocationAnalysisOperation_DoesNotAffectStatus() {
        DetectConfigurationFactory configurationFactory = Mockito.mock(DetectConfigurationFactory.class);
        Mockito.when(configurationFactory.doesComponentLocatorAffectStatus()).thenReturn(false);

        StatusEventPublisher statusEventPublisher = Mockito.mock(StatusEventPublisher.class);
        ExitCodePublisher exitCodePublisher = Mockito.mock(ExitCodePublisher.class);

        GenerateComponentLocationAnalysisOperation op = new GenerateComponentLocationAnalysisOperation(
            configurationFactory,
            statusEventPublisher,
            exitCodePublisher
        );

        try {
            op.failComponentLocationAnalysisOperation();
        } catch (ComponentLocatorException e) {
            Mockito.verifyNoInteractions(statusEventPublisher, exitCodePublisher);
            return;
        }
        assertFalse(true, "An exception should have been thrown");
    }    
}
