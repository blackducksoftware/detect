package com.blackduck.integration.detectable.detectables.npm.lockfile.model;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class PackageLockDependency {
    @SerializedName("version")
    public String version;

    @SerializedName("dev")
    public Boolean dev;

    @SerializedName("peer")
    public Boolean peer;
    
    @SerializedName("optional")
    public Boolean optional;

    @SerializedName("requires")
    public Map<String, String> requires;

    @SerializedName("dependencies")
    public Map<String, PackageLockDependency> dependencies;

}