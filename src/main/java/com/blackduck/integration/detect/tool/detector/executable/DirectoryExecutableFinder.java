package com.blackduck.integration.detect.tool.detector.executable;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.util.OperatingSystemType;

// Finds an executable in a directory.
public class DirectoryExecutableFinder {
    private final List<String> extensions;
    private final FileFinder fileFinder;

    public static DirectoryExecutableFinder forCurrentOperatingSystem(FileFinder fileFinder) {
        return DirectoryExecutableFinder.forOperatingSystem(OperatingSystemType.determineFromSystem(), fileFinder);
    }

    public static DirectoryExecutableFinder forOperatingSystem(OperatingSystemType operatingSystemType, FileFinder fileFinder) {
        if (operatingSystemType == OperatingSystemType.WINDOWS) {
            return new DirectoryExecutableFinder(Arrays.asList(".cmd", ".bat", ".exe"), fileFinder);
        } else {
            return new DirectoryExecutableFinder(Collections.emptyList(), fileFinder);
        }
    }

    public DirectoryExecutableFinder(List<String> extensions, FileFinder fileFinder) {
        this.extensions = extensions;
        this.fileFinder = fileFinder;
    }

    private List<String> executablesFromName(String name) {
        if (extensions.isEmpty()) {
            return Collections.singletonList(name);
        }

        // If the name already has an extension (like "ant.bat"), don't add more extensions
        // This allows explicit specification of the exact executable file
        for (String ext : extensions) {
            if (name.endsWith(ext)) {
                return Collections.singletonList(name);
            }
        }

        // Otherwise, add all possible extensions
        return extensions.stream().map(ext -> name + ext).collect(Collectors.toList());
    }

    @Nullable
    public File findExecutable(String executable, File location) {
        return findExecutable(executable, Collections.singletonList(location));
    }

    @Nullable
    public File findExecutable(String executable, List<File> locations) {
        List<String> executables = executablesFromName(executable);

        for (File location : locations) {
            for (String possibleExecutable : executables) {
                File foundFile = fileFinder.findFile(location, possibleExecutable);
                if (foundFile != null && foundFile.exists() && foundFile.canExecute()) {
                    return foundFile;
                }
            }
        }

        return null;
    }
}