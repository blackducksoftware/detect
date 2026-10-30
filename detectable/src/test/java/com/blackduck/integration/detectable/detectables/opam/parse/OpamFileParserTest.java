package com.blackduck.integration.detectable.detectables.opam.parse;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
        Assertions.assertNull(parsed.get("depends"));
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
}
