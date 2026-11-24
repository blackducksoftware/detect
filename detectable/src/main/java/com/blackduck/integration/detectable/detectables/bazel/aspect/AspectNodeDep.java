package com.blackduck.integration.detectable.detectables.bazel.aspect;

import com.google.gson.annotations.SerializedName;

/**
 * One entry in the direct_deps list of an aspect node — the label of a dependency
 * and which BUILD attribute it came through (deps, runtime_deps, or data).
 */
public class AspectNodeDep {

    private final String label;

    @SerializedName("attribute")
    private final String attribute;

    public AspectNodeDep(String label, String attribute) {
        this.label = label;
        this.attribute = attribute;
    }

    public String getLabel() {
        return label;
    }

    /** The BUILD attribute this edge came through: "deps", "runtime_deps", or "data". */
    public String getAttribute() {
        return attribute;
    }
}
