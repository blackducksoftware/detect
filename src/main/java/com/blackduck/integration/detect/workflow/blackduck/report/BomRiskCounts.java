package com.blackduck.integration.detect.workflow.blackduck.report;

import static com.blackduck.integration.blackduck.api.generated.enumeration.RiskPriorityType.CRITICAL;
import static com.blackduck.integration.blackduck.api.generated.enumeration.RiskPriorityType.HIGH;
import static com.blackduck.integration.blackduck.api.generated.enumeration.RiskPriorityType.LOW;
import static com.blackduck.integration.blackduck.api.generated.enumeration.RiskPriorityType.MEDIUM;

import com.blackduck.integration.blackduck.api.generated.component.RiskProfileCountsView;
import com.google.gson.annotations.SerializedName;

public class BomRiskCounts {

    @SerializedName("critical")
    private int critical;

    @SerializedName("high")
    private int high;

    @SerializedName("medium")
    private int medium;

    @SerializedName("low")
    private int low;

    public void add(RiskProfileCountsView countsView) {
        int count = countsView.getCount().intValue();
        if ((CRITICAL == countsView.getCountType())) {
            critical += count;
        } else if (HIGH == countsView.getCountType()) {
            high += count;
        } else if (MEDIUM == countsView.getCountType()) {
            medium += count;
        } else if (LOW == countsView.getCountType()) {
            low += count;
        }
    }

    public void add(BomRiskCounts bomRiskCounts) {
        if (bomRiskCounts.getCritical() > 0) {
            critical++;
        } else if (bomRiskCounts.getHigh() > 0) {
            high++;
        } else if (bomRiskCounts.getMedium() > 0) {
            medium++;
        } else if (bomRiskCounts.getLow() > 0) {
            low++;
        }
    }

    public int getCritical() {
        return critical;
    }

    public int getHigh() {
        return high;
    }

    public int getMedium() {
        return medium;
    }

    public int getLow() {
        return low;
    }

}

