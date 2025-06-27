package com.blackduck.integration.detect.tool.signaturescanner.operation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import com.blackduck.integration.blackduck.codelocation.signaturescanner.command.ApiScannerInstaller;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.command.ScanPaths;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.command.ScanPathsUtility;
import com.blackduck.integration.blackduck.exception.BlackDuckIntegrationException;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScannerLogger;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScannerVersion;
import com.blackduck.integration.util.IntEnvironmentVariables;
import com.blackduck.integration.util.OperatingSystemType;

public class SignatureScanVersionChecker {
    private static final String SIGNATURE_SCAN_VERSION_FORMAT = "^(\\d+)\\.(\\d+)\\.(\\d+)(?:[-\\w]*)?$";
    
    public static SignatureScannerVersion getSignatureScannerVersion(Logger logger,
            Optional<Path> localScannerInstallPath, File toolsDirectory) throws IOException {
        SignatureScannerVersion scannerVersion = null;
        
        // If user overrides where the signature scanner is it will be stored here.         
        if (localScannerInstallPath.isPresent()) {
            scannerVersion = obtainVersionFromRunningScanCli(logger, localScannerInstallPath, scannerVersion);
        } else {
            scannerVersion = obtainVersionFromTxtFile(toolsDirectory);
        }
        
        return scannerVersion;
    }

    private static SignatureScannerVersion obtainVersionFromRunningScanCli(Logger logger,
            Optional<Path> localScannerInstallPath, SignatureScannerVersion scannerVersion) throws IOException {
        Path localScanCliPath = localScannerInstallPath.get();

        ScanPathsUtility scanPathsUtility = new ScanPathsUtility(new SignatureScannerLogger(logger),
                IntEnvironmentVariables.includeSystemEnv(), OperatingSystemType.determineFromSystem());
        try {
            ScanPaths scannerLocation = scanPathsUtility.searchForScanPaths(localScanCliPath.toFile());

            List<String> cmd = new ArrayList<>();
            scannerLocation.addJavaAndOnePathArguments(cmd);
            scannerLocation.addScanExecutableArguments(cmd);
            cmd.add("--no-prompt");
            cmd.add("--version");

            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            Process blackDuckCliProcess = processBuilder.start();

            try (InputStream inputStream = blackDuckCliProcess.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.matches(SIGNATURE_SCAN_VERSION_FORMAT)) {
                        scannerVersion = parseSemVer(line);
                        break;
                    }
                }
            }
        } catch (BlackDuckIntegrationException e) {
            // Indicate we could not obtain the version
            logger.debug("Unable to determine signature scanner version from executing --version operation: {}", e.getMessage());
            scannerVersion = null;
        }
        return scannerVersion;
    }

    private static SignatureScannerVersion obtainVersionFromTxtFile(File toolsDirectory) throws IOException {
        SignatureScannerVersion scannerVersion;
        
        // Read blackDuckVersion.txt file to determine version
        File scanCliDirectory = new File(toolsDirectory, ApiScannerInstaller.BLACK_DUCK_SIGNATURE_SCANNER_INSTALL_DIRECTORY);
        File scanCliVersionFile = new File(scanCliDirectory, ApiScannerInstaller.VERSION_FILENAME);
        String localScannerVersion = FileUtils.readFileToString(scanCliVersionFile, Charset.defaultCharset());
        scannerVersion = parseSemVer(localScannerVersion);
        return scannerVersion;
    }
    
    private static SignatureScannerVersion parseSemVer(String version) throws IllegalArgumentException {
        // Regular expression to match x.y.z format, optionally followed by a suffix (e.g., -SNAPSHOT)
        String regex = SIGNATURE_SCAN_VERSION_FORMAT;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(version);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid Signature Scanner version format: " + version);
        }

        try {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            int patch = Integer.parseInt(matcher.group(3));
            return new SignatureScannerVersion(major, minor, patch);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Signature Scanner version components must be integers: " + version, e);
        }
    }
}
