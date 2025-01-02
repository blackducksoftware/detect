package com.blackduck.integration.detect.lifecycle.shutdown;

import java.io.File;

import org.jetbrains.annotations.Nullable;

public class CleanupDecision {
    private final boolean shouldCleanup;
    private final boolean shouldPreserveScan;
    private final boolean shouldPreserveBdio;
    private final boolean shouldPreserveCsv;   
    private final boolean shouldPreserveAirGap;
    private final boolean shouldPreserveIac;

    @Nullable
    private final File airGapZip;

    public CleanupDecision(
        boolean shouldCleanup,
        boolean shouldPreserveScan,
        boolean shouldPreserveBdio,
        boolean shouldPreserveCsv,
        boolean shouldPreserveAirGap,
        boolean shouldPreserveIac,
        @Nullable File airGapZip
    ) {
        this.shouldCleanup = shouldCleanup;
        this.shouldPreserveScan = shouldPreserveScan;
        this.shouldPreserveBdio = shouldPreserveBdio;
        this.shouldPreserveCsv = shouldPreserveCsv;
        this.shouldPreserveAirGap = shouldPreserveAirGap;
        this.shouldPreserveIac = shouldPreserveIac;
        this.airGapZip = airGapZip;
    }

    public static CleanupDecision skip() {
        return new CleanupDecision(false, false, false, false, false, false, null);
    }

    public boolean shouldCleanup() {
        return shouldCleanup;
    }

    public boolean shouldPreserveScan() {
        return shouldPreserveScan;
    }

    public boolean shouldPreserveBdio() {
        return shouldPreserveBdio;
    }
    
    public boolean shouldPreserveCsv() {
        return shouldPreserveCsv;
    }

    public boolean shouldPreserveAirGap() {
        return shouldPreserveAirGap;
    }

    public boolean shouldPreserveIac() {
        return shouldPreserveIac;
    }

    public File getAirGapZip() {
        return airGapZip;
    }
}
