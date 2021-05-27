/*
 * detector
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detector.base;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.synopsys.integration.detectable.detectable.result.CargoLockfileNotFoundDetectableResult;
import com.synopsys.integration.detectable.detectable.result.ExceptionDetectableResult;
import com.synopsys.integration.detectable.detectable.result.ExcludedDetectableResult;
import com.synopsys.integration.detectable.detectable.result.ExecutableNotFoundDetectableResult;
import com.synopsys.integration.detectable.detectable.result.FailedDetectableResult;
import com.synopsys.integration.detectable.detectable.result.FileNotFoundDetectableResult;
import com.synopsys.integration.detectable.detectable.result.FilesNotFoundDetectableResult;
import com.synopsys.integration.detectable.detectable.result.GivenFileNotFoundDetectableResult;
import com.synopsys.integration.detectable.detectable.result.GoPkgLockfileNotFoundDetectableResult;
import com.synopsys.integration.detectable.detectable.result.InspectorNotFoundDetectableResult;
import com.synopsys.integration.detectable.detectable.result.MaxDepthExceededDetectableResult;
import com.synopsys.integration.detectable.detectable.result.NotNestableDetectableResult;
import com.synopsys.integration.detectable.detectable.result.NotSelfNestableDetectableResult;
import com.synopsys.integration.detectable.detectable.result.NpmNodeModulesNotFoundDetectableResult;
import com.synopsys.integration.detectable.detectable.result.PassedDetectableResult;
import com.synopsys.integration.detectable.detectable.result.PoetryLockfileNotFoundDetectableResult;
import com.synopsys.integration.detectable.detectable.result.PropertyInsufficientDetectableResult;
import com.synopsys.integration.detectable.detectable.result.SbtMissingPluginDetectableResult;
import com.synopsys.integration.detectable.detectable.result.SectionNotFoundDetectableResult;
import com.synopsys.integration.detectable.detectable.result.WrongOperatingSystemResult;
import com.synopsys.integration.detector.result.ExcludedDetectorResult;
import com.synopsys.integration.detector.result.FailedDetectorResult;
import com.synopsys.integration.detector.result.ForcedNestedPassedDetectorResult;
import com.synopsys.integration.detector.result.MaxDepthExceededDetectorResult;
import com.synopsys.integration.detector.result.NotNestableDetectorResult;
import com.synopsys.integration.detector.result.NotSelfNestableDetectorResult;
import com.synopsys.integration.detector.result.PassedDetectorResult;
import com.synopsys.integration.detector.result.YieldedDetectorResult;

public class DetectorResultStatusCodeLookup {

    public static DetectorResultStatusCodeLookup standardLookup = new DetectorResultStatusCodeLookup();

    private final Map<Class, DetectorStatusCode> resultClassesToStatusCodes;

    private DetectorResultStatusCodeLookup() {
        this.resultClassesToStatusCodes = populateMap();
    }

    private Map<Class, DetectorStatusCode> populateMap() {
        Map<Class, DetectorStatusCode> map = new HashMap<>();

        map.put(CargoLockfileNotFoundDetectableResult.class, DetectorStatusCode.CARGO_LOCKFILE_NOT_FOUND);
        map.put(ExceptionDetectableResult.class, DetectorStatusCode.EXCEPTION);
        map.put(ExcludedDetectableResult.class, DetectorStatusCode.EXCLUDED);
        map.put(ExcludedDetectorResult.class, DetectorStatusCode.EXCLUDED);
        map.put(ExecutableNotFoundDetectableResult.class, DetectorStatusCode.EXECUTABLE_NOT_FOUND);
        map.put(FailedDetectableResult.class, DetectorStatusCode.FAILED);
        map.put(FailedDetectorResult.class, DetectorStatusCode.FAILED);
        map.put(FilesNotFoundDetectableResult.class, DetectorStatusCode.FILES_NOT_FOUND);
        map.put(FileNotFoundDetectableResult.class, DetectorStatusCode.FILE_NOT_FOUND);
        map.put(GivenFileNotFoundDetectableResult.class, DetectorStatusCode.FILE_NOT_FOUND);
        map.put(ForcedNestedPassedDetectorResult.class, DetectorStatusCode.FORCED_NESTED_PASSED);
        map.put(GoPkgLockfileNotFoundDetectableResult.class, DetectorStatusCode.GO_PKG_LOCKFILE_NOT_FOUND);
        map.put(InspectorNotFoundDetectableResult.class, DetectorStatusCode.INSPECTOR_NOT_FOUND);
        map.put(MaxDepthExceededDetectorResult.class, DetectorStatusCode.MAX_DEPTH_EXCEEDED);
        map.put(MaxDepthExceededDetectableResult.class, DetectorStatusCode.MAX_DEPTH_EXCEEDED);
        map.put(NotNestableDetectorResult.class, DetectorStatusCode.NOT_NESTABLE);
        map.put(NotNestableDetectableResult.class, DetectorStatusCode.NOT_NESTABLE);
        map.put(NotSelfNestableDetectorResult.class, DetectorStatusCode.NOT_SELF_NESTABLE);
        map.put(NotSelfNestableDetectableResult.class, DetectorStatusCode.NOT_SELF_NESTABLE);
        map.put(NpmNodeModulesNotFoundDetectableResult.class, DetectorStatusCode.NPM_NODE_MODULES_NOT_FOUND);
        map.put(PassedDetectorResult.class, DetectorStatusCode.PASSED);
        map.put(PassedDetectableResult.class, DetectorStatusCode.PASSED);
        map.put(PoetryLockfileNotFoundDetectableResult.class, DetectorStatusCode.POETRY_LOCKFILE_NOT_FOUND);
        map.put(PropertyInsufficientDetectableResult.class, DetectorStatusCode.PROPERTY_INSUFFICIENT);
        map.put(SectionNotFoundDetectableResult.class, DetectorStatusCode.SECTION_NOT_FOUND);
        map.put(SbtMissingPluginDetectableResult.class, DetectorStatusCode.SBT_PLUGIN_MISSING);
        map.put(WrongOperatingSystemResult.class, DetectorStatusCode.WRONG_OPERATING_SYSTEM_RESULT);
        map.put(YieldedDetectorResult.class, DetectorStatusCode.YIELDED);

        return map;
    }

    @Nullable
    public DetectorStatusCode getStatusCode(Class resultClass) {
        return resultClassesToStatusCodes.getOrDefault(resultClass, null);
    }
}
