package com.blackduck.integration.detectable.detectables.yarn.unit.parse.entry.element;

import com.blackduck.integration.detectable.detectables.yarn.VersionUtility;
import com.blackduck.integration.util.NameVersion;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class VersionUtilityTest {

    private final VersionUtility versionUtility = new VersionUtility();

    static Stream<Arguments> getNameVersionTestCases() {
        return Stream.of(
            Arguments.of(
                "{\"pieces\":[\"STRING\",\"ora@npm:^5.4.1\"]}",
                true,
                "ora",
                "^5.4.1"
            ),
            Arguments.of(
                "{\"pieces\":[\"STRING\",\"@types\\/base-64@npm:^1.0.2\"]}",
                true,
                "@types\\/base-64",
                "^1.0.2"
            ),
            Arguments.of(
                "{\"pieces\":[\"STRING\",\"@babel\\/runtime@npm:7.12.5\"]}",
                true,
                "@babel\\/runtime",
                "7.12.5"
            ),
            Arguments.of(
                "{\"pieces\":[\"NAME_VERSION\",\"unbox-primitive\",\"npm:^1.0.2\"]}",
                true,
                "unbox-primitive",
                "^1.0.2"
            ),
            Arguments.of(
                "{\"pieces\":[\"NAME_VERSION\",\"@eslint/plugin-kit\",\"npm:^0.2.5\"]}",
                true,
                "@eslint/plugin-kit",
                "^0.2.5"
            ),
            Arguments.of(
                "{\"pieces\":[\"NAME_VERSION\",\"@types/react\",\"^18.2.39\"]}",
                true,
                "@types/react",
                "^18.2.39"
            ),
            Arguments.of(
                "{\"pieces\":[\"NAME_VERSION\",\"@types/react-dom\",\"^18.2.17\"]}",
                true,
                "@types/react-dom",
                "^18.2.17"
            ),
            Arguments.of(
                "invalid-dependency-string",
                false,
                null,
                null
            )
        );
    }

    @ParameterizedTest
    @MethodSource("getNameVersionTestCases")
    void testGetNameVersion(String dependencyIdString, boolean shouldBePresent, String expectedName, String expectedVersion) {
        Optional<NameVersion> result = versionUtility.getNameVersion(dependencyIdString);

        if (shouldBePresent) {
            assertTrue(result.isPresent(), "Expected NameVersion to be present for: " + dependencyIdString);
            assertEquals(expectedName, result.get().getName(), "Name mismatch for: " + dependencyIdString);
            assertEquals(expectedVersion, result.get().getVersion(), "Version mismatch for: " + dependencyIdString);
        } else {
            assertFalse(result.isPresent(), "Expected NameVersion to be empty for: " + dependencyIdString);
        }
    }
}
