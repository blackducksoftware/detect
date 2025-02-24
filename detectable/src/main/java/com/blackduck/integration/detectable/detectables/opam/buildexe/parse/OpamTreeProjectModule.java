package com.blackduck.integration.detectable.detectables.opam.buildexe.parse;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OpamTreeProjectModule {
    @SerializedName("name")
    public String name;

    @SerializedName("version")
    public String version;

    @SerializedName("dependencies")
    public List<OpamTreeDependencyModule> dependencies;
}
