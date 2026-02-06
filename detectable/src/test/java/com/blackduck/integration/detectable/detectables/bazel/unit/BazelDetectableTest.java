package com.blackduck.integration.detectable.detectables.bazel.unit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.executable.resolver.BazelResolver;
import com.blackduck.integration.detectable.detectables.bazel.BazelDetectable;
import com.blackduck.integration.detectable.detectables.bazel.BazelDetectableOptions;
import com.blackduck.integration.detectable.detectables.bazel.v2.unit.BazelDetectableOptionsTestBuilder;
import com.blackduck.integration.detectable.detectables.bazel.BazelExtractor;
import com.blackduck.integration.detectable.util.MockDetectableEnvironment;

public class BazelDetectableTest {

    @Test
    public void testApplicable() {
        DetectableEnvironment environment = MockDetectableEnvironment.empty();
        FileFinder fileFinder = Mockito.mock(FileFinder.class);
        Mockito.when(fileFinder.findFile(new File("."), "WORKSPACE")).thenReturn(new File("src/test/resources/functional/bazel/WORKSPACE"));
        BazelExtractor bazelExtractor = null;
        BazelResolver bazelResolver = null;
        BazelDetectableOptions bazelDetectableOptions = BazelDetectableOptionsTestBuilder.forTarget("target");
        BazelDetectable detectable = new BazelDetectable(environment, fileFinder, bazelExtractor, bazelResolver, bazelDetectableOptions.getTargetName().orElse(null));

        assertTrue(detectable.applicable().getPassed());
    }
}
