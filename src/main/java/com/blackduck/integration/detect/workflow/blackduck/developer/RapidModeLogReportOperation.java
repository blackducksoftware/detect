package com.blackduck.integration.detect.workflow.blackduck.developer;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.api.generated.enumeration.PolicyRuleSeverityType;
import com.blackduck.integration.blackduck.api.generated.view.DeveloperScansScanView;
import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.configuration.enumeration.BlackduckScanMode;
import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.detect.lifecycle.shutdown.ExitCodePublisher;
import com.blackduck.integration.detect.workflow.blackduck.developer.aggregate.RapidScanAggregateResult;
import com.blackduck.integration.detect.workflow.blackduck.developer.aggregate.RapidScanResultAggregator;
import com.blackduck.integration.detect.workflow.blackduck.developer.aggregate.RapidScanResultSummary;
import com.blackduck.integration.log.Slf4jIntLogger;

public class RapidModeLogReportOperation {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ExitCodePublisher exitCodePublisher;
    private final RapidScanResultAggregator rapidScanResultAggregator;
    private String scanMode;

    public RapidModeLogReportOperation(ExitCodePublisher exitCodePublisher, RapidScanResultAggregator rapidScanResultAggregator, BlackduckScanMode mode) {
        this.exitCodePublisher = exitCodePublisher;
        this.rapidScanResultAggregator = rapidScanResultAggregator;
        this.scanMode = mode.displayName();
    }

    public RapidScanResultSummary perform(List<DeveloperScansScanView> results, List<PolicyRuleSeverityType> severitiesToFailPolicyCheck) throws DetectUserFriendlyException {
        RapidScanAggregateResult aggregateResult = rapidScanResultAggregator.aggregateData(results, severitiesToFailPolicyCheck);
        logger.info(String.format("%s:", scanMode + RapidScanDetectResult.NONPERSISTENT_SCAN_RESULT_DETAILS_HEADING));
        aggregateResult.logResult(new Slf4jIntLogger(logger));
        RapidScanResultSummary summary = aggregateResult.getSummary();
        if (summary.hasErrors()) {
            exitCodePublisher.publishExitCode(ExitCodeType.FAILURE_POLICY_VIOLATION, createViolationMessage(summary.getPolicyViolationNames()));
        }
        return summary;
    }

    private String createViolationMessage(Set<String> violatedPolicyNames) {
        StringBuilder stringBuilder = new StringBuilder(200);
        stringBuilder.append("Black Duck found:");
        stringBuilder.append(fixComponentPlural(" %d %s in violation", violatedPolicyNames.size()));
        return stringBuilder.toString();
    }

    private String fixComponentPlural(String formatString, int count) {
        String label = "components";
        if (count == 1)
            label = "component";
        return String.format(formatString, count, label);
    }
}
