package com.blackduck.integration.detectable.detectables.cargo.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectable.executable.resolver.CargoResolver;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.cargo.CargoCliDetectable;
import com.blackduck.integration.detectable.detectables.cargo.CargoCliExtractor;
import com.blackduck.integration.detectable.detectables.cargo.CargoDetectableOptions;
import com.blackduck.integration.detectable.util.MockDetectableEnvironment;
import com.blackduck.integration.executable.ExecutableOutput;

class CargoCliVersionValidationTest {

    private CargoCliDetectable buildDetectable(String cargoVersionLine) throws Exception {
        DetectableEnvironment environment = MockDetectableEnvironment.empty();
        FileFinder fileFinder = mock(FileFinder.class);
        CargoResolver cargoResolver = mock(CargoResolver.class);
        CargoCliExtractor cargoCliExtractor = mock(CargoCliExtractor.class);
        DetectableExecutableRunner executableRunner = mock(DetectableExecutableRunner.class);

        when(cargoResolver.resolveCargo(any())).thenReturn(ExecutableTarget.forCommand("cargo"));
        when(executableRunner.executeSuccessfully(any())).thenReturn(new ExecutableOutput(0, cargoVersionLine, ""));

        return new CargoCliDetectable(
            environment,
            fileFinder,
            cargoResolver,
            cargoCliExtractor,
            executableRunner,
            new CargoDetectableOptions(EnumListFilter.excludeNone())
        );
    }

    @Test
    void testVersionWithPreReleaseSuffixAboveMinimumPassesVersionCheck() throws Exception {
        CargoCliDetectable detectable = buildDetectable("cargo 1.44.6-nightly (abc123 2020-06-17)");

        assertTrue(detectable.extractable().getPassed());
    }

    @Test
    void testVersionBelowMinimumFailsVersionCheck() throws Exception {
        CargoCliDetectable detectable = buildDetectable("cargo 1.43.0 (abc123 2020-06-17)");

        assertFalse(detectable.extractable().getPassed());
    }

    @Test
    void testVersionOutputWithSingleTokenDoesNotPropagateException() throws Exception {
        CargoCliDetectable detectable = buildDetectable("1.85.0");

        assertFalse(detectable.extractable().getPassed());
    }
}
