package com.blackduck.integration.detectable.detectables.conda.model;

import com.google.gson.annotations.SerializedName;

public class CondaListElement {
    @SerializedName("name")
    public String name;

    @SerializedName("version")
    public String version;

    @SerializedName("build_string")
    public String buildString;

    @SerializedName("channel")
    public String channel;

    @SerializedName("platform")
    public String platform;
}
