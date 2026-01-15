package com.blackduck.integration.detectable.detectables.bazel.v2.unit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.executable.resolver.BazelResolver;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.util.MockDetectableEnvironment;
import com.blackduck.integration.detectable.detectables.bazel.BazelDetectableOptions;
import com.blackduck.integration.detectable.detectables.bazel.v2.BazelV2Detectable;
import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectables.bazel.BazelProjectNameGenerator;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelVariableSubstitutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.HaskellCabalLibraryJsonProtoParser;

public class BazelV2DetectableTest {

    @Test
    public void testApplicable_withTarget_passes() {
        DetectableEnvironment environment = MockDetectableEnvironment.empty();
        FileFinder fileFinder = Mockito.mock(FileFinder.class);
        DetectableExecutableRunner executableRunner = Mockito.mock(DetectableExecutableRunner.class);
        ExternalIdFactory externalIdFactory = Mockito.mock(ExternalIdFactory.class);
        BazelResolver bazelResolver = Mockito.mock(BazelResolver.class);
        BazelVariableSubstitutor substitutor = Mockito.mock(BazelVariableSubstitutor.class);
        HaskellCabalLibraryJsonProtoParser haskellParser = Mockito.mock(HaskellCabalLibraryJsonProtoParser.class);
        BazelProjectNameGenerator projectNameGenerator = Mockito.mock(BazelProjectNameGenerator.class);

        BazelDetectableOptions options = new BazelDetectableOptions("//:target", null, null);
        BazelV2Detectable detectable = new BazelV2Detectable(environment, fileFinder, executableRunner, externalIdFactory, bazelResolver, options, substitutor, haskellParser, projectNameGenerator);

        assertTrue(detectable.applicable().getPassed());
    }

    @Test
    public void testExtractable_resolverResolves_passes() throws DetectableException {
        DetectableEnvironment environment = MockDetectableEnvironment.empty();
        FileFinder fileFinder = Mockito.mock(FileFinder.class);
        DetectableExecutableRunner executableRunner = Mockito.mock(DetectableExecutableRunner.class);
        ExternalIdFactory externalIdFactory = Mockito.mock(ExternalIdFactory.class);
        BazelResolver bazelResolver = Mockito.mock(BazelResolver.class);
        BazelVariableSubstitutor substitutor = Mockito.mock(BazelVariableSubstitutor.class);
        HaskellCabalLibraryJsonProtoParser haskellParser = Mockito.mock(HaskellCabalLibraryJsonProtoParser.class);
        BazelProjectNameGenerator projectNameGenerator = Mockito.mock(BazelProjectNameGenerator.class);

        when(bazelResolver.resolveBazel()).thenReturn(ExecutableTarget.forCommand("bazel"));

        BazelDetectableOptions options = new BazelDetectableOptions("//:target", null, null);
        BazelV2Detectable detectable = new BazelV2Detectable(environment, fileFinder, executableRunner, externalIdFactory, bazelResolver, options, substitutor, haskellParser, projectNameGenerator);

        assertTrue(detectable.extractable().getPassed());
    }

    @Test
    public void testExtractable_resolverMissing_fails() throws DetectableException {
        DetectableEnvironment environment = MockDetectableEnvironment.empty();
        FileFinder fileFinder = Mockito.mock(FileFinder.class);
        DetectableExecutableRunner executableRunner = Mockito.mock(DetectableExecutableRunner.class);
        ExternalIdFactory externalIdFactory = Mockito.mock(ExternalIdFactory.class);
        BazelResolver bazelResolver = Mockito.mock(BazelResolver.class);
        BazelVariableSubstitutor substitutor = Mockito.mock(BazelVariableSubstitutor.class);
        HaskellCabalLibraryJsonProtoParser haskellParser = Mockito.mock(HaskellCabalLibraryJsonProtoParser.class);
        BazelProjectNameGenerator projectNameGenerator = Mockito.mock(BazelProjectNameGenerator.class);

        when(bazelResolver.resolveBazel()).thenReturn(null);

        BazelDetectableOptions options = new BazelDetectableOptions("//:target", null, null);
        BazelV2Detectable detectable = new BazelV2Detectable(environment, fileFinder, executableRunner, externalIdFactory, bazelResolver, options, substitutor, haskellParser, projectNameGenerator);

        assertFalse(detectable.extractable().getPassed());
    }

    @Test
    public void testExtract_missingTarget_throws() {
        DetectableEnvironment environment = MockDetectableEnvironment.empty();
        FileFinder fileFinder = Mockito.mock(FileFinder.class);
        DetectableExecutableRunner executableRunner = Mockito.mock(DetectableExecutableRunner.class);
        ExternalIdFactory externalIdFactory = Mockito.mock(ExternalIdFactory.class);
        BazelResolver bazelResolver = Mockito.mock(BazelResolver.class);
        BazelVariableSubstitutor substitutor = Mockito.mock(BazelVariableSubstitutor.class);
        HaskellCabalLibraryJsonProtoParser haskellParser = Mockito.mock(HaskellCabalLibraryJsonProtoParser.class);
        BazelProjectNameGenerator projectNameGenerator = Mockito.mock(BazelProjectNameGenerator.class);

        BazelDetectableOptions options = new BazelDetectableOptions(null, null, null);
        BazelV2Detectable detectable = new BazelV2Detectable(environment, fileFinder, executableRunner, externalIdFactory, bazelResolver, options, substitutor, haskellParser, projectNameGenerator);

        assertThrows(DetectableException.class, () -> detectable.extract(null));
    }
}

