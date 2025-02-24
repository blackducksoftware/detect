package com.blackduck.integration.detectable.detectables.pip.parser.unit;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.blackduck.integration.detectable.python.util.PythonDependencyTransformer;

public class RequirementsFileTransformerTest {
    private static final String EXPECTED_DEPENDENCY_NAME = "requests";
    private static final String EXPECTED_DEPENDENCY_VERSION = "12.3.3";

    private static PythonDependencyTransformer requirementsFileTransformer;

    @BeforeAll
    static void setUp() {
        requirementsFileTransformer = new PythonDependencyTransformer();
    }


    @Test
    void testExtractTokensForValidLineWithSpaces() {
        String validDependencyLine = "requests == 12.3.3";
        List<String> expectedTokensBeforeOperator = Collections.singletonList(EXPECTED_DEPENDENCY_NAME);
        List<String> expectedTokensAfterOperator = Collections.singletonList(EXPECTED_DEPENDENCY_VERSION);
        List<List<String>> expectedTokens = Arrays.asList(expectedTokensBeforeOperator, expectedTokensAfterOperator);

        List<List<String>> extractedTokens = requirementsFileTransformer.extractTokens(validDependencyLine);
        Assertions.assertEquals(expectedTokens, extractedTokens);
    }

    @Test
    void testExtractTokensForValidLineWithoutSpaces() {
        String validDependencyLine = "requests==12.3.3";
        List<String> expectedTokensBeforeOperator = Collections.singletonList(EXPECTED_DEPENDENCY_NAME);
        List<String> expectedTokensAfterOperator = Collections.singletonList(EXPECTED_DEPENDENCY_VERSION);
        List<List<String>> expectedTokens = Arrays.asList(expectedTokensBeforeOperator, expectedTokensAfterOperator);

        List<List<String>> extractedTokens = requirementsFileTransformer.extractTokens(validDependencyLine);
        Assertions.assertEquals(expectedTokens, extractedTokens);
    }

    @Test
    void testExtractTokensForValidLineWithExtras() {
        String validDependencyLine = "requests [security] >= 12.3.3";
        List<String> expectedTokensBeforeOperator = Arrays.asList(EXPECTED_DEPENDENCY_NAME, "[security]");
        List<String> expectedTokensAfterOperator = Collections.singletonList(EXPECTED_DEPENDENCY_VERSION);
        List<List<String>> expectedTokens = Arrays.asList(expectedTokensBeforeOperator, expectedTokensAfterOperator);

        List<List<String>> extractedTokens = requirementsFileTransformer.extractTokens(validDependencyLine);
        Assertions.assertEquals(expectedTokens, extractedTokens);
    }

    @Test
    void testFormatLineForRawLineWithComments() {
        String rawInputLine = "requests [security] >= 12.3.3    # sample comment";
        String expectedLine = "requests [security] >= 12.3.3";

        String formattedLine = requirementsFileTransformer.formatLine(rawInputLine);
        Assertions.assertEquals(expectedLine, formattedLine);
    }

    @Test
    void testFormatLineForRawLineWithSemiColon() {
        String rawInputLine = "requests <= 12.3.3; sys_platform == win32";
        String expectedLine = "requests <= 12.3.3";

        String formattedLine = requirementsFileTransformer.formatLine(rawInputLine);
        Assertions.assertEquals(expectedLine, formattedLine);
    }

    @Test
    void testFormatLineForRawLineWithCommas() {
        String rawInputLine = "requests == 12.3.3, < 14.0";
        String expectedLine = "requests == 12.3.3, < 14.0";

        String formattedLine = requirementsFileTransformer.formatLine(rawInputLine);
        Assertions.assertEquals(expectedLine, formattedLine);
    }

    @Test
    void testFormatLineForRawLineWithFlags() {
        String rawInputLine = "-r /path/to/other/requirements.txt";
        String expectedLine = "";

        String formattedLine = requirementsFileTransformer.formatLine(rawInputLine);
        Assertions.assertEquals(expectedLine, formattedLine);
    }

    @Test
    void testFormatLineForRawLineWithEmptyWhiteSpace() {
        String rawInputLine = "          ";
        String expectedLine = "";

        String formattedLine = requirementsFileTransformer.formatLine(rawInputLine);
        Assertions.assertEquals(expectedLine, formattedLine);
    }

    @Test
    void testFormatLineWithNullCharacters() {
        String rawInputLine = "a\u0000niso8601==9.0\u0000.1";
        String expectedLine = "aniso8601==9.0.1";

        String formattedLine = requirementsFileTransformer.formatLine(rawInputLine);
        Assertions.assertEquals(expectedLine, formattedLine);
    }

    @Test
    void testFormatLineWithReplacementCharacters() {
        String rawInputLine = "a\uFFFDniso8601==9.0\uFFFD.1";
        String expectedLine = "aniso8601==9.0.1";

        String formattedLine = requirementsFileTransformer.formatLine(rawInputLine);
        Assertions.assertEquals(expectedLine, formattedLine);
    }

    public static Stream<String> getFormatTokenRawDependencyInputs() {
        return Stream.of(
            EXPECTED_DEPENDENCY_NAME + "[security]",
            "'" + EXPECTED_DEPENDENCY_NAME + "'",
            "\"" + EXPECTED_DEPENDENCY_NAME +"\""
        );
    }

    public static Stream<String> getFormatTokenRawVersionInputs() {
        return Stream.of(
            EXPECTED_DEPENDENCY_VERSION + ",",
            "\"" + EXPECTED_DEPENDENCY_VERSION + "\"",
            "'" + EXPECTED_DEPENDENCY_VERSION + "'"
        );
    }

    @ParameterizedTest
    @MethodSource("getFormatTokenRawDependencyInputs")
    void testFormatTokenForDependencyToken(String rawToken) {
        String formattedToken = requirementsFileTransformer.formatToken(rawToken);
        Assertions.assertEquals(EXPECTED_DEPENDENCY_NAME, formattedToken);
    }

    @ParameterizedTest
    @MethodSource("getFormatTokenRawVersionInputs")
    void testFormatTokenForVersionToken(String rawToken) {
        String formattedToken = requirementsFileTransformer.formatToken(rawToken);
        Assertions.assertEquals(EXPECTED_DEPENDENCY_VERSION, formattedToken);
    }

}
