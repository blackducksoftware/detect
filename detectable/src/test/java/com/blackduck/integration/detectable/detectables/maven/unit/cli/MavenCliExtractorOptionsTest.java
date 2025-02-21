package com.blackduck.integration.detectable.detectables.maven.unit.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.blackduck.integration.common.util.parse.CommandParser;
import com.blackduck.integration.detectable.detectables.maven.cli.MavenCliExtractorOptions;

public class MavenCliExtractorOptionsTest {
    @ParameterizedTest
    @MethodSource("provideTestCasesForBuildCliArguments")
    public void testBuildCliArguments(String mavenBuildCommand, List<String> expected) {
        CommandParser commandParser = new CommandParser();
        MavenCliExtractorOptions options = new MavenCliExtractorOptions(mavenBuildCommand, null, null, null, null, false);
        assertEquals(
            expected,
            options.buildCliArguments(commandParser, null)
        );
    }

    private static Stream<Arguments> provideTestCasesForBuildCliArguments() {
        return Stream.of(
            Arguments.of("-T4 test", Arrays.asList("test", "dependency:tree", "-T1")),
            Arguments.of("-T 4 test", Arrays.asList("test", "dependency:tree", "-T1")),
            Arguments.of("--T4 test", Arrays.asList("test", "dependency:tree", "-T1")),
            Arguments.of("--T 4 test", Arrays.asList("test", "dependency:tree", "-T1")),
            Arguments.of("--threads 4 test", Arrays.asList("test", "dependency:tree", "-T1")),
            Arguments.of("--threads4 test", Arrays.asList("test", "dependency:tree", "-T1")),
            Arguments.of("-threads 4 test", Arrays.asList("test", "dependency:tree", "-T1")),
            Arguments.of("-threads4 test", Arrays.asList("test", "dependency:tree", "-T1")),
            Arguments.of("--threads 4 -TC1 test someOtherCommand -threads256", Arrays.asList("test", "someOtherCommand", "dependency:tree", "-T1"))
        );
    }
}
