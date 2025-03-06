package com.blackduck.integration.detectable.detectables.conan.cli.process;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.executable.ExecutableOutput;
import com.blackduck.integration.executable.ExecutableRunnerException;

public class ConanCommandRunner {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DetectableExecutableRunner executableRunner;
    @Nullable
    private final Path lockfilePath;
    @Nullable
    private final String additionalArguments;

    public ConanCommandRunner(DetectableExecutableRunner executableRunner, @Nullable Path lockfilePath, @Nullable String additionalArguments) {
        this.executableRunner = executableRunner;
        this.lockfilePath = lockfilePath;
        this.additionalArguments = additionalArguments;
    }

    public ExecutableOutput runConanInfoCommand(File projectDir, ExecutableTarget conanExe) throws ExecutableRunnerException {
        List<String> conanInfoArguments = new ArrayList<>();
        conanInfoArguments.add("info");
        appendCommonGraphInfoArguments(conanInfoArguments, projectDir);
        return executableRunner.execute(ExecutableUtils.createFromTarget(projectDir, conanExe, conanInfoArguments));
    }

    public ExecutableOutput runConanGraphInfoCommand(File projectDir, ExecutableTarget conanExe) throws ExecutableFailedException {
        List<String> conanGraphInfoArguments = new ArrayList<>();
        conanGraphInfoArguments.add("graph");
        conanGraphInfoArguments.add("info");
        conanGraphInfoArguments.add("-verror");

        conanGraphInfoArguments.add("-f");
        conanGraphInfoArguments.add("json");

        appendCommonGraphInfoArguments(conanGraphInfoArguments, projectDir);

        return executableRunner.executeSuccessfully(ExecutableUtils.createFromTarget(projectDir, conanExe, conanGraphInfoArguments));
    }

    public boolean wasSuccess(ExecutableOutput conanInfoOutput) {
        String errorOutput = conanInfoOutput.getErrorOutput();
        if (StringUtils.isNotBlank(errorOutput) && errorOutput.contains("ERROR: ")) {
            logger.error("The conan info command reported errors: {}", errorOutput);
            return false;
        }
        if (StringUtils.isNotBlank(errorOutput)) {
            logger.debug("The conan info command wrote to stderr: {}", errorOutput);
        }
        return true;
    }

    public boolean producedOutput(ExecutableOutput conanInfoOutput) {
        String standardOutput = conanInfoOutput.getStandardOutput();
        if (StringUtils.isBlank(standardOutput)) {
            logger.error("Nothing returned from conan info command");
            return false;
        }
        return true;
    }

    public ExecutableOutput runConanVersionCommand(File projectDir, ExecutableTarget conanExe) throws ExecutableRunnerException {
        return executableRunner.execute(ExecutableUtils.createFromTarget(projectDir, conanExe, "--version"));
    }

    private void appendCommonGraphInfoArguments(List<String> leadingArgs, File projectDir) {
        if (lockfilePath != null) {
            leadingArgs.add("--lockfile");
            leadingArgs.add(lockfilePath.toString());
        }
        if (StringUtils.isNotEmpty(additionalArguments)) {
            String[] additionalArgs = additionalArguments.split(" +");
            leadingArgs.addAll(Arrays.asList(additionalArgs));
        }
        leadingArgs.add(projectDir.getAbsolutePath());
    }
}
