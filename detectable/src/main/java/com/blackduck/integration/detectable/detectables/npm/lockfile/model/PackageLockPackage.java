package com.blackduck.integration.detectable.detectables.npm.lockfile.model;

import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class PackageLockPackage {
    @SerializedName("version")
    public String version;

    @SerializedName("dependencies")
    public Map<String, String> dependencies;
    
    @SerializedName("devDependencies")
    public Map<String, String> devDependencies;
    
    @SerializedName("peerDependencies")
    public Map<String, String> peerDependencies;
    
    @SerializedName("optionalDependencies")
    public Map<String, String> optionalDependencies;
    
    @SerializedName("workspaces")
    public List<String> workspaces;
    
    // Note: version 3 of the package-lock.json no longer has a concept of nested dependencies.
    // Dependencies are just just printed as packages at the root of the packages object in the form of
    // multiple entries in the form of:
    // node_modules/x (parent)
    // node_modules/x/node_modules/y (child)
    public Map<String, PackageLockPackage> packages;
    
    // These fields are not in the JSON but are used internally by detect calculations
    public Boolean dev;

    public Boolean peer;
    
    public Boolean optional;

    public Boolean extraneous;
}
