package com.blackduck.integration.detectable.detectables.bazel.v2.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import com.blackduck.integration.detectable.detectables.bazel.v2.BazelVersion;
import com.blackduck.integration.detectable.detectables.bazel.v2.BazelVersionChecker;
import com.blackduck.integration.executable.ExecutableOutput;

public class BazelVersionCheckerTest {

    @Test
    public void detectVersion_successfulCommand_returnsVersion() {
        BazelCommandExecutor executor = mock(BazelCommandExecutor.class);
        when(executor.executeWithoutThrowing(anyList()))
                .thenReturn(new ExecutableOutput(0, "bazel 7.4.1", ""));

        BazelVersionChecker checker = new BazelVersionChecker(executor);
        Optional<BazelVersion> result = checker.detectVersion();

        assertTrue(result.isPresent());
        assertEquals(new BazelVersion(7, 4, 1), result.get());
    }


    @Test
    public void detectVersion_nonZeroExitCode_returnsEmpty() {
        BazelCommandExecutor executor = mock(BazelCommandExecutor.class);
        when(executor.executeWithoutThrowing(anyList()))
                .thenReturn(new ExecutableOutput(1, "", "command not found"));

        BazelVersionChecker checker = new BazelVersionChecker(executor);
        assertFalse(checker.detectVersion().isPresent());
    }

    @Test
    public void detectVersion_unparsableOutput_returnsEmpty() {
        BazelCommandExecutor executor = mock(BazelCommandExecutor.class);
        when(executor.executeWithoutThrowing(anyList()))
                .thenReturn(new ExecutableOutput(0, "not a version string", ""));

        BazelVersionChecker checker = new BazelVersionChecker(executor);
        assertFalse(checker.detectVersion().isPresent());
    }

    @Test
    public void detectVersion_commandThrows_returnsEmpty() {
        BazelCommandExecutor executor = mock(BazelCommandExecutor.class);
        when(executor.executeWithoutThrowing(anyList()))
                .thenThrow(new RuntimeException("bazel not found"));

        BazelVersionChecker checker = new BazelVersionChecker(executor);
        assertFalse(checker.detectVersion().isPresent());
    }
}
