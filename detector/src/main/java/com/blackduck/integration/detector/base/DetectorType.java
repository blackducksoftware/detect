package com.blackduck.integration.detector.base;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// TODO: Get as close to the software managing the packages as possible
public enum DetectorType { // TODO: 8.0.0 Rename DetectorTypes
    BITBAKE,
    CARGO,
    CARTHAGE,
    COCOAPODS,
    CONAN,
    CONDA,
    CPAN,
    CRAN,
    DART, // PUB
    GIT,
    GO_MOD,
    GO_DEP,
    GO_VNDR,
    GO_VENDOR,
    GO_GRADLE,
    GRADLE,
    HEX, // REBAR
    IVY,
    LERNA,
    MAVEN,
    MAVENRESOLVER,
    NPM,
    NUGET, // MSBUILD
    PACKAGIST,
    PEAR, // COMPOSER
    PIP,
    PNPM,
    POETRY,
    RUBYGEMS,
    SBT,
    SETUPTOOLS,
    SWIFT, // SWIFT_PM
    YARN,
    CLANG,
    XCODE, // Remove this
    OPAM,
    UV;
    protected static final List<String> POSSIBLE_NAMES = Arrays.stream(DetectorType.values()).map(DetectorType::name).collect(Collectors.toList());

    public static List<String> getPossibleNames() {
        return POSSIBLE_NAMES;
    }
}
