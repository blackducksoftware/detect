package com.blackduck.integration.detectable.detectables.rush.model;

import com.blackduck.integration.detectable.detectables.yarn.packagejson.NullSafePackageJson;
import com.google.gson.annotations.SerializedName;

public class RushProject {

    @SerializedName("packageName")
    private String packageName;

    @SerializedName("projectFolder")
    private String projectFolder;
    private transient NullSafePackageJson packageJson;

    public String getPackageName() {
        return packageName;
    }

    public String getProjectFolder() {
        return projectFolder;
    }

    public NullSafePackageJson getPackageJson() {
        return packageJson;
    }

    public void setPackageJson(NullSafePackageJson packageJson) {
        this.packageJson = packageJson;
    }
}
