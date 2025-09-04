package com.blackduck.integration.detectable.detectables.dart.unit;

import java.io.File;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectable.result.FileNotFoundDetectableResult;
import com.blackduck.integration.detectable.detectables.dart.pubdep.DartPubDepDetectable;

public class PubDepsDetectableTest {
    @Test
    public void testThrowExceptionWhenLockFilePresentButNotYaml() throws DetectableException {
        File mockDirectory = Mockito.mock(File.class);
        DetectableEnvironment mockEnvironment = Mockito.mock(DetectableEnvironment.class);
        Mockito.when(mockEnvironment.getDirectory()).thenReturn(mockDirectory);
        
        FileFinder fileFinder = Mockito.mock(FileFinder.class);
        Mockito.when(fileFinder.findFile(mockDirectory, "pubspec.yaml")).thenReturn(null);
        Mockito.when(fileFinder.findFile(mockDirectory, "pubspec.lock")).thenReturn(new File(""));
        DartPubDepDetectable dartPubDepDetectable = new DartPubDepDetectable(mockEnvironment, fileFinder, null, null, null, null);

        DetectableResult applicable = dartPubDepDetectable.applicable();
        Assertions.assertTrue(applicable.getPassed());

        DetectableResult extractable = dartPubDepDetectable.extractable();
        Assertions.assertTrue(extractable instanceof FileNotFoundDetectableResult);
    }
}
