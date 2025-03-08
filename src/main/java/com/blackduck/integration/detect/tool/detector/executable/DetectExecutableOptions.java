package com.blackduck.integration.detect.tool.detector.executable;

import java.nio.file.Path;

public class DetectExecutableOptions {
    private final Path bashUserPath;
    private final Path bazelUserPath;
    private final Path conanUserPath;
    private final Path condaUserPath;
    private final Path cpanUserPath;
    private final Path cpanmUserPath;
    private final Path dartPath;
    private final Path flutterPath;
    private final Path gradleUserPath;
    private final Path mavenUserPath;
    private final Path npmUserPath;
    private final Path pearUserPath;
    private final Path pipUserPath;
    private final Path pipenvUserPath;
    private final Path pythonUserPath;
    private final Path rebarUserPath;
    private final Path javaUserPath;
    private final Path dockerUserPath;
    private final Path gitUserPath;
    private final Path goUserPath;
    private final Path swiftUserPath;
    private final Path sbtUserPath;
    private final Path lernaUserPath;
    private final Path opamUserPath;
    private final Path cargoUserPath;

    public DetectExecutableOptions(
        Path bashUserPath,
        Path bazelUserPath,
        Path conanUserPath,
        Path condaUserPath,
        Path cpanUserPath,
        Path cpanmUserPath,
        Path dartPath,
        Path flutterPath,
        Path gradleUserPath,
        Path mavenUserPath,
        Path npmUserPath,
        Path pearUserPath,
        Path pipUserPath,
        Path pipenvUserPath,
        Path pythonUserPath,
        Path rebarUserPath,
        Path javaUserPath,
        Path dockerUserPath,
        Path gitUserPath,
        Path goUserPath,
        Path swiftUserPath,
        Path sbtUserPath,
        Path lernaUserPath,
        Path opamUserPath,
        Path cargoUserPath
    ) {
        this.bashUserPath = bashUserPath;
        this.bazelUserPath = bazelUserPath;
        this.conanUserPath = conanUserPath;
        this.condaUserPath = condaUserPath;
        this.cpanUserPath = cpanUserPath;
        this.cpanmUserPath = cpanmUserPath;
        this.dartPath = dartPath;
        this.flutterPath = flutterPath;
        this.gradleUserPath = gradleUserPath;
        this.mavenUserPath = mavenUserPath;
        this.npmUserPath = npmUserPath;
        this.pearUserPath = pearUserPath;
        this.pipUserPath = pipUserPath;
        this.pipenvUserPath = pipenvUserPath;
        this.pythonUserPath = pythonUserPath;
        this.rebarUserPath = rebarUserPath;
        this.javaUserPath = javaUserPath;
        this.dockerUserPath = dockerUserPath;
        this.gitUserPath = gitUserPath;
        this.goUserPath = goUserPath;
        this.swiftUserPath = swiftUserPath;
        this.sbtUserPath = sbtUserPath;
        this.lernaUserPath = lernaUserPath;
        this.opamUserPath = opamUserPath;
        this.cargoUserPath = cargoUserPath;
    }

    public Path getBashUserPath() {
        return bashUserPath;
    }

    public Path getBazelUserPath() {
        return bazelUserPath;
    }

    public Path getConanUserPath() {
        return conanUserPath;
    }

    public Path getCondaUserPath() {
        return condaUserPath;
    }

    public Path getCpanUserPath() {
        return cpanUserPath;
    }

    public Path getCpanmUserPath() {
        return cpanmUserPath;
    }

    public Path getDartUserPath() {
        return dartPath;
    }

    public Path getFlutterUserPath() {
        return flutterPath;
    }

    public Path getGradleUserPath() {
        return gradleUserPath;
    }

    public Path getMavenUserPath() {
        return mavenUserPath;
    }

    public Path getNpmUserPath() {
        return npmUserPath;
    }

    public Path getPearUserPath() {
        return pearUserPath;
    }

    public Path getPipenvUserPath() {
        return pipenvUserPath;
    }

    public Path getPythonUserPath() {
        return pythonUserPath;
    }

    public Path getRebarUserPath() {
        return rebarUserPath;
    }

    public Path getJavaUserPath() {
        return javaUserPath;
    }

    public Path getDockerUserPath() {
        return dockerUserPath;
    }

    public Path getGitUserPath() {
        return gitUserPath;
    }

    public Path getGoUserPath() {
        return goUserPath;
    }

    public Path getSwiftUserPath() {
        return swiftUserPath;
    }

    public Path getLernaUserPath() {
        return lernaUserPath;
    }

    public Path getPipUserPath() {
        return pipUserPath;
    }

    public Path getSbtUserPath() {
        return sbtUserPath;
    }

    public Path getOpamUserPath() {
        return opamUserPath;
    }

    public Path getCargoUserPath() { return cargoUserPath; }
}