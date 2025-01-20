package com.blackduck.integration.detect.util.filter;

import java.util.Arrays;
import java.util.List;

import com.blackduck.integration.detect.configuration.ExcludeIncludeEnumFilter;
import com.blackduck.integration.detect.configuration.enumeration.BlackduckScanMode;
import com.blackduck.integration.detect.configuration.enumeration.DetectTargetType;
import com.blackduck.integration.detect.configuration.enumeration.DetectTool;
import com.blackduck.integration.detect.lifecycle.boot.decision.BlackDuckDecision;
import com.blackduck.integration.detect.lifecycle.boot.decision.RunDecision;

/**
 * Using this class as a temporary way to handle 'tool' decisions but instead of being an immutable calculation it re-calculates.
 * The idea is in the future, this is pre-calculated at detect-boot and is passed around as a decision so instead of shouldInclude we just ask willBlackDuckRun
 * -jp 3/29/20
 */
public class DetectToolFilter {
    private final ExcludeIncludeEnumFilter<DetectTool> excludedIncludedFilter;
    private final boolean impactEnabled;
    private final RunDecision runDecision;
    private final BlackDuckDecision blackDuckDecision;
    
    // If a rapid scan is specified, default to running package manager and detector scans. Using
    // detect.tools you can run a subset of this list but scans not mentioned here are not possible.
    private final List<DetectTool> rapidTools = Arrays.asList(DetectTool.DETECTOR, DetectTool.DOCKER);
    
    // If a Stateless scan is specified, default to running package manager and signature scans.
    private final List<DetectTool> defaultStatelessTools = Arrays.asList(DetectTool.DETECTOR, DetectTool.SIGNATURE_SCAN);
    
    // A list of all possible Stateless scan types.
    private final List<DetectTool> allowedStatelessTools = Arrays.asList(DetectTool.BAZEL, DetectTool.DETECTOR, DetectTool.SIGNATURE_SCAN, DetectTool.DOCKER, DetectTool.BINARY_SCAN, DetectTool.CONTAINER_SCAN);    

    public DetectToolFilter(
        ExcludeIncludeEnumFilter<DetectTool> excludedIncludedFilter,
        boolean impactEnabled,
        RunDecision runDecision,
        BlackDuckDecision blackDuckDecision
    ) {
        this.excludedIncludedFilter = excludedIncludedFilter;
        this.impactEnabled = impactEnabled;
        this.runDecision = runDecision;
        this.blackDuckDecision = blackDuckDecision;
    }

    public boolean shouldInclude(DetectTool detectTool) { //Only turn tools OFF, turning a tool ON prevents the user from being able to turn an undesired tool OFF.
        if (detectTool == DetectTool.IMPACT_ANALYSIS) {
            return impactEnabled;
        }
        if (detectTool == DetectTool.DETECTOR && runDecision.getDockerMode() == DetectTargetType.IMAGE) {
            return false;
        }
        if (blackDuckDecision.scanMode() == BlackduckScanMode.RAPID && !rapidTools.contains(detectTool)) {
            return false;
        }
        if (blackDuckDecision.scanMode() == BlackduckScanMode.STATELESS) {
            // If the user specifically asked for something, check that it is an allowed
            // tool or we are in docker mode..
            if (excludedIncludedFilter.includeSpecified() || runDecision.isDockerMode()) {
                if (!allowedStatelessTools.contains(detectTool)) {
                    return false;
                }
            // Otherwise only allow default tools.
            } else if (!defaultStatelessTools.contains(detectTool) ) {
                return false;
            }
        }
        
        return excludedIncludedFilter.shouldInclude(detectTool);
    }
    
    public ExcludeIncludeEnumFilter<DetectTool> getExcludedIncludedFilter() {
        return excludedIncludedFilter;
    }
}
