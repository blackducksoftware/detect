package com.blackduck.integration.detect.lifecycle.run.step;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.ArrayList;

import com.blackduck.integration.detect.lifecycle.AggregateOperationException;
import com.blackduck.integration.detect.lifecycle.run.data.CommonScanResult;
import com.blackduck.integration.detect.lifecycle.run.step.utility.ConcurrentScanWaiter;
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
    ) throws Exception {

        logger.debug("Starting parallel scan execution at {}", System.currentTimeMillis());


        ExecutorService executorService = Executors.newFixedThreadPool(operationRunner.maxParallelProcessors());
        ConcurrentScanWaiter concurrentScanWaiter = new ConcurrentScanWaiter(executorService, operationRunner);

        List<CompletableFuture<Void>> scanFutures = new ArrayList<>();

        ProjectVersionWrapper projectVersion = stepHelper.runAsGroup(
            "Create or Locate Project",
            OperationType.INTERNAL,
            () -> new BlackDuckProjectVersionStepRunner(operationRunner).runAll(projectNameVersion, blackDuckRunData)
        );

        logger.debug("Completed project and version actions.");
        logger.debug("Processing Detect Code Locations.");

        Queue<String> scanIdsToWaitFor = new ConcurrentLinkedQueue<>();
        AtomicBoolean mustWaitAtBomSummaryLevel = new AtomicBoolean(false);

        CodeLocationAccumulator codeLocationAccumulator = new CodeLocationAccumulator();

        if (bdioResult.isNotEmpty()) {
            scanFutures.add(CompletableFuture.runAsync(() -> {
                try {
                    Thread.currentThread().setName("Package Manager Scan Thread");
                    invokePackageManagerScanningWorkflow(projectNameVersion, blackDuckRunData, scanIdsToWaitFor, bdioResult, codeLocationAccumulator, concurrentScanWaiter, projectVersion);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executorService));
        } else {
            logger.debug("No BDIO results to upload. Skipping.");
        }

        logger.debug("Completed Detect Code Location processing.");

        scanFutures.add(CompletableFuture.runAsync(() -> {
            try {
                Thread.currentThread().setName("Signature Scan Thread");
                stepHelper.runToolIfIncluded(DetectTool.SIGNATURE_SCAN, "Signature Scanner", () -> {
                    Queue<String> scanIds = new ConcurrentLinkedQueue<>();
                    SignatureScanStepRunner signatureScanStepRunner = new SignatureScanStepRunner(operationRunner, blackDuckRunData);
                    SignatureScannerCodeLocationResult signatureScannerCodeLocationResult = signatureScanStepRunner.runSignatureScannerOnline(
                            detectRunUuid,
                            projectNameVersion,
                            dockerTargetData,
                            scanIds,
                            gson
                    );

                    for (String scanId : scanIds) {
                        logger.debug("Waiting for signature scan id {}", scanId);
                        concurrentScanWaiter.startWaitingForScan(scanId, blackDuckRunData, projectVersion, "Signature Scan");
                    }

                    codeLocationAccumulator.addWaitableCodeLocations(signatureScannerCodeLocationResult.getWaitableCodeLocationData());
                    codeLocationAccumulator.addNonWaitableCodeLocation(signatureScannerCodeLocationResult.getNonWaitableCodeLocationData());
                });
            } catch (OperationException e) {
                throw new RuntimeException(e);
            }
        }, executorService));


        scanFutures.add(CompletableFuture.runAsync(() -> {
            try {
                Thread.currentThread().setName("Binary Scan Thread");
                stepHelper.runToolIfIncluded(DetectTool.BINARY_SCAN, "Binary Scanner", () -> {
                    invokeBinaryScanningWorkflow(DetectTool.BINARY_SCAN, dockerTargetData, projectNameVersion, blackDuckRunData, binaryTargets, scanIdsToWaitFor, codeLocationAccumulator, mustWaitAtBomSummaryLevel, concurrentScanWaiter, projectVersion);
                });
            } catch (OperationException e) {
                throw new RuntimeException(e);
            }
        }, executorService));



        scanFutures.add(CompletableFuture.runAsync(() -> {
            try {
                Thread.currentThread().setName("Container Scan Thread");
                stepHelper.runToolIfIncluded(
                        DetectTool.CONTAINER_SCAN,
                        "Container Scanner",
                        () -> invokeContainerScanningWorkflow(scanIdsToWaitFor, codeLocationAccumulator, blackDuckRunData, projectNameVersion, concurrentScanWaiter, projectVersion)
                );
            } catch (OperationException e) {
                throw new RuntimeException(e);
            }
        }, executorService));



        scanFutures.add(CompletableFuture.runAsync(() -> {
            try {
                Thread.currentThread().setName("Impact Analysis Thread");
                stepHelper.runToolIfIncludedWithCallbacks(
                        DetectTool.IMPACT_ANALYSIS,
                        "Vulnerability Impact Analysis",
                        () -> {
                            runImpactAnalysisOnline(projectNameVersion, projectVersion, codeLocationAccumulator, blackDuckRunData.getBlackDuckServicesFactory());
                            mustWaitAtBomSummaryLevel.set(true);
                        },
                        operationRunner::publishImpactSuccess,
                        operationRunner::publishImpactFailure
                );
            } catch (OperationException e) {
                throw new RuntimeException(e);
            }
        }, executorService));


        scanFutures.add(CompletableFuture.runAsync(() -> {
            try {
                Thread.currentThread().setName("IAC Scan Thread");
                stepHelper.runToolIfIncluded(DetectTool.IAC_SCAN, "IaC Scanner", () -> {
                    IacScanStepRunner iacScanStepRunner = new IacScanStepRunner(operationRunner);
                    IacScanCodeLocationData iacScanCodeLocationData = iacScanStepRunner.runIacScanOnline(detectRunUuid, projectNameVersion, blackDuckRunData);
                    codeLocationAccumulator.addNonWaitableCodeLocation(iacScanCodeLocationData.getCodeLocationNames());
                    mustWaitAtBomSummaryLevel.set(true);
                });
            } catch (OperationException e) {
                throw new RuntimeException(e);
            }
        }, executorService));


        List<Exception> exceptions = new ArrayList<>();

        for (Future<Void> future : scanFutures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException && cause.getCause() instanceof OperationException) {
                    exceptions.add((OperationException) cause.getCause());
                } else {
                    exceptions.add(new OperationException(e));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new OperationException(e);
            }
        }

        if (!exceptions.isEmpty()) {
            throw new AggregateOperationException(exceptions); // Multiple exceptions
        }



        if (operationRunner.createBlackDuckPostOptions().isCorrelatedScanningEnabled()) {
            stepHelper.runAsGroup("Upload Correlated Scan Counts", OperationType.INTERNAL, () -> {
                uploadCorrelatedScanCounts(blackDuckRunData, codeLocationAccumulator, detectRunUuid);
            });
        }
        operationRunner.attemptToGenerateComponentLocationAnalysisIfEnabled();

        stepHelper.runAsGroup("Wait for Results", OperationType.INTERNAL, () -> {
            // Calculate code locations. We do this even if we don't wait as we want to report code location data 
            // in various reports.
            CodeLocationResults codeLocationResults = calculateCodeLocations(codeLocationAccumulator);
            
            if (operationRunner.createBlackDuckPostOptions().shouldWaitForResults()) {                  
                // Waiting at the scan level is more reliable, do that if the BD server is new enough.+++++++++
                concurrentScanWaiter.waitForAllScansToComplete();

                // If the BD server is older, or we can't detect its version, or if we have scans that we are 
                // not yet able to obtain the scanID for, use the original notification based waiting.
                if (!blackDuckRunData.shouldWaitAtScanLevel() || mustWaitAtBomSummaryLevel.get()) {
                    waitForCodeLocations(codeLocationResults.getCodeLocationWaitData(), projectNameVersion, blackDuckRunData);
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

    private void invokePackageManagerScanningWorkflow(NameVersion projectNameVersion, BlackDuckRunData blackDuckRunData, Queue<String> scanIdsToWaitFor, BdioResult bdioResult, CodeLocationAccumulator codeLocationAccumulator, ConcurrentScanWaiter concurrentScanWaiter, ProjectVersionWrapper projectVersion) throws OperationException {
        if (PackageManagerStepRunner.areScassScansPossible(blackDuckRunData.getBlackDuckServerVersion())) {
            PackageManagerStepRunner packageManagerScanStepRunner = new PackageManagerStepRunner(operationRunner);

            CommonScanResult commonScanResult = packageManagerScanStepRunner.invokePackageManagerScanningWorkflow(projectNameVersion, blackDuckRunData, bdioResult);
            String scanId = null;
            if(commonScanResult != null) {
                scanId = commonScanResult.getScanId() == null ? null : commonScanResult.getScanId().toString();
                if(commonScanResult.isPackageManagerScassPossible()) {
                    scanIdsToWaitFor.add(scanId);
                    logger.debug("Waiting for package manager scan id {}", scanId);
                    concurrentScanWaiter.startWaitingForScan(scanId, blackDuckRunData, projectVersion, "Package Manager");
                    codeLocationAccumulator.addNonWaitableCodeLocation(commonScanResult.getCodeLocationName());
                    codeLocationAccumulator.incrementAdditionalCounts(DetectTool.DETECTOR, 1);
                    return;
                }
            }
            invokePreScassPackageManagerWorkflow(blackDuckRunData, bdioResult, scanIdsToWaitFor, codeLocationAccumulator, scanId, concurrentScanWaiter, projectVersion);
        } else {
            String scanId = null;
            invokePreScassPackageManagerWorkflow(blackDuckRunData, bdioResult, scanIdsToWaitFor, codeLocationAccumulator, scanId, concurrentScanWaiter, projectVersion);
        }
    }

    private void invokePreScassPackageManagerWorkflow(BlackDuckRunData blackDuckRunData, BdioResult bdioResult, Queue<String> scanIdsToWaitFor, CodeLocationAccumulator codeLocationAccumulator, String scanId, ConcurrentScanWaiter scanWaiter, ProjectVersionWrapper projectVersion) throws OperationException {
        stepHelper.runAsGroup("Upload Bdio", OperationType.INTERNAL, () -> {
            uploadBdio(blackDuckRunData, bdioResult, scanIdsToWaitFor, codeLocationAccumulator, operationRunner.calculateDetectTimeout(), scanId, scanWaiter, projectVersion);
        });
    }

    private void invokeBinaryScanningWorkflow(
        DetectTool detectTool,
        DockerTargetData dockerTargetData,
        NameVersion projectNameVersion,
        BlackDuckRunData blackDuckRunData,
        Set<String> binaryTargets,
        Queue<String> scanIdsToWaitFor,
        CodeLocationAccumulator codeLocationAccumulator,
        AtomicBoolean mustWaitAtBomSummaryLevel,
        ConcurrentScanWaiter scanWaiter,
        ProjectVersionWrapper projectVersion
    )
        throws IntegrationException, OperationException {
        logger.debug("Invoking intelligent persistent binary scan.");
        
        AbstractBinaryScanStepRunner binaryScanStepRunner = CommonScanStepRunner.areScassScansPossible(blackDuckRunData.getBlackDuckServerVersion()) ?
            new ScassOrBdbaBinaryScanStepRunner(operationRunner) :
            new PreScassBinaryScanStepRunner(operationRunner);

        Optional<UUID> scanId = binaryScanStepRunner.invokeBinaryScanningWorkflow(dockerTargetData, projectNameVersion, 
                blackDuckRunData, binaryTargets);
        
        if (scanId.isPresent()) {
            scanIdsToWaitFor.add(scanId.get().toString());
            logger.debug("Waiting for binary scan id {}", scanId);
            scanWaiter.startWaitingForScan(scanId.get().toString(), blackDuckRunData, projectVersion, "Binary Scan");
        } else {
            Optional<CodeLocationCreationData<BinaryScanBatchOutput>> codeLocations = binaryScanStepRunner.getCodeLocations();
            
            if (codeLocations.isPresent()) {
                codeLocationAccumulator.addWaitableCodeLocations(detectTool, codeLocations.get());
                mustWaitAtBomSummaryLevel.set(true);
            }
        }
    }

    private void invokeContainerScanningWorkflow(
        Queue<String> scanIdsToWaitFor,
        CodeLocationAccumulator codeLocationAccumulator,
        BlackDuckRunData blackDuckRunData,
        NameVersion projectNameVersion,
        ConcurrentScanWaiter scanWaiter,
        ProjectVersionWrapper projectVersion
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
            logger.debug("Waiting for comtainer scan id {}", scanId);
            scanWaiter.startWaitingForScan(uuid.toString(), blackDuckRunData, projectVersion, "Container Scan");
        });
        Set<String> containerScanCodeLocations = new HashSet<>();
        if(containerScanStepRunner.getCodeLocationName() != null) {
            containerScanCodeLocations.add(containerScanStepRunner.getCodeLocationName());
            codeLocationAccumulator.addNonWaitableCodeLocation(containerScanCodeLocations);
        }
    }

    public void uploadBdio(BlackDuckRunData blackDuckRunData, BdioResult bdioResult, Queue<String> scanIdsToWaitFor, CodeLocationAccumulator codeLocationAccumulator, Long timeout, String scassScanId, ConcurrentScanWaiter concurrentScanWaiter, ProjectVersionWrapper projectVersion) throws OperationException {
        BdioUploadResult uploadResult = operationRunner.uploadBdioIntelligentPersistent(blackDuckRunData, bdioResult, timeout, scassScanId);
        Optional<CodeLocationCreationData<UploadBatchOutput>> codeLocationCreationData = uploadResult.getUploadOutput();
        codeLocationCreationData.ifPresent(uploadBatchOutputCodeLocationCreationData -> codeLocationAccumulator.addWaitableCodeLocations(
            DetectTool.DETECTOR,
            uploadBatchOutputCodeLocationCreationData
        ));
        if (uploadResult.getUploadOutput().isPresent()) {
            for (UploadOutput result : uploadResult.getUploadOutput().get().getOutput()) {
                result.getScanId().ifPresent((scanId) ->  {
                    scanIdsToWaitFor.add(scanId);
                    concurrentScanWaiter.startWaitingForScan(scanId, blackDuckRunData, projectVersion, "Package Manager");
                });
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

    public CodeLocationResults calculateCodeLocations(CodeLocationAccumulator codeLocationAccumulator) throws OperationException { //this is waiting....
        logger.info(ReportConstants.RUN_SEPARATOR);

        Set<String> allCodeLocationNames = new HashSet<>(codeLocationAccumulator.getNonWaitableCodeLocations());
        CodeLocationWaitData waitData = operationRunner.calculateCodeLocationWaitData(codeLocationAccumulator.getWaitableCodeLocations());
        allCodeLocationNames.addAll(waitData.getCodeLocationNames());
        
        Set<FormattedCodeLocation> allCodeLocationData = new HashSet<>();
        for (String codeLocationName : allCodeLocationNames) {
            FormattedCodeLocation codeLocation = new FormattedCodeLocation(codeLocationName, null, null);
            allCodeLocationData.add(codeLocation);
        }
        
        operationRunner.publishCodeLocationData(allCodeLocationData);
        return new CodeLocationResults(allCodeLocationNames, waitData);
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
    
    public void waitForCodeLocations(CodeLocationWaitData codeLocationWaitData, NameVersion projectNameVersion, BlackDuckRunData blackDuckRunData)
            throws OperationException {
            logger.info("Checking to see if Detect should wait for bom tool calculations to finish.");
            if (operationRunner.createBlackDuckPostOptions().shouldWaitForResults() && codeLocationWaitData.getExpectedNotificationCount() > 0) {
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
        CodeLocationCreationData<ImpactAnalysisBatchOutput> uploadData = operationRunner.uploadImpactAnalysisFile(
            impactFile,
            projectNameVersion,
            impactAnalysisName,
            blackDuckServicesFactory
        );
        operationRunner.mapImpactAnalysisCodeLocations(impactFile, uploadData, projectVersionWrapper, blackDuckServicesFactory);
        /* TODO: There is currently no mechanism within Black Duck for checking the completion status of an Impact Analysis code location. Waiting should happen here when such a mechanism exists. See HUB-25142. JM - 08/2020 */
        codeLocationAccumulator.addNonWaitableCodeLocation(uploadData.getOutput().getSuccessfulCodeLocationNames());
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
