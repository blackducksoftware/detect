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

import com.google.gson.Gson;
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
import com.blackduck.integration.detect.lifecycle.run.data.ScanCreationResponse.UploadUrlData;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.detect.lifecycle.run.step.utility.UploaderHelper;
import com.blackduck.integration.detect.tool.signaturescanner.ScanBatchRunnerUserResult;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScanPath;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScannerCodeLocationResult;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScannerReport;
import com.blackduck.integration.detect.tool.signaturescanner.operation.SignatureScanOuputResult;
import com.blackduck.integration.detect.tool.signaturescanner.operation.SignatureScanResult;
import com.blackduck.integration.detect.workflow.blackduck.codelocation.CodeLocationAccumulator;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.rest.HttpMethod;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.sca.upload.client.uploaders.ScassUploader;
import com.blackduck.integration.sca.upload.client.uploaders.UploaderFactory;
import com.blackduck.integration.sca.upload.rest.status.ScassUploadStatus;
import com.blackduck.integration.util.NameVersion;

public class SignatureScanStepRunner {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    private ScassUploader scaasScanUploader;
    private BlackDuckRunData blackDuckRunData;

    private final OperationRunner operationRunner;

    public SignatureScanStepRunner(OperationRunner operationRunner) {
        this.operationRunner = operationRunner;
    }

    public SignatureScannerCodeLocationResult runSignatureScannerOnline(String detectRunUuid, BlackDuckRunData blackDuckRunData, NameVersion projectNameVersion, DockerTargetData dockerTargetData, Set<String> scanIdsToWaitFor, Gson gson)
        throws DetectUserFriendlyException, OperationException, IOException {

        UploaderFactory uploadFactory;
        try {
            uploadFactory = UploaderHelper.getUploaderFactory(blackDuckRunData);
            scaasScanUploader = uploadFactory.createScassUploader();
            
            // TODO temp
            this.blackDuckRunData = blackDuckRunData;
        } catch (IntegrationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
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
        SignatureScanOuputResult scanOuputResult = operationRunner.signatureScan(scanBatch, scanBatchRunner);
        
        // Do not attempt to gather additional information, and parse files that are potentially not
        // there, if we should not wait at the scan level
        if (shouldWaitAtScanLevel && scanIdsToWaitFor != null) {
            addScansToWaitFor(scanIdsToWaitFor, scanOuputResult, gson);
        }
        
        // Check if we need to copy csv files. Only do this if the user asked for it and we are not
        // connected to BlackDuck. If we are connected to BlackDuck the scanner is responsible for 
        // sending the csv there.
        if (scanBatch.isCsvArchive() && !isOnline) {
            for (ScanCommandOutput output : scanOuputResult.getScanBatchOutput().getOutputs()) {
                copyCsvFiles(output.getSpecificRunOutputDirectory(), operationRunner.getDirectoryManager().getCsvOutputDirectory());
            }
        }

        List<SignatureScannerReport> reports = operationRunner.createSignatureScanReport(scanPaths, scanOuputResult.getScanBatchOutput().getOutputs());
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
    
    private void addScansToWaitFor(Set<String> scanIdsToWaitFor, SignatureScanOuputResult signatureScanOutputResult, Gson gson) throws IOException {
        List<ScanCommandOutput> outputs = signatureScanOutputResult.getScanBatchOutput().getOutputs();

        for (ScanCommandOutput output : outputs) {
            File specificRunOutputDirectory = output.getSpecificRunOutputDirectory();
            String scanOutputLocation = specificRunOutputDirectory.toString() + SignatureScanResult.OUTPUT_FILE_PATH;

            try {
                Reader reader = Files.newBufferedReader(Paths.get(scanOutputLocation));

                // TODO need to move this out of this method as we are getting SCASS info
                // from it now as well.
                SignatureScanResult result = gson.fromJson(reader, SignatureScanResult.class);
                
                try {
                    tempUploadAndReport(result, specificRunOutputDirectory.toString());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IntegrationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
                scanIdsToWaitFor.addAll(result.parseScanIds());
            } catch (NoSuchFileException e) {
                logger.warn("Unable to find scanOutput.json file at location: " + scanOutputLocation
                        + ". Will skip waiting for this signature scan.");
            }
        }
    }
    
    private void tempUploadAndReport(SignatureScanResult result, String runDirectory) throws IOException, IntegrationException {
        // TODO can we ever have more than one BDIO? See parseScanIds as we can get multiple IDs sometimes
        // even though scanId just returns one.
        String pathToBdio = runDirectory + "/bdio/" + result.getScanId() + ".bdio";
        
        // TODO should I add md5 checks?
        
        ScassUploadStatus uploadResult = scaasScanUploader.upload(
                HttpMethod.fromMethod(result.getUploadUrlData().getMethod()), 
                result.getUploadUrl(), 
                //result.getUploadUrlData().getHeaders(), 
                getAllHeaders(result.getUploadUrlData()),
                Path.of(pathToBdio));
        
        // TODO error handling like in ScassScanStepRunner
        
        notifyUploadComplete(result.getScanId());
        
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
