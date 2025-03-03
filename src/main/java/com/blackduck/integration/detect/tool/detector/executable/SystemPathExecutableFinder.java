package com.blackduck.integration.detect.tool.detector.executable;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Finds an executable on the system path.
public class SystemPathExecutableFinder {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DirectoryExecutableFinder executableFinder;

    public SystemPathExecutableFinder(DirectoryExecutableFinder executableFinder) {
        this.executableFinder = executableFinder;
    }

    public File findExecutable(String executable) {
        String systemPath = System.getenv("PATH");
        String[] pathStrings = Optional.ofNullable(systemPath).map(path -> path.split(File.pathSeparator)).orElse(new String[] {});
        List<File> systemPathLocations = Arrays.stream(pathStrings)
            .map(File::new)
            .collect(Collectors.toList());

        File found = executableFinder.findExecutable(executable, systemPathLocations);
        if (found == null) {
            logger.debug(String.format("Could not find the executable: %s while searching through: %s", executable, systemPath));
        }
        return found;
    }
}
