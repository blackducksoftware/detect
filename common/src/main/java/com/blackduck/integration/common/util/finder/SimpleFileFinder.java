package com.blackduck.integration.common.util.finder;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.jetbrains.annotations.NotNull;

public class SimpleFileFinder implements FileFinder {

    // filter.test(file) should return true for: dirs to Exclude, and files to Include
    @NotNull
    @Override
    public List<File> findFiles(File directoryToSearch, Predicate<File> filter, boolean followSymLinks, int depth, boolean findInsideMatchingDirectories) {
        List<File> foundFiles = new ArrayList<>();
        if (depth < 0) {
            return foundFiles;
        }
        if (!shouldFindInDirectory(directoryToSearch, followSymLinks)) {
            return foundFiles;
        }
        File[] allFiles = directoryToSearch.listFiles();
        if (allFiles == null) {
            return foundFiles;
        }
        for (File file : allFiles) {
            boolean matches = filter.test(file);
            if (matches) {
                foundFiles.add(file);
            }
            if (!matches || findInsideMatchingDirectories) {
                foundFiles.addAll(findFiles(file, filter, followSymLinks, depth - 1, findInsideMatchingDirectories));
            }
        }

        return foundFiles;
    }

    private boolean shouldFindInDirectory(File file, boolean followSymLinks) {
        return file.isDirectory() && (followSymLinks || !Files.isSymbolicLink(file.toPath()));
    }

    @NotNull
    @Override
    public List<File> findFiles(File directoryToSearch, List<String> filenamePatterns, boolean followSymLinks, int depth, boolean findInsideMatchingDirectories) {
        Predicate<File> wildcardFilter = file -> {
            WildcardFileFilter filter = new WildcardFileFilter(filenamePatterns);
            return filter.accept(file);
        };
        return findFiles(directoryToSearch, wildcardFilter, followSymLinks, depth, findInsideMatchingDirectories);
    }

}
