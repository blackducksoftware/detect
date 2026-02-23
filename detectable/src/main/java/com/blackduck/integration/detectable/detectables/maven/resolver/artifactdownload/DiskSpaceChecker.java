package com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Checks available disk space before downloads.
 */
public class DiskSpaceChecker {
    private static final Logger logger = LoggerFactory.getLogger(DiskSpaceChecker.class);
    private static final long SAFETY_MARGIN_BYTES = 100 * 1024 * 1024; // 100 MB safety margin

    public static class DiskSpaceCheckResult {
        private final boolean hasEnoughSpace;
        private final long availableBytes;
        private final long requiredBytes;

        public DiskSpaceCheckResult(boolean hasEnoughSpace, long availableBytes, long requiredBytes) {
            this.hasEnoughSpace = hasEnoughSpace;
            this.availableBytes = availableBytes;
            this.requiredBytes = requiredBytes;
        }

        public boolean hasEnoughSpace() {
            return hasEnoughSpace;
        }

        public long getAvailableBytes() {
            return availableBytes;
        }

        public long getRequiredBytes() {
            return requiredBytes;
        }
    }

    public DiskSpaceCheckResult checkAvailableSpace(Path targetPath, long requiredBytes, long startOffset) {
        try {
            FileStore fileStore = Files.getFileStore(targetPath.getParent());
            long availableBytes = fileStore.getUsableSpace();
            long bytesNeeded = requiredBytes - startOffset + SAFETY_MARGIN_BYTES;

            boolean hasEnough = availableBytes >= bytesNeeded;

            if (!hasEnough) {
                logger.warn("Insufficient disk space: available={} KB, required={} KB",
                    availableBytes / 1024, bytesNeeded / 1024);
            }

            return new DiskSpaceCheckResult(hasEnough, availableBytes, bytesNeeded);

        } catch (IOException e) {
            logger.debug("Could not check disk space: {}", e.getMessage());
            // Assume enough space if we can't check
            return new DiskSpaceCheckResult(true, -1, requiredBytes);
        }
    }
}