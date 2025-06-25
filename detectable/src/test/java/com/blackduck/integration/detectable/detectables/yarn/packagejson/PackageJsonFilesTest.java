package com.blackduck.integration.detectable.detectables.yarn.packagejson;

import com.blackduck.integration.detectable.detectables.yarn.workspace.YarnWorkspace;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackageJsonFilesTest {

    private PackageJsonReader packageJsonReader;
    private PackageJsonFiles packageJsonFiles;
    private File tempDir;

    @BeforeEach
    void setUp() throws IOException {
        packageJsonReader = new PackageJsonReader(new Gson());
        packageJsonFiles = new PackageJsonFiles(packageJsonReader);
        tempDir = Files.createTempDirectory("yarnws").toFile();
    }

    @AfterEach
    void tearDown() throws IOException {
        FileUtils.deleteDirectory(tempDir);
    }

    @Test
    void returnsEmptyWhenNoWorkspacePatterns() throws IOException {
        // Setup a root package.json with no workspaces
        File rootPackageJson = new File(tempDir, "package.json");
        Files.write(rootPackageJson.toPath(), "{ \"workspaces\": [] }".getBytes(StandardCharsets.UTF_8));

        Collection<YarnWorkspace> result = packageJsonFiles.readWorkspacePackageJsonFiles(tempDir);
        assertTrue(result.isEmpty());
    }

    @Test
    void findsWorkspacePackageJsonFiles() throws IOException {
        // Setup a root package.json with a workspace pattern
        File rootPackageJson = new File(tempDir, "package.json");
        Files.write(rootPackageJson.toPath(), "{ \"workspaces\": [\"packages/*\"] }".getBytes(StandardCharsets.UTF_8));

        // Create a workspace directory and its package.json
        File wsDir = new File(tempDir, "packages/a");
        wsDir.mkdirs();
        File wsPackageJson = new File(wsDir, "package.json");
        Files.write(wsPackageJson.toPath(), "{ \"name\": \"a\" }".getBytes(StandardCharsets.UTF_8));

        Collection<YarnWorkspace> result = packageJsonFiles.readWorkspacePackageJsonFiles(tempDir);
        assertEquals(1, result.size());

        YarnWorkspace foundWorkspace = result.iterator().next();
        assertEquals("a", foundWorkspace.getName().orElse(null));
        assertEquals(wsPackageJson.getAbsolutePath(), foundWorkspace.getWorkspacePackageJson().getFile().getAbsolutePath());
    }

    @Test
    void findsMultipleWorkspacesWithDoubleWildcard() throws IOException {
        // Setup a root package.json with a double wildcard workspace pattern
        File rootPackageJson = new File(tempDir, "package.json");
        Files.write(rootPackageJson.toPath(), "{ \"workspaces\": [\"packages/**\"] }".getBytes(StandardCharsets.UTF_8));

        // Create two workspace directories and their package.json files
        File wsDir1 = new File(tempDir, "packages/a");
        wsDir1.mkdirs();
        File wsPackageJson1 = new File(wsDir1, "package.json");
        Files.write(wsPackageJson1.toPath(), "{ \"name\": \"a\" }".getBytes(StandardCharsets.UTF_8));

        File wsDir2 = new File(tempDir, "packages/b/subdir");
        wsDir2.mkdirs();
        File wsPackageJson2 = new File(wsDir2, "package.json");
        Files.write(wsPackageJson2.toPath(), "{ \"name\": \"b\" }".getBytes(StandardCharsets.UTF_8));

        Collection<YarnWorkspace> result = packageJsonFiles.readWorkspacePackageJsonFiles(tempDir);
        assertEquals(2, result.size());

        // Verify the workspaces
        YarnWorkspace workspaceA = result.stream()
            .filter(ws -> "a".equals(ws.getName().orElse(null)))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Workspace 'a' not found"));
        assertEquals(wsPackageJson1.getAbsolutePath(), workspaceA.getWorkspacePackageJson().getFile().getAbsolutePath());

        YarnWorkspace workspaceB = result.stream()
            .filter(ws -> "b".equals(ws.getName().orElse(null)))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Workspace 'b' not found"));
        assertEquals(wsPackageJson2.getAbsolutePath(), workspaceB.getWorkspacePackageJson().getFile().getAbsolutePath());
    }
}
