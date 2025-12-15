package com.blackduck.integration.detect.tool.cdxgen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detect.util.BundledExecutableExtractor;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.executable.Executable;
import com.blackduck.integration.executable.ExecutableOutput;
import com.blackduck.integration.executable.ExecutableRunnerException;

/**
 * Runs CycloneDX cdxgen to generate Software Bill of Materials (SBOM) in CycloneDX format.
 * Cdxgen is bundled as a platform-specific standalone binary within the JAR.
 */
public class CdxgenRunner {
    private static final Logger logger = LoggerFactory.getLogger(CdxgenRunner.class);
    private static final String CDXGEN_RESOURCE_BASE = "/tools/cdxgen";
    private static final String CDXGEN_EXECUTABLE_NAME = "cdxgen";

    private final BundledExecutableExtractor executableExtractor;
    private final DetectableExecutableRunner executableRunner;
    private File extractedExecutable;

    public CdxgenRunner(DetectableExecutableRunner executableRunner) {
        this.executableRunner = executableRunner;
        this.executableExtractor = new BundledExecutableExtractor();
    }

    /**
     * Initialize cdxgen by extracting the bundled binary to a temporary directory.
     *
     * @param tempDirectory Directory to extract the cdxgen binary to
     * @return true if initialization was successful, false otherwise
     */
    public boolean initialize(File tempDirectory) {
        if (extractedExecutable != null && extractedExecutable.exists()) {
            logger.debug("Cdxgen already initialized at: {}", extractedExecutable);
            return true;
        }

        String executableName = executableExtractor.getPlatformExecutableName(CDXGEN_EXECUTABLE_NAME);
        String resourcePath = executableExtractor.getPlatformResourcePath(CDXGEN_RESOURCE_BASE, executableName);

        logger.info("Initializing cdxgen from bundled binary: {}", resourcePath);
        extractedExecutable = executableExtractor.extractExecutable(resourcePath, tempDirectory, executableName);

        if (extractedExecutable == null || !extractedExecutable.exists()) {
            logger.error("Failed to initialize cdxgen. The bundled binary could not be extracted.");
            return false;
        }

        logger.info("Cdxgen initialized successfully at: {}", extractedExecutable.getAbsolutePath());
        return true;
    }

    /**
     * Run cdxgen to generate an SBOM for the specified source path.
     *
     * @param sourcePath Path to the source code to analyze
     * @param outputPath Path where the SBOM file should be written
     * @param projectTypes List of project types to include (e.g., "java", "javascript", "python")
     * @param fetchLicenses Whether to fetch license information for components
     * @return CdxgenResult containing the execution result and SBOM file location
     */
    public CdxgenResult runCdxgen(File sourcePath, File outputPath, List<String> projectTypes, boolean fetchLicenses) {
        if (extractedExecutable == null || !extractedExecutable.exists()) {
            logger.error("Cdxgen has not been initialized. Call initialize() first.");
            return CdxgenResult.failure("Cdxgen not initialized");
        }

        if (!sourcePath.exists()) {
            logger.error("Source path does not exist: {}", sourcePath);
            return CdxgenResult.failure("Source path does not exist: " + sourcePath);
        }

        List<String> arguments = buildCdxgenArguments(outputPath, sourcePath, projectTypes);

        try {
            logger.info("Running cdxgen on: {}", sourcePath.getAbsolutePath());
            logger.debug("Cdxgen command: {} {}", extractedExecutable, String.join(" ", arguments));

            // Set environment variables to enable license fetching if requested
            Map<String, String> environmentVariables = new HashMap<>();
            if (fetchLicenses) {
                environmentVariables.put("FETCH_LICENSE", "true");
            }

            Executable executable = Executable.create(
                sourcePath,
                environmentVariables,
                extractedExecutable.getAbsolutePath(),
                arguments
            );

            ExecutableOutput output = executableRunner.execute(executable);

            if (output.getReturnCode() == 0) {
                // Verify that the output file was actually created
                // Cdxgen can return 0 even if it fails to create the SBOM
                if (!outputPath.exists() || outputPath.length() == 0) {
                    logger.error("Cdxgen completed with return code 0 but did not create the output file: {}", outputPath);
                    if (fetchLicenses) {
                        logger.error("Note: FETCH_LICENSE was enabled, which may have contributed to the failure.");
                    }
                    logger.error("Cdxgen stdout: {}", output.getStandardOutput());
                    logger.error("Cdxgen stderr: {}", output.getErrorOutput());
                    return CdxgenResult.failure("Cdxgen did not create output file");
                }
                logger.info("Cdxgen completed successfully. SBOM written to: {}", outputPath);
                return CdxgenResult.success(outputPath);
            } else {
                logger.error("Cdxgen failed with return code: {}", output.getReturnCode());
                logger.error("Cdxgen stderr: {}", output.getErrorOutput());
                return CdxgenResult.failure("Cdxgen failed with return code: " + output.getReturnCode());
            }

        } catch (ExecutableRunnerException e) {
            logger.error("Failed to execute cdxgen: {}", e.getMessage(), e);
            return CdxgenResult.failure("Failed to execute cdxgen: " + e.getMessage());
        }
    }

    /**
     * Build the command-line arguments for cdxgen.
     */
    private List<String> buildCdxgenArguments(File outputPath, File sourcePath, List<String> projectTypes) {
        List<String> arguments = new ArrayList<>();

        // Output file
        arguments.add("-o");
        arguments.add(outputPath.getAbsolutePath());

        // Project types (if specified)
        if (projectTypes != null && !projectTypes.isEmpty()) {
            for (String projectType : projectTypes) {
                if (projectType != null && !projectType.trim().isEmpty()) {
                    arguments.add("-t");
                    arguments.add(projectType.trim());
                }
            }
        }

        // Additional recommended flags
        arguments.add("--spec-version");
        arguments.add("1.6");

        // Source path to scan
        arguments.add(sourcePath.getAbsolutePath());

        return arguments;
    }

    /**
     * Get the version of cdxgen.
     *
     * @return Version string or null if unable to determine
     */
    public String getVersion() {
        if (extractedExecutable == null || !extractedExecutable.exists()) {
            logger.warn("Cannot get cdxgen version - not initialized");
            return null;
        }

        try {
            Executable executable = Executable.create(
                new File("."),
                extractedExecutable.getAbsolutePath(),
                List.of("--version")
            );

            ExecutableOutput output = executableRunner.execute(executable);
            if (output.getReturnCode() == 0) {
                return output.getStandardOutput().trim();
            }
        } catch (ExecutableRunnerException e) {
            logger.debug("Failed to get cdxgen version: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Clean up extracted executable if needed.
     */
    public void cleanup() {
        if (extractedExecutable != null && extractedExecutable.exists()) {
            try {
                Files.deleteIfExists(extractedExecutable.toPath());
                logger.debug("Cleaned up cdxgen executable: {}", extractedExecutable);
            } catch (IOException e) {
                logger.debug("Failed to clean up cdxgen executable: {}", e.getMessage());
            }
        }
    }
}
