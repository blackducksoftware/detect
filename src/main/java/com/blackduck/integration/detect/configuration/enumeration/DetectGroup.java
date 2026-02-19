package com.blackduck.integration.detect.configuration.enumeration;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.configuration.util.Group;

public enum DetectGroup implements Group {
    //Super Groups
    DETECTORS("Detectors"),

    //Recommended Primary Groups
    ARTIFACTORY("artifactory"),
    BLACKDUCK_SERVER("blackduck server"),
    CLEANUP("cleanup"),
    CODE_LOCATION("codelocation"),
    GENERAL("general"),
    LOGGING("logging"),
    PATHS("paths"),
    POLICY_CHECK("policy check"),
    PROJECT("project"),
    PROJECT_INFO("project info"),
    PROXY("proxy"),
    REPORT("report"),
    SOURCE_SCAN("source scan"),
    SOURCE_PATH("source path"),
    RAPID_SCAN("rapid scan"),
    PROJECT_INSPECTOR("project inspector"),

    //Tool Groups
    DETECTOR("detector"),
    SIGNATURE_SCANNER("signature scanner"),
    BINARY_SCANNER("binary scanner"),
    CONTAINER_SCANNER("container scanner"),
    IMPACT_ANALYSIS("impact analysis"),
    IAC_SCAN("iac scan"),

    //Detector Groups
    BAZEL("bazel", DETECTORS),
    BITBAKE("bitbake", DETECTORS),
    CARGO("cargo", DETECTORS),
    CONAN("conan", DETECTORS),
    CONDA("conda", DETECTORS),
    CPAN("cpan", DETECTORS),
    DART("dart", DETECTORS),
    DOCKER("docker", DETECTORS),
    GO("go", DETECTORS),
    GRADLE("gradle", DETECTORS),
    HEX("hex", DETECTORS),
    IVY("ivy", DETECTORS),
    LERNA("lerna", DETECTORS),
    MAVEN("maven", DETECTORS),
    NPM("npm", DETECTORS),
    NUGET("nuget", DETECTORS),
    PACKAGIST("packagist", DETECTORS),
    PEAR("pear", DETECTORS),
    PIP("pip", DETECTORS),
    PNPM("pnpm", DETECTORS),
    POETRY("poetry", DETECTORS),
    PYTHON("python", DETECTORS),
    RUBY("ruby", DETECTORS),
    SBT("sbt", DETECTORS),
    SWIFT("swift", DETECTORS),
    YARN("yarn", DETECTORS),
    OPAM("opam", DETECTORS),
    UV("uv", DETECTORS),

    //Additional groups (should not be used as a primary group
    BLACKDUCK("blackduck"),
    DEBUG("debug"),
    GLOBAL("global"),
    OFFLINE("offline"),
    POLICY("policy"),
    PROJECT_SETTING("project setting"),
    REPORT_SETTING("report setting"),
    SEARCH("search"),
    DEFAULT("default");

    private final String name;
    private final Group superGroup;

    DetectGroup(@NotNull String name) {
        this(name, null);
    }

    DetectGroup(@NotNull String name, @Nullable Group superGroup) {
        this.name = name;
        this.superGroup = superGroup;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Optional<Group> getSuperGroup() {
        return Optional.ofNullable(superGroup);
    }
}