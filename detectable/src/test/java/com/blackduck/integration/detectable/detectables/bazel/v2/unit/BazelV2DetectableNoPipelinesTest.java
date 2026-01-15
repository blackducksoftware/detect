package com.blackduck.integration.detectable.detectables.bazel.v2.unit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

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
import com.blackduck.integration.detectable.detectables.bazel.WorkspaceRule;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelVariableSubstitutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.HaskellCabalLibraryJsonProtoParser;
import com.blackduck.integration.detectable.detectables.bazel.v2.BazelV2Detectable;
import com.blackduck.integration.detectable.detectables.bazel.v2.BazelV2Detectable.BazelGraphProberFactory;
import com.blackduck.integration.detectable.detectables.bazel.v2.BazelGraphProber;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;

public class BazelV2DetectableNoPipelinesTest {

    @Test
    public void extract_whenProberReturnsNoPipelines_throwsDetectableException() throws IOException, DetectableException {
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
        BazelGraphProberFactory proberFactory = (bazelCmd, target, timeout, era) -> Mockito.mock(BazelGraphProber.class);
        BazelGraphProber mockProber = proberFactory.create(null, "//:test", 20, null);
        when(mockProber.decidePipelines()).thenReturn(Collections.emptySet());

        BazelDetectableOptions options = new BazelDetectableOptions("//:test", null, null);

        BazelV2Detectable detectable = new BazelV2Detectable(environment, fileFinder, executableRunner, externalIdFactory, bazelResolver, options, substitutor, haskellParser, projectNameGenerator, (bazelCmd, target, timeout, era) -> mockProber);

        // Act & Assert
        assertThrows(DetectableException.class, () -> detectable.extract(new com.blackduck.integration.detectable.extraction.ExtractionEnvironment(new File("out"))));
    }
}

