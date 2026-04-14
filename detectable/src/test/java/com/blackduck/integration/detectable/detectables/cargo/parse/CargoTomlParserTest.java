package com.blackduck.integration.detectable.detectables.cargo.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.util.NameVersion;

class CargoTomlParserTest {

    private static final String WORKSPACE_TOML = String.join("\n",
        "[workspace]",
        "members = [\"extra-lib\"]",
        "resolver = \"2\"",
        "",
        "[workspace.package]",
        "version = \"0.5.0\"",
        "edition = \"2021\"",
        "repository = \"https://github.com/example/repo\"",
        "license = \"CLOSED\"",
        "",
        "[package]",
        "name = \"root-app\"",
        "version.workspace = true",
        "edition.workspace = true",
        "repository.workspace = true",
        "license.workspace = true"
    );

    @Test
    void extractNameVersion() {
        Optional<NameVersion> nameVersion = parseCargoTomlLines(
            "[package]",
            "name = \"my-name\"",
            "version = \"my-version\""
        );

        assertTrue(nameVersion.isPresent());
        assertEquals("my-name", nameVersion.get().getName());
        assertEquals("my-version", nameVersion.get().getVersion());
    }

    @Test
    void extractNameNoVersion() {
        Optional<NameVersion> nameVersion = parseCargoTomlLines(
            "[package]",
            "name = \"my-name\""
        );

        assertTrue(nameVersion.isPresent());
        assertEquals("my-name", nameVersion.get().getName());
        assertNull(nameVersion.get().getVersion());
    }

    @Test
    void extractNoName() {
        Optional<NameVersion> nameVersion = parseCargoTomlLines(
            "[package]",
            "some-other-key  = \"other-value\""
        );

        assertFalse(nameVersion.isPresent());
    }

    @Test
    void extractNoPackage() {
        Optional<NameVersion> nameVersion = parseCargoTomlLines(
            "[not-the-package-you-are-looking-for]",
            "some-other-key  = \"other-value\""
        );

        assertFalse(nameVersion.isPresent());
    }

    @Test
    void extractNameVersionFromWorkspacePackage() {
        CargoTomlParser parser = new CargoTomlParser();

        Optional<NameVersion> nameVersion = parser.parseNameVersionFromCargoToml(WORKSPACE_TOML);

        assertTrue(nameVersion.isPresent());
        assertEquals("root-app", nameVersion.get().getName());
        assertEquals("0.5.0", nameVersion.get().getVersion());
    }

    private Optional<NameVersion> parseCargoTomlLines(String... lines) {
        CargoTomlParser parser = new CargoTomlParser();
        String cargoTomlContents = StringUtils.joinWith(System.lineSeparator(), (Object[]) lines);
        return parser.parseNameVersionFromCargoToml(cargoTomlContents);
    }

}
