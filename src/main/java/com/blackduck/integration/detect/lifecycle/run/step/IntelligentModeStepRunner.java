package com.blackduck.integration.detect.lifecycle.run.step;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import com.blackduck.integration.detect.lifecycle.run.data.CommonScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.api.generated.view.ProjectVersionView;
import com.blackduck.integration.blackduck.codelocation.CodeLocationCreationData;
import com.blackduck.integration.blackduck.codelocation.binaryscanner.BinaryScanBatchOutput;
import com.blackduck.integration.blackduck.codelocation.upload.UploadBatchOutput;
import com.blackduck.integration.blackduck.codelocation.upload.UploadOutput;
import com.blackduck.integration.blackduck.service.BlackDuckServicesFactory;
import com.blackduck.integration.blackduck.service.model.ProjectVersionWrapper;
import com.blackduck.integration.detect.configuration.enumeration.DetectTool;
import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.data.DockerTargetData;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.detect.lifecycle.run.operation.blackduck.BdioUploadResult;
import com.blackduck.integration.detect.lifecycle.run.step.binary.AbstractBinaryScanStepRunner;
import com.blackduck.integration.detect.lifecycle.run.step.binary.PreScassBinaryScanStepRunner;
import com.blackduck.integration.detect.lifecycle.run.step.binary.ScassOrBdbaBinaryScanStepRunner;
import com.blackduck.integration.detect.lifecycle.run.step.container.AbstractContainerScanStepRunner;
import com.blackduck.integration.detect.lifecycle.run.step.container.PreScassContainerScanStepRunner;
import com.blackduck.integration.detect.lifecycle.run.step.container.ScassOrBdbaContainerScanStepRunner;
import com.blackduck.integration.detect.lifecycle.run.step.packagemanager.PackageManagerStepRunner;
import com.blackduck.integration.detect.lifecycle.run.step.utility.StepHelper;
import com.blackduck.integration.detect.tool.iac.IacScanCodeLocationData;
import com.blackduck.integration.detect.tool.impactanalysis.service.ImpactAnalysisBatchOutput;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScannerCodeLocationResult;
import com.blackduck.integration.detect.util.filter.DetectToolFilter;
import com.blackduck.integration.detect.workflow.bdio.BdioResult;
import com.blackduck.integration.detect.workflow.blackduck.codelocation.CodeLocationAccumulator;
import com.blackduck.integration.detect.workflow.blackduck.codelocation.CodeLocationResults;
import com.blackduck.integration.detect.workflow.blackduck.codelocation.CodeLocationWaitData;
import com.blackduck.integration.detect.workflow.blackduck.integratedmatching.ScanCountsPayloadCreator;
import com.blackduck.integration.detect.workflow.blackduck.integratedmatching.model.ScanCountsPayload;
import com.blackduck.integration.detect.workflow.report.util.ReportConstants;
import com.blackduck.integration.detect.workflow.blackduck.report.ReportData;
import com.blackduck.integration.detect.workflow.blackduck.report.service.ReportService;
import com.blackduck.integration.detect.workflow.result.BlackDuckBomDetectResult;
import com.blackduck.integration.detect.workflow.result.DetectResult;
import com.blackduck.integration.detect.workflow.result.ReportDetectResult;
import com.blackduck.integration.detect.workflow.status.FormattedCodeLocation;
import com.blackduck.integration.detect.workflow.status.OperationType;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.util.NameVersion;
import com.google.gson.Gson;

public class IntelligentModeStepRunner {
    private final OperationRunner operationRunner;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final StepHelper stepHelper;
    private final Gson gson;
    private final ScanCountsPayloadCreator scanCountsPayloadCreator;
    private final String detectRunUuid;

    public IntelligentModeStepRunner(OperationRunner operationRunner, StepHelper stepHelper, Gson gson, ScanCountsPayloadCreator scanCountsPayloadCreator, String detectRunUuid) {
        this.operationRunner = operationRunner;
        this.stepHelper = stepHelper;
        this.gson = gson;
        this.scanCountsPayloadCreator = scanCountsPayloadCreator;
        this.detectRunUuid = detectRunUuid;
    }

    public void runOffline(NameVersion projectNameVersion, DockerTargetData dockerTargetData, BdioResult bdio) throws OperationException {
        stepHelper.runToolIfIncluded(DetectTool.SIGNATURE_SCAN, "Signature Scanner", () -> { //Internal: Sig scan publishes its own status.
            SignatureScanStepRunner signatureScanStepRunner = new SignatureScanStepRunner(operationRunner, null);
            signatureScanStepRunner.runSignatureScannerOffline(detectRunUuid, projectNameVersion, dockerTargetData);
        });
        stepHelper.runToolIfIncludedWithCallbacks(
            DetectTool.IMPACT_ANALYSIS,
            "Vulnerability Impact Analysis",
            () -> generateImpactAnalysis(projectNameVersion),
            operationRunner::publishImpactSuccess,
            operationRunner::publishImpactFailure
        );
        stepHelper.runToolIfIncluded(DetectTool.IAC_SCAN, "IaC Scanner", () -> {
            IacScanStepRunner iacScanStepRunner = new IacScanStepRunner(operationRunner);
            iacScanStepRunner.runIacScanOffline();
        });

        operationRunner.generateComponentLocationAnalysisIfEnabled(bdio);
    }

    //TODO: Change black duck post options to a decision and stick it in Run Data somewhere.
    //TODO: Change detect tool filter to a decision and stick it in Run Data somewhere
    public void runOnline(
        BlackDuckRunData blackDuckRunData,
        BdioResult bdioResult,
        NameVersion projectNameVersion,
        DetectToolFilter detectToolFilter,
        DockerTargetData dockerTargetData,
        Set<String> binaryTargets
    ) throws OperationException {

        ProjectVersionWrapper projectVersion = stepHelper.runAsGroup(
            "Create or Locate Project",
            OperationType.INTERNAL,
            () -> new BlackDuckProjectVersionStepRunner(operationRunner).runAll(projectNameVersion, blackDuckRunData)
        );

        logger.debug("Completed project and version actions.");
        logger.debug("Processing Detect Code Locations.");

        // Used for notifications method of waiting for results (scan level)
        long codeLocationsUploadStartTime = System.currentTimeMillis();

        // Used by new BOM status polling method of waiting for results (BOM summary level)
        Set<String> scanIdsToWaitFor = new HashSet<>();
        // Only true if binary scan + bd version too old for multipart upload
        AtomicBoolean mustWaitAtBomSummaryLevel = new AtomicBoolean(false);

        CodeLocationAccumulator codeLocationAccumulator = new CodeLocationAccumulator();

        if (bdioResult.isNotEmpty()) {
            invokePackageManagerScanningWorkflow(projectNameVersion, blackDuckRunData, scanIdsToWaitFor, bdioResult, codeLocationAccumulator);
        } else {
            logger.debug("No BDIO results to upload. Skipping.");
        }

        stepHelper.runToolIfIncluded(DetectTool.SIGNATURE_SCAN, "Signature Scanner", () -> {
            SignatureScanStepRunner signatureScanStepRunner = new SignatureScanStepRunner(operationRunner, blackDuckRunData);
            SignatureScannerCodeLocationResult signatureScannerCodeLocationResult = signatureScanStepRunner.runSignatureScannerOnline(
                detectRunUuid,
                projectNameVersion,
                dockerTargetData,
                scanIdsToWaitFor,
                gson
            );
            codeLocationAccumulator.addNonWaitableCodeLocations(signatureScannerCodeLocationResult.getWaitableCodeLocationData().getSuccessfulCodeLocationNames());
            codeLocationAccumulator.addNonWaitableCodeLocations(signatureScannerCodeLocationResult.getNonWaitableCodeLocationData());
        });

        stepHelper.runToolIfIncluded(DetectTool.BINARY_SCAN, "Binary Scanner", () -> {           
            invokeBinaryScanningWorkflow(DetectTool.BINARY_SCAN, dockerTargetData, projectNameVersion, blackDuckRunData, binaryTargets, scanIdsToWaitFor, codeLocationAccumulator, mustWaitAtBomSummaryLevel);
        });

        stepHelper.runToolIfIncluded(
            DetectTool.CONTAINER_SCAN,
            "Container Scanner",
            () -> invokeContainerScanningWorkflow(scanIdsToWaitFor, codeLocationAccumulator, blackDuckRunData, projectNameVersion)
        );

        stepHelper.runToolIfIncludedWithCallbacks(
            DetectTool.IMPACT_ANALYSIS,
            "Vulnerability Impact Analysis",
            () -> {
                runImpactAnalysisOnline(projectNameVersion, projectVersion, codeLocationAccumulator, blackDuckRunData.getBlackDuckServicesFactory());
            },
            operationRunner::publishImpactSuccess,
            operationRunner::publishImpactFailure
        );

        stepHelper.runToolIfIncluded(DetectTool.IAC_SCAN, "IaC Scanner", () -> {
            IacScanStepRunner iacScanStepRunner = new IacScanStepRunner(operationRunner);
            IacScanCodeLocationData iacScanCodeLocationData = iacScanStepRunner.runIacScanOnline(detectRunUuid, projectNameVersion, blackDuckRunData);
            codeLocationAccumulator.addNonWaitableCodeLocations(iacScanCodeLocationData.getCodeLocationNames());
        });

        logger.debug("Completed Detect Code Location processing.");

        if (operationRunner.createBlackDuckPostOptions().isCorrelatedScanningEnabled()) {
            stepHelper.runAsGroup("Upload Correlated Scan Counts", OperationType.INTERNAL, () -> {
                uploadCorrelatedScanCounts(blackDuckRunData, codeLocationAccumulator, detectRunUuid);
            });
        }
        operationRunner.attemptToGenerateComponentLocationAnalysisIfEnabled();

        stepHelper.runAsGroup("Wait for Results", OperationType.INTERNAL, () -> {
            // Calculate code locations. We do this even if we don't wait as we want to report code location data 
            // in various reports.
            CodeLocationResults codeLocationResults = calculateCodeLocations(codeLocationAccumulator, codeLocationsUploadStartTime);

            if (operationRunner.createBlackDuckPostOptions().shouldWaitForResults()) {
                // Waiting at the scan level is more reliable, do that if the BD server is >= 2023.1.1
                    pollForBomScanCompletion(blackDuckRunData, projectVersion, scanIdsToWaitFor);

                // If we have scans that we are not yet able to obtain the scanID for, use the original notification based waiting.
                if (mustWaitAtBomSummaryLevel.get()) {
                    waitForWaitableCodeLocations(codeLocationResults.getCodeLocationWaitData(), projectNameVersion, blackDuckRunData);
                }
            }
        });

        stepHelper.runAsGroup("Black Duck Post Actions", OperationType.INTERNAL, () -> {
            checkPolicy(projectVersion.getProjectVersionView(), blackDuckRunData);
            riskReport(blackDuckRunData, projectVersion);
            noticesReport(blackDuckRunData, projectVersion);
            publishPostResults(bdioResult, projectVersion, detectToolFilter);
        });
    }

    private void invokePackageManagerScanningWorkflow(NameVersion projectNameVersion, BlackDuckRunData blackDuckRunData, Set<String> scanIdsToWaitFor, BdioResult bdioResult, CodeLocationAccumulator codeLocationAccumulator) throws OperationException {
        if (PackageManagerStepRunner.areScassScansPossible(blackDuckRunData.getBlackDuckServerVersion())) {
            PackageManagerStepRunner packageManagerScanStepRunner = new PackageManagerStepRunner(operationRunner);

            CommonScanResult commonScanResult = packageManagerScanStepRunner.invokePackageManagerScanningWorkflow(projectNameVersion, blackDuckRunData, bdioResult);
            String scanId = null;
            if(commonScanResult != null) {
                scanId = commonScanResult.getScanId() == null ? null : commonScanResult.getScanId().toString();
                if(commonScanResult.isPackageManagerScassPossible())
                {
                    scanIdsToWaitFor.add(scanId);
                    logger.debug("Added package manager scan {} to list of scanIds to wait for.", scanId);
                    codeLocationAccumulator.addNonWaitableCodeLocation(commonScanResult.getCodeLocationName());
                    codeLocationAccumulator.incrementAdditionalCounts(DetectTool.DETECTOR, 1);
                    return;
                }
            }
            invokePreScassPackageManagerWorkflow(blackDuckRunData, bdioResult, scanIdsToWaitFor, codeLocationAccumulator, scanId); // becauuuuuuse ... if we got a scan ID back, then we know the upload failed and we need to do it ourselves, if we didnt get a scan ID back, then we are in the old workflow and also need to do the upload ourselves, but in that case we have no scan ID to pass in. so either way, we call the same method, just with different scan ID values.... is that so?
        } else {
            String scanId = null;
            invokePreScassPackageManagerWorkflow(blackDuckRunData, bdioResult, scanIdsToWaitFor, codeLocationAccumulator, scanId); // only called if bd version <2025.7.0. in this scassscanId is null, but the bdio upload response has a scan id and we dd that to wait for.
        }
    }

    private void invokePreScassPackageManagerWorkflow(BlackDuckRunData blackDuckRunData, BdioResult bdioResult, Set<String> scanIdsToWaitFor, CodeLocationAccumulator codeLocationAccumulator, String scanId) throws OperationException {
        stepHelper.runAsGroup("Upload Bdio", OperationType.INTERNAL, () -> {
            uploadBdio(blackDuckRunData, bdioResult, scanIdsToWaitFor, codeLocationAccumulator, operationRunner.calculateDetectTimeout(), scanId); // even when called prescass style w/ null scanId, this method adds to scanIds towait for.
        });
    }

    private void invokeBinaryScanningWorkflow(
        DetectTool detectTool,
        DockerTargetData dockerTargetData,
        NameVersion projectNameVersion,
        BlackDuckRunData blackDuckRunData,
        Set<String> binaryTargets,
        Set<String> scanIdsToWaitFor,
        CodeLocationAccumulator codeLocationAccumulator,
        AtomicBoolean mustWaitAtBomSummaryLevel
    )
        throws IntegrationException, OperationException {
        logger.debug("Invoking intelligent persistent binary scan.");

        AbstractBinaryScanStepRunner binaryScanStepRunner = CommonScanStepRunner.areScassScansPossible(blackDuckRunData.getBlackDuckServerVersion()) ?
            new ScassOrBdbaBinaryScanStepRunner(operationRunner) :
            new PreScassBinaryScanStepRunner(operationRunner);

        Optional<UUID> scanId = binaryScanStepRunner.invokeBinaryScanningWorkflow(dockerTargetData, projectNameVersion, 
                blackDuckRunData, binaryTargets); // takes wait for results property value AND creates code locations WITH task range ... but does it call notifications api? only if legacy is kicked off --> document.
        if (scanId.isPresent()) {
            scanIdsToWaitFor.add(scanId.get().toString());
            logger.debug("Added binary scan {} to list of scanIds to wait for.", scanId);
        } else {
            Optional<CodeLocationCreationData<BinaryScanBatchOutput>> codeLocations = binaryScanStepRunner.getCodeLocations(); // empty unless multipart upload couldnt be done, in which case, great we need it ... right?

            // Waitable code Locations are only present if server version was too old for multipart binary upload (<2024.7.0)
            if (codeLocations.isPresent()) {
                codeLocationAccumulator.addWaitableCodeLocations(detectTool, codeLocations.get());
                mustWaitAtBomSummaryLevel.set(true);
            }
        }
    }

    private void invokeContainerScanningWorkflow(
        Set<String> scanIdsToWaitFor,
        CodeLocationAccumulator codeLocationAccumulator,
        BlackDuckRunData blackDuckRunData,
        NameVersion projectNameVersion
    ) throws IntegrationException, OperationException{
        logger.debug("Invoking intelligent persistent container scan.");

        AbstractContainerScanStepRunner containerScanStepRunner;
        if (CommonScanStepRunner.areScassScansPossible(blackDuckRunData.getBlackDuckServerVersion())) {
            containerScanStepRunner = new ScassOrBdbaContainerScanStepRunner(operationRunner, projectNameVersion, blackDuckRunData, gson);
        } else {
            containerScanStepRunner = new PreScassContainerScanStepRunner(operationRunner, projectNameVersion, blackDuckRunData, gson);
        }

        Optional<UUID> scanId = containerScanStepRunner.invokeContainerScanningWorkflow();
        scanId.ifPresent(uuid -> {
            scanIdsToWaitFor.add(uuid.toString());
            logger.debug("Added container scan {} to list of scanIds to wait for.", uuid);
        });
        Set<String> containerScanCodeLocations = new HashSet<>();
        containerScanCodeLocations.add(containerScanStepRunner.getCodeLocationName());
        codeLocationAccumulator.addNonWaitableCodeLocations(containerScanCodeLocations);
    }

    private void pollForBomScanCompletion(BlackDuckRunData blackDuckRunData, ProjectVersionWrapper projectVersion,
            Set<String> scanIdsToWaitFor) throws IntegrationException, OperationException {
        HttpUrl bomToSearchFor = projectVersion.getProjectVersionView().getFirstLink(ProjectVersionView.BOM_STATUS_LINK);

        for (String scanId : scanIdsToWaitFor) {
            if (scanId == null) {
                logger.debug("Unexpected null scanID for project version" + projectVersion.getProjectVersionView().getVersionName()
                        + " skipping waiting for this scan.");
                continue;
            }
            
            HttpUrl scanToSearchFor = new HttpUrl(bomToSearchFor.toString() + "/" + scanId);
            operationRunner.waitForBomCompletion(blackDuckRunData, scanToSearchFor);
        }
    }

    // note this is fallback/prescass pkg mngr workflow's bdio upload. used to call to get task range.
    public void uploadBdio(BlackDuckRunData blackDuckRunData, BdioResult bdioResult, Set<String> scanIdsToWaitFor, CodeLocationAccumulator codeLocationAccumulator, Long timeout, String scassScanId) throws OperationException {
        BdioUploadResult uploadResult = operationRunner.uploadBdioForIntelligentPersistentMode(blackDuckRunData, bdioResult, timeout, scassScanId); // uploadBdio --> uploadBdioWithoutNotificationsQuery

        Optional<CodeLocationCreationData<UploadBatchOutput>> codeLocationCreationData = uploadResult.getUploadOutput();

        codeLocationCreationData.ifPresent(uploadBatchOutputCodeLocationCreationData -> codeLocationAccumulator.addNonWaitableCodeLocations(
                uploadBatchOutputCodeLocationCreationData.getOutput().getSuccessfulCodeLocationNames()
                ));
        codeLocationAccumulator.incrementAdditionalCounts(DetectTool.DETECTOR, 1);

        if (uploadResult.getUploadOutput().isPresent()) {
            for (UploadOutput result : uploadResult.getUploadOutput().get().getOutput()) {
                result.getScanId().ifPresent((scanId) -> {
                    scanIdsToWaitFor.add(scanId); // the upload result basically always gives scanID. and so we always add it for scanIdsToWaitFor
                    logger.debug("Added BDIO upload (prescass pkg mngr) scan ID to list of scanIds to wait for: {}", scanId);
                        }
                );
            }
        }
    }
    
    public void uploadCorrelatedScanCounts(BlackDuckRunData blackDuckRunData, CodeLocationAccumulator codeLocationAccumulator, String detectRunUuid) throws OperationException {
        logger.debug("Uploading correlated scan counts to Black Duck SCA (correlation ID: {})", detectRunUuid);
        ScanCountsPayload scanCountsPayload = scanCountsPayloadCreator.create(codeLocationAccumulator.getWaitableCodeLocations(), codeLocationAccumulator.getAdditionalCountsByTool());
        
        if (scanCountsPayload.isValid()) {
            operationRunner.uploadCorrelatedScanCounts(blackDuckRunData, detectRunUuid, scanCountsPayload);
        } else {
            logger.debug("Upload skipped, there was no correlation data to send.");
        }
    }

    public CodeLocationResults calculateCodeLocations(CodeLocationAccumulator codeLocationAccumulator, long codeLocationsUploadStartTime) throws OperationException { //this is waiting....
        logger.info(ReportConstants.RUN_SEPARATOR);

        Set<String> allCodeLocationNames = new HashSet<>(codeLocationAccumulator.getNonWaitableCodeLocations());
        CodeLocationWaitData waitData = operationRunner.calculateCodeLocationWaitDataGivenNotificationRangeStart(codeLocationAccumulator.getWaitableCodeLocations(), codeLocationsUploadStartTime);
        allCodeLocationNames.addAll(waitData.getCodeLocationNames());
        
        Set<FormattedCodeLocation> allCodeLocationData = new HashSet<>();
        for (String codeLocationName : allCodeLocationNames) {
            FormattedCodeLocation codeLocation = new FormattedCodeLocation(codeLocationName, null, null);
            allCodeLocationData.add(codeLocation);
        }
        
        operationRunner.publishCodeLocationData(allCodeLocationData); // needs no range.
        return new CodeLocationResults(allCodeLocationNames, waitData); // the return value is the only place the "wait data" is used...
    }

    private boolean shouldPublishBomLinkForTool(DetectToolFilter detectToolFilter) {
        return detectToolFilter.shouldInclude(DetectTool.SIGNATURE_SCAN) ||
            detectToolFilter.shouldInclude(DetectTool.CONTAINER_SCAN) ||
            detectToolFilter.shouldInclude(DetectTool.BINARY_SCAN);
    }

    private void publishPostResults(BdioResult bdioResult, ProjectVersionWrapper projectVersionWrapper, DetectToolFilter detectToolFilter) {
        if ((!bdioResult.getUploadTargets().isEmpty() || shouldPublishBomLinkForTool(detectToolFilter))) {
            Optional<String> componentsLink = Optional.ofNullable(projectVersionWrapper)
                .map(ProjectVersionWrapper::getProjectVersionView)
                .flatMap(projectVersionView -> projectVersionView.getFirstLinkSafely(ProjectVersionView.COMPONENTS_LINK))
                .map(HttpUrl::string);

            if (componentsLink.isPresent()) {
                DetectResult detectResult = new BlackDuckBomDetectResult(componentsLink.get());
                operationRunner.publishResult(detectResult);
            }
        }
    }

    private void checkPolicy(ProjectVersionView projectVersionView, BlackDuckRunData blackDuckRunData) throws OperationException {
        logger.info("Checking to see if Detect should check policy for violations.");
        if (operationRunner.createBlackDuckPostOptions().shouldPerformSeverityPolicyCheck()) {
            operationRunner.checkPolicyBySeverity(blackDuckRunData, projectVersionView);
        }
        if (operationRunner.createBlackDuckPostOptions().shouldPerformNamePolicyCheck()) {
            operationRunner.checkPolicyByName(blackDuckRunData, projectVersionView);
        }
    }
    
    public void waitForWaitableCodeLocations(CodeLocationWaitData codeLocationWaitData, NameVersion projectNameVersion, BlackDuckRunData blackDuckRunData)
            throws OperationException {
            logger.info("Checking to see if Detect should wait for bom tool calculations to finish.");
            if (codeLocationWaitData.getExpectedNotificationCount() > 0) {
                logger.debug("Will use old notifications based waiting for the following code locations: {}", codeLocationWaitData.getCodeLocationNames()); // wait why is bdio and signature here but not impact analysis
                logger.debug("Notifications after {} will be considered.", codeLocationWaitData.getNotificationRange().getStartDate());
                operationRunner.waitForCodeLocations(blackDuckRunData, codeLocationWaitData, projectNameVersion);
            }
        }

    public void runImpactAnalysisOnline(
        NameVersion projectNameVersion,
        ProjectVersionWrapper projectVersionWrapper,
        CodeLocationAccumulator codeLocationAccumulator,
        BlackDuckServicesFactory blackDuckServicesFactory
    ) throws OperationException {
        String impactAnalysisName = operationRunner.generateImpactAnalysisCodeLocationName(projectNameVersion);
        Path impactFile = operationRunner.generateImpactAnalysisFile(impactAnalysisName);
        CodeLocationCreationData<ImpactAnalysisBatchOutput> uploadData = operationRunner.uploadImpactAnalysisFile( // note: we never make use of uploadData's notificationTaskRange field. it is null with the changes. should do better than null in future iteration. but it is used in SO many places. (38 to be exact)
            impactFile,
            projectNameVersion,
            impactAnalysisName,
            blackDuckServicesFactory
        );
        operationRunner.mapImpactAnalysisCodeLocations(impactFile, uploadData, projectVersionWrapper, blackDuckServicesFactory);
        /* TODO: There is currently no mechanism within Black Duck for checking the completion status of an Impact Analysis code location. Waiting should happen here when such a mechanism exists. See HUB-25142. JM - 08/2020 */
        codeLocationAccumulator.addNonWaitableCodeLocations(uploadData.getOutput().getSuccessfulCodeLocationNames());
    }

    private Path generateImpactAnalysis(NameVersion projectNameVersion) throws OperationException {
        String impactAnalysisName = operationRunner.generateImpactAnalysisCodeLocationName(projectNameVersion);
        return operationRunner.generateImpactAnalysisFile(impactAnalysisName);
    }

    public void riskReport(BlackDuckRunData blackDuckRunData, ProjectVersionWrapper projectVersion) throws IOException, OperationException, IntegrationException {
        Optional<File> riskReportPdfFile = operationRunner.calculateRiskReportPdfFileLocation();
        Optional<File> riskReportJsonFile = operationRunner.calculateRiskReportJsonFileLocation();

        ReportService reportService = null;
        ReportData reportData = null;

        if (riskReportPdfFile.isPresent() || riskReportJsonFile.isPresent()) {
            reportService = operationRunner.creatReportService(blackDuckRunData);
            reportData = reportService.getRiskReportData(projectVersion.getProjectView(), projectVersion.getProjectVersionView());
        }

        if (riskReportPdfFile.isPresent()) {
            riskReportCreation(reportData, reportService, "pdf", riskReportPdfFile);
        }

        if (riskReportJsonFile.isPresent()) {
            riskReportCreation(reportData, reportService, "json", riskReportJsonFile);
        }
    }

    private void riskReportCreation(ReportData reportData, ReportService reportService, String reportType, Optional<File> riskReportFile) throws OperationException, IOException {
        logger.info("Creating risk report {}", reportType);
        File reportDirectory = riskReportFile.get();

        if (!reportDirectory.exists() && !reportDirectory.mkdirs()) {
            logger.warn(String.format("Failed to create risk report %s directory: %s", reportType, reportDirectory));
        }

        File createdReport = operationRunner.createRiskReportFile(reportDirectory, reportType, reportService, reportData);

        logger.info(String.format("Created risk report %s: %s", reportType, createdReport.getCanonicalPath()));
        operationRunner.publishReport(new ReportDetectResult("Risk Report", createdReport.getCanonicalPath()));
    }

    public void noticesReport(BlackDuckRunData blackDuckRunData, ProjectVersionWrapper projectVersion) throws OperationException, IOException {
        Optional<File> noticesReportDirectory = operationRunner.calculateNoticesDirectory();
        if (noticesReportDirectory.isPresent()) {
            logger.info("Creating notices report");
            File noticesDirectory = noticesReportDirectory.get();

            if (!noticesDirectory.exists() && !noticesDirectory.mkdirs()) {
                logger.warn(String.format("Failed to create notices directory at %s", noticesDirectory));
            }

            File noticesFile = operationRunner.createNoticesReportFile(blackDuckRunData, projectVersion, noticesDirectory);
            logger.info(String.format("Created notices report: %s", noticesFile.getCanonicalPath()));

            operationRunner.publishReport(new ReportDetectResult("Notices Report", noticesFile.getCanonicalPath()));

        }
    }
}
