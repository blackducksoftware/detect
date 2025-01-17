package com.blackduck.integration.detect.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.tika.Tika;
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
        try (ZipFile zipFile = new ZipFile(zip, ZipFile.OPEN_READ, charset)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path entryPath = destPath.resolve(entry.getName());
                if (!entryPath.normalize().startsWith(dest.toPath().normalize())) {
                    throw new IOException("Zip entry contained path traversal");
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (InputStream in = zipFile.getInputStream(entry)) {
                        try (OutputStream out = new FileOutputStream(entryPath.toFile())) {
                            IOUtils.copy(in, out);
                        }
                    }
                }
            }
        }
    }

    public static boolean isZipFile(File file) throws IOException {
        return new Tika().detect(file).equals("application/zip");
    }
}
