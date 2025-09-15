package com.blackduck.integration.detect.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the ProjectSettingsJsonFileReader class which reads and parses JSON project settings files.
 */
public class ProjectSettingsJsonFileReaderTest {
    private final ProjectSettingsJsonFileReader reader = new ProjectSettingsJsonFileReader();

    @Test
    public void testReadValidJsonFile(@TempDir Path tempDir) throws IOException {
        String validJson = "{\n" +
                "  \"name\": \"TestProject\",\n" +
                "  \"description\": \"A test project\",\n" +
                "  \"tier\": 3,\n" +
                "  \"version\": {\n" +
                "    \"name\": \"1.0.0\",\n" +
                "    \"phase\": \"DEVELOPMENT\"\n" +
                "  }\n" +
                "}";

        Path jsonFile = tempDir.resolve("project-settings.json");
        Files.writeString(jsonFile, validJson);

        ProjectSettings projectSettings = reader.readProjectSettings(jsonFile);

        assertNotNull(projectSettings);
        assertEquals("TestProject", projectSettings.getName());
        assertEquals("A test project", projectSettings.getDescription());
        assertEquals(Integer.valueOf(3), projectSettings.getTier());
        
        assertNotNull(projectSettings.getVersion());
        assertEquals("1.0.0", projectSettings.getVersion().getName());
        assertEquals("DEVELOPMENT", projectSettings.getVersion().getPhase());
    }

    @Test
    public void testReadComplexJsonFile(@TempDir Path tempDir) throws IOException {
        String complexJson = "{\n" +
                "  \"name\": \"ComplexProject\",\n" +
                "  \"tags\": [\"Production\", \"Critical\"],\n" +
                "  \"userGroups\": [\"Admins\", \"Developers\"],\n" +
                "  \"levelAdjustments\": true,\n" +
                "  \"deepLicense\": false,\n" +
                "  \"version\": {\n" +
                "    \"name\": \"2.1.0\",\n" +
                "    \"nickname\": \"Release\",\n" +
                "    \"license\": \"MIT\"\n" +
                "  }\n" +
                "}";

        Path jsonFile = tempDir.resolve("complex-settings.json");
        Files.writeString(jsonFile, complexJson);

        ProjectSettings projectSettings = reader.readProjectSettings(jsonFile);

        assertNotNull(projectSettings);
        assertEquals("ComplexProject", projectSettings.getName());
        assertEquals(2, projectSettings.getTags().size());
        assertEquals("Production", projectSettings.getTags().get(0));
        assertEquals("Critical", projectSettings.getTags().get(1));
        assertEquals(Boolean.TRUE, projectSettings.getLevelAdjustments());
        assertEquals(Boolean.FALSE, projectSettings.getDeepLicense());
        
        assertNotNull(projectSettings.getVersion());
        assertEquals("2.1.0", projectSettings.getVersion().getName());
        assertEquals("Release", projectSettings.getVersion().getNickname());
        assertEquals("MIT", projectSettings.getVersion().getLicense());
    }

    @Test
    public void testNullPathReturnsNull() {
        ProjectSettings projectSettings = reader.readProjectSettings(null);
        assertNull(projectSettings);
    }

    @Test
    public void testNonExistentFileReturnsNull(@TempDir Path tempDir) {
        Path nonExistentFile = tempDir.resolve("does-not-exist.json");
        ProjectSettings projectSettings = reader.readProjectSettings(nonExistentFile);
        assertNull(projectSettings);
    }

    @Test
    public void testEmptyFileReturnsNull(@TempDir Path tempDir) throws IOException {
        Path emptyFile = tempDir.resolve("empty.json");
        Files.writeString(emptyFile, "");

        ProjectSettings projectSettings = reader.readProjectSettings(emptyFile);
        assertNull(projectSettings);
    }

    @Test
    public void testWhitespaceOnlyFileReturnsNull(@TempDir Path tempDir) throws IOException {
        Path whitespaceFile = tempDir.resolve("whitespace.json");
        Files.writeString(whitespaceFile, "   \n\t  \n  ");

        ProjectSettings projectSettings = reader.readProjectSettings(whitespaceFile);
        assertNull(projectSettings);
    }

    @Test
    public void testInvalidJsonReturnsNull(@TempDir Path tempDir) throws IOException {
        String invalidJson = "{ \"name\": \"TestProject\", invalid }";
        
        Path jsonFile = tempDir.resolve("invalid.json");
        Files.writeString(jsonFile, invalidJson);

        ProjectSettings projectSettings = reader.readProjectSettings(jsonFile);
        assertNull(projectSettings);
    }

    @Test
    public void testDirectoryPathReturnsNull(@TempDir Path tempDir) {
        ProjectSettings projectSettings = reader.readProjectSettings(tempDir);
        assertNull(projectSettings);
    }
}