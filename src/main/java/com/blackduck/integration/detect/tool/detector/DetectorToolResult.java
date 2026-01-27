package com.blackduck.integration.detect.tool.detector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import com.blackduck.integration.detect.tool.detector.report.DetectorDirectoryReport;
import com.blackduck.integration.blackduck.bdio2.model.GitInfo;
import com.blackduck.integration.detect.workflow.codelocation.DetectCodeLocation;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detector.base.DetectorType;
import com.blackduck.integration.util.NameVersion;

public class DetectorToolResult {
    @Nullable
    private final NameVersion bomToolProjectNameVersion;
    private final GitInfo gitInfo;
    private final List<DetectCodeLocation> bomToolCodeLocations;

    private final Set<DetectorType> applicableDetectorTypes;
    private final Set<DetectorType> failedDetectorTypes;

    public List<DetectorDirectoryReport> getReports() {
        return reports;
    }

    private final List<DetectorDirectoryReport> reports;
    private final Map<CodeLocation, DetectCodeLocation> codeLocationMap;

    public DetectorToolResult(
        @Nullable NameVersion bomToolProjectNameVersion,
        GitInfo gitInfo,
        List<DetectCodeLocation> bomToolCodeLocations,
        Set<DetectorType> applicableDetectorTypes,
        Set<DetectorType> failedDetectorTypes,
        List<DetectorDirectoryReport> reports,
        Map<CodeLocation, DetectCodeLocation> codeLocationMap
    ) {
        this.bomToolProjectNameVersion = bomToolProjectNameVersion;
        this.gitInfo = gitInfo;
        this.bomToolCodeLocations = bomToolCodeLocations;
        this.applicableDetectorTypes = applicableDetectorTypes;
        this.failedDetectorTypes = failedDetectorTypes;
        this.reports = reports;
        this.codeLocationMap = codeLocationMap;
    }

    public DetectorToolResult() {
        this.bomToolProjectNameVersion = new NameVersion();
        this.gitInfo = GitInfo.none();
        this.bomToolCodeLocations = new ArrayList<>();
        this.applicableDetectorTypes = new HashSet<>();
        this.failedDetectorTypes = new HashSet<>();
        this.reports = new ArrayList<>();
        this.codeLocationMap = new HashMap<>();
    }

    public Optional<NameVersion> getBomToolProjectNameVersion() {
        return Optional.ofNullable(bomToolProjectNameVersion);
    }

    public GitInfo getGitInfo() {
        return gitInfo;
    }

    public List<DetectCodeLocation> getBomToolCodeLocations() {
        return bomToolCodeLocations;
    }

    public Set<DetectorType> getApplicableDetectorTypes() {
        return applicableDetectorTypes;
    }

    public Set<DetectorType> getFailedDetectorTypes() {
        return failedDetectorTypes;
    }

    public boolean anyDetectorsFailed() {
        return !getFailedDetectorTypes().isEmpty();
    }

    public List<DetectorDirectoryReport> getDetectorReports() {
        return reports;
    }

    public Map<CodeLocation, DetectCodeLocation> getCodeLocationMap() {
        return codeLocationMap;
    }

}
