package com.blackduck.integration.detect.tool.impactanalysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.method.analyzer.core.MethodUseAnalyzer;

public class GenerateImpactAnalysisOperation {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public Path generateImpactAnalysis(File toScan, String impactAnalysisCodeLocationName, Path outputDirectory) throws IOException {
        MethodUseAnalyzer analyzer = new MethodUseAnalyzer();
        Path sourceDirectory = toScan.toPath();
        Path outputReportFile = analyzer.analyze(sourceDirectory, outputDirectory, impactAnalysisCodeLocationName);
        logger.info(String.format("Vulnerability Impact Analysis generated report at %s", outputReportFile));
        cleanupTempFiles();
        return outputReportFile;
    }

    // TODO: Stop doing this once the impact analysis library allows us to specify a working directory. See IDETECT-2185.
    private void cleanupTempFiles() throws IOException {
        // Impact Analysis generates temporary directories which need to be moved into directories under Detect control for cleanup.
        String tempDirectoryPrefix = "blackduck-method-uses";
        Path tempDirectory = Files.createTempDirectory(tempDirectoryPrefix);

        try (Stream<Path> stream = Files.walk(tempDirectory.getParent(), 1)) {
            stream.filter(tempPath -> tempPath.getFileName().toString().startsWith(tempDirectoryPrefix))
                .forEach(tempPath -> FileUtils.deleteQuietly(tempPath.toFile()));
        } catch (Exception ignore) {
            // We won't notify the user that we failed to move a temp file for cleanup.
        }
    }
}
