package com.blackduck.integration.detectable.detectables.uv.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.blackduck.integration.detectable.detectables.uv.parse.UVTomlParser;
import com.blackduck.integration.util.NameVersion;

class UVTomlParserTest {

    @TempDir
    public File tempDir;

    @Test
    void parseNameVersionWithValidNameAndVersion() throws IOException {
        String tomlContent = String.join("\n",
            "[project]",
            "name = \"my-uv-project\"",
            "version = \"1.2.3\""
        );

        UVTomlParser parser = createParser(tomlContent);
        parser.parseUVToml();
        Optional<NameVersion> result = parser.parseNameVersion();

        assertTrue(result.isPresent());
        assertEquals("my-uv-project", result.get().getName());
        assertEquals("1.2.3", result.get().getVersion());
    }

    @Test
    void parseNameVersionWithMissingVersion() throws IOException {
        String tomlContent = String.join("\n",
            "[project]",
            "name = \"my-uv-project\""
        );

        UVTomlParser parser = createParser(tomlContent);
        parser.parseUVToml();
        Optional<NameVersion> result = parser.parseNameVersion();

        assertFalse(result.isPresent());
    }

    @Test
    void parseNameVersionWithMissingName() throws IOException {
        String tomlContent = String.join("\n",
            "[project]",
            "version = \"1.2.3\""
        );

        UVTomlParser parser = createParser(tomlContent);
        parser.parseUVToml();
        Optional<NameVersion> result = parser.parseNameVersion();

        assertFalse(result.isPresent());
    }

    @Test
    void parseNameVersionWithMissingProjectSection() throws IOException {
        String tomlContent = String.join("\n",
            "[tool.uv]",
            "managed = true"
        );

        UVTomlParser parser = createParser(tomlContent);
        parser.parseUVToml();
        Optional<NameVersion> result = parser.parseNameVersion();

        assertFalse(result.isPresent());
    }

    @Test
    void parseManagedKeyReturnsTrue() throws IOException {
        String tomlContent = String.join("\n",
            "[project]",
            "name = \"my-project\"",
            "[tool.uv]",
            "managed = true"
        );

        UVTomlParser parser = createParser(tomlContent);
        boolean result = parser.parseManagedKey();

        assertTrue(result);
    }

    @Test
    void parseManagedKeyReturnsFalse() throws IOException {
        String tomlContent = String.join("\n",
            "[project]",
            "name = \"my-project\"",
            "[tool.uv]",
            "managed = false"
        );

        UVTomlParser parser = createParser(tomlContent);
        boolean result = parser.parseManagedKey();

        assertFalse(result);
    }

    @Test
    void parseManagedKeyReturnsTrueWhenManagedKeyMissing() throws IOException {
        String tomlContent = String.join("\n",
            "[project]",
            "name = \"my-project\"",
            "[tool.uv]",
            "dev-dependencies = [\"pytest\"]"
        );

        UVTomlParser parser = createParser(tomlContent);
        boolean result = parser.parseManagedKey();

        assertTrue(result);
    }

    @Test
    void parseManagedKeyReturnsFalseWhenToolUvSectionMissing() throws IOException {
        String tomlContent = String.join("\n",
            "[project]",
            "name = \"my-project\"",
            "version = \"1.0.0\""
        );

        UVTomlParser parser = createParser(tomlContent);
        boolean result = parser.parseManagedKey();

        assertFalse(result);
    }

    @Test
    void getProjectNameReturnsDefaultWhenNotParsed() throws IOException {
        String tomlContent = String.join("\n",
            "[project]",
            "name = \"custom-name\"",
            "version = \"1.0.0\""
        );

        UVTomlParser parser = createParser(tomlContent);
        assertEquals("uvProject", parser.getProjectName());
    }

    @Test
    void getProjectNameReturnsNameAfterParsing() throws IOException {
        String tomlContent = String.join("\n",
            "[project]",
            "name = \"custom-name\"",
            "version = \"1.0.0\""
        );

        UVTomlParser parser = createParser(tomlContent);
        parser.parseUVToml();
        parser.parseNameVersion();
        assertEquals("custom-name", parser.getProjectName());
    }

    private UVTomlParser createParser(String tomlContent) throws IOException {
        File tomlFile = new File(tempDir, "pyproject.toml");
        FileUtils.writeStringToFile(tomlFile, tomlContent, StandardCharsets.UTF_8);
        return new UVTomlParser(tomlFile);
    }
}
