package com.blackduck.integration.detectable.detectables.bazel.v2.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelFatalWorkspaceException;
import com.blackduck.integration.executable.ExecutableOutput;

public class BazelCommandExecutorTest {

    private static final List<String> QUERY_ARGS = Arrays.asList("query", "deps(//:target)");

    @Test
    public void executeToString_whenExecutableFails_throwsExecutableFailedException() throws Exception {
        DetectableExecutableRunner executableRunner = Mockito.mock(DetectableExecutableRunner.class);
        // executeToString now routes through executeToleratingExitCode (execute), treating any
        // non-zero exit as a hard failure → ExecutableFailedException.
        when(executableRunner.execute(any())).thenReturn(new ExecutableOutput(1, "", "ERROR: build failed"));

        BazelCommandExecutor executor = makeExecutor(executableRunner);

        assertThrows(ExecutableFailedException.class, () -> executor.executeToString(Arrays.asList("cquery", "--output", "build")));
    }

    @Test
    public void executeQueryToString_exitCode0_returnsStdout() throws Exception {
        DetectableExecutableRunner executableRunner = Mockito.mock(DetectableExecutableRunner.class);
        when(executableRunner.execute(any())).thenReturn(new ExecutableOutput(0, "@repo//:lib", ""));

        Optional<String> result = makeExecutor(executableRunner).executeQueryToString(QUERY_ARGS);

        assertTrue(result.isPresent());
        assertEquals("@repo//:lib", result.get());
    }

    @Test
    public void executeQueryToString_exitCode3_withStdout_returnsPartialResult() throws Exception {
        DetectableExecutableRunner executableRunner = Mockito.mock(DetectableExecutableRunner.class);
        when(executableRunner.execute(any())).thenReturn(new ExecutableOutput(3, "@repo//:lib", "ERROR: some repo failed"));

        Optional<String> result = makeExecutor(executableRunner).executeQueryToString(QUERY_ARGS);

        assertTrue(result.isPresent());
        assertEquals("@repo//:lib", result.get());
    }

    @Test
    public void executeQueryToString_exitCode3_emptyStdout_returnsEmpty() throws Exception {
        DetectableExecutableRunner executableRunner = Mockito.mock(DetectableExecutableRunner.class);
        when(executableRunner.execute(any())).thenReturn(new ExecutableOutput(3, "", "ERROR: all repos failed"));

        Optional<String> result = makeExecutor(executableRunner).executeQueryToString(QUERY_ARGS);

        assertFalse(result.isPresent());
    }

    @Test
    public void executeQueryToString_nonZeroExitCode_throwsExecutableFailedException() throws Exception {
        DetectableExecutableRunner executableRunner = Mockito.mock(DetectableExecutableRunner.class);
        when(executableRunner.execute(any())).thenReturn(new ExecutableOutput(1, "", "ERROR: build failed"));

        BazelCommandExecutor executor = makeExecutor(executableRunner);

        assertThrows(ExecutableFailedException.class, () -> executor.executeQueryToString(QUERY_ARGS));
    }

    private BazelCommandExecutor makeExecutor(DetectableExecutableRunner runner) {
        return new BazelCommandExecutor(runner, new File("."), ExecutableTarget.forCommand("bazel"));
    }

    @Test
    public void executeToleratingExitCode_fatalWorkspaceSignature_throwsBazelFatalWorkspaceException() throws Exception {
        DetectableExecutableRunner executableRunner = Mockito.mock(DetectableExecutableRunner.class);
        when(executableRunner.execute(any())).thenReturn(new ExecutableOutput(
            1, "",
            "ERROR: /ws/WORKSPACE.bzlmod:7:17: fetching local_repository rule //external:problematic_dep: "
                + "java.io.IOException: No MODULE.bazel, REPO.bazel, or WORKSPACE file found in /ws/problematic_dep"
        ));

        BazelCommandExecutor executor = makeExecutor(executableRunner);

        assertThrows(BazelFatalWorkspaceException.class, () -> executor.executeToleratingExitCode(QUERY_ARGS));
    }

    @Test
    public void executeToleratingExitCode_afterFatalWorkspaceSignature_doesNotReinvokeBazel() throws Exception {
        DetectableExecutableRunner executableRunner = Mockito.mock(DetectableExecutableRunner.class);
        when(executableRunner.execute(any())).thenReturn(new ExecutableOutput(
            1, "",
            "ERROR: fetching local_repository rule //external:problematic_dep: java.io.IOException: "
                + "No MODULE.bazel, REPO.bazel, or WORKSPACE file found in /ws/problematic_dep"
        ));

        BazelCommandExecutor executor = makeExecutor(executableRunner);

        // First call observes and records the fatal signature.
        assertThrows(BazelFatalWorkspaceException.class, () -> executor.executeToleratingExitCode(QUERY_ARGS));
        // Second call (different args, simulating the next probe/query) must fail fast WITHOUT
        // invoking Bazel again — this is what prevents the real-world hang on a repeated
        // encounter with this exact broken-repository signature.
        List<String> otherArgs = Arrays.asList("cquery", "--noimplicit_deps", "filter('@.*:jar', deps(//...))");
        assertThrows(BazelFatalWorkspaceException.class, () -> executor.executeToleratingExitCode(otherArgs));

        verify(executableRunner, times(1)).execute(any());
    }

    @Test
    public void executeToleratingExitCode_nonFatalNonZeroExit_doesNotThrowFatalException() throws Exception {
        DetectableExecutableRunner executableRunner = Mockito.mock(DetectableExecutableRunner.class);
        when(executableRunner.execute(any())).thenReturn(new ExecutableOutput(1, "", "ERROR: some unrelated build failure"));

        BazelCommandExecutor executor = makeExecutor(executableRunner);

        ExecutableOutput result = executor.executeToleratingExitCode(QUERY_ARGS);
        assertEquals(1, result.getReturnCode());
    }
}

