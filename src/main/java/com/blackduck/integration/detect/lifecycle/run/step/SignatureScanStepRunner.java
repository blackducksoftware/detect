package com.blackduck.integration.detect.lifecycle.run.step;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.codelocation.Result;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.ScanBatch;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.ScanBatchRunner;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.command.ScanCommandOutput;
import com.blackduck.integration.blackduck.service.model.NotificationTaskRange;
import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.data.DockerTargetData;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.detect.tool.signaturescanner.ScanBatchRunnerUserResult;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScanPath;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScannerCodeLocationResult;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScannerReport;
import com.blackduck.integration.detect.tool.signaturescanner.operation.SignatureScanOuputResult;
import com.blackduck.integration.detect.tool.signaturescanner.operation.SignatureScanResult;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.util.NameVersion;
import com.google.gson.Gson;

public class SignatureScanStepRunner {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final BlackDuckRunData blackDuckRunData;
    private final OperationRunner operationRunner;

    public SignatureScanStepRunner(OperationRunner operationRunner, BlackDuckRunData blackDuckRunData) {
        this.operationRunner = operationRunner;
        this.blackDuckRunData = blackDuckRunData;
    }

    public SignatureScannerCodeLocationResult runSignatureScannerOnline(String detectRunUuid, NameVersion projectNameVersion, DockerTargetData dockerTargetData, Set<String> scanIdsToWaitFor, Gson gson)
        throws DetectUserFriendlyException, OperationException, IOException {        
        ScanBatchRunner scanBatchRunner = resolveOnlineScanBatchRunner(blackDuckRunData);

        List<SignatureScanPath> scanPaths = operationRunner.createScanPaths(projectNameVersion, dockerTargetData);
        ScanBatch scanBatch = operationRunner.createScanBatchOnline(detectRunUuid, scanPaths, projectNameVersion, dockerTargetData, blackDuckRunData);

        NotificationTaskRange notificationTaskRange = operationRunner.createCodeLocationRange(blackDuckRunData);
        List<SignatureScannerReport> reports = executeScan(scanBatch, scanBatchRunner, scanPaths, scanIdsToWaitFor, gson, blackDuckRunData.shouldWaitAtScanLevel(), true);

        return operationRunner.calculateWaitableSignatureScannerCodeLocations(notificationTaskRange, reports);
    }
    
    public SignatureScanOuputResult runRapidSignatureScannerOnline(String detectRunUuid, BlackDuckRunData blackDuckRunData, NameVersion projectNameVersion, DockerTargetData dockerTargetData)
            throws DetectUserFriendlyException, OperationException {
            ScanBatchRunner scanBatchRunner = resolveOnlineScanBatchRunner(blackDuckRunData);

            List<SignatureScanPath> scanPaths = operationRunner.createScanPaths(projectNameVersion, dockerTargetData);
            ScanBatch scanBatch = operationRunner.createScanBatchOnline(detectRunUuid, scanPaths, projectNameVersion, dockerTargetData, blackDuckRunData);

            SignatureScanOuputResult scanResult =  operationRunner.signatureScan(scanBatch, scanBatchRunner);

            // publish report/scan results to status file
            List<SignatureScannerReport> reports = operationRunner.createSignatureScanReport(scanPaths, scanResult.getScanBatchOutput().getOutputs(), Collections.emptySet());
            operationRunner.publishSignatureScanReport(reports);

            return scanResult;
        }

    public void runSignatureScannerOffline(String detectRunUuid, NameVersion projectNameVersion, DockerTargetData dockerTargetData) throws DetectUserFriendlyException, OperationException, IOException {
        ScanBatchRunner scanBatchRunner = resolveOfflineScanBatchRunner();

        List<SignatureScanPath> scanPaths = operationRunner.createScanPaths(projectNameVersion, dockerTargetData);
        ScanBatch scanBatch = operationRunner.createScanBatchOffline(detectRunUuid, scanPaths, projectNameVersion, dockerTargetData);

        executeScan(scanBatch, scanBatchRunner, scanPaths, null, null, false, false);
    }

    private List<SignatureScannerReport> executeScan(ScanBatch scanBatch, ScanBatchRunner scanBatchRunner, List<SignatureScanPath> scanPaths, Set<String> scanIdsToWaitFor, Gson gson, boolean shouldWaitAtScanLevel, boolean isOnline) throws OperationException, IOException {
        // Step 1: Run Scan CLI
        SignatureScanOuputResult scanOuputResult = operationRunner.signatureScan(scanBatch, scanBatchRunner);      

        // Step 2: Check results and upload BDIO
        Set<String> failedScans = processEachScan(scanIdsToWaitFor, scanOuputResult, gson, shouldWaitAtScanLevel, scanBatch.isScassScan(), isOnline, scanBatch.isCsvArchive()); 

        // Step 3: Report on results
        List<SignatureScannerReport> reports = operationRunner.createSignatureScanReport(scanPaths, scanOuputResult.getScanBatchOutput().getOutputs(), failedScans);
        operationRunner.publishSignatureScanReport(reports);
        return reports;
    }

    private ScanBatchRunner resolveOfflineScanBatchRunner() throws DetectUserFriendlyException, OperationException {
        return resolveScanBatchRunner(null);
    }

    private ScanBatchRunner resolveOnlineScanBatchRunner(BlackDuckRunData blackDuckRunData) throws DetectUserFriendlyException, OperationException {
        return resolveScanBatchRunner(blackDuckRunData);
    }

    private ScanBatchRunner resolveScanBatchRunner(@Nullable BlackDuckRunData blackDuckRunData) throws DetectUserFriendlyException, OperationException {
        Optional<File> localScannerPath = operationRunner.calculateOnlineLocalScannerInstallPath();
        ScanBatchRunnerUserResult userProvided = findUserProvidedScanBatchRunner(localScannerPath);
        File installDirectory = determineScanInstallDirectory(userProvided);

        ScanBatchRunner scanBatchRunner;
        if (userProvided.getScanBatchRunner().isPresent()) {
            scanBatchRunner = userProvided.getScanBatchRunner().get();
        } else {
            if (blackDuckRunData != null) {
                scanBatchRunner = operationRunner.createScanBatchRunnerWithBlackDuck(blackDuckRunData, installDirectory);
            } else {
                scanBatchRunner = operationRunner.createScanBatchRunnerFromLocalInstall(installDirectory);
            }
        }

        return scanBatchRunner;
    }

    private File determineScanInstallDirectory(ScanBatchRunnerUserResult userProvided) throws OperationException {
        if (userProvided.getInstallDirectory().isPresent()) {
            return userProvided.getInstallDirectory().get();
        } else {
            return operationRunner.calculateDetectControlledInstallDirectory();
        }
    }

    private ScanBatchRunnerUserResult findUserProvidedScanBatchRunner(Optional<File> localScannerPath)
        throws OperationException { //TODO: This should be handled by a decision somewhere.
        if (localScannerPath.isPresent()) {
            logger.debug("Signature scanner given an existing path for the scanner - we won't attempt to manage the install.");
            return ScanBatchRunnerUserResult.fromLocalInstall(operationRunner.createScanBatchRunnerFromLocalInstall(localScannerPath.get()), localScannerPath.get());
        }
        return ScanBatchRunnerUserResult.none();
    }

    private Set<String> processEachScan(Set<String> scanIdsToWaitFor, SignatureScanOuputResult signatureScanOutputResult, Gson gson, boolean shouldWaitAtScanLevel, boolean scassScan, boolean isOnline, boolean isCsvArchive) throws IOException {
        List<ScanCommandOutput> outputs = signatureScanOutputResult.getScanBatchOutput().getOutputs();
        Set<String> failedScans = new HashSet<>();

        for (ScanCommandOutput output : outputs) {
            if (output.getResult() != Result.SUCCESS) {
                continue;
            }
            
            // Check if we need to copy csv files. Only do this if the user asked for it and we are not
            // connected to BlackDuck. If we are connected to BlackDuck the scanner is responsible for 
            // sending the csv there.
            if (isCsvArchive && !isOnline) {
                copyCsvFiles(output.getSpecificRunOutputDirectory(), operationRunner.getDirectoryManager().getCsvOutputDirectory());
            }
            
            if (isOnline) {
                File specificRunOutputDirectory = output.getSpecificRunOutputDirectory();
                String scanOutputLocation = specificRunOutputDirectory.toString()
                        + SignatureScanResult.OUTPUT_FILE_PATH;

                processOnlineScan(scanIdsToWaitFor, gson, shouldWaitAtScanLevel, scassScan, failedScans, output,
                        specificRunOutputDirectory, scanOutputLocation);
            }
        }
        
        return failedScans;
    }

    private void processOnlineScan(Set<String> scanIdsToWaitFor, Gson gson, boolean shouldWaitAtScanLevel,
            boolean scassScan, Set<String> failedScans, ScanCommandOutput output, File specificRunOutputDirectory,
            String scanOutputLocation) throws IOException {
        try {
            Reader reader = Files.newBufferedReader(Paths.get(scanOutputLocation));

            SignatureScanResult result = gson.fromJson(reader, SignatureScanResult.class);
            
            // This is a SCASS scan if we have an upload URL. We'll need to upload the BDIO.
            // If it is not a SCASS scan skip this section as the signature scanner already uploaded
            // the BDIO.
            if (result.getUploadUrl() != null) {
                ScassScanStepRunner scassScanStepRunner = new ScassScanStepRunner(blackDuckRunData);
                String pathToBdio = specificRunOutputDirectory.toString() + "/bdio/" + result.getScanId() + ".bdio";
                Optional<File> optionalBdio = Optional.of(new File(pathToBdio));

                scassScanStepRunner.runScassScan(optionalBdio, result);
            }
            
            if (shouldWaitAtScanLevel && scanIdsToWaitFor != null) {
                scanIdsToWaitFor.addAll(result.parseScanIds());
            }
        } catch (NoSuchFileException e) {
            failedScans.add(output.getCodeLocationName());
            handleNoScanStatusFile(scanIdsToWaitFor, shouldWaitAtScanLevel, scassScan, scanOutputLocation);
        } catch (IntegrationException e) {
            failedScans.add(output.getCodeLocationName());
            operationRunner.publishSignatureFailure(e.getMessage());
        }
    }

    private void handleNoScanStatusFile(Set<String> scanIdsToWaitFor, boolean shouldWaitAtScanLevel, boolean scassScan,
            String scanOutputLocation) {        
        if (scassScan) {
            String errorMessage = String.format("Unable to find scanOutput.json file at location: {}. Unable to upload BDIO to continue signature scan.", scanOutputLocation);
            operationRunner.publishSignatureFailure(errorMessage);
        } else if (shouldWaitAtScanLevel && scanIdsToWaitFor != null) {
            logger.warn("Unable to find scanOutput.json file at location: " + scanOutputLocation
                    + ". Will skip waiting for this signature scan.");
        }
    }

    private void copyCsvFiles(File sourceFolder, File destFolder) throws IOException {
        File[] files = sourceFolder.listFiles((dir, name) -> name.endsWith(".csv"));
        if (files != null) {
            for (File file : files) {
                Path source = file.toPath();
                Path dest = destFolder.toPath().resolve(file.getName());
                Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

}
