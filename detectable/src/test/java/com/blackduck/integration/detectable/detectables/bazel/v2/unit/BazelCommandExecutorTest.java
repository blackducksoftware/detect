package com.blackduck.integration.detectable.detectables.bazel.v2.unit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;

public class BazelCommandExecutorTest {

    @Test
    public void executeToString_whenExecutableFails_throwsExecutableFailedException() throws Exception {
        DetectableExecutableRunner executableRunner = Mockito.mock(DetectableExecutableRunner.class);
        // Mock an ExecutableFailedException to be thrown by executeSuccessfully
        ExecutableFailedException failedException = Mockito.mock(ExecutableFailedException.class);
        when(executableRunner.executeSuccessfully(any())).thenThrow(failedException);

        ExecutableTarget bazelTarget = ExecutableTarget.forCommand("bazel");
        File workspaceDir = new File(".");
        BazelCommandExecutor executor = new BazelCommandExecutor(executableRunner, workspaceDir, bazelTarget);

        assertThrows(ExecutableFailedException.class, () -> executor.executeToString(Arrays.asList("cquery", "--output", "build")));
    }
}

