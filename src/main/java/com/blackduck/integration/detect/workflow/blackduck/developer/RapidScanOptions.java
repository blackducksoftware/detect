package com.blackduck.integration.detect.workflow.blackduck.developer;

import java.util.List;

import com.blackduck.integration.blackduck.api.generated.enumeration.PolicyRuleSeverityType;
import com.blackduck.integration.detect.configuration.enumeration.BlackduckScanMode;
import com.blackduck.integration.detect.configuration.enumeration.RapidCompareMode;

public class RapidScanOptions {
    private final RapidCompareMode compareMode;
    private final BlackduckScanMode scanMode;
    private final long detectTimeout;
    private final List<PolicyRuleSeverityType> severitiesToFailPolicyCheck;

    public RapidScanOptions(RapidCompareMode compareMode, BlackduckScanMode scanMode, long detectTimeout, List<PolicyRuleSeverityType> severitiesToFailPolicyCheck) {
        this.compareMode = compareMode;
        this.scanMode = scanMode;
        this.detectTimeout = detectTimeout;
        this.severitiesToFailPolicyCheck = severitiesToFailPolicyCheck;
    }

    public RapidCompareMode getCompareMode() {
        return compareMode;
    }

    public BlackduckScanMode getScanMode() {
        return scanMode;
    }

    public long getDetectTimeout() {
        return detectTimeout;
    }
    
    public List<PolicyRuleSeverityType> getSeveritiesToFailPolicyCheck() {
        return severitiesToFailPolicyCheck;
    }
}
