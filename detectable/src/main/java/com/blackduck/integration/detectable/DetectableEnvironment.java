package com.blackduck.integration.detectable;

import java.io.File;

public class DetectableEnvironment {
    private final DirectoryCache directoryCache;

    public DetectableEnvironment(File directory) {
        this.directoryCache = new DirectoryCache(directory);
    }

    public File getDirectory() {
        return directoryCache.getWrappedDirectory();
    }

    // since many detectables perform identical IO operations on the same directory,
    // we use this wrapper to memoize the results
    static class DirectoryCache {
        private final File originalDirectory;
        private final DirectoryWrapper wrappedDirectory;
        
        private File[] files;
        private boolean attemptedList = false;
        private boolean isDirectory;
        private boolean attemptedIsDirectory = false;

        public DirectoryCache(File directory) {
            this.originalDirectory = directory;
            this.wrappedDirectory = new DirectoryWrapper();
        }

        public File getWrappedDirectory() {
            return wrappedDirectory;
        }

        public File[] listFiles() {
            //if (!attemptedList) {
                attemptedList = true;
                files = originalDirectory.listFiles();
            //}
            return files;
        }

        public boolean isDirectory() {
            //if (!attemptedIsDirectory) {
                attemptedIsDirectory = true;
                isDirectory = originalDirectory.isDirectory();
            //}
            return isDirectory;
        }

        private class DirectoryWrapper extends File {
            public DirectoryWrapper() {
                super(originalDirectory.getAbsolutePath());
            }

            @Override
            public File[] listFiles() {
                return DirectoryCache.this.listFiles();
            }

            @Override
            public boolean isDirectory() {
                return DirectoryCache.this.isDirectory();
            }
        }
    }
}
