package com.blackduck.integration.detect.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for extracting bundled executable binaries from JAR resources to temporary directory.
 * Used for tools like cdxgen that are bundled as platform-specific standalone binaries.
 */
public class BundledExecutableExtractor {
    private static final Logger logger = LoggerFactory.getLogger(BundledExecutableExtractor.class);

    /**
     * Extract a bundled executable from JAR resources to a temporary directory.
     *
     * @param resourcePath The path to the resource within the JAR (e.g., "/tools/cdxgen/linux-x64/cdxgen")
     * @param tempDirectory The temporary directory to extract the executable to
     * @param executableName The name to give the extracted executable
     * @return The File representing the extracted executable, or null if extraction failed
     */
    public File extractExecutable(String resourcePath, File tempDirectory, String executableName) {
        try {
            logger.debug("Attempting to extract bundled executable from: {}", resourcePath);

            InputStream resourceStream = getClass().getResourceAsStream(resourcePath);
            if (resourceStream == null) {
                logger.warn("Could not find bundled executable at resource path: {}", resourcePath);
                return null;
            }

            if (!tempDirectory.exists() && !tempDirectory.mkdirs()) {
                logger.error("Failed to create temporary directory: {}", tempDirectory);
                return null;
            }

            Path executablePath = new File(tempDirectory, executableName).toPath();
            Files.copy(resourceStream, executablePath, StandardCopyOption.REPLACE_EXISTING);
            resourceStream.close();

            File executableFile = executablePath.toFile();
            if (!executableFile.setExecutable(true)) {
                logger.warn("Failed to set executable permission on: {}", executableFile);
            }

            logger.info("Successfully extracted bundled executable to: {}", executableFile.getAbsolutePath());
            return executableFile;

        } catch (IOException e) {
            logger.error("Failed to extract bundled executable from {}: {}", resourcePath, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Determine the platform-specific resource path for a bundled executable.
     *
     * @param basePath The base path in resources (e.g., "/tools/cdxgen")
     * @param executableName The name of the executable (e.g., "cdxgen" or "cdxgen.exe")
     * @return The full resource path for the current platform
     */
    public String getPlatformResourcePath(String basePath, String executableName) {
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();

        String platform = determinePlatform(osName, osArch);
        return basePath + "/" + platform + "/" + executableName;
    }

    /**
     * Determine the platform identifier based on OS name and architecture.
     *
     * @param osName Operating system name (lowercase)
     * @param osArch Operating system architecture (lowercase)
     * @return Platform identifier (e.g., "linux-x64", "macos-arm64", "windows-x64")
     */
    private String determinePlatform(String osName, String osArch) {
        String os;
        if (osName.contains("win")) {
            os = "windows";
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            os = "macos";
        } else if (osName.contains("linux")) {
            os = "linux";
        } else {
            logger.warn("Unknown operating system: {}, defaulting to linux", osName);
            os = "linux";
        }

        String arch;
        if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            arch = "arm64";
        } else if (osArch.contains("amd64") || osArch.contains("x86_64")) {
            arch = "x64";
        } else {
            logger.warn("Unknown architecture: {}, defaulting to x64", osArch);
            arch = "x64";
        }

        return os + "-" + arch;
    }

    /**
     * Get the platform-specific executable name (adding .exe for Windows).
     *
     * @param baseName The base name of the executable
     * @return The platform-specific executable name
     */
    public String getPlatformExecutableName(String baseName) {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return baseName + ".exe";
        }
        return baseName;
    }
}
