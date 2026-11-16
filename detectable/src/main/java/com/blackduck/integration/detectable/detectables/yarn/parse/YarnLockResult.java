package com.blackduck.integration.detectable.detectables.yarn.parse;

import com.blackduck.integration.detectable.detectables.yarn.packagejson.NullSafePackageJson;
import com.blackduck.integration.detectable.detectables.yarn.workspace.YarnWorkspaces;
import java.util.Collections;

public class YarnLockResult {
    private final NullSafePackageJson rootPackageJson;
    private final YarnWorkspaces workspaceData;
    private final YarnLock yarnLock;
    private final YarnLockResolutionParser resolutionParser;

    public YarnLockResult(NullSafePackageJson rootPackageJson, YarnWorkspaces workspaceData, YarnLock yarnLock, YarnLockResolutionParser resolutionParser) {
        this.rootPackageJson = rootPackageJson;
        this.workspaceData = workspaceData;
        this.yarnLock = yarnLock;
        this.resolutionParser = resolutionParser;
    }

    public YarnLockResult(NullSafePackageJson rootPackageJson, YarnLock yarnLock, YarnLockResolutionParser resolutionParser) {
        this(rootPackageJson, new YarnWorkspaces(Collections.EMPTY_SET), yarnLock, resolutionParser);
    }

    public YarnLockResolutionParser getResolutionParser() {
        return resolutionParser;
    }

    public YarnLock getYarnLock() {
        return yarnLock;
    }

    public NullSafePackageJson getRootPackageJson() {
        return rootPackageJson;
    }

    public YarnWorkspaces getWorkspaceData() {
        return workspaceData;
    }
}
