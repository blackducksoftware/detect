package com.blackduck.integration.detect.lifecycle.run.step;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;

import com.blackduck.integration.rest.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.blackduck.integration.blackduck.api.generated.view.DeveloperScansScanView;
import com.blackduck.integration.blackduck.codelocation.Result;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.command.ScanCommandOutput;
import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.configuration.enumeration.BlackduckScanMode;
import com.blackduck.integration.detect.configuration.enumeration.DetectTool;
import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.data.DockerTargetData;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.detect.lifecycle.run.step.container.PreScassContainerScanStepRunner;
import com.blackduck.integration.detect.lifecycle.run.step.utility.StepHelper;
import com.blackduck.integration.detect.tool.signaturescanner.operation.SignatureScanOuputResult;
import com.blackduck.integration.detect.tool.signaturescanner.operation.SignatureScanResult;
import com.blackduck.integration.detect.workflow.bdio.BdioResult;
import com.blackduck.integration.detect.workflow.blackduck.developer.aggregate.RapidScanResultSummary;
import com.blackduck.integration.detect.workflow.file.DirectoryManager;
import com.blackduck.integration.detect.workflow.status.FormattedCodeLocation;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.util.NameVersion;

public class RapidModeStepRunner {
    private final OperationRunner operationRunner;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final StepHelper stepHelper;
    private final Gson gson;
    private final String detectRunUuid;
    private final DirectoryManager directoryManager;
    public static final String RAPID_SCAN_ENDPOINT = "/api/developer-scans";


    public RapidModeStepRunner(OperationRunner operationRunner, StepHelper stepHelper, Gson gson, String detectRunUuid, DirectoryManager directoryManager) {
        this.operationRunner = operationRunner;
        this.stepHelper = stepHelper;
        this.gson = gson;
        this.detectRunUuid = detectRunUuid;
        this.directoryManager = directoryManager;
    }

    public void runOnline(BlackDuckRunData blackDuckRunData, NameVersion projectVersion, BdioResult bdioResult,
            DockerTargetData dockerTargetData, Optional<String> scaaasFilePath) throws OperationException {
        operationRunner.phoneHome(blackDuckRunData);
        Optional<File> rapidScanConfig = operationRunner.findRapidScanConfig();
        String scanMode = blackDuckRunData.getScanMode().displayName();
        rapidScanConfig.ifPresent(config -> logger.info("Found " + scanMode.toLowerCase() + " scan config file: {}", config));

        String blackDuckUrl = blackDuckRunData.getBlackDuckServerConfig().getBlackDuckUrl().toString();
        List<HttpUrl> parsedUrls = new ArrayList<>();
        Set<FormattedCodeLocation> formattedCodeLocations = new HashSet<>();

        List<HttpUrl> uploadResultsUrls = operationRunner.performRapidUpload(blackDuckRunData, bdioResult, rapidScanConfig.orElse(null));
        if (uploadResultsUrls != null && uploadResultsUrls.size() > 0) {
            processScanResults(uploadResultsUrls, parsedUrls, formattedCodeLocations, DetectTool.DETECTOR.name());
        }

        stepHelper.runToolIfIncluded(DetectTool.SIGNATURE_SCAN, "Signature Scanner", () -> {
            logger.debug("Stateless scan signature scan detected.");

            SignatureScanStepRunner signatureScanStepRunner = new SignatureScanStepRunner(operationRunner, blackDuckRunData);
            SignatureScanOuputResult signatureScanOutputResult = signatureScanStepRunner
                    .runRapidSignatureScannerOnline(detectRunUuid, blackDuckRunData, projectVersion, dockerTargetData);

            List<HttpUrl> parseScanUrls = parseScanUrls(scanMode, signatureScanOutputResult, blackDuckUrl);
            processScanResults(parseScanUrls, parsedUrls, formattedCodeLocations, DetectTool.SIGNATURE_SCAN.name());
        });

        stepHelper.runToolIfIncluded(DetectTool.BINARY_SCAN, "Binary Scanner", () -> {
            logger.debug("Stateless binary scan detected.");
            
            // Check if this is an SCA environment. Stateless Binary Scans are only supported there.
            if (scaaasFilePath.isPresent()) {
                List<HttpUrl> bdbaResultUrls = new ArrayList<>();
                invokeBdbaRapidScan(blackDuckRunData, blackDuckUrl, bdbaResultUrls, false, scaaasFilePath.get());
                processScanResults(bdbaResultUrls, parsedUrls, formattedCodeLocations, DetectTool.BINARY_SCAN.name());
            } else {
                logger.debug("Stateless binary scan detected but no detect.scaaas.scan.path specified, skipping.");
            }
        });
        
        stepHelper.runToolIfIncluded(DetectTool.CONTAINER_SCAN, "Container Scanner", () -> {
                logger.debug("Stateless container scan detected.");
                // Check if this is an SCA environment.
                if (scaaasFilePath.isPresent()) {
                    List<HttpUrl> containerResultUrls = new ArrayList<>();
                    invokeBdbaRapidScan(blackDuckRunData, blackDuckUrl, containerResultUrls, true, scaaasFilePath.get());
                    processScanResults(containerResultUrls, parsedUrls, formattedCodeLocations, DetectTool.CONTAINER_SCAN.name());
                } else {
                    PreScassContainerScanStepRunner containerScanStepRunner = new PreScassContainerScanStepRunner(operationRunner, projectVersion, blackDuckRunData, gson);
                    logger.debug("Invoking stateless container scan.");
                    Optional<UUID> scanId = containerScanStepRunner.invokeContainerScanningWorkflow();
                    if (scanId.isPresent()) {
                        String statelessScanEndpoint = operationRunner.getScanServicePostEndpoint();
                        HttpUrl scanServiceUrlToPoll = new HttpUrl(blackDuckUrl + statelessScanEndpoint + "/" + scanId.get());
                        logger.info("Stateless mode container scan URL: {}", scanServiceUrlToPoll);
                        parsedUrls.add(scanServiceUrlToPoll);
                        formattedCodeLocations.add(new FormattedCodeLocation(containerScanStepRunner.getCodeLocationName(), scanId.get(), DetectTool.CONTAINER_SCAN.name()));
                    }
                }
            });

        // Fetch scan results using the V6 full-result endpoint (scan-6+json) for all rapid scans.
        // Previously, a compact V5 call (scan-5+json) was made unconditionally here, followed by a
        // separate V6 call only for QuackPatch. Since V6 is a superset of V5 for every field that
        // Detect actually reads internally, the V5 call is redundant and replaced here entirely.
        //
        // The V6 response is converted back into List<DeveloperScansScanView> (the existing V5 type)
        // via Gson so all downstream operations (aggregation, JSON output, component location analysis)
        // remain unchanged. V6-only fields are ignored by Gson; V5-only fields (originId, policyStatuses)
        // will be null — this is the documented breaking change for users parsing the output JSON.
        //
        // Content strings are read from each Response immediately and cached: Response.getContentString()
        // reads the underlying HTTP entity stream which can only be consumed once. The cached strings
        // are reused by both convertContentsToScanViews() below and generateFullRapidJsonFile() in the
        // QuackPatch block.
        BlackduckScanMode mode = blackDuckRunData.getScanMode();
        List<Response> rapidFullResults = operationRunner.waitForRapidFullResults(blackDuckRunData, parsedUrls, mode);
        List<String> fullResultContents = extractContentStrings(rapidFullResults);
        List<DeveloperScansScanView> rapidResults = convertContentsToScanViews(fullResultContents);

        if (operationRunner.shouldAttemptQuackPatchFullResults()) {
            logger.info("Quack Patch is enabled, using full Rapid scan results.");
            if (fullResultContents.isEmpty()) {
                logger.info("Quack Patch requires non-empty Rapid Scan results. Skipping Quack Patch.");
            } else {
                File jsonFileFULL = operationRunner.generateFullRapidJsonFile(fullResultContents.get(0));
                operationRunner.runQuackPatch(jsonFileFULL);
            }
        }

        // Generate a report, even an empty one if no scans were done as that is what previous detect versions did.
        File jsonFile = operationRunner.generateRapidJsonFile(projectVersion, rapidResults);
        operationRunner.generateComponentLocationAnalysisIfEnabled(rapidResults, bdioResult);
        RapidScanResultSummary summary = operationRunner.logRapidReport(rapidResults, mode);

        operationRunner.publishRapidResults(jsonFile, summary, mode);
        operationRunner.publishCodeLocationData(formattedCodeLocations);
    }

    /**
     * This method takes a list of URLs for a given scan type and adds them to the parsedUrls structure so
     * results can be retrieved from BD after all scans are done. It also stores information for the status.json
     * file in formattedCodeLocations so scanId and type can be reported.
     */
    private void processScanResults(List<HttpUrl> scanResultUrls, List<HttpUrl> parsedUrls,
            Set<FormattedCodeLocation> formattedCodeLocations, String scanType) {
        for (HttpUrl httpUrl : scanResultUrls) {
            UUID scanId;
            try {
                scanId = operationRunner.getScanIdFromScanUrl(httpUrl);
                parsedUrls.add(httpUrl);
                FormattedCodeLocation codeLocationData = new FormattedCodeLocation(null, scanId, scanType);
                formattedCodeLocations.add(codeLocationData);
            } catch (IllegalArgumentException e) {
                logger.info(String.format("Unable to parse scanId from URL %s", httpUrl));
            }
        }
    }

    private void invokeBdbaRapidScan(BlackDuckRunData blackDuckRunData, String blackDuckUrl,
            List<HttpUrl> parsedUrls, boolean isContainerScan, String scaasFilePath)
            throws IntegrationException, IOException, InterruptedException, OperationException, DetectUserFriendlyException {
        // Generate the UUID we use to communicate with BDBA
        UUID bdbaScanId = UUID.randomUUID();
        
        RapidBdbaStepRunner rapidBdbaStepRunner = new RapidBdbaStepRunner(gson, bdbaScanId, blackDuckRunData.getBlackDuckServerConfig().getTimeout());
        rapidBdbaStepRunner.submitScan(isContainerScan, scaasFilePath);
        rapidBdbaStepRunner.pollForResults();
        rapidBdbaStepRunner.downloadAndExtractBdio(directoryManager);

        UUID bdScanId = operationRunner.initiateStatelessBdbaScan(blackDuckRunData);
        operationRunner.uploadBdioEntriesForRapidMode(blackDuckRunData, bdScanId);

        // add this scan to the URLs to wait for
        parsedUrls.add(new HttpUrl(blackDuckUrl + String.format(RAPID_SCAN_ENDPOINT + "/" + bdScanId.toString())));
    }

    /**
     * The signature scanner only returns a high level success or failure to us. Details are in the
     * output directory's scanOutput.json. We need to crack that open to get the scanId so we can poll
     * for the true results from BlackDuck later.
     * 
     * @return a list of URLs that BlackDuck should poll for rapid signature scan results.
     */
    private List<HttpUrl> parseScanUrls(String scanMode, SignatureScanOuputResult signatureScanOutputResult, String blackDuckUrl) throws IOException, IntegrationException {
        List<ScanCommandOutput> outputs = signatureScanOutputResult.getScanBatchOutput().getOutputs();
        List<HttpUrl> parsedUrls = new ArrayList<>(outputs.size());
        
        for (ScanCommandOutput output : outputs) {
        	// Don't bother further processing scans that have failed. We have already reported errors on them.
        	if (output.getResult().equals(Result.FAILURE)) {
        		continue;
        	}

            try {
                File specificRunOutputDirectory = output.getSpecificRunOutputDirectory();
                String scanOutputLocation = specificRunOutputDirectory.toString() + SignatureScanResult.OUTPUT_FILE_PATH;
                Reader reader = Files.newBufferedReader(Paths.get(scanOutputLocation));

                SignatureScanResult result = gson.fromJson(reader, SignatureScanResult.class);

                if (result.getExitStatus() == null || !result.getExitStatus().equalsIgnoreCase("FAILURE")) {

                    Set<String> parsedIds = result.parseScanIds();

                    for (String id : parsedIds) {
                        HttpUrl url = new HttpUrl(blackDuckUrl + RAPID_SCAN_ENDPOINT + "/" + id);

                        logger.info(scanMode + " mode signature scan URL: {}", url);
                        parsedUrls.add(url);
                    }
                } else {
                    logger.debug("{} mode signature scan result not processed for scan IDs due to exit status from BD: {}", scanMode, result.getExitStatus());
                }
            } catch (Exception e) {
                throw new IntegrationException("Unable to parse rapid signature scan results.");
            }
        }
        return parsedUrls;
    }

    // Reads each Response body string upfront and returns them as a plain List<String>.
    // This must be done before any other consumer touches the responses: the underlying
    // Apache HttpClient entity stream can only be read once per Response object.
    private List<String> extractContentStrings(List<Response> responses) throws OperationException {
        try {
            List<String> contents = new ArrayList<>();
            for (Response response : responses) {
                contents.add(response.getContentString());
            }
            return contents;
        } catch (IntegrationException e) {
            throw new OperationException(e);
        }
    }

    // Converts V6 full-result page content strings into the V5 DeveloperScansScanView type.
    // Each content string is a paged API response: { "items": [ ... ], "totalCount": N, ... }.
    // Gson deserializes each item by field name — fields present in both V5 and V6 map correctly,
    // V6-only fields (matchTypes, allVulnerabilities, etc.) are silently ignored, and V5-only
    // fields (originId, policyStatuses) are left null since they do not exist in the V6 schema.
    private List<DeveloperScansScanView> convertContentsToScanViews(List<String> contents) {
        List<DeveloperScansScanView> scanViews = new ArrayList<>();
        for (String content : contents) {
            JsonObject page = gson.fromJson(content, JsonObject.class);
            JsonArray items = page.getAsJsonArray("items");
            if (items != null) {
                for (JsonElement item : items) {
                    scanViews.add(gson.fromJson(item, DeveloperScansScanView.class));
                }
            }
        }
        return scanViews;
    }
}
