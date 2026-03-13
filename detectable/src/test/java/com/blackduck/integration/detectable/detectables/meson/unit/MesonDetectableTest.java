package com.blackduck.integration.detectable.detectables.meson.unit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectables.meson.MesonDetectable;
import com.blackduck.integration.detectable.detectables.meson.MesonExtractor;
import com.blackduck.integration.detectable.util.MockDetectableEnvironment;

public class MesonDetectableTest {

    @Test
    public void testApplicable() {
        DetectableEnvironment environment = MockDetectableEnvironment.empty();
        File envDir = environment.getDirectory();

        FileFinder fileFinder = Mockito.mock(FileFinder.class);
        Mockito.when(fileFinder.findFile(envDir, "meson.build")).thenReturn(new File("meson.build"));
        Mockito.when(fileFinder.findFile(envDir, "intro-projectinfo.json", false, 2)).thenReturn(new File("builddir/meson-info/intro-projectinfo.json"));
        Mockito.when(fileFinder.findFile(envDir, "intro-dependencies.json", false, 2)).thenReturn(new File("builddir/meson-info/intro-dependencies.json"));
        MesonExtractor mesonExtractor = new MesonExtractor(null, null, fileFinder, null);
        MesonDetectable detectable = new MesonDetectable(environment, fileFinder, mesonExtractor);

        assertTrue(detectable.applicable().getPassed());
    }

    @Test
    public void testNotApplicable() {
        DetectableEnvironment environment = MockDetectableEnvironment.empty();
        FileFinder fileFinder = Mockito.mock(FileFinder.class);
        File envDir = environment.getDirectory();
        Mockito.when(fileFinder.findFile(envDir, "meson.build", true, 0)).thenReturn(new File("meson.build"));
        MesonExtractor mesonExtractor = new MesonExtractor(null, null, fileFinder, null);
        MesonDetectable detectable = new MesonDetectable(environment, fileFinder, mesonExtractor);

        assertTrue(!detectable.applicable().getPassed());
    }
}
