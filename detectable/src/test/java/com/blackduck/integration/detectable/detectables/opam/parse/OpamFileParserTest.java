package com.blackduck.integration.detectable.detectables.opam.parse;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class OpamFileParserTest {
    private final OpamFileParser parser = new OpamFileParser();

    private String dependsOf(String... lines) {
        Map<String, String> parsed = parser.parseData(Arrays.asList(lines));
        return parsed.get("depends");
    }

    // this is how opam 2.1.6 prints ocamlfind 1.9.8 with "opam show ocamlfind --raw"
    @Test
    void versionConstraintWrappedOverTwoLinesIsNotADependency() {
        String depends = dependsOf(
            "depends: [",
            "  \"ocaml\"",
            "    {>= \"3.08.0\" & (os != \"cygwin\" & os-distribution != \"msys2\" | < \"5.0\") &",
            "     < \"5.5.0~\"}",
            "]"
        );
        Assertions.assertEquals("ocaml", depends);
    }

    // this is how opam 2.1.6 prints dune with "opam show dune --raw"
    @Test
    void filterOnItsOwnLineIsNotADependency() {
        String depends = dependsOf(
            "depends: [",
            "  \"js_of_ocaml\" {with-dev-setup & >= \"6.1.0\" & < \"6.4\" & os != \"win32\"}",
            "  \"js_of_ocaml-compiler\"",
            "    {with-dev-setup & >= \"6.1.0\" & < \"6.4\" & os != \"win32\"}",
            "  \"mdx\" {with-dev-setup & >= \"2.3.0\" & os != \"win32\"}",
            "]"
        );
        Assertions.assertEquals("js_of_ocaml, js_of_ocaml-compiler, mdx", depends);
    }

    @Test
    void filterOpenedOnPackageLineAndClosedLaterIsNotADependency() {
        String depends = dependsOf(
            "depends: [",
            "  \"ocaml\" {>= \"3.08.0\" &",
            "     (os != \"linux\" | < \"5.0\") &",
            "     < \"5.5.0~\"}",
            "  \"dune\" {>= \"2.0\"}",
            "]"
        );
        Assertions.assertEquals("ocaml, dune", depends);
    }

    @Test
    void osSpecificDependenciesAreReported() {
        String depends = dependsOf(
            "depends: [",
            "  \"flexdll\" {>= \"0.42\" & os = \"win32\"}",
            "  \"msvs-detect\" {os = \"cygwin\"}",
            "  \"conf-mingw\" {os-distribution = \"cygwin\" & arch = \"x86_64\"}",
            "  \"needs-not-cygwin\" {os != \"cygwin\"}",
            "  \"ctypes\" {>= \"0.17.1\" & (os = \"win32\" | os = \"cygwin\")}",
            "  \"host-arch-arm64\" {arch = \"arm64\"}",
            "  \"conf-msys2\" {os = \"win32\" & os-distribution = \"msys2\"}",
            "]"
        );
        Assertions.assertEquals("flexdll, msvs-detect, conf-mingw, needs-not-cygwin, ctypes, host-arch-arm64, conf-msys2", depends);
    }

    @Test
    void osSpecificDependencyWithWrappedFilterIsReported() {
        String depends = dependsOf(
            "depends: [",
            "  \"flexdll\"",
            "    {os = \"cygwin\" &",
            "     >= \"0.40\"}",
            "  \"dune\"",
            "]"
        );
        Assertions.assertEquals("flexdll, dune", depends);
    }

    @Test
    void severalDependenciesOnOneLineAreAllReported() {
        String depends = dependsOf(
            "depends: [",
            "  \"a\" {os != \"cygwin\"} \"b\"",
            "  \"c\" \"d\" {os = \"cygwin\"} \"e\"",
            "]"
        );
        Assertions.assertEquals("a, b, c, d, e", depends);
    }

    @Test
    void stringsInsideFiltersAreNeverDependencies() {
        String depends = dependsOf(
            "depends: [",
            "  \"a\" {arch = \"arm64\" | arch = \"x86_64\" | os = \"win32\"}",
            "  \"b\" {build & (\"cc\" = \"x\")}",
            "  \"c\"",
            "]"
        );
        Assertions.assertEquals("a, b, c", depends);
    }

    @Test
    void bracesAndBracketsInsideStringsDoNotChangeNesting() {
        String depends = dependsOf(
            "depends: [",
            "  \"a\" {\"%{name}%\" = \"x\" & \"]\" = \"}\"}",
            "  \"b\"",
            "]"
        );
        Assertions.assertEquals("a, b", depends);
    }

    @Test
    void singleLineDependsIsParsed() {
        Assertions.assertEquals("ocaml, dune", dependsOf("depends: [\"ocaml\" \"dune\"]"));
        Assertions.assertEquals("ocaml, dune, flexdll", dependsOf("depends: [ \"ocaml\" {>= \"4.08\"} \"dune\" {os != \"cygwin\"} \"flexdll\" {os = \"cygwin\"} ]"));
    }

    @Test
    void openingBracketOnNextLineIsParsed() {
        Assertions.assertEquals("a", dependsOf("depends:", "[", "  \"a\"", "]"));
    }

    @Test
    void commentsAreIgnored() {
        String depends = dependsOf(
            "depends: [",
            "  # \"commented\"",
            "  \"a\" {>= \"1\" # \"also-commented\"",
            "        & < \"2\"}",
            "  (* \"block-commented\"",
            "     \"still-commented\" *) \"b\"",
            "]"
        );
        Assertions.assertEquals("a, b", depends);
    }

    @Test
    void alternativesAreAllReported() {
        Assertions.assertEquals("a, b", dependsOf("depends: [", "  (\"a\" {>= \"1\"} | \"b\")", "]"));
    }

    @Test
    void duplicateDependenciesAreReportedOnce() {
        Assertions.assertEquals("a, b", dependsOf("depends: [", "  \"a\" {os = \"linux\"}", "  \"b\"", "  \"a\" {os = \"macos\"}", "]"));
    }

    @Test
    void sectionsOtherThanDependsAreIgnored() {
        String depends = dependsOf(
            "opam-version: \"2.0\"",
            "depopts: [",
            "  \"optional\"",
            "]",
            "depends: [",
            "  \"a\"",
            "]",
            "build: [",
            "  [\"dune\" \"build\"]",
            "]",
            "conflicts: [ \"x\" ]"
        );
        Assertions.assertEquals("a", depends);
    }

    @Test
    void nonBracketedDependsDoesNotSwallowTheRestOfTheFile() {
        Map<String, String> parsed = parser.parseData(Arrays.asList(
            "depends: \"a\"",
            "name: \"project\"",
            "version: \"1.0\""
        ));
        Assertions.assertEquals("a", parsed.get("depends"));
        Assertions.assertEquals("project", parsed.get("name"));
        Assertions.assertEquals("1.0", parsed.get("version"));
    }

    @Test
    void singletonWithFilterDoesNotSwallowSubsequentFields() {
        Map<String, String> parsed = parser.parseData(Arrays.asList(
            "depends: \"ocaml\" {>= \"4.08\" & os != \"cygwin\"}",
            "depopts: [\"optional\"]",
            "name: \"project\"",
            "version: \"1.0\""
        ));
        Assertions.assertEquals("ocaml", parsed.get("depends"));
        Assertions.assertEquals("project", parsed.get("name"));
        Assertions.assertEquals("1.0", parsed.get("version"));
    }

    @Test
    void singletonAndFilterMaySpanLines() {
        Map<String, String> parsed = parser.parseData(Arrays.asList(
            "depends:",
            "  # comment before the package",
            "  \"ocaml\"",
            "  # comment before its filter",
            "  {>= \"3.08.0\" &",
            "   (os != \"cygwin\" | < \"5.0\") &",
            "   < \"5.5.0~\"}",
            "name: \"project\"",
            "version: \"1.0\""
        ));
        Assertions.assertEquals("ocaml", parsed.get("depends"));
        Assertions.assertEquals("project", parsed.get("name"));
        Assertions.assertEquals("1.0", parsed.get("version"));
    }

    @Test
    void singletonAtEndOfFileIsReported() {
        Assertions.assertEquals("ocaml", dependsOf("depends: \"ocaml\""));
        Assertions.assertEquals("ocaml", dependsOf("depends: \"ocaml\" {>= \"4.08\"}"));
        Assertions.assertEquals("flexdll", dependsOf("depends: \"flexdll\" {os = \"cygwin\"}"));
    }

    @Test
    void singletonDoesNotConsumeFollowingSections() {
        for (String section : Arrays.asList("url {", "extra-source \"archive.tar.gz\" {")) {
            Map<String, String> parsed = parser.parseData(Arrays.asList(
                "depends: \"ocaml\" {>= \"4.08\"}",
                section,
                "  src: \"https://example.com/archive.tar.gz\"",
                "  checksum: [\"sha256=abc\"]",
                "}",
                "name: \"project\"",
                "version: \"1.0\""
            ));
            Assertions.assertEquals("ocaml", parsed.get("depends"));
            Assertions.assertEquals("project", parsed.get("name"));
            Assertions.assertEquals("1.0", parsed.get("version"));
        }
    }

    @Test
    void singletonAlternativeDoesNotConsumeFollowingFields() {
        Map<String, String> parsed = parser.parseData(Arrays.asList(
            "depends: (\"ocaml-base-compiler\" {>= \"4.08\"} |",
            "  \"ocaml-system\" {>= \"4.08\"})",
            "# comment after the dependency value",
            "",
            "synopsis: \"Not a dependency\"",
            "name: \"project\"",
            "version: \"1.0\""
        ));
        Assertions.assertEquals("ocaml-base-compiler, ocaml-system", parsed.get("depends"));
        Assertions.assertEquals("project", parsed.get("name"));
        Assertions.assertEquals("1.0", parsed.get("version"));
    }

    @Test
    void nameAndVersionAreStillParsed() {
        Map<String, String> parsed = parser.parseData(Arrays.asList(
            "name: \"my-project\"",
            "version: \"2.1.0\"",
            "depends: [",
            "  \"ocaml\"",
            "]"
        ));
        Assertions.assertEquals("my-project", parsed.get("name"));
        Assertions.assertEquals("2.1.0", parsed.get("version"));
        Assertions.assertEquals("ocaml", parsed.get("depends"));
    }

    @Test
    void emptyDependsMeansNoDependsEntry() {
        Assertions.assertNull(dependsOf("name: \"x\""));
        Assertions.assertNull(dependsOf("depends: [", "]"));
        Assertions.assertNull(dependsOf("depends: []"));
    }

    @Test
    void parserStateDoesNotLeakBetweenCalls() {
        Assertions.assertEquals("a", dependsOf("depends: [", "  \"a\" {", "   >= \"1\"")); // filter and list are never closed
        Assertions.assertEquals("b", dependsOf("depends: [", "  \"b\"", "]"));
    }

    @Test
    void parsesOpamFile() throws IOException {
        List<String> lines = Arrays.asList(
            "opam-version: \"2.0\"",
            "name: \"project\"",
            "version: \"1.0.0\"",
            "depends: [",
            "  \"ocaml\"",
            "    {>= \"3.08.0\" & (os != \"cygwin\" & os-distribution != \"msys2\" | < \"5.0\") &",
            "     < \"5.5.0~\"}",
            "  \"flexdll\" {os = \"cygwin\"}",
            "  \"dune\" {>= \"2.0\"}",
            "]"
        );
        File opamFile = File.createTempFile("project", ".opam");
        try {
            Files.write(opamFile.toPath(), lines, StandardCharsets.UTF_8);
            OpamParsedResult result = parser.parse(opamFile);
            Assertions.assertEquals("project", result.getProjectName());
            Assertions.assertEquals("1.0.0", result.getProjectVersion());
            Assertions.assertEquals(Arrays.asList("ocaml", "flexdll", "dune"), result.getParsedDirectDependencies());
        } finally {
            Files.deleteIfExists(opamFile.toPath());
        }
    }

    @Test
    void emptyInput() {
        Assertions.assertTrue(parser.parseData(Collections.emptyList()).isEmpty());
    }

    private List<String> dependencyList(String... lines) {
        String depends = dependsOf(lines);
        return depends == null ? Collections.emptyList() : Arrays.asList(depends.split(", "));
    }

    // The output of "opam show ocamlfind --raw" (ocamlfind 1.9.8) with opam 2.1.6, from the reported issue: the filter is
    // wrapped and mentions cygwin, and "5.5.0~" was read as a package name.
    @Test
    void reportedIssueOutputOnlyHasTheRealDependency() {
        Map<String, String> parsed = parser.parseData(Arrays.asList(
            "opam-version: \"2.0\"",
            "name: \"ocamlfind\"",
            "version: \"1.9.8\"",
            "synopsis: \"A library manager for OCaml\"",
            "description: \"\"\"",
            "Findlib is a library manager for OCaml.\"\"\"",
            "depends: [",
            "  \"ocaml\"",
            "    {>= \"3.08.0\" & (os != \"cygwin\" & os-distribution != \"msys2\" | < \"5.0\") &",
            "     < \"5.5.0~\"}",
            "]",
            "depopts: [\"graphics\"]",
            "build: [",
            "  [\"./configure\" \"-bindir\" bin]",
            "  [make \"all\"]",
            "]",
            "url {",
            "  src: \"https://download.camlcity.org/download/findlib-1.9.8.tar.gz\"",
            "  checksum: \"md5=00000000000000000000000000000000\"",
            "}"
        ));
        Assertions.assertEquals("ocaml", parsed.get("depends"));
        Assertions.assertEquals("ocamlfind", parsed.get("name"));
        Assertions.assertEquals("1.9.8", parsed.get("version"));
    }

    @Test
    void linesMentioningCygwinAreNoLongerSkipped() {
        Map<String, String> parsed = parser.parseData(Arrays.asList(
            "name: \"cygwin-tools\"",
            "version: \"1.0\"",
            "depends: [ \"needs-not-cygwin\" {os != \"cygwin\"} \"flexdll\" {os = \"cygwin\"} ]"
        ));
        Assertions.assertEquals("cygwin-tools", parsed.get("name"));
        Assertions.assertEquals("needs-not-cygwin, flexdll", parsed.get("depends"));
    }

    @Test
    void textAfterTheListOnTheSameLineIsIgnoredAndTheNextLineIsRead() {
        Map<String, String> parsed = parser.parseData(Arrays.asList(
            "depends: [ \"a\" ] # comment",
            "name: \"project\""
        ));
        Assertions.assertEquals("a", parsed.get("depends"));
        Assertions.assertEquals("project", parsed.get("name"));
    }

    @Test
    void singletonFormulaJoinedOverSeveralLines() {
        Map<String, String> parsed = parser.parseData(Arrays.asList(
            "depends: \"a\" {>= \"1\"} &",
            "  (\"b\" | \"c\" {os = \"linux\"})",
            "name: \"project\""
        ));
        Assertions.assertEquals("a, b, c", parsed.get("depends"));
        Assertions.assertEquals("project", parsed.get("name"));
    }

    @Test
    void tripleQuotedPackageInList() {
        Assertions.assertEquals(Arrays.asList("ocaml", "dune"),
            dependencyList("depends: [ \"\"\"ocaml\"\"\" \"dune\" ]"));
    }

    @Test
    void tripleQuotedSingletonPackage() {
        Assertions.assertEquals(Collections.singletonList("ocaml"),
            dependencyList("depends: \"\"\"ocaml\"\"\""));
    }

    @Test
    void tripleQuotedFilterWithLoneQuoteAndBrace() {
        Assertions.assertEquals(Arrays.asList("foo", "bar"),
            dependencyList("depends: [ \"foo\" { os = \"\"\"a\"}b\"\"\" } \"bar\" ]"));
    }

    @Test
    void tripleQuotedFilterWithEscapedQuote() {
        Assertions.assertEquals(Arrays.asList("foo", "bar"),
            dependencyList("depends: [ \"foo\" { os = \"\"\"a\\\"\"\"b\"\"\" } \"bar\" ]"));
    }

    @Test
    void tripleQuotedFilterSpanningTwoLines() {
        Assertions.assertEquals(Arrays.asList("foo", "bar"),
            dependencyList("depends: [ \"foo\" { os = \"\"\"a\"}",
                "b\"\"\" } \"bar\" ]"));
    }

    @Test
    void tripleQuotedDoesNotTreatDoubleQuotesAsTripleQuotes() {
        Assertions.assertEquals(Collections.emptyList(),
            Assertions.assertDoesNotThrow(() -> dependencyList("depends: [ \"\"foo\"\" \"bar\" ]")));
    }

    @Test
    void tripleQuotedPreservesEmptyStringBehavior() {
        Assertions.assertEquals(Collections.emptyList(),
            Assertions.assertDoesNotThrow(() -> dependencyList("depends: [ \"\" \"dune\" ]")));
    }

    @Test
    void tripleQuotedUnclosedStringMatchesUnclosedNormalString() {
        List<String> tripleQuoted = Assertions.assertDoesNotThrow(() -> dependencyList("depends: [ \"\"\"ocaml"));
        List<String> normalQuoted = Assertions.assertDoesNotThrow(() -> dependencyList("depends: [ \"ocaml"));
        Assertions.assertEquals(Collections.emptyList(), tripleQuoted);
        Assertions.assertEquals(Collections.emptyList(), normalQuoted);
        Assertions.assertEquals(normalQuoted, tripleQuoted);
    }

    @Test
    void escapedQuotesInsideFiltersDoNotEndTheString() {
        Assertions.assertEquals(Arrays.asList("a", "b"), dependencyList("depends: [ \"a\" {\"\\\"}\" = \"x\"} \"b\" ]"));
    }

    @Test
    void nestedBlockComments() {
        Assertions.assertEquals(Collections.singletonList("realdep"),
            dependencyList("depends: [", "  (* outer (* inner *) \"fake\" *)", "  \"realdep\"", "]"));
        Assertions.assertEquals(Collections.singletonList("realdep"),
            dependencyList("depends: [", "  (* outer", "    (* inner", "    *)", "    \"fake\"", "  *)", "  \"realdep\"", "]"));
    }

    @Test
    void windowsLineEndings() {
        Map<String, String> parsed = parser.parseData(Arrays.asList("name: \"project\"\r", "depends: [\r", "  \"a\" {>= \"1\"}\r", "]\r"));
        Assertions.assertEquals("project", parsed.get("name"));
        Assertions.assertEquals("a", parsed.get("depends"));
    }

    @Test
    void validPackageNames() {
        Assertions.assertEquals(Arrays.asList("conf-g++", "ocaml_beta", "0install", "a-1", "B"),
            dependencyList("depends: [ \"conf-g++\" \"ocaml_beta\" \"0install\" \"a-1\" \"B\" ]"));
    }

    // Text after "depends:" that cannot be a list of packages is not read as dependencies, and nothing is thrown.
    @Test
    void textThatIsNotAListOfPackagesIsIgnored() {
        Assertions.assertNull(dependsOf("depends: nothing."));
        Assertions.assertNull(dependsOf("depends: [ \"ocaml\" \"5.5.0~\" ]")); // a version is not a package name
        Assertions.assertNull(dependsOf("depends: [ \"\" ]"));
        Assertions.assertNull(dependsOf("depends: [ \"a b\" ]"));
        Assertions.assertNull(dependsOf("depends: [ \"a\" foo ]"));
        Assertions.assertNull(dependsOf("depends: [ \"a\" ) ]"));
        Assertions.assertNull(dependsOf("depends: \"a\" & more text"));
    }

    @Test
    void falseDependsStartInsideADescriptionDoesNotHideTheRealOne() {
        Assertions.assertEquals(Collections.singletonList("realdep"), dependencyList(
            "description: \"\"\"",
            "depends: [",
            "  see the documentation",
            "\"\"\"",
            "depends: [ \"realdep\" ]"
        ));
        Assertions.assertEquals(Collections.singletonList("realdep"), dependencyList(
            "description: \"\"\"",
            "depends: [ \"fake\"",
            "\"\"\"",
            "depends: [ \"realdep\" ]"
        ));
    }

    private static Arguments description(String name, List<String> expected, String... descriptionLines) {
        return Arguments.of(name, expected, Arrays.asList(descriptionLines));
    }

    static Stream<Arguments> descriptionsContainingDepends() {
        List<String> realOnly = Collections.singletonList("realdep");
        return Stream.of(
            description("depends: nothing.", realOnly, "description: \"\"\"", "depends: nothing.", "\"\"\""),
            description("depends: [ \"fake\" ]", realOnly, "description: \"\"\"", "depends: [ \"fake\" ]", "\"\"\""),
            description("depends: \"fake\"", realOnly, "description: \"\"\"", "depends: \"fake\"", "\"\"\""),
            description("depends: foo.", realOnly, "description: \"\"\"", "depends: foo.", "\"\"\""),
            description("depends: install PostgreSQL first.", realOnly, "description: \"\"\"", "depends: install PostgreSQL first.", "\"\"\""),
            description("one-line description", realOnly, "description: \"\"\"depends: nothing.\"\"\""),
            description("closing on the same line", realOnly, "description: \"\"\"", "depends: nothing.\"\"\"")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("descriptionsContainingDepends")
    void dependsTextInsideADescriptionNeverThrows(String name, List<String> expected, List<String> descriptionLines) {
        List<String> lines = new ArrayList<>(descriptionLines);
        lines.add("depends: [ \"realdep\" ]");
        Assertions.assertEquals(expected, dependencyList(lines.toArray(new String[0])));
    }

    static Stream<Arguments> contentContainingFakeFields() {
        return Stream.of(
            Arguments.of("same-line description", Arrays.asList("description: \"\"\" depends: [ \"fake\" ] \"\"\"")),
            Arguments.of("multiline description", Arrays.asList("description: \"\"\"", "depends: [ \"fake\" ]", "\"\"\"")),
            Arguments.of("normal multiline string", Arrays.asList("description: \"text", "depends: [ \\\"fake\\\" ]", "\"")),
            Arguments.of("escaped quote", Arrays.asList("description: \"\\\" depends: [ \\\"fake\\\" ]\"")),
            Arguments.of("triple string with escaped delimiter", Arrays.asList(
                "description: \"\"\"a\\\"\"\"b", "depends: [ \"fake\" ]", "\"\"\"")),
            Arguments.of("line comment", Arrays.asList("# depends: [ \"fake\" ]")),
            Arguments.of("block comment", Arrays.asList("(*", "depends: [ \"fake\" ]", "*)")),
            Arguments.of("nested block comment", Arrays.asList(
                "(* outer (* inner *)", "depends: [ \"fake\" ]", "*)")),
            Arguments.of("section", Arrays.asList("url {", "depends: [ \"fake\" ]", "}")),
            Arguments.of("nested named section", Arrays.asList(
                "extra-source \"archive\" {", "inner {", "depends: [ \"fake\" ]", "}", "}")),
            Arguments.of("section with quoted delimiters", Arrays.asList(
                "extra-source \"}\" { description: \"\"\" } ] (* \"\"\"", "depends: [ \"fake\" ]", "}")),
            Arguments.of("unrelated list", Arrays.asList("build: [", "depends: [ \"fake\" ]", "]")),
            Arguments.of("unrelated group", Arrays.asList("x-extension: (", "depends: [ \"fake\" ]", ")")),
            Arguments.of("unrelated option", Arrays.asList("x-extension: \"value\" {", "depends: [ \"fake\" ]", "}"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contentContainingFakeFields")
    void onlyTopLevelDependencyFieldsAreRead(String name, List<String> content) {
        Assertions.assertEquals(Collections.emptyList(), dependencyList(content.toArray(new String[0])));

        List<String> before = new ArrayList<>(content);
        before.add("depends: [ \"realdep\" ]");
        Assertions.assertEquals(Collections.singletonList("realdep"), dependencyList(before.toArray(new String[0])));

        List<String> after = new ArrayList<>();
        after.add("depends: [ \"realdep\" ]");
        after.addAll(content);
        Assertions.assertEquals(Collections.singletonList("realdep"), dependencyList(after.toArray(new String[0])));
    }

    @Test
    void fieldTokensAllowWhitespaceAndComments() {
        Assertions.assertEquals(Collections.singletonList("foo"), dependencyList(
            "depends (* outer (* inner *) comment *)",
            "# comment before the colon",
            ": (* comment before value *) [ \"\"\"foo\"\"\" {>= \"1.0\"} ]"));
        Assertions.assertEquals(Collections.singletonList("realdep"), dependencyList("depends : [ \"realdep\" ]"));
    }

    @Test
    void adjacentFieldsAndSectionsDoNotRequireLineBreaks() {
        Map<String, String> parsed = parser.parseData(Collections.singletonList(
            "description: \"\"\"depends: [ \"fake\" ]\"\"\" url { src: \"}\" } "
                + "depends: \"foo\" {>= \"1.0\"} & \"bar\" name : \"project\" version: \"1:2\""));
        Assertions.assertEquals("foo, bar", parsed.get("depends"));
        Assertions.assertEquals("project", parsed.get("name"));
        Assertions.assertEquals("1:2", parsed.get("version"));
        Assertions.assertEquals(Arrays.asList("foo", "bar"),
            dependencyList("depends: [ \"foo\" ] depends: [ \"bar\" ]"));
    }

    @Test
    void qualifiedVariablesAndExpressionsAreNotFieldBindings() {
        Assertions.assertEquals(Collections.singletonList("realdep"), dependencyList(
            "x-extension: foo:depends",
            "available: os = \"linux\" & !foo:installed",
            "depends: [ \"realdep\" ]"));
    }

    @Test
    void commentsAndStringsInsideFiltersDoNotChangeNesting() {
        Assertions.assertEquals(Arrays.asList("foo", "bar"), dependencyList(
            "depends: [ \"foo\" {",
            "  (* } (* nested *) \"fake\" *)",
            "  os = \"\"\"} depends: [ \"fake\" ] (*\"\"\" # }",
            "} \"bar\" ]"));
    }

    @Test
    void unterminatedStringsAndCommentsDoNotExposeFields() {
        for (String opening : Arrays.asList("description: \"", "description: \"\"\"", "(*", "(* (* nested *)")) {
            Assertions.assertEquals(Collections.emptyList(), dependencyList(opening, "depends: [ \"fake\" ]"));
        }
    }

    @Test
    void rejectedDependencyListsCannotExposeNestedFields() {
        Assertions.assertEquals(Collections.singletonList("realdep"), dependencyList(
            "depends: [ invalid depends: [ \"fake\" ] ]",
            "depends: [ \"realdep\" ]"));
    }

    @Test
    void projectMetadataInsideDescriptionsAndSectionsIsIgnored() {
        Map<String, String> parsed = parser.parseData(Arrays.asList(
            "name: \"project\" version: \"1.0\"",
            "description: \"\"\"", "name: \"fake\"", "version: \"9.9\"", "\"\"\"",
            "url { name: \"nested\" version: \"8.8\" }"));
        Assertions.assertEquals("project", parsed.get("name"));
        Assertions.assertEquals("1.0", parsed.get("version"));
        Assertions.assertFalse(parsed.containsKey("depends"));
    }
}
