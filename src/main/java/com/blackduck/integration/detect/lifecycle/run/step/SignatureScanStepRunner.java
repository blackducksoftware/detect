package com.blackduck.integration.detect.lifecycle.run.step;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.codelocation.signaturescanner.ScanBatch;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.ScanBatchRunner;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.command.ScanCommandOutput;
import com.blackduck.integration.blackduck.http.BlackDuckRequestBuilder;
import com.blackduck.integration.blackduck.service.BlackDuckApiClient;
import com.blackduck.integration.blackduck.service.BlackDuckServicesFactory;
import com.blackduck.integration.blackduck.service.model.NotificationTaskRange;
import com.blackduck.integration.blackduck.service.request.BlackDuckResponseRequest;
import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.data.DockerTargetData;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.detect.lifecycle.run.step.utility.UploaderHelper;
import com.blackduck.integration.detect.tool.signaturescanner.ScanBatchRunnerUserResult;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScanPath;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScanReportStatus;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScannerCodeLocationResult;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScannerReport;
import com.blackduck.integration.detect.tool.signaturescanner.operation.SignatureScanOuputResult;
import com.blackduck.integration.detect.tool.signaturescanner.operation.SignatureScanResult;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.rest.HttpMethod;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.sca.upload.client.uploaders.ScassUploader;
import com.blackduck.integration.sca.upload.client.uploaders.UploaderFactory;
import com.blackduck.integration.sca.upload.rest.status.ScassUploadStatus;
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
            List<SignatureScannerReport> reports = operationRunner.createSignatureScanReport(scanPaths, scanResult.getScanBatchOutput().getOutputs());
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
        // TODO can invoke the same way for SCASS and legacy but need to make a small change
        // to pass in a boolean or something to trigger the new flag option in blackduck-common command creation class.
        SignatureScanOuputResult scanOuputResult = operationRunner.signatureScan(scanBatch, scanBatchRunner);
        
        List<SignatureScannerReport> reports = operationRunner.createSignatureScanReport(scanPaths, scanOuputResult.getScanBatchOutput().getOutputs());
        SignatureScanReportStatus successStatus = operationRunner.publishSignatureScanReport(reports);

        if (successStatus.isSuccess()) {
            // TODO At this point the signature scan has been run and we will have reported on any failures in
            // its execution. We now need to a) upload the file if this is a SCASS scan and b) wait for results
            // if necessary.
            
            // TODO // > whatever signature scanner can do multiple scans and SCASS, then upload file
            
            
            // TODO if failures in upload then need to publish an exit code update to say scan failed
            
            // Check if we need to copy csv files. Only do this if the user asked for it and we are not
            // connected to BlackDuck. If we are connected to BlackDuck the scanner is responsible for 
            // sending the csv there.
            if (scanBatch.isCsvArchive() && !isOnline) {
                for (ScanCommandOutput output : scanOuputResult.getScanBatchOutput().getOutputs()) {
                    copyCsvFiles(output.getSpecificRunOutputDirectory(), operationRunner.getDirectoryManager().getCsvOutputDirectory());
                }
            }
            
            // Do not attempt to gather additional information, and parse files that are potentially not
            // there, if we should not wait at the scan level
            processEachScan(scanIdsToWaitFor, scanOuputResult, gson, shouldWaitAtScanLevel); 
        }

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
    
    /**
     * A single signature scan (scan CLI) invocation can result in multiple scans being done. For example,
     * in addition to the main signature scan, a snippet scan or other secondary scan can occur. We need
     * to handle each of these in turn.
     */
    private void processEachScan(Set<String> scanIdsToWaitFor, SignatureScanOuputResult signatureScanOutputResult, Gson gson, boolean shouldWaitAtScanLevel) throws IOException {
        List<ScanCommandOutput> outputs = signatureScanOutputResult.getScanBatchOutput().getOutputs();

        for (ScanCommandOutput output : outputs) {
            File specificRunOutputDirectory = output.getSpecificRunOutputDirectory();
            String scanOutputLocation = specificRunOutputDirectory.toString() + SignatureScanResult.OUTPUT_FILE_PATH;

            try {
                Reader reader = Files.newBufferedReader(Paths.get(scanOutputLocation));

                SignatureScanResult result = gson.fromJson(reader, SignatureScanResult.class);
                
                // TODO if (hub > min can do signature scass)
                performScassSteps(specificRunOutputDirectory.toString(), result);
                
                if (shouldWaitAtScanLevel && scanIdsToWaitFor != null) {
                    scanIdsToWaitFor.addAll(result.parseScanIds());
                }
            } catch (NoSuchFileException e) {
                logger.warn("Unable to find scanOutput.json file at location: " + scanOutputLocation
                        + ". Will skip waiting for this signature scan.");
                
                // TODO if SCASS this is an error since the upload cannot occur, use same conditional as above
                // todo.
            }
        }
    }
    
    // TODO this is very similar to ScassScanStepRunner.runScassScan
    private void performScassSteps(String runDirectory, SignatureScanResult result) throws IOException {
        // TODO if the bdio is not there then SCASS might not be enabled and the
        // signature scanner would operate in legacy mode.
        String pathToBdio = runDirectory + "/bdio/" + result.getScanId() + ".bdio";

        // TODO should I add md5 checks?

        ScassUploader scaasScanUploader;
        try {
            UploaderFactory uploadFactory = UploaderHelper.getUploaderFactory(blackDuckRunData);
            scaasScanUploader = uploadFactory.createScassUploader();

            ScassUploadStatus uploadResult = scaasScanUploader.upload(
                    HttpMethod.fromMethod(result.getUploadUrlData().getMethod()), result.getUploadUrl(),
                    // result.getUploadUrlData().getHeaders(),
                    getAllHeaders(result.getUploadUrlData()), Path.of(pathToBdio));

            // TODO error handling like in ScassScanStepRunner

            notifyUploadComplete(result.getScanId());
        } catch (IntegrationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    // TODO duplicate code
    private Map<String, String> getAllHeaders(SignatureScanResult.UploadUrlData uploadUrlData) {
        Map<String, String> allHeaders = new HashMap<>();
        List<Map<String, String>> headers = uploadUrlData.getHeaders();
        
        for (Map<String, String> singleHeader : headers) {
            allHeaders.put(singleHeader.get("name"), singleHeader.get("value"));
        }
        
        return allHeaders;
    }
    
    // TODO duplicate code
    private Response notifyUploadComplete(String scanId) throws IntegrationException {
        BlackDuckServicesFactory blackDuckServicesFactory = blackDuckRunData.getBlackDuckServicesFactory();
        BlackDuckApiClient blackDuckApiClient = blackDuckServicesFactory.getBlackDuckApiClient();
        
        String notfyUrl = "/api/scans/{}/scass-scan-processing".replace("{}", scanId);

        HttpUrl postUrl = blackDuckRunData.getBlackDuckServerConfig().getBlackDuckUrl().appendRelativeUrl(notfyUrl);

        BlackDuckResponseRequest buildBlackDuckResponseRequest = new BlackDuckRequestBuilder()
            .addHeader("Content-Type", "application/vnd.blackducksoftware.scan-6+json")
            .post()
            .buildBlackDuckResponseRequest(postUrl);

        try (Response response = blackDuckApiClient.execute(buildBlackDuckResponseRequest)) {
            return response;
        } catch (IntegrationException e) {
            logger.trace("Could not notify scan container that scan upload is complete.");
            throw new IntegrationException("Could not execute SCASS notification request.", e);
        } catch (IOException e) {
            logger.trace("I/O error occurred during SCASS notification request.");
            throw new IntegrationException("I/O error occurred during SCASS notification request.", e);
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
