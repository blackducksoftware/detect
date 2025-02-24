package com.blackduck.integration.detectable.detectables.opam.buildexe.parse;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OpamJsonFileModule {

    @SerializedName("opam-version")
    public String opamVersion;

    @SerializedName("command-line")
    public List<String> commandLineArgs;

    @SerializedName("switch")
    public String switchName;

    @SerializedName("tree")
    public List<OpamTreeProjectModule> opamProjects;

}
