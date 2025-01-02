package com.blackduck.integration.detectable.detectables.opam.buildexe.parse;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OpamTreeDependencyModule {

    @SerializedName("name")
    public String name;

    @SerializedName("version")
    public String version;

    @SerializedName("satisfies")
    public String satifies;

    @SerializedName("is_duplicate")
    public boolean isDuplicate;

    @SerializedName("dependencies")
    public List<OpamTreeDependencyModule> dependencies;
}
