package com.blackduck.integration.detectable.detectables.bazel.v2.unit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.common.util.finder.SimpleFileFinder;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectable.executable.resolver.BazelResolver;
import com.blackduck.integration.detectable.detectables.bazel.BazelDetectableOptions;
import com.blackduck.integration.detectable.detectables.bazel.BazelProjectNameGenerator;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelVariableSubstitutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.HaskellCabalLibraryJsonProtoParser;
import com.blackduck.integration.detectable.detectables.bazel.v2.BazelV2Detectable;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;
import com.blackduck.integration.executable.Executable;
import com.blackduck.integration.executable.ExecutableOutput;

/**
 * Verifies that a fatally misconfigured Bazel workspace (a repository rule pointing at a location
 * with no MODULE.bazel/REPO.bazel/WORKSPACE file) causes the whole extraction to fail fast with a
 * clear DetectableException, rather than hanging (the real-world symptom this signature detection
 * is meant to prevent) or silently reporting an incomplete/incorrect BOM.
 */
public class BazelV2DetectableFatalWorkspaceTest {

    @Test
    public void extract_whenBazelReportsFatalWorkspaceMisconfiguration_throwsDetectableExceptionWithoutHanging() throws Exception {
        DetectableEnvironment environment = new DetectableEnvironment(new File("."));
        FileFinder fileFinder = new SimpleFileFinder();
        ExternalIdFactory externalIdFactory = new ExternalIdFactory();
        BazelResolver bazelResolver = () -> ExecutableTarget.forCommand("bazel");
        BazelVariableSubstitutor substitutor = new BazelVariableSubstitutor("//:test", java.util.Collections.emptyList(), java.util.Collections.emptyList());
        HaskellCabalLibraryJsonProtoParser haskellParser = new HaskellCabalLibraryJsonProtoParser(new com.google.gson.Gson());
        BazelProjectNameGenerator projectNameGenerator = new BazelProjectNameGenerator();

        ExecutableOutput fatalOutput = new ExecutableOutput(
            1, "",
            "ERROR: /ws/WORKSPACE.bzlmod:7:17: fetching local_repository rule //external:problematic_dep: "
                + "java.io.IOException: No MODULE.bazel, REPO.bazel, or WORKSPACE file found in /ws/problematic_dep"
        );
        ExecutableOutput versionOutput = new ExecutableOutput(0, "bazel 7.1.0", "");

        // `bazel --version` (run before probing, to gate 7.1+ features) must succeed normally so the
        // fatal signature is genuinely first observed during probing (BazelGraphProber) - the actual
        // regression path - rather than incidentally during version detection.
        DetectableExecutableRunner executableRunner = Mockito.mock(DetectableExecutableRunner.class);
        Mockito.when(executableRunner.execute(any())).thenAnswer((InvocationOnMock invocation) -> {
            Executable executable = invocation.getArgument(0, Executable.class);
            List<String> commandWithArguments = executable.getCommandWithArguments();
            if (commandWithArguments.contains("--version")) {
                return versionOutput;
            }
            return fatalOutput;
        });

        // Use mode override to skip auto-detection's own 'mod graph' call, isolating the assertion
        // to the probing phase (BazelGraphProber), which is exactly where the real hang occurred.
        BazelDetectableOptions options = BazelDetectableOptionsTestBuilder.builder()
            .target("//:test")
            .modeOverride("BZLMOD")
            .build();

        BazelV2Detectable detectable = new BazelV2Detectable(
            environment, fileFinder, executableRunner, externalIdFactory, bazelResolver, options,
            substitutor, haskellParser, projectNameGenerator
        );
        detectable.extractable(); // resolves bazelExe via bazelResolver; extract() requires this to have run first

        DetectableException thrown = assertThrows(
            DetectableException.class,
            () -> detectable.extract(new ExtractionEnvironment(new File("out")))
        );
        assertTrue(thrown.getMessage().contains("misconfigured"), "Expected message to explain the workspace is misconfigured: " + thrown.getMessage());

        // Exactly 2 invocations: the successful 'bazel --version' call, followed by the first probe
        // (BazelGraphProber) call which observes the fatal signature. Every later probe must
        // short-circuit instead of invoking Bazel again - this is what prevents the hang seen in the
        // real repro.
        verify(executableRunner, times(2)).execute(any());
    }
}

