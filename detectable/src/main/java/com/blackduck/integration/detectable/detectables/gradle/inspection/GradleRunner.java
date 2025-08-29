package com.blackduck.integration.detectable.detectables.gradle.inspection;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.rest.proxy.ProxyInfo;

public class GradleRunner {
    private final DetectableExecutableRunner executableRunner;

    public GradleRunner(DetectableExecutableRunner executableRunner) {
        this.executableRunner = executableRunner;
    }

    public List<String> splitUserArguments(@Nullable String gradleCommand) {
        List<String> arguments = new ArrayList<>();
        if (StringUtils.isNotBlank(gradleCommand)) {
            Arrays.stream(gradleCommand.split(" "))
                .filter(StringUtils::isNotBlank)
                .filter(it -> !it.equals("dependencies"))
                .forEach(arguments::add);
        }
        return arguments;
    }

    public void runGradleDependencies(File directory, ExecutableTarget gradleExe, File gradleInspector, @Nullable String gradleCommand, ProxyInfo proxyInfo, File outputDirectory)
        throws IOException, ExecutableFailedException {
        List<String> arguments = splitUserArguments(gradleCommand);
        // this gradle task is defined in the init script
        arguments.add("gatherDependencies");
        arguments.add(String.format("--init-script=%s", gradleInspector));
        arguments.add(String.format("-DGRADLEEXTRACTIONDIR=%s", outputDirectory.getCanonicalPath()));
        // starting in Gradle 9 configuration cache is preferred; we want to disable it as some features used by inspector are incompatible with configuration cache
        arguments.add("--no-configuration-cache");
        arguments.add("--info");

        if (proxyInfo.getHost().isPresent()) {
            arguments.add("-Dhttps.proxyHost=" + proxyInfo.getHost().get());
        }
        if (proxyInfo.getPort() != 0) {
            arguments.add("-Dhttps.proxyPort=" + proxyInfo.getPort());
        }

        executableRunner.executeSuccessfully(ExecutableUtils.createFromTarget(directory, gradleExe, arguments));
    }

    public DetectableExecutableRunner getExecutableRunner() {
        return executableRunner;
    }
}
