package com.blackduck.integration.detect.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DetectZipUtil { //TODO: Add method for extracting without the wrapper method.
    private static final Logger logger = LoggerFactory.getLogger(DetectZipUtil.class);

    public static void unzip(File zip, File dest) throws IOException {
        unzip(zip, dest, Charset.defaultCharset());
    }

    public static void zip(File zip, Map<String, Path> entries) throws IOException {
        try (FileOutputStream fileStream = new FileOutputStream(zip)) {
            zip(fileStream, entries);
        }
    }

    public static void zip(OutputStream stream, Map<String, Path> entries) throws IOException {
        try (ZipOutputStream outputStream = new ZipOutputStream(stream)) {
            byte[] buffer = new byte[1024];
            int length;
            for (Map.Entry<String, Path> entry : entries.entrySet()) {
                if (entry.getValue().toFile().isFile()) {
                    logger.debug("Adding entry '{}' to zip as '{}'.", entry.getValue().toString(), entry.getKey());
                    outputStream.putNextEntry(new ZipEntry(entry.getKey()));
                    try (InputStream in = Files.newInputStream(entry.getValue())) {
                        while ((length = in.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }
                    }
                    outputStream.closeEntry();
                } else {
                    logger.trace("Non-file {} skipped", entry.getValue().toFile().getAbsolutePath());
                }
            }
        }
    }

    public static void unzip(File zip, File dest, Charset charset) throws IOException {
        Path destPath = dest.toPath();
        try (ZipFile zipFile = ZipFile.builder().setFile(zip).setCharset(charset).get()) {
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                Path entryPath = destPath.resolve(entry.getName());
                if (!entryPath.normalize().startsWith(dest.toPath().normalize())) {
                    throw new IOException("Zip entry contained path traversal");
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (InputStream in = zipFile.getInputStream(entry)) {
                        Files.copy(in, entryPath);
                    }
                    applyUnixPermissions(entryPath, entry.getUnixMode());
                }
            }
        }
    }

    private static void applyUnixPermissions(Path filePath, int unixMode) {
        if (unixMode == 0) {
            return; // no Unix permissions stored in this entry
        }
        try {
            Set<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);
            if ((unixMode & 0400) != 0) permissions.add(PosixFilePermission.OWNER_READ);
            if ((unixMode & 0200) != 0) permissions.add(PosixFilePermission.OWNER_WRITE);
            if ((unixMode & 0100) != 0) permissions.add(PosixFilePermission.OWNER_EXECUTE);
            if ((unixMode & 0040) != 0) permissions.add(PosixFilePermission.GROUP_READ);
            if ((unixMode & 0020) != 0) permissions.add(PosixFilePermission.GROUP_WRITE);
            if ((unixMode & 0010) != 0) permissions.add(PosixFilePermission.GROUP_EXECUTE);
            if ((unixMode & 0004) != 0) permissions.add(PosixFilePermission.OTHERS_READ);
            if ((unixMode & 0002) != 0) permissions.add(PosixFilePermission.OTHERS_WRITE);
            if ((unixMode & 0001) != 0) permissions.add(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(filePath, permissions);
        } catch (UnsupportedOperationException e) {
            // Windows filesystems do not support POSIX permissions; fall back to setExecutable
            if ((unixMode & 0111) != 0) {
                filePath.toFile().setExecutable(true, false);
            }
        } catch (IOException e) {
            logger.warn("Could not set permissions on extracted file: {}", filePath);
        }
    }
}
