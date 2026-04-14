package com.blackduck.integration.detect.workflow.blackduck.developer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.blackduck.integration.blackduck.api.generated.component.DeveloperScansScanItemsViolatingPoliciesView;
import com.blackduck.integration.blackduck.api.generated.enumeration.PolicyRuleSeverityType;
import com.blackduck.integration.blackduck.api.generated.view.DeveloperScansScanView;
import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.configuration.enumeration.BlackduckScanMode;
import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.detect.lifecycle.shutdown.ExitCodePublisher;
import com.blackduck.integration.detect.workflow.blackduck.developer.RapidModeLogReportOperation;
import com.blackduck.integration.detect.workflow.blackduck.developer.aggregate.RapidScanAggregateResult;
import com.blackduck.integration.detect.workflow.blackduck.developer.aggregate.RapidScanResultAggregator;
import com.blackduck.integration.detect.workflow.blackduck.developer.aggregate.RapidScanResultSummary;

public class RapidModeLogReportOperationTest {

    @Test
    void testPublishesPolicyViolation() throws DetectUserFriendlyException {
        ExitCodePublisher exitCodePublisher = Mockito.mock(ExitCodePublisher.class);
        RapidScanResultAggregator rapidScanResultAggregator = Mockito.mock(RapidScanResultAggregator.class);
        RapidModeLogReportOperation op = new RapidModeLogReportOperation(exitCodePublisher, rapidScanResultAggregator, BlackduckScanMode.RAPID);

        List<DeveloperScansScanView> results = new LinkedList<>();
        DeveloperScansScanView resultView = Mockito.mock(DeveloperScansScanView.class);
        results.add(resultView);
        
        RapidScanAggregateResult aggregateResult = Mockito.mock(RapidScanAggregateResult.class);
        List<PolicyRuleSeverityType> violatingPoicies = 
                new ArrayList<>(Arrays.asList(PolicyRuleSeverityType.BLOCKER, PolicyRuleSeverityType.CRITICAL));
        Mockito.when(rapidScanResultAggregator.aggregateData(results, violatingPoicies)).thenReturn(aggregateResult);

        RapidScanResultSummary summary = Mockito.mock(RapidScanResultSummary.class);
        Mockito.when(summary.hasErrors()).thenReturn(true);
        Mockito.when(aggregateResult.getSummary()).thenReturn(summary);

        Set<String> policyViolationNames = new HashSet<>();
        policyViolationNames.add("testPolicy1");
        policyViolationNames.add("testPolicy2");
        Mockito.when(summary.getPolicyViolationNames()).thenReturn(policyViolationNames);

        RapidScanResultSummary returnedSummary = op.perform(results, violatingPoicies);

        assertEquals(summary, returnedSummary);
        Mockito.verify(exitCodePublisher, Mockito.times(1))
            .publishExitCode(Mockito.eq(ExitCodeType.FAILURE_POLICY_VIOLATION));
    }
}
