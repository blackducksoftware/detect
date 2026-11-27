package com.blackduck.integration.detect.tool.detector.executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import com.blackduck.integration.common.util.finder.SimpleFileFinder;
import com.blackduck.integration.util.OperatingSystemType;

@DisabledOnOs(OS.WINDOWS)
class DirectoryExecutableFinderTest {
    private final DirectoryExecutableFinder finder = DirectoryExecutableFinder.forOperatingSystem(OperatingSystemType.LINUX, new SimpleFileFinder());

    @Test
    void findsExecutableFile(@TempDir Path tempDir) throws IOException {
        File executable = createExecutableFile(tempDir, "go");

        File found = finder.findExecutable("go", tempDir.toFile());

        assertEquals(executable, found);
    }

    @Test
    void ignoresDirectoryWithExecutableName(@TempDir Path tempDir) throws IOException {
        File directory = Files.createDirectory(tempDir.resolve("go")).toFile();
        assertTrue(directory.canExecute(), "Precondition: directories are searchable/executable on POSIX");

        File found = finder.findExecutable("go", tempDir.toFile());

        assertNull(found);
    }

    @Test
    void skipsDirectoryEarlierInSearchPathAndFindsLaterExecutable(@TempDir Path tempDir) throws IOException {
        // Mirrors a PATH like /opt/hostedtoolcache:/usr/bin where /opt/hostedtoolcache/go is a directory.
        Path toolCache = Files.createDirectory(tempDir.resolve("toolcache"));
        Files.createDirectory(toolCache.resolve("go"));
        Path bin = Files.createDirectory(tempDir.resolve("bin"));
        File executable = createExecutableFile(bin, "go");

        File found = finder.findExecutable("go", Arrays.asList(toolCache.toFile(), bin.toFile()));

        assertEquals(executable, found);
    }

    @Test
    void ignoresNonExecutableFile(@TempDir Path tempDir) throws IOException {
        File file = Files.createFile(tempDir.resolve("go")).toFile();
        assertTrue(file.setExecutable(false, false));

        File found = finder.findExecutable("go", Collections.singletonList(tempDir.toFile()));

        assertNull(found);
    }

    private File createExecutableFile(Path directory, String name) throws IOException {
        File file = Files.createFile(directory.resolve(name)).toFile();
        assertTrue(file.setExecutable(true));
        return file;
    }
}
