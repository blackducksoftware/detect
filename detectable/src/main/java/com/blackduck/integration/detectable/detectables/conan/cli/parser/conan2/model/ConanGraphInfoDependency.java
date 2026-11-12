package com.blackduck.integration.detectable.detectables.conan.cli.parser.conan2.model;

import com.blackduck.integration.util.Stringable;

public class ConanGraphInfoDependency extends Stringable {
    boolean direct;

    boolean build;

    boolean test;

    public ConanGraphInfoDependency(boolean direct, boolean build, boolean test) {
        this.direct = direct;
        this.build = build;
        this.test = test;
    }

    public boolean isDirect() {
        return direct;
    }

    public boolean isBuild() {
        return build;
    }

    public boolean isTest() {
        return test;
    }
}
