package com.blackduck.integration.detect.workflow.bdio;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.blackduck.integration.blackduck.codelocation.upload.UploadTarget;
import com.blackduck.integration.detect.workflow.codelocation.DetectCodeLocationNamesResult;
import com.blackduck.integration.detector.base.DetectorType;

public class BdioResult {
    private final List<UploadTarget> uploadTargets;
    private final DetectCodeLocationNamesResult codeLocationNamesResult;
    private final Set<DetectorType> applicableDetectorTypes;
    private final boolean hasComponents;

    public static BdioResult none() {
        DetectCodeLocationNamesResult emptyNamesResult = new DetectCodeLocationNamesResult(Collections.emptyMap());
        return new BdioResult(Collections.emptyList(), emptyNamesResult, Collections.emptySet(), false);
    }

    public BdioResult(List<UploadTarget> uploadTargets, DetectCodeLocationNamesResult codeLocationNamesResult, Set<DetectorType> applicableDetectorTypes, boolean hasComponents) {
        this.uploadTargets = uploadTargets;
        this.codeLocationNamesResult = codeLocationNamesResult;
        this.applicableDetectorTypes = applicableDetectorTypes;
        this.hasComponents = hasComponents;
    }

    public List<UploadTarget> getUploadTargets() {
        return uploadTargets;
    }

    public DetectCodeLocationNamesResult getCodeLocationNamesResult() {
        return codeLocationNamesResult;
    }

    public boolean isNotEmpty() {
        return !uploadTargets.isEmpty();
    }

    public boolean hasComponents() {
        return hasComponents;
    }

    public Set<DetectorType> getApplicableDetectorTypes() {
        return applicableDetectorTypes;
    }
}
