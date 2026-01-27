package com.blackduck.integration.detectable.detectables.npm.packagejson.model;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class YarnPackageJson {
    @SerializedName("name")
    public String name;

    @SerializedName("version")
    public String version;

    @SerializedName("dependencies")
    public Map<String, String> dependencies = new HashMap<>();

    @SerializedName("devDependencies")
    public Map<String, String> devDependencies = new HashMap<>();

    @SerializedName("peerDependencies")
    public Map<String, String> peerDependencies = new HashMap<>();

    @SerializedName("optionalDependencies")
    public Map<String, String> optionalDependencies = new HashMap<>();
}
