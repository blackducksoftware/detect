package com.blackduck.integration.detect.lifecycle.boot.decision;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CorrelatedScanningDecision {
    public enum DecisionSource {
        USER_ENABLED,
        USER_DISABLED,
        SERVER_ENABLED,
        SERVER_DISABLED,
        DEFAULT_DISABLED,
        OFFLINE_MODE_DISABLED
    }

    private final boolean enabled;
    private final Set<String> supportedScanTypes;
    private final DecisionSource source;

    private CorrelatedScanningDecision(boolean enabled, Set<String> supportedScanTypes, DecisionSource source) {
        this.enabled = enabled;
        this.supportedScanTypes = supportedScanTypes != null ? new HashSet<>(supportedScanTypes) : Collections.emptySet();
        this.source = source;
    }

    public static CorrelatedScanningDecision userEnabled() {
        // When user enables, support all scan types by default
        return new CorrelatedScanningDecision(
            true,
            createAllScanTypes(),
            DecisionSource.USER_ENABLED
        );
    }

    public static CorrelatedScanningDecision userDisabled() {
        return new CorrelatedScanningDecision(
            false,
            Collections.emptySet(),
            DecisionSource.USER_DISABLED
        );
    }

    public static CorrelatedScanningDecision serverEnabled(List<String> supportedScanTypes) {
        return new CorrelatedScanningDecision(
            true,
            new HashSet<>(supportedScanTypes != null ? supportedScanTypes : Collections.emptyList()),
            DecisionSource.SERVER_ENABLED
        );
    }

    public static CorrelatedScanningDecision serverDisabled() {
        return new CorrelatedScanningDecision(
            false,
            Collections.emptySet(),
            DecisionSource.SERVER_DISABLED
        );
    }

    public static CorrelatedScanningDecision defaultDisabled() {
        return new CorrelatedScanningDecision(
            false,
            Collections.emptySet(),
            DecisionSource.DEFAULT_DISABLED
        );
    }

    public static CorrelatedScanningDecision offlineModeDisabled() {
        return new CorrelatedScanningDecision(
            false,
            Collections.emptySet(),
            DecisionSource.OFFLINE_MODE_DISABLED
        );
    }

    private static Set<String> createAllScanTypes() {
        Set<String> allTypes = new HashSet<>();
        allTypes.add("PACKAGE_MANAGER");
        allTypes.add("SIGNATURE");
        allTypes.add("BINARY");
        return allTypes;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Set<String> getSupportedScanTypes() {
        return Collections.unmodifiableSet(supportedScanTypes);
    }

    public boolean isScanTypeSupported(String scanType) {
        return supportedScanTypes.contains(scanType);
    }

    public DecisionSource getSource() {
        return source;
    }
}
