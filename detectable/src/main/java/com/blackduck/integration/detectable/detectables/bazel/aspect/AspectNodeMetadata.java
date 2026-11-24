package com.blackduck.integration.detectable.detectables.bazel.aspect;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

/**
 * Metadata for one target node as produced by detect_deps_aspect.bzl.
 * Fields map directly to the JSON written by the aspect — absent fields deserialise to null.
 */
public class AspectNodeMetadata {

    private final String label;
    private final String kind;

    @SerializedName("direct_deps")
    private final List<AspectNodeDep> directDeps;

    // Maven — rules_jvm_external stamps "maven_coordinates=g:a:v" on jvm_import targets
    @SerializedName("maven_coordinates")
    private final String mavenCoordinates;

    // Rust — crates_repository stamps "crate-name=foo" tag + version attribute on rust_library
    @SerializedName("crate_name")
    private final String crateName;

    @SerializedName("crate_version")
    private final String crateVersion;

    // Python — pip.parse stamps "pypi_name=x" and "pypi_version=y" tags on py_library
    @SerializedName("pypi_name")
    private final String pypiName;

    @SerializedName("pypi_version")
    private final String pypiVersion;

    // Go — rules_go sets importpath on go_library targets; no version on the target itself
    @SerializedName("go_importpath")
    private final String goImportpath;

    public AspectNodeMetadata(
            String label,
            String kind,
            List<AspectNodeDep> directDeps,
            String mavenCoordinates,
            String crateName,
            String crateVersion,
            String pypiName,
            String pypiVersion,
            String goImportpath) {
        this.label = label;
        this.kind = kind;
        this.directDeps = directDeps != null ? directDeps : Collections.emptyList();
        this.mavenCoordinates = mavenCoordinates;
        this.crateName = crateName;
        this.crateVersion = crateVersion;
        this.pypiName = pypiName;
        this.pypiVersion = pypiVersion;
        this.goImportpath = goImportpath;
    }

    public String getLabel() { return label; }
    public String getKind() { return kind; }
    public List<AspectNodeDep> getDirectDeps() { return directDeps; }

    public String getMavenCoordinates() { return mavenCoordinates; }
    public String getCrateName() { return crateName; }
    public String getCrateVersion() { return crateVersion; }
    public String getPypiName() { return pypiName; }
    public String getPypiVersion() { return pypiVersion; }
    public String getGoImportpath() { return goImportpath; }

    public boolean hasMavenCoordinates() { return mavenCoordinates != null && !mavenCoordinates.isEmpty(); }
    public boolean hasCrateCoordinates() { return crateName != null && crateVersion != null; }
    public boolean hasPypiCoordinates() { return pypiName != null && pypiVersion != null; }
    public boolean hasGoImportpath() { return goImportpath != null && !goImportpath.isEmpty(); }

    /** True for targets that live in an external repository (label starts with @@). */
    public boolean isExternalRepo() {
        return label != null && label.startsWith("@@") && !label.startsWith("@@//");
    }
}
