package com.blackduck.integration.detector.base;

import java.util.HashMap;
import java.util.Map;

import com.blackduck.integration.detectable.detectable.result.*;
import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.detector.result.ExcludedDetectorResult;
import com.blackduck.integration.detector.result.FailedDetectorResult;
import com.blackduck.integration.detector.result.ForcedNestedPassedDetectorResult;
import com.blackduck.integration.detector.result.MaxDepthExceededDetectorResult;
import com.blackduck.integration.detector.result.NotNestableBeneathDetectableDetectorResult;
import com.blackduck.integration.detector.result.NotNestableBeneathDetectorResult;
import com.blackduck.integration.detector.result.NotNestableDetectorResult;
import com.blackduck.integration.detector.result.PassedDetectorResult;
import com.blackduck.integration.detector.result.YieldedDetectorResult;

// TODO: Why does this class exist? Why isn't the exit code encoded in the Result class? JM-04/2022
public class DetectorResultStatusCodeLookup {

    public static DetectorResultStatusCodeLookup standardLookup = new DetectorResultStatusCodeLookup();

    private final Map<Class, DetectorStatusCode> resultClassesToStatusCodes;

    private DetectorResultStatusCodeLookup() {
        this.resultClassesToStatusCodes = populateMap();
    }

    private Map<Class, DetectorStatusCode> populateMap() {
        Map<Class, DetectorStatusCode> map = new HashMap<>();

        map.put(CargoLockfileNotFoundDetectableResult.class, DetectorStatusCode.CARGO_LOCKFILE_NOT_FOUND);
        map.put(CartfileResolvedNotFoundDetectableResult.class, DetectorStatusCode.CARTFILE_RESOLVED_FILE_NOT_FOUND);
        map.put(ExecutablesNotFoundDetectableResult.class, DetectorStatusCode.EXECUTABLES_NOT_FOUND);
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
        map.put(NotNestableDetectorResult.class, DetectorStatusCode.NOT_NESTABLE);
        map.put(NotNestableBeneathDetectorResult.class, DetectorStatusCode.NOT_NESTABLE_BENEATH);
        map.put(NotNestableBeneathDetectableDetectorResult.class, DetectorStatusCode.NOT_NESTABLE_BENEATH);
        map.put(NpmNodeModulesNotFoundDetectableResult.class, DetectorStatusCode.NPM_NODE_MODULES_NOT_FOUND);
        map.put(PassedDetectorResult.class, DetectorStatusCode.PASSED);
        map.put(PassedDetectableResult.class, DetectorStatusCode.PASSED);
        map.put(PipfileLockNotFoundDetectableResult.class, DetectorStatusCode.PIPFILE_LOCK_NOT_FOUND);
        map.put(PoetryLockfileNotFoundDetectableResult.class, DetectorStatusCode.POETRY_LOCKFILE_NOT_FOUND);
        map.put(PoorlyFormattedJson.class, DetectorStatusCode.POORLY_FORMATTED_JSON);
        map.put(PropertyInsufficientDetectableResult.class, DetectorStatusCode.PROPERTY_INSUFFICIENT);
        map.put(PubSpecLockNotFoundDetectableResult.class, DetectorStatusCode.PUBSPEC_LOCK_NOT_FOUND);
        map.put(SectionNotFoundDetectableResult.class, DetectorStatusCode.SECTION_NOT_FOUND);
        map.put(SbtMissingPluginDetectableResult.class, DetectorStatusCode.SBT_PLUGIN_MISSING);
        map.put(WrongOperatingSystemResult.class, DetectorStatusCode.WRONG_OPERATING_SYSTEM_RESULT);
        map.put(WrongConanExecutableVersionResult.class, DetectorStatusCode.EXECUTABLE_VERSION_MISMATCH);
        map.put(YieldedDetectorResult.class, DetectorStatusCode.YIELDED);
        map.put(PackageResolvedNotFoundDetectableResult.class, DetectorStatusCode.EXCLUDED);
        map.put(SetupToolsRequiresNotFoundDetectableResult.class, DetectorStatusCode.SETUP_TOOLS_REQUIRES_NOT_FOUND);
        map.put(SetupToolsNoDependenciesDetectableResult.class, DetectorStatusCode.SETUP_TOOLS_NO_DEPENDENCIES);
        map.put(CargoExecutableVersionMismatchResult.class, DetectorStatusCode.ATTEMPTED);

        return map;
    }

    @Nullable
    public DetectorStatusCode getStatusCode(Class resultClass) {
        return resultClassesToStatusCodes.getOrDefault(resultClass, null);
    }
}
