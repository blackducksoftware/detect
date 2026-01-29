package com.blackduck.integration.detectable.detectables.bazel.v2.unit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.common.util.finder.SimpleFileFinder;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectable.executable.resolver.BazelResolver;
import com.blackduck.integration.detectable.detectables.bazel.BazelDetectableOptions;
import com.blackduck.integration.detectable.detectables.bazel.BazelProjectNameGenerator;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelVariableSubstitutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.HaskellCabalLibraryJsonProtoParser;
import com.blackduck.integration.detectable.detectables.bazel.v2.BazelV2Detectable;
import com.blackduck.integration.detectable.detectables.bazel.v2.BazelV2Detectable.BazelGraphProberFactory;
import com.blackduck.integration.detectable.detectables.bazel.v2.BazelGraphProber;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectables.bazel.v2.BazelEnvironmentAnalyzer;

public class BazelV2DetectableNoPipelinesTest {

    @Test
    public void extract_whenProberReturnsNoPipelines_throwsDetectableException() {
        // Arrange
        DetectableEnvironment environment = new DetectableEnvironment(new File("."));
        FileFinder fileFinder = new SimpleFileFinder();
        DetectableExecutableRunner executableRunner = Mockito.mock(DetectableExecutableRunner.class);
        ExternalIdFactory externalIdFactory = new ExternalIdFactory();
        BazelResolver bazelResolver = () -> ExecutableTarget.forCommand("bazel");
        BazelVariableSubstitutor substitutor = new BazelVariableSubstitutor("//:test", Collections.emptyList());
        HaskellCabalLibraryJsonProtoParser haskellParser = new HaskellCabalLibraryJsonProtoParser(new com.google.gson.Gson());
        BazelProjectNameGenerator projectNameGenerator = new BazelProjectNameGenerator();

        // Mock a prober that returns empty set
        BazelGraphProberFactory proberFactory = (bazelCmd, target, mode, httpProbeLimit) -> Mockito.mock(BazelGraphProber.class);
        BazelGraphProber mockProber = proberFactory.create(null, "//:test", BazelEnvironmentAnalyzer.Mode.UNKNOWN, 30);
        when(mockProber.decidePipelines()).thenReturn(Collections.emptySet());

        // Use mode override UNKNOWN to avoid auto-detection which would execute Bazel
        BazelDetectableOptions options = BazelDetectableOptionsTestBuilder.builder()
            .target("//:test")
            .modeOverride("UNKNOWN")
            .httpProbeLimit(30)
            .build();

        BazelV2Detectable detectable = new BazelV2Detectable(environment, fileFinder, executableRunner, externalIdFactory, bazelResolver, options, substitutor, haskellParser, projectNameGenerator, (bazelCmd, target, mode, httpProbeLimit) -> mockProber);

        // Act & Assert
        assertThrows(DetectableException.class, () -> detectable.extract(new com.blackduck.integration.detectable.extraction.ExtractionEnvironment(new File("out"))));
    }
}
