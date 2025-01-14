package com.blackduck.integration.detect.lifecycle.run.operation;

import static com.blackduck.integration.componentlocator.ComponentLocator.SUPPORTED_DETECTORS;
import static com.blackduck.integration.detect.workflow.componentlocationanalysis.GenerateComponentLocationAnalysisOperation.OPERATION_NAME;
import static com.blackduck.integration.detect.workflow.componentlocationanalysis.GenerateComponentLocationAnalysisOperation.SUPPORTED_DETECTORS_LOG_MSG;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.bdio.graph.ProjectDependencyGraph;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.blackduck.api.generated.discovery.ApiDiscovery;
import com.blackduck.integration.blackduck.api.generated.enumeration.PolicyRuleSeverityType;
import com.blackduck.integration.blackduck.api.generated.view.BomStatusScanView;
import com.blackduck.integration.blackduck.api.generated.view.DeveloperScansScanView;
import com.blackduck.integration.blackduck.api.generated.view.ProjectVersionView;
import com.blackduck.integration.blackduck.bdio2.model.GitInfo;
import com.blackduck.integration.blackduck.bdio2.util.Bdio2ContentExtractor;
import com.blackduck.integration.blackduck.bdio2.util.Bdio2Factory;
import com.blackduck.integration.blackduck.codelocation.CodeLocationCreationData;
import com.blackduck.integration.blackduck.codelocation.CodeLocationCreationService;
import com.blackduck.integration.blackduck.codelocation.CodeLocationWaitResult;
import com.blackduck.integration.blackduck.codelocation.binaryscanner.BinaryScanBatchOutput;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.ScanBatch;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.ScanBatchRunner;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.command.ScanCommandOutput;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.command.ScanCommandRunner;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.command.ScanPathsUtility;
import com.blackduck.integration.blackduck.http.BlackDuckRequestBuilder;
import com.blackduck.integration.blackduck.service.BlackDuckApiClient;
import com.blackduck.integration.blackduck.service.BlackDuckServicesFactory;
import com.blackduck.integration.blackduck.service.model.NotificationTaskRange;
import com.blackduck.integration.blackduck.service.model.ProjectVersionWrapper;
import com.blackduck.integration.blackduck.service.request.BlackDuckResponseRequest;
import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.componentlocator.beans.Component;
import com.blackduck.integration.detect.configuration.DetectConfigurationFactory;
import com.blackduck.integration.detect.configuration.DetectInfo;
import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.configuration.DetectorToolOptions;
import com.blackduck.integration.detect.configuration.connection.ConnectionFactory;
import com.blackduck.integration.detect.configuration.enumeration.BlackduckScanMode;
import com.blackduck.integration.detect.configuration.enumeration.DetectTool;
import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.lifecycle.autonomous.AutonomousManager;
import com.blackduck.integration.detect.lifecycle.run.DetectFontLoaderFactory;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.data.DockerTargetData;
import com.blackduck.integration.detect.lifecycle.run.data.ScanCreationResponse;
import com.blackduck.integration.detect.lifecycle.run.operation.blackduck.BdioUploadResult;
import com.blackduck.integration.detect.lifecycle.run.operation.blackduck.ScassScanInitiationResult;
import com.blackduck.integration.detect.lifecycle.run.singleton.BootSingletons;
import com.blackduck.integration.detect.lifecycle.run.singleton.EventSingletons;
import com.blackduck.integration.detect.lifecycle.run.singleton.UtilitySingletons;
import com.blackduck.integration.detect.lifecycle.run.step.utility.OperationAuditLog;
import com.blackduck.integration.detect.lifecycle.run.step.utility.OperationWrapper;
import com.blackduck.integration.detect.lifecycle.shutdown.ExitCodePublisher;
import com.blackduck.integration.detect.lifecycle.shutdown.ExitCodeRequest;
import com.blackduck.integration.detect.tool.DetectableTool;
import com.blackduck.integration.detect.tool.DetectableToolResult;
import com.blackduck.integration.detect.tool.binaryscanner.BinaryScanFindMultipleTargetsOperation;
import com.blackduck.integration.detect.tool.binaryscanner.BinaryScanOptions;
import com.blackduck.integration.detect.tool.binaryscanner.BinaryUploadOperation;
import com.blackduck.integration.detect.tool.detector.CodeLocationConverter;
import com.blackduck.integration.detect.tool.detector.DetectorEventPublisher;
import com.blackduck.integration.detect.tool.detector.DetectorIssuePublisher;
import com.blackduck.integration.detect.tool.detector.DetectorRuleFactory;
import com.blackduck.integration.detect.tool.detector.DetectorTool;
import com.blackduck.integration.detect.tool.detector.DetectorToolResult;
import com.blackduck.integration.detect.tool.detector.executable.DetectExecutableRunner;
import com.blackduck.integration.detect.tool.detector.extraction.ExtractionEnvironmentProvider;
import com.blackduck.integration.detect.tool.detector.factory.DetectDetectableFactory;
import com.blackduck.integration.detect.tool.iac.CalculateIacScanTargetsOperation;
import com.blackduck.integration.detect.tool.iac.IacScanOperation;
import com.blackduck.integration.detect.tool.iac.IacScanReport;
import com.blackduck.integration.detect.tool.iac.IacScannerInstaller;
import com.blackduck.integration.detect.tool.iac.PublishIacScanReportOperation;
import com.blackduck.integration.detect.tool.iac.UploadIacScanResultsOperation;
import com.blackduck.integration.detect.tool.impactanalysis.GenerateImpactAnalysisOperation;
import com.blackduck.integration.detect.tool.impactanalysis.ImpactAnalysisMapCodeLocationsOperation;
import com.blackduck.integration.detect.tool.impactanalysis.ImpactAnalysisNamingOperation;
import com.blackduck.integration.detect.tool.impactanalysis.ImpactAnalysisUploadOperation;
import com.blackduck.integration.detect.tool.impactanalysis.service.ImpactAnalysisBatchOutput;
import com.blackduck.integration.detect.tool.impactanalysis.service.ImpactAnalysisUploadService;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScanPath;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScannerCodeLocationResult;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScannerReport;
import com.blackduck.integration.detect.tool.signaturescanner.operation.CalculateScanPathsOperation;
import com.blackduck.integration.detect.tool.signaturescanner.operation.CalculateWaitableSignatureScanCodeLocations;
import com.blackduck.integration.detect.tool.signaturescanner.operation.CreateScanBatchOperation;
import com.blackduck.integration.detect.tool.signaturescanner.operation.CreateScanBatchRunnerWithBlackDuck;
import com.blackduck.integration.detect.tool.signaturescanner.operation.CreateScanBatchRunnerWithLocalInstall;
import com.blackduck.integration.detect.tool.signaturescanner.operation.CreateSignatureScanReports;
import com.blackduck.integration.detect.tool.signaturescanner.operation.PublishSignatureScanReports;
import com.blackduck.integration.detect.tool.signaturescanner.operation.SignatureScanOperation;
import com.blackduck.integration.detect.tool.signaturescanner.operation.SignatureScanOuputResult;
import com.blackduck.integration.detect.util.DetectZipUtil;
import com.blackduck.integration.detect.util.bdio.protobuf.DetectProtobufBdioHeaderUtil;
import com.blackduck.integration.detect.util.finder.DetectExcludedDirectoryFilter;
import com.blackduck.integration.detect.workflow.ArtifactResolver;
import com.blackduck.integration.detect.workflow.bdio.AggregateCodeLocation;
import com.blackduck.integration.detect.workflow.bdio.BdioResult;
import com.blackduck.integration.detect.workflow.bdio.CreateAggregateBdio2FileOperation;
import com.blackduck.integration.detect.workflow.bdio.CreateAggregateCodeLocationOperation;
import com.blackduck.integration.detect.workflow.bdio.aggregation.FullAggregateGraphCreator;
import com.blackduck.integration.detect.workflow.blackduck.BlackDuckPostOptions;
import com.blackduck.integration.detect.workflow.blackduck.BomScanWaitOperation;
import com.blackduck.integration.detect.workflow.blackduck.DetectFontLoader;
import com.blackduck.integration.detect.workflow.blackduck.bdio.IntelligentPersistentUploadOperation;
import com.blackduck.integration.detect.workflow.blackduck.codelocation.CodeLocationWaitCalculator;
import com.blackduck.integration.detect.workflow.blackduck.codelocation.CodeLocationWaitData;
import com.blackduck.integration.detect.workflow.blackduck.codelocation.WaitableCodeLocationData;
import com.blackduck.integration.detect.workflow.blackduck.developer.RapidModeConfigFindOperation;
import com.blackduck.integration.detect.workflow.blackduck.developer.RapidModeGenerateJsonOperation;
import com.blackduck.integration.detect.workflow.blackduck.developer.RapidModeLogReportOperation;
import com.blackduck.integration.detect.workflow.blackduck.developer.RapidModeUploadOperation;
import com.blackduck.integration.detect.workflow.blackduck.developer.RapidModeWaitOperation;
import com.blackduck.integration.detect.workflow.blackduck.developer.RapidScanDetectResult;
import com.blackduck.integration.detect.workflow.blackduck.developer.RapidScanOptions;
import com.blackduck.integration.detect.workflow.blackduck.developer.aggregate.RapidScanResultAggregator;
import com.blackduck.integration.detect.workflow.blackduck.developer.aggregate.RapidScanResultSummary;
import com.blackduck.integration.detect.workflow.blackduck.developer.blackduck.DetectRapidScanService;
import com.blackduck.integration.detect.workflow.blackduck.integratedmatching.CorrelatedScanCountUploadService;
import com.blackduck.integration.detect.workflow.blackduck.integratedmatching.model.ScanCountsPayload;
import com.blackduck.integration.detect.workflow.blackduck.policy.PolicyChecker;
import com.blackduck.integration.detect.workflow.blackduck.project.AddTagsToProjectOperation;
import com.blackduck.integration.detect.workflow.blackduck.project.AddUserGroupsToProjectOperation;
import com.blackduck.integration.detect.workflow.blackduck.project.FindCloneByLatestOperation;
import com.blackduck.integration.detect.workflow.blackduck.project.FindCloneByNameOperation;
import com.blackduck.integration.detect.workflow.blackduck.project.FindLicenseUrlOperation;
import com.blackduck.integration.detect.workflow.blackduck.project.FindProjectGroupOperation;
import com.blackduck.integration.detect.workflow.blackduck.project.MapToParentOperation;
import com.blackduck.integration.detect.workflow.blackduck.project.SetApplicationIdOperation;
import com.blackduck.integration.detect.workflow.blackduck.project.SyncProjectOperation;
import com.blackduck.integration.detect.workflow.blackduck.project.UnmapCodeLocationsOperation;
import com.blackduck.integration.detect.workflow.blackduck.project.UpdateCustomFieldsOperation;
import com.blackduck.integration.detect.workflow.blackduck.project.customfields.CustomFieldDocument;
import com.blackduck.integration.detect.workflow.blackduck.project.options.CloneFindResult;
import com.blackduck.integration.detect.workflow.blackduck.project.options.FindCloneOptions;
import com.blackduck.integration.detect.workflow.blackduck.project.options.ParentProjectMapOptions;
import com.blackduck.integration.detect.workflow.blackduck.project.options.ProjectGroupFindResult;
import com.blackduck.integration.detect.workflow.blackduck.project.options.ProjectGroupOptions;
import com.blackduck.integration.detect.workflow.blackduck.project.options.ProjectVersionLicenseFindResult;
import com.blackduck.integration.detect.workflow.blackduck.project.options.ProjectVersionLicenseOptions;
import com.blackduck.integration.detect.workflow.blackduck.report.service.ReportService;
import com.blackduck.integration.detect.workflow.codelocation.CodeLocationEventPublisher;
import com.blackduck.integration.detect.workflow.codelocation.CodeLocationNameManager;
import com.blackduck.integration.detect.workflow.codelocation.DetectCodeLocation;
import com.blackduck.integration.detect.workflow.componentlocationanalysis.BdioToComponentListTransformer;
import com.blackduck.integration.detect.workflow.componentlocationanalysis.GenerateComponentLocationAnalysisOperation;
import com.blackduck.integration.detect.workflow.componentlocationanalysis.ScanResultToComponentListTransformer;
import com.blackduck.integration.detect.workflow.event.Event;
import com.blackduck.integration.detect.workflow.event.EventSystem;
import com.blackduck.integration.detect.workflow.file.DirectoryManager;
import com.blackduck.integration.detect.workflow.phonehome.PhoneHomeManager;
import com.blackduck.integration.detect.workflow.project.DetectToolProjectInfo;
import com.blackduck.integration.detect.workflow.project.ProjectEventPublisher;
import com.blackduck.integration.detect.workflow.project.ProjectNameVersionDecider;
import com.blackduck.integration.detect.workflow.project.ProjectNameVersionOptions;
import com.blackduck.integration.detect.workflow.report.util.ReportConstants;
import com.blackduck.integration.detect.workflow.result.DetectResult;
import com.blackduck.integration.detect.workflow.result.ReportDetectResult;
import com.blackduck.integration.detect.workflow.status.FormattedCodeLocation;
import com.blackduck.integration.detect.workflow.status.OperationSystem;
import com.blackduck.integration.detect.workflow.status.Status;
import com.blackduck.integration.detect.workflow.status.StatusEventPublisher;
import com.blackduck.integration.detect.workflow.status.StatusType;
import com.blackduck.integration.detector.accuracy.detectable.DetectableEvaluator;
import com.blackduck.integration.detector.accuracy.directory.DirectoryEvaluator;
import com.blackduck.integration.detector.accuracy.entrypoint.DetectorRuleEvaluator;
import com.blackduck.integration.detector.accuracy.search.SearchEvaluator;
import com.blackduck.integration.detector.accuracy.search.SearchOptions;
import com.blackduck.integration.detector.base.DetectorType;
import com.blackduck.integration.detector.finder.DirectoryFinder;
import com.blackduck.integration.detector.rule.DetectorRuleSet;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.log.IntLogger;
import com.blackduck.integration.log.Slf4jIntLogger;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.body.FileBodyContent;
import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.sca.upload.rest.status.BinaryUploadStatus;
import com.blackduck.integration.util.IntEnvironmentVariables;
import com.blackduck.integration.util.IntegrationEscapeUtil;
import com.blackduck.integration.util.NameVersion;
import com.blackduck.integration.util.OperatingSystemType;
import com.blackducksoftware.bdio2.Bdio;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class OperationRunner {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DetectDetectableFactory detectDetectableFactory;
    private final DetectFontLoaderFactory detectFontLoaderFactory;

    private final Gson htmlEscapeDisabledGson;
    private final CodeLocationConverter codeLocationConverter;
    private final ExtractionEnvironmentProvider extractionEnvironmentProvider;

    private final StatusEventPublisher statusEventPublisher;
    private final ExitCodePublisher exitCodePublisher;
    private final CodeLocationEventPublisher codeLocationEventPublisher;
    private final DetectorEventPublisher detectorEventPublisher;

    private final CodeLocationNameManager codeLocationNameManager;

    private final DirectoryManager directoryManager;
    private final DetectConfigurationFactory detectConfigurationFactory;
    private final EventSystem eventSystem;
    private final FileFinder fileFinder;
    private final DetectInfo detectInfo;
    private final RapidScanResultAggregator rapidScanResultAggregator;
    private final ProjectEventPublisher projectEventPublisher;
    private final DetectExecutableRunner executableRunner;
    private final OperationAuditLog auditLog;
    private static final int[] LIMITED_FIBONACCI_SEQUENCE = {0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55};
    private static final int MIN_POLLING_INTERVAL_THRESHOLD_IN_SECONDS = 5;
    private static final String DEVELOPER_SCAN_ENDPOINT = ApiDiscovery.DEVELOPER_SCANS_PATH.getPath();
    private static final String DEVELOPER_SCAN_CONTENT_TYPE = "application/vnd.blackducksoftware.scan-evidence-1+protobuf";
    private static final String INTELLIGENT_SCAN_ENDPOINT = ApiDiscovery.INTELLIGENT_PERSISTENCE_SCANS_PATH.getPath();
    private static final String INTELLIGENT_SCAN_CONTENT_TYPE = "application/vnd.blackducksoftware.intelligent-persistence-scan-3+protobuf";
    private static final String INTELLIGENT_SCAN_SCASS_CONTENT_TYPE = "application/vnd.blackducksoftware.intelligent-persistence-scan-4+protobuf-jsonld";
    public static final ImmutableList<Integer> RETRYABLE_AFTER_WAIT_HTTP_EXCEPTIONS = ImmutableList.of(408, 429, 502, 503, 504);
    public static final ImmutableList<Integer> RETRYABLE_WITH_BACKOFF_HTTP_EXCEPTIONS = ImmutableList.of(425, 500);
    private List<File> binaryUserTargets = new ArrayList<>();
    BinaryScanFindMultipleTargetsOperation binaryScanFindMultipleTargetsOperation;

    //Internal: Operation -> Action
    //Leave OperationSystem, but it becomes 'user facing groups of actions or steps'
    public OperationRunner(
        DetectDetectableFactory detectDetectableFactory,
        DetectFontLoaderFactory detectFontLoaderFactory,
        BootSingletons bootSingletons,
        UtilitySingletons utilitySingletons,
        EventSingletons eventSingletons
    ) {
        this.detectDetectableFactory = detectDetectableFactory;
        this.detectFontLoaderFactory = detectFontLoaderFactory;

        statusEventPublisher = eventSingletons.getStatusEventPublisher();
        exitCodePublisher = eventSingletons.getExitCodePublisher();
        codeLocationEventPublisher = eventSingletons.getCodeLocationEventPublisher();
        detectorEventPublisher = eventSingletons.getDetectorEventPublisher();
        projectEventPublisher = eventSingletons.getProjectEventPublisher();

        directoryManager = bootSingletons.getDirectoryManager();
        detectConfigurationFactory = bootSingletons.getDetectConfigurationFactory();
        eventSystem = bootSingletons.getEventSystem();
        fileFinder = bootSingletons.getFileFinder();
        detectInfo = bootSingletons.getDetectInfo();
        executableRunner = utilitySingletons.getExecutableRunner();

        OperationSystem operationSystem = utilitySingletons.getOperationSystem();
        codeLocationNameManager = utilitySingletons.getCodeLocationNameManager();

        //My Managed Dependencies
        this.htmlEscapeDisabledGson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        this.codeLocationConverter = new CodeLocationConverter(utilitySingletons.getExternalIdFactory());
        this.extractionEnvironmentProvider = new ExtractionEnvironmentProvider(directoryManager);
        this.rapidScanResultAggregator = new RapidScanResultAggregator();
        this.auditLog = new OperationAuditLog(utilitySingletons.getOperationWrapper(), operationSystem);
    }

    public CodeLocationNameManager getCodeLocationNameManager() {
        return codeLocationNameManager;
    }

    public final Optional<DetectableTool> checkForDocker() throws OperationException {//TODO: refactor bazel+docker out of detectable
        return auditLog.namedInternal("Check For Docker", () -> {
            DetectableTool detectableTool = new DetectableTool(detectDetectableFactory::createDockerDetectable,
                extractionEnvironmentProvider, codeLocationConverter, "DOCKER", DetectTool.DOCKER,
                statusEventPublisher, exitCodePublisher
            );

            if (detectableTool.initializeAndCheckForApplicable(directoryManager.getSourceDirectory())) {
                return Optional.of(detectableTool);
            } else {
                return Optional.empty();
            }
        });
    }

    public final Optional<DetectableTool> checkForBazel() throws OperationException {//TODO: refactor bazel+docker out of detectable
        return auditLog.namedInternal("Check For Bazel", () -> {
            DetectableTool detectableTool = new DetectableTool(detectDetectableFactory::createBazelDetectable,
                extractionEnvironmentProvider, codeLocationConverter, "BAZEL", DetectTool.BAZEL,
                statusEventPublisher, exitCodePublisher
            );

            if (detectableTool.initializeAndCheckForApplicable(directoryManager.getSourceDirectory())) {
                return Optional.of(detectableTool);
            } else {
                return Optional.empty();
            }
        });
    }

    public DetectableToolResult executeDocker(DetectableTool detectableTool) throws OperationException {//TODO: refactor bazel+docker out of detectable
        return auditLog.namedPublic("Execute Docker", "Docker", detectableTool::extract);
    }

    public DetectableToolResult executeBazel(DetectableTool detectableTool) throws OperationException {//TODO: refactor bazel+docker out of detectable
        return auditLog.namedPublic("Execute Bazel", "Bazel", detectableTool::extract);
    }

    public final DetectorToolResult executeDetectors() throws OperationException {
        return auditLog.namedPublic("Execute Detectors", "Detectors", () -> {
            DetectorToolOptions detectorToolOptions = detectConfigurationFactory.createDetectorToolOptions();
            SearchOptions searchOptions = detectConfigurationFactory.createDetectorSearchOptions();
            DetectorRuleFactory detectorRuleFactory = new DetectorRuleFactory();
            DetectorRuleSet detectRuleSet = detectorRuleFactory.createRules(detectDetectableFactory);
            DetectorRuleEvaluator detectorRuleEvaluator = new DetectorRuleEvaluator(new SearchEvaluator(searchOptions), new DetectableEvaluator());
            DirectoryEvaluator directoryEvaluator = new DirectoryEvaluator(
                detectorRuleEvaluator,
                extractionEnvironmentProvider::createExtractionEnvironment
            );

            DetectorTool detectorTool = new DetectorTool(
                new DirectoryFinder(),
                codeLocationConverter,
                new DetectorIssuePublisher(),
                statusEventPublisher,
                exitCodePublisher,
                detectorEventPublisher,
                directoryEvaluator
            );
            return detectorTool.performDetectors(
                directoryManager.getSourceDirectory(),
                detectRuleSet,
                detectConfigurationFactory.createDetectorFinderOptions(),
                detectorToolOptions.getProjectBomTool(),
                detectorToolOptions.getRequiredDetectors(),
                detectorToolOptions.getRequiredAccuracyTypes(),
                fileFinder
            );
        });
    }

    public final void phoneHome(BlackDuckRunData blackDuckRunData) throws OperationException {
        auditLog.namedPublic("Phone Home", () -> blackDuckRunData.getPhoneHomeManager().ifPresent(PhoneHomeManager::startPhoneHome));
    }

    public DirectoryManager getDirectoryManager() {
        return directoryManager;
    }

    //Rapid
    public UUID initiateStatelessBdbaScan(BlackDuckRunData blackDuckRunData) throws OperationException {
        return auditLog.namedInternal("Initial Stateless BDBA Scan", () -> {
            File bdioHeader = new File(directoryManager.getBdioOutputDirectory() + "/bdio-header.pb");

            if (!bdioHeader.exists()) {
                throw new DetectUserFriendlyException(
                        "Unble to locate BDIO header from BDBA scan.",
                        ExitCodeType.FAILURE_SCAN
                    );
            }

            String uploadHeaderOperationName = "Upload BDIO Header to Initiate Scan";
            return uploadBdioHeaderToInitiateScan(blackDuckRunData, bdioHeader, uploadHeaderOperationName);
        });
    }


    public String getScanServicePostEndpoint() {
        if (detectConfigurationFactory.createScanMode() == BlackduckScanMode.INTELLIGENT) {
            return INTELLIGENT_SCAN_ENDPOINT;
        }
        return DEVELOPER_SCAN_ENDPOINT;
    }

    public String getScanServicePostContentType() {
        if (detectConfigurationFactory.createScanMode() == BlackduckScanMode.INTELLIGENT) {
            return INTELLIGENT_SCAN_CONTENT_TYPE;
        }
        return DEVELOPER_SCAN_CONTENT_TYPE;
    }

    public Optional<String> getContainerScanFilePath() {
        return detectConfigurationFactory.getContainerScanFilePath();
    }

    public Optional<String> getThreatIntelScanFilePath() {
        return detectConfigurationFactory.getThreatIntelScanFilePath();
    }
    
    public File downloadContainerImage(Gson gson, File downloadDirectory, String containerImageUri) throws DetectUserFriendlyException, IntegrationException, IOException {
        String targetPathName = downloadDirectory.toString().concat("/targetImage");
        ConnectionFactory connectionFactory = new ConnectionFactory(detectConfigurationFactory.createConnectionDetails());
        ArtifactResolver artifactResolver = new ArtifactResolver(connectionFactory, gson);
        return artifactResolver.downloadArtifact(new File(targetPathName), containerImageUri);
    }

    public File getContainerScanImage(Gson gson, File downloadDirectory) throws OperationException {
        return auditLog.namedPublic("Retrieve Container Scan Image File", () -> {
            Optional<String> containerImageFilePath = getContainerScanFilePath();
            File containerImageFile = null;
            if (containerImageFilePath.isPresent()) {
                String containerImageUri = containerImageFilePath.get();
                if (containerImageUri.startsWith("http")) {
                    containerImageFile = downloadContainerImage(gson, downloadDirectory, containerImageUri);
                } else {
                    containerImageFile = new File(containerImageUri);
                }
            }
            return containerImageFile;
        });
    }

    public List<File> getMultiBinaryTargets() {
        return binaryScanFindMultipleTargetsOperation.getMultipleBinaryTargets();
    }
    public void updateBinaryUserTargets(File file) {
        binaryUserTargets.add(file);
    }

    public void saveAutonomousScanSettingsFile(AutonomousManager autonomousManager) throws OperationException {
        autonomousManager.updateUserProvidedBinaryScanTargets(binaryUserTargets);
        if (autonomousManager.getAutonomousScanEnabled()) {
            auditLog.namedPublic("Generate Autonomous Scan Settings File", () -> {
                autonomousManager.writeScanSettingsModelToTarget();
            });
        }
    }

    public JsonObject createScanMetadata(UUID scanId, NameVersion projectNameVersion, String type) {
        String scanPersistence = detectConfigurationFactory.createScanMode() == BlackduckScanMode.INTELLIGENT ? "STATEFUL" : "STATELESS";
        String projectGroupName = detectConfigurationFactory.createProjectGroupOptions().getProjectGroup();

        JsonObject imageMetadataObject = new JsonObject();
        imageMetadataObject.addProperty("scanId", scanId.toString());
        imageMetadataObject.addProperty("scanType", type);
        imageMetadataObject.addProperty("scanPersistence", scanPersistence);
        imageMetadataObject.addProperty("projectName", projectNameVersion.getName());
        imageMetadataObject.addProperty("projectVersionName", projectNameVersion.getVersion());
        imageMetadataObject.addProperty("projectGroupName", projectGroupName);

        return imageMetadataObject;
    }
    
    // Generic method to POST a file to /api/storage/containers endpoint of storage service
    public Response uploadFileToStorageService(BlackDuckRunData blackDuckRunData, String storageServiceEndpoint, File payloadFile, String postContentType, String operationName)
        throws OperationException {
        return uploadFileToStorageServiceWithHeaders(blackDuckRunData, storageServiceEndpoint, payloadFile, postContentType, operationName, null);
    }
    
    public Response uploadFileToStorageServiceWithHeaders(BlackDuckRunData blackDuckRunData, String storageServiceEndpoint, File payloadFile, String postContentType, String operationName, Map<String, String> headers) 
            throws OperationException {
        return auditLog.namedPublic(operationName, () -> {
            BlackDuckServicesFactory blackDuckServicesFactory = blackDuckRunData.getBlackDuckServicesFactory();
            BlackDuckApiClient blackDuckApiClient = blackDuckServicesFactory.getBlackDuckApiClient();

            HttpUrl postUrl = blackDuckRunData.getBlackDuckServerConfig().getBlackDuckUrl().appendRelativeUrl(storageServiceEndpoint);
            
            BlackDuckRequestBuilder requestBuilder = new BlackDuckRequestBuilder()
                    .postFile(payloadFile, ContentType.create(postContentType));
            
            if (headers != null) {
                for (String headerName : headers.keySet()) {
                    requestBuilder.addHeader(headerName, headers.get(headerName));
                }
            }
            
            BlackDuckResponseRequest buildBlackDuckResponseRequest = requestBuilder
                    .buildBlackDuckResponseRequest(postUrl);
            
            try (Response response = blackDuckApiClient.execute(buildBlackDuckResponseRequest)) {
                return response;
            } catch (IntegrationException e) {
                logger.trace("Could not execute file upload request to storage service.");
                throw new IntegrationException("Could not execute file upload request to storage service.", e);
            } catch (IOException e) {
                logger.trace("I/O error occurred during file upload request.");
                throw new IOException("I/O error occurred during file upload request to storage service.", e);
            }
        });  
    }

    public Response uploadJsonToStorageService(BlackDuckRunData blackDuckRunData, String storageServiceEndpoint, String jsonPayload, String postContentType, String operationName)
        throws OperationException {

        return auditLog.namedInternal(operationName, () -> {
            BlackDuckServicesFactory blackDuckServicesFactory = blackDuckRunData.getBlackDuckServicesFactory();
            BlackDuckApiClient blackDuckApiClient = blackDuckServicesFactory.getBlackDuckApiClient();

            HttpUrl postUrl = blackDuckRunData.getBlackDuckServerConfig().getBlackDuckUrl().appendRelativeUrl(storageServiceEndpoint);

            BlackDuckResponseRequest buildBlackDuckResponseRequest = new BlackDuckRequestBuilder()
                .postString(jsonPayload, ContentType.create(postContentType))
                .buildBlackDuckResponseRequest(postUrl);

            try (Response response = blackDuckApiClient.execute(buildBlackDuckResponseRequest)) {
                return response;
            } catch (IntegrationException e) {
                logger.trace("Could not execute JSON upload request to storage service.");
                throw new IntegrationException("Could not execute JSON upload request to storage service.", e);
            } catch (IOException e) {
                logger.trace("I/O error occurred during JSON upload request.");
                throw new IOException("I/O error occurred during JSON upload request to storage service.", e);
            }
        });
    }

    public UUID uploadBdioHeaderToInitiateScan(BlackDuckRunData blackDuckRunData, File bdioHeaderFile, String operationName) throws OperationException {
        return auditLog.namedInternal(operationName, () -> {
            BlackDuckServicesFactory blackDuckServicesFactory = blackDuckRunData.getBlackDuckServicesFactory();
            BlackDuckApiClient blackDuckApiClient = blackDuckServicesFactory.getBlackDuckApiClient();

            String scanServicePostEndpoint = getScanServicePostEndpoint();
            HttpUrl postUrl = blackDuckRunData.getBlackDuckServerConfig().getBlackDuckUrl().appendRelativeUrl(scanServicePostEndpoint);

            String scanServicePostContentType = getScanServicePostContentType();
            BlackDuckResponseRequest buildBlackDuckResponseRequest = new BlackDuckRequestBuilder()
                .postFile(bdioHeaderFile, ContentType.create(scanServicePostContentType))
                .buildBlackDuckResponseRequest(postUrl);

            HttpUrl responseUrl = blackDuckApiClient.executePostRequestAndRetrieveURL(buildBlackDuckResponseRequest);
            String path = responseUrl.uri().getPath();

            return UUID.fromString(path.substring(path.lastIndexOf('/') + 1));
        });
    }

    public ScanCreationResponse uploadBdioHeaderToInitiateScassScan(BlackDuckRunData blackDuckRunData, File bdioHeaderFile, String operationName, Gson gson, String computedMd5) throws OperationException {
        return auditLog.namedInternal(operationName, () -> {
            BlackDuckServicesFactory blackDuckServicesFactory = blackDuckRunData.getBlackDuckServicesFactory();
            BlackDuckApiClient blackDuckApiClient = blackDuckServicesFactory.getBlackDuckApiClient();

            String scanServicePostEndpoint = getScanServicePostEndpoint();
            HttpUrl postUrl = blackDuckRunData.getBlackDuckServerConfig().getBlackDuckUrl().appendRelativeUrl(scanServicePostEndpoint);

            String scanServicePostContentType = INTELLIGENT_SCAN_SCASS_CONTENT_TYPE;
            BlackDuckResponseRequest buildBlackDuckResponseRequest = new BlackDuckRequestBuilder()
                .addHeader("X-BASE64-MD5", computedMd5)
                .postFile(bdioHeaderFile, ContentType.create(scanServicePostContentType))
                .buildBlackDuckResponseRequest(postUrl);

            Response response = blackDuckApiClient.execute(buildBlackDuckResponseRequest);
            String contentString = response.getContentString();
            ScanCreationResponse scanCreationResponse = gson.fromJson(contentString, ScanCreationResponse.class);
            
            return scanCreationResponse;
        });
    }
    
    public ScassScanInitiationResult initiateScan(NameVersion projectNameVersion, File scanFile, File outputDirectory, BlackDuckRunData blackDuckRunData, String type, Gson gson, String codeLocationName) throws OperationException, IntegrationException {
        String projectGroupName = calculateProjectGroupOptions().getProjectGroup();

        DetectProtobufBdioHeaderUtil detectProtobufBdioHeaderUtil = new DetectProtobufBdioHeaderUtil(
            UUID.randomUUID().toString(),
            type,
            projectNameVersion,
            projectGroupName,
            codeLocationName,
            scanFile.length());
        
        File bdioHeaderFile;

        ScassScanInitiationResult initResult = new ScassScanInitiationResult();
        try {
            bdioHeaderFile = detectProtobufBdioHeaderUtil.createProtobufBdioHeader(outputDirectory);
            zipFileIfNecessaryAndComputeMD5Base64(scanFile, initResult, outputDirectory);
        } catch (IOException e) {
            throw new IntegrationException("Unable to perform file computations. Ensure the file and output directory are accessible.");
        }
        
        String operationName = "Upload BDIO Header to Initiate Scan";
        
        ScanCreationResponse scanCreationResponse 
            = uploadBdioHeaderToInitiateScassScan(blackDuckRunData, bdioHeaderFile, operationName, gson, initResult.getZipMd5());

        initResult.setScanCreationResponse(scanCreationResponse);

        String scanId = scanCreationResponse.getScanId();
        
        if (scanId == null) {
            logger.warn("Scan ID was not found in the response from the server.");
            throw new IntegrationException("Scan ID was not found in the response from the server.");
        }
        String scanIdString = scanId.toString();
        logger.debug("Scan initiated with scan service. Scan ID received: {}", scanIdString);
        
        return initResult;
    }

    public void uploadBdioEntries(BlackDuckRunData blackDuckRunData, UUID bdScanId) throws IntegrationException, IOException {
        // parse directory and upload all chunks
        File bdioDirectory = directoryManager.getBdioOutputDirectory();

        BlackDuckServicesFactory blackDuckServicesFactory = blackDuckRunData.getBlackDuckServicesFactory();
        BlackDuckApiClient blackDuckApiClient = blackDuckServicesFactory.getBlackDuckApiClient();

        final String contentType = DEVELOPER_SCAN_CONTENT_TYPE;
        HttpUrl putUrl = new HttpUrl(blackDuckRunData.getBlackDuckServerConfig().getBlackDuckUrl().toString()
                + "/api/developer-scans/" + bdScanId);

        int sentChunks = 0;

        for (File bdioEntry : bdioDirectory.listFiles()) {
            if (bdioEntry.getName().equals("bdio-header.pb")) {
                continue;
            }

            // Send the chunks using append mode.

            BlackDuckResponseRequest buildBlackDuckResponseRequest = new BlackDuckRequestBuilder()
                    .addHeader("X-BD-MODE", "APPEND")
                    .putBodyContent(new FileBodyContent(bdioEntry, ContentType.create(contentType)))
                    .buildBlackDuckResponseRequest(putUrl);

            try (Response response = blackDuckApiClient.execute(buildBlackDuckResponseRequest)) {
                if (response.isStatusCodeSuccess()) {
                    logger.debug("Uploaded BDIO entry file: " + bdioEntry.getName());
                } else {
                    logger.trace("Unable to upload BDIO entry. Response code: " + response.getStatusCode() + " " + response.getStatusMessage());
                    throw new IntegrationException("Unable to upload BDIO entry. Response code: " + response.getStatusCode() + " " + response.getStatusMessage());
                }
            }
            sentChunks++;
        }

        if (sentChunks != 0) {
            // We sent something to BD, send a finish message
            BlackDuckResponseRequest buildBlackDuckResponseRequest = new BlackDuckRequestBuilder()
                    .addHeader("X-BD-MODE", "FINISH")
                    .addHeader("X-BD-DOCUMENT-COUNT", String.valueOf(sentChunks))
                    .addHeader("Content-type", contentType)
                    .putString(StringUtils.EMPTY, ContentType.create(contentType, StandardCharsets.UTF_8))
                    .buildBlackDuckResponseRequest(putUrl);

            try (Response response = blackDuckApiClient.execute(buildBlackDuckResponseRequest)) {
                if (response.isStatusCodeSuccess()) {
                    logger.debug("Sent FINISH chunk to Black Duck.");
                } else {
                    logger.trace("Sent FINISH chunk to Black Duck. Response code: " + response.getStatusCode() + " " + response.getStatusMessage());
                    throw new IntegrationException("Sent FINISH chunk to Black Duck. Response code: " + response.getStatusCode() + " " + response.getStatusMessage());
                }
            }
        }
    }

    public final List<HttpUrl> performRapidUpload(BlackDuckRunData blackDuckRunData, BdioResult bdioResult, @Nullable File rapidScanConfig) throws OperationException {
        return auditLog.namedInternal("Rapid Upload", () -> {
            BlackDuckServicesFactory blackDuckServicesFactory = blackDuckRunData.getBlackDuckServicesFactory();
            RapidScanOptions rapidScanOptions = detectConfigurationFactory.createRapidScanOptions();
            RapidModeUploadOperation operation = new RapidModeUploadOperation(DetectRapidScanService.fromBlackDuckServicesFactory(directoryManager, blackDuckServicesFactory));
            return operation.run(
                bdioResult,
                rapidScanOptions,
                rapidScanConfig
            );
        });
    }

    public List<DeveloperScansScanView> waitForRapidResults(BlackDuckRunData blackDuckRunData, List<HttpUrl> rapidScans, BlackduckScanMode mode) throws OperationException {
        return auditLog.namedInternal("Rapid Wait", () -> {
            BlackDuckServicesFactory blackDuckServicesFactory = blackDuckRunData.getBlackDuckServicesFactory();
            int fibonacciSequenceIndex = getFibonacciSequenceIndex();
            return new RapidModeWaitOperation(blackDuckServicesFactory.getBlackDuckApiClient()).waitForScans(
                rapidScans,
                detectConfigurationFactory.findTimeoutInSeconds(),
                RapidModeWaitOperation.DEFAULT_WAIT_INTERVAL_IN_SECONDS,
                mode,
                calculateMaxWaitInSeconds(fibonacciSequenceIndex)
            );
        });
    }

    public final RapidScanResultSummary logRapidReport(List<DeveloperScansScanView> scanResults, BlackduckScanMode mode) throws OperationException {
        return auditLog.namedInternal("Print Rapid Mode Results", () -> new RapidModeLogReportOperation(exitCodePublisher, rapidScanResultAggregator, mode).perform(scanResults));
    }

    public final File generateRapidJsonFile(NameVersion projectNameVersion, List<DeveloperScansScanView> scanResults) throws OperationException {
        return auditLog.namedPublic(
            "Generate Rapid Json File",
            "RapidScan",
            () -> new RapidModeGenerateJsonOperation(htmlEscapeDisabledGson, directoryManager).generateJsonFile(projectNameVersion, scanResults)
        );
    }

    public final void publishRapidResults(File jsonFile, RapidScanResultSummary summary, BlackduckScanMode mode) throws OperationException {
        auditLog.namedInternal("Publish Rapid Results", () -> statusEventPublisher.publishDetectResult(new RapidScanDetectResult(jsonFile.getCanonicalPath(), summary, mode)));
    }
    //End Rapid

    private void failComponentLocationAnalysisOperationTask(String reason) throws OperationException {
        logger.info(ReportConstants.RUN_SEPARATOR);
        logger.info(reason);
        logger.info(ReportConstants.RUN_SEPARATOR);
        auditLog.namedPublic(
                OPERATION_NAME,
                () -> {
                    new GenerateComponentLocationAnalysisOperation(detectConfigurationFactory, statusEventPublisher, exitCodePublisher)
                        .failComponentLocationAnalysisOperation();
                }
        );
    }

    /**
     * Given a BDIO, creates a JSON file called {@value GenerateComponentLocationAnalysisOperation#DETECT_OUTPUT_FILE_NAME} containing
     * every detected component's {@link ExternalId} along with its declaration location when applicable.
     * @param bdio
     * @throws OperationException
     */
    public void generateComponentLocationAnalysisIfEnabled(BdioResult bdio) throws OperationException {
        if (detectConfigurationFactory.isComponentLocationAnalysisEnabled()) {
            if (bdio.getCodeLocationNamesResult().getCodeLocationNames().isEmpty()) {
                failComponentLocationAnalysisOperationTask("Component Location Analysis requires non-empty BDIO results. Skipping location analysis.");
            } else if (!applicableDetectorsIncludeAtLeastOneSupportedDetector(bdio.getApplicableDetectorTypes())) {
                failComponentLocationAnalysisOperationTask(SUPPORTED_DETECTORS_LOG_MSG);
            } else {
                Set<Component> componentsSet = new BdioToComponentListTransformer().transformBdioToComponentSet(bdio);
                if (componentsSet.isEmpty()) {
                    failComponentLocationAnalysisOperationTask("Component Location Analysis requires at least one dependency in BDIO results. Skipping location analysis.");
                } else {
                    auditLog.namedPublic(
                            OPERATION_NAME,
                            () -> {
                                publishResult(
                                    new GenerateComponentLocationAnalysisOperation(detectConfigurationFactory, statusEventPublisher, exitCodePublisher)
                                        .locateComponents(componentsSet, directoryManager.getScanOutputDirectory(), directoryManager.getSourceDirectory())
                                );
                            }
                    );
                }
            }
        }
    }

    /**
     * Given a Rapid/Stateless Detector Scan result, creates a JSON file called {@value GenerateComponentLocationAnalysisOperation#DETECT_OUTPUT_FILE_NAME} containing
     * every reported component's {@link ExternalId} along with its declaration location and upgrade guidance information when applicable.
     * @param rapidResults
     * @param bdio
     * @throws OperationException
     */
    public void generateComponentLocationAnalysisIfEnabled(List<DeveloperScansScanView> rapidResults, BdioResult bdio) throws OperationException {
        if (detectConfigurationFactory.isComponentLocationAnalysisEnabled()) {
            if (rapidResults.isEmpty()) {
                failComponentLocationAnalysisOperationTask("Component Location Analysis requires non-empty Rapid/Stateless Scan results. Skipping location analysis.");
            } else if (!applicableDetectorsIncludeAtLeastOneSupportedDetector(bdio.getApplicableDetectorTypes())) {
                failComponentLocationAnalysisOperationTask(SUPPORTED_DETECTORS_LOG_MSG);
            } else {
                Set<Component> componentsSet = new ScanResultToComponentListTransformer().transformScanResultToComponentList(rapidResults);
                if (componentsSet.isEmpty()) {
                    failComponentLocationAnalysisOperationTask("Component Location Analysis requires at least one dependency in Rapid/Stateless Detector Scan results. Skipping location analysis.");
                } else {
                    auditLog.namedPublic(
                            OPERATION_NAME,
                            () -> {
                                publishResult(
                                    new GenerateComponentLocationAnalysisOperation(detectConfigurationFactory, statusEventPublisher, exitCodePublisher)
                                        .locateComponents(componentsSet, directoryManager.getScanOutputDirectory(), directoryManager.getSourceDirectory())
                                );
                            }
                    );
                }
            }
        }
    }

    private boolean applicableDetectorsIncludeAtLeastOneSupportedDetector(Set<DetectorType> applicableDetectors) {
        if (SUPPORTED_DETECTORS.isEmpty()) {
            // Idler CLL will always give an empty list
            return true;
        }
        Set<String> applicableDetectorsAsStrings = getApplicableDetectorTypesAsStrings(applicableDetectors);
        applicableDetectorsAsStrings.retainAll(SUPPORTED_DETECTORS);
        return !applicableDetectorsAsStrings.isEmpty();
    }

    private Set<String> getApplicableDetectorTypesAsStrings(Set<DetectorType> applicableDetectors) {
        Set<String> applicableDetectorsAsStrings = new HashSet<>();
        for (DetectorType detectorType : applicableDetectors) {
            applicableDetectorsAsStrings.add(detectorType.toString());
        }
        return applicableDetectorsAsStrings;
    }

    /**
     * Since component location analysis is not supported for online Intelligent scans in 8.11, an appropriate console
     * msg is logged and status=FAILURE is recorded in the status.json file
     * @throws OperationException
     */
    public void attemptToGenerateComponentLocationAnalysisIfEnabled() throws OperationException {
        if (detectConfigurationFactory.isComponentLocationAnalysisEnabled()) {
            auditLog.namedPublic(
                    OPERATION_NAME,
                    () -> publishResult(
                        new GenerateComponentLocationAnalysisOperation(detectConfigurationFactory, statusEventPublisher, exitCodePublisher)
                            .locateComponentsForOnlineIntelligentScan()
                    )
            );
        }
    }

    //Post actions
    //End post actions

    public final BdioUploadResult uploadBdioIntelligentPersistent(BlackDuckRunData blackDuckRunData, BdioResult bdioResult, Long timeout) throws OperationException {
        return auditLog.namedPublic(
            "Upload Intelligent Persistent Bdio",
            () -> new IntelligentPersistentUploadOperation(
                blackDuckRunData.getBlackDuckServicesFactory().createIntelligentPersistenceService(),
                timeout
            ).uploadBdioFiles(bdioResult)
        );
    }

    public final CodeLocationWaitData calculateCodeLocationWaitData(List<WaitableCodeLocationData> codeLocationCreationDatas) throws OperationException {
        return auditLog.namedInternal("Calculate Code Location Wait Data", () -> new CodeLocationWaitCalculator().calculateWaitData(codeLocationCreationDatas));
    }

    public final void publishCodeLocationData(Set<FormattedCodeLocation> codeLocationData) throws OperationException {
        auditLog.namedInternal(
            "Publish CodeLocationsCompleted Event",
            () -> codeLocationEventPublisher.publishCodeLocationsCompleted(codeLocationData)
        );
    }

    public final String generateImpactAnalysisCodeLocationName(NameVersion projectNameVersion) throws OperationException {
        return auditLog.namedInternal("Calculate Impact Analysis Code Location Name", () -> {
            ImpactAnalysisNamingOperation impactAnalysisNamingOperation = new ImpactAnalysisNamingOperation(codeLocationNameManager);
            return impactAnalysisNamingOperation.createCodeLocationName(
                directoryManager.getSourceDirectory(),
                projectNameVersion
            );
        });
    }

    public final Path generateImpactAnalysisFile(String codeLocationName) throws OperationException {
        return auditLog.namedPublic("Generate Impact Analysis File", "ImpactAnalysis", () -> {
            GenerateImpactAnalysisOperation generateImpactAnalysisOperation = new GenerateImpactAnalysisOperation();
            return generateImpactAnalysisOperation.generateImpactAnalysis(
                directoryManager.getSourceDirectory(),
                codeLocationName,
                directoryManager.getImpactAnalysisOutputDirectory().toPath()
            );
        });
    }

    public final CodeLocationCreationData<ImpactAnalysisBatchOutput> uploadImpactAnalysisFile(
        Path impactAnalysisFile,
        NameVersion projectNameVersion,
        String codeLocationName,
        BlackDuckServicesFactory blackDuckServicesFactory
    )
        throws OperationException {
        return auditLog.namedPublic("Upload Impact Analysis File", () -> {
            ImpactAnalysisUploadOperation impactAnalysisUploadOperation = new ImpactAnalysisUploadOperation(ImpactAnalysisUploadService.create(blackDuckServicesFactory));
            return impactAnalysisUploadOperation.uploadImpactAnalysis(impactAnalysisFile, projectNameVersion, codeLocationName);
        });
    }

    public final void mapImpactAnalysisCodeLocations(
        Path impactAnalysisFile, CodeLocationCreationData<ImpactAnalysisBatchOutput> impactCodeLocationData, ProjectVersionWrapper projectVersionWrapper,
        BlackDuckServicesFactory blackDuckServicesFactory
    ) throws OperationException {
        auditLog.namedInternal("Map Impact Analysis Code Locations", () -> {
            ImpactAnalysisMapCodeLocationsOperation mapCodeLocationsOperation = new ImpactAnalysisMapCodeLocationsOperation(blackDuckServicesFactory.getBlackDuckApiClient());
            mapCodeLocationsOperation.mapCodeLocations(impactAnalysisFile, impactCodeLocationData, projectVersionWrapper);
        });
    }

    public final NameVersion createProjectDecisionOperation(List<DetectToolProjectInfo> detectToolProjectInfo) throws OperationException {
        return auditLog.namedInternal("Decide Project Name Version", () -> {
            ProjectNameVersionOptions projectNameVersionOptions = detectConfigurationFactory.createProjectNameVersionOptions(directoryManager.getSourceDirectory().getName());
            ProjectNameVersionDecider projectNameVersionDecider = new ProjectNameVersionDecider(projectNameVersionOptions);
            return projectNameVersionDecider.decideProjectNameVersion(detectConfigurationFactory.createPreferredProjectTools(), detectToolProjectInfo);
        });
    }

    public void checkPolicyBySeverity(BlackDuckRunData blackDuckRunData, ProjectVersionView projectVersionView) throws OperationException {
        auditLog.namedPublic("Check for Policy by Severity", "PolicyCheckSeverity", () -> {
            PolicyChecker policyChecker = new PolicyChecker(
                exitCodePublisher,
                blackDuckRunData.getBlackDuckServicesFactory().getBlackDuckApiClient(),
                blackDuckRunData.getBlackDuckServicesFactory().createProjectBomService()
            );
            BlackDuckPostOptions blackDuckPostOptions = detectConfigurationFactory.createBlackDuckPostOptions();
            List<PolicyRuleSeverityType> severitiesToFailPolicyCheck = blackDuckPostOptions.getSeveritiesToFailPolicyCheck();
            policyChecker.checkPolicyBySeverity(severitiesToFailPolicyCheck, projectVersionView);
        });
    }

    public void checkPolicyByName(BlackDuckRunData blackDuckRunData, ProjectVersionView projectVersionView) throws OperationException {
        auditLog.namedPublic("Check for Policy by Name", "PolicyCheckName", () -> {
            PolicyChecker policyChecker = new PolicyChecker(
                exitCodePublisher,
                blackDuckRunData.getBlackDuckServicesFactory().getBlackDuckApiClient(),
                blackDuckRunData.getBlackDuckServicesFactory().createProjectBomService()
            );
            BlackDuckPostOptions blackDuckPostOptions = detectConfigurationFactory.createBlackDuckPostOptions();
            List<String> policyNamesToFailPolicyCheck = blackDuckPostOptions.getPolicyNamesToFailPolicyCheck();
            policyChecker.checkPolicyByName(policyNamesToFailPolicyCheck, projectVersionView);
        });
    }

    public void publishReport(ReportDetectResult report) throws OperationException {
        auditLog.namedInternal(
            "Publish DetectResult Event",
            () -> statusEventPublisher.publishDetectResult(report)
        );
    }

    public File createRiskReportFile(BlackDuckRunData blackDuckRunData, ProjectVersionWrapper projectVersionWrapper, File reportDirectory) throws OperationException {
        return auditLog.namedPublic("Create Risk Report File", "RiskReport", () -> {
            DetectFontLoader detectFontLoader = detectFontLoaderFactory.detectFontLoader();
            ReportService reportService = creatReportService(blackDuckRunData);
            return reportService.createReportPdfFile(
                reportDirectory,
                projectVersionWrapper.getProjectView(),
                projectVersionWrapper.getProjectVersionView(),
                detectFontLoader::loadFont,
                detectFontLoader::loadBoldFont
            );
        });
    }

    public void uploadCorrelatedScanCounts(BlackDuckRunData blackDuckRunData, String correlationId, ScanCountsPayload scanCountsPayload) throws OperationException {
        auditLog.namedPublic("Upload Correlated Scan Counts by Detect tool", "UploadCorrelatedScanCounts", () -> {

            CorrelatedScanCountUploadService correlatedScanCountUploadService = createCorrelatedScanCountUploadService(blackDuckRunData);
            correlatedScanCountUploadService.uploadCorrelatedScanCounts(correlationId, scanCountsPayload);
        });
    }

    public File createNoticesReportFile(BlackDuckRunData blackDuckRunData, ProjectVersionWrapper projectVersion, File noticesDirectory) throws OperationException {
        return auditLog.namedPublic("Create Notices Report File", "NoticesReport", () -> {
            ReportService reportService = creatReportService(blackDuckRunData);
            return reportService.createNoticesReportFile(noticesDirectory, projectVersion.getProjectView(), projectVersion.getProjectVersionView());
        });
    }

    private ReportService creatReportService(BlackDuckRunData blackDuckRunData) throws OperationException {
        return auditLog.namedInternal("Create Report Service", () -> {
            BlackDuckServicesFactory blackDuckServicesFactory = blackDuckRunData.getBlackDuckServicesFactory();
            Gson gson = blackDuckServicesFactory.getGson();
            HttpUrl blackDuckUrl = blackDuckRunData.getBlackDuckServerConfig().getBlackDuckUrl();
            BlackDuckApiClient blackDuckApiClient = blackDuckServicesFactory.getBlackDuckApiClient();
            ApiDiscovery apiDiscovery = blackDuckServicesFactory.getApiDiscovery();
            IntLogger reportServiceLogger = blackDuckServicesFactory.getLogger();
            IntegrationEscapeUtil integrationEscapeUtil = blackDuckServicesFactory.createIntegrationEscapeUtil();
            long reportServiceTimeout = detectConfigurationFactory.findTimeoutInSeconds() * 1000;
            return new ReportService(gson, blackDuckUrl, blackDuckApiClient,
                apiDiscovery, reportServiceLogger, integrationEscapeUtil, reportServiceTimeout
            );
        });
    }

    private CorrelatedScanCountUploadService createCorrelatedScanCountUploadService(BlackDuckRunData blackDuckRunData) throws OperationException {
        return auditLog.namedInternal("Create Correlated Scan Count Upload Service", () -> {
            BlackDuckServicesFactory blackDuckServicesFactory = blackDuckRunData.getBlackDuckServicesFactory();
            Gson gson = blackDuckServicesFactory.getGson();
            BlackDuckApiClient blackDuckApiClient = blackDuckServicesFactory.getBlackDuckApiClient();
            ApiDiscovery apiDiscovery = blackDuckServicesFactory.getApiDiscovery();
            IntLogger countUploadServiceLogger = blackDuckServicesFactory.getLogger();
            return new CorrelatedScanCountUploadService(gson, blackDuckApiClient,
                apiDiscovery, countUploadServiceLogger
            );
        });
    }

    public void publishProjectNameVersionChosen(NameVersion nameVersion) throws OperationException {
        auditLog.namedInternal("Project Name Version Chosen", () -> projectEventPublisher.publishProjectNameVersionChosen(nameVersion));
    }

    public void publishResult(DetectResult detectResult) {
        statusEventPublisher.publishDetectResult(detectResult); //Not in the audit log as it's too broad. Might be good to massage.
    }

    public List<SignatureScanPath> createScanPaths(NameVersion projectNameVersion, DockerTargetData dockerTargetData) throws OperationException {
        return auditLog.namedInternal(
            "Calculate Signature Scan Paths",
            () -> {
                List<String> exclusions = detectConfigurationFactory.collectSignatureScannerDirectoryExclusions();
                DetectExcludedDirectoryFilter detectExcludedDirectoryFilter = new DetectExcludedDirectoryFilter(exclusions);
                return new CalculateScanPathsOperation(detectConfigurationFactory.createBlackDuckSignatureScannerOptions(), directoryManager, fileFinder,
                    detectExcludedDirectoryFilter::isExcluded
                )
                    .determinePathsAndExclusions(projectNameVersion, detectConfigurationFactory.createBlackDuckSignatureScannerOptions().getMaxDepth(), dockerTargetData);
            }
        );
    }

    public ScanBatch createScanBatchOnline(
        String detectRunUuid,
        List<SignatureScanPath> scanPaths,
        NameVersion projectNameVersion,
        DockerTargetData dockerTargetData,
        BlackDuckRunData blackDuckRunData
    )
        throws OperationException {
        return auditLog.namedPublic("Create Online Signature Scan Batch", "OnlineSigScan",
            () -> new CreateScanBatchOperation(detectConfigurationFactory.createBlackDuckSignatureScannerOptions(), directoryManager, codeLocationNameManager)
                .createScanBatchWithBlackDuck(detectRunUuid, projectNameVersion, scanPaths, blackDuckRunData.getBlackDuckServerConfig(), dockerTargetData)
        );
    }

    public ScanBatch createScanBatchOffline(String detectRunUuid, List<SignatureScanPath> scanPaths, NameVersion projectNameVersion, DockerTargetData dockerTargetData)
        throws OperationException {
        return auditLog.namedPublic("Create Offline Signature Scan Batch", "OfflineSigScan",
            () -> new CreateScanBatchOperation(detectConfigurationFactory.createBlackDuckSignatureScannerOptions(), directoryManager, codeLocationNameManager)
                .createScanBatchWithoutBlackDuck(detectRunUuid, projectNameVersion, scanPaths, dockerTargetData)
        );
    }

    public File calculateDetectControlledInstallDirectory() throws OperationException {
        return auditLog.namedInternal("Calculate Scanner Install Directory", (OperationWrapper.OperationSupplier<File>) directoryManager::getPermanentDirectory);
    }

    public Optional<File> calculateOnlineLocalScannerInstallPath() throws OperationException {
        return auditLog.namedInternal(
            "Calculate Online Local Scanner Path",
            () -> detectConfigurationFactory.createBlackDuckSignatureScannerOptions().getLocalScannerInstallPath().map(Path::toFile)
        );
    }

    public Long calculateDetectTimeout() {
        return detectConfigurationFactory.findTimeoutInSeconds();
    }

    public ScanBatchRunner createScanBatchRunnerWithBlackDuck(BlackDuckRunData blackDuckRunData, File installDirectory) throws OperationException {
        return auditLog.namedInternal("Create Scan Batch Runner with Black Duck", () -> {
            ExecutorService executorService = Executors.newFixedThreadPool(detectConfigurationFactory.createBlackDuckSignatureScannerOptions().getParallelProcessors());
            IntEnvironmentVariables intEnvironmentVariables = IntEnvironmentVariables.includeSystemEnv();
            return new CreateScanBatchRunnerWithBlackDuck(intEnvironmentVariables, OperatingSystemType.determineFromSystem(), executorService).createScanBatchRunner(
                blackDuckRunData.getBlackDuckServerConfig(),
                installDirectory,
                blackDuckRunData.getBlackDuckServerVersion()
            );
        });
    }

    public ScanBatchRunner createScanBatchRunnerFromLocalInstall(File installDirectory) throws OperationException {
        return auditLog.namedInternal("Create Scan Batch Runner From Local Install", () -> {
            IntEnvironmentVariables intEnvironmentVariables = IntEnvironmentVariables.includeSystemEnv();
            ScanPathsUtility scanPathsUtility = new ScanPathsUtility(
                new Slf4jIntLogger(LoggerFactory.getLogger(ScanPathsUtility.class)),
                intEnvironmentVariables,
                OperatingSystemType.determineFromSystem()
            );
            ScanCommandRunner scanCommandRunner = new ScanCommandRunner(
                new Slf4jIntLogger(LoggerFactory.getLogger(ScanCommandRunner.class)),
                intEnvironmentVariables,
                scanPathsUtility,
                createExecutorServiceForScanner()
            );
            return new CreateScanBatchRunnerWithLocalInstall(intEnvironmentVariables, scanPathsUtility, scanCommandRunner).createScanBatchRunner(installDirectory);
        });
    }

    public NotificationTaskRange createCodeLocationRange(BlackDuckRunData blackDuckRunData) throws OperationException {
        return auditLog.namedInternal(
            "Create Code Location Task Range",
            () -> blackDuckRunData.getBlackDuckServicesFactory().createCodeLocationCreationService().calculateCodeLocationRange()
        );
    }

    public SignatureScanOuputResult signatureScan(ScanBatch scanBatch, ScanBatchRunner scanBatchRunner) throws OperationException {
        return auditLog.namedPublic("Execute Signature Scan CLI", "SigScan", () -> new SignatureScanOperation().performScanActions(scanBatch, scanBatchRunner));
    }

    public List<SignatureScannerReport> createSignatureScanReport(List<SignatureScanPath> signatureScanPaths, List<ScanCommandOutput> scanCommandOutputList)
        throws OperationException {
        return auditLog.namedInternal("Create Signature Scanner Report", () -> new CreateSignatureScanReports().createReports(signatureScanPaths, scanCommandOutputList));
    }

    public void publishSignatureScanReport(List<SignatureScannerReport> report) throws OperationException {
        auditLog.namedInternal("Publish Signature Scan Report", () -> {
            Boolean treatSkippedAsFailure = detectConfigurationFactory.createBlackDuckSignatureScannerOptions().getTreatSkippedScansAsSuccess();
            new PublishSignatureScanReports(exitCodePublisher, statusEventPublisher, treatSkippedAsFailure).publishReports(report);
        });
    }

    public SignatureScannerCodeLocationResult calculateWaitableSignatureScannerCodeLocations(
        NotificationTaskRange notificationTaskRange,
        List<SignatureScannerReport> reports
    ) throws OperationException {
        return auditLog.namedInternal(
            "Calculate Signature Scanner Waitable Code Locations",
            () -> new CalculateWaitableSignatureScanCodeLocations()
                .calculateWaitableCodeLocations(notificationTaskRange, reports)
        );
    }

    public List<File> calculateIacScanScanTargets() throws OperationException {
        return auditLog.namedInternal(
            "Calculate IacScan Scan Targets",
            () -> new CalculateIacScanTargetsOperation(detectConfigurationFactory.createIacScanOptions(), directoryManager)
                .calculateIacScanTargets()
        );
    }

    public Optional<File> calculateUserProvidedIacScanPath() throws OperationException {
        return auditLog.namedInternal(
            "Calculate Local IacScan Path",
            () -> detectConfigurationFactory.createIacScanOptions().getLocalIacScannerPath().map(Path::toFile)
        );
    }

    public File resolveIacScanOnline(BlackDuckRunData blackDuckRunData) throws OperationException {
        return auditLog.namedInternal("Resolve IacScan Online", () -> new IacScannerInstaller(
            blackDuckRunData.getBlackDuckServerConfig().createBlackDuckHttpClient(new Slf4jIntLogger(logger)),
            detectInfo,
            blackDuckRunData.getBlackDuckServerConfig().getBlackDuckUrl(),
            directoryManager
        ).installOrUpdateScanner());
    }

    public String createIacScanCodeLocationName(File scanTarget, NameVersion projectNameVersion) {
        return codeLocationNameManager.createIacScanCodeLocationName(
            scanTarget,
            projectNameVersion.getName(),
            projectNameVersion.getVersion(),
            detectConfigurationFactory.createIacScanOptions().getCodeLocationPrefix().orElse(null),
            detectConfigurationFactory.createIacScanOptions().getCodeLocationSuffix().orElse(null)
        );
    }

    public File performIacScanScan(File scanTarget, File iacScanExe, int count) throws OperationException {
        return auditLog.namedInternal("Perform IacScan Scan", "IacScan",
            () -> new IacScanOperation(directoryManager, executableRunner).performIacScan(
                scanTarget,
                iacScanExe,
                detectConfigurationFactory.createIacScanOptions().getAdditionalArguments().orElse(null),
                count
            )
        );
    }

    public void uploadIacScanResults(BlackDuckRunData blackDuckRunData, File iacScanResultsFile, String scanId) throws OperationException {
        auditLog.namedInternal(
            "Upload IacScan Results",
            () -> new UploadIacScanResultsOperation(blackDuckRunData.getBlackDuckServicesFactory().createIacScanUploadService())
                .uploadResults(iacScanResultsFile, scanId)
        );
    }

    public void publishIacScanReport(List<IacScanReport> iacScanReports) throws OperationException {
        auditLog.namedInternal(
            "Publish IacScan Report",
            () -> new PublishIacScanReportOperation(exitCodePublisher, statusEventPublisher).publishReports(iacScanReports)
        );
    }

    public Optional<File> calculateNoticesDirectory() throws OperationException { //TODO Should be a decision in boot
        return auditLog.namedInternal("Decide Notices Report Path", () -> {
            BlackDuckPostOptions postOptions = detectConfigurationFactory.createBlackDuckPostOptions();
            if (postOptions.shouldGenerateNoticesReport()) {
                return Optional.of(postOptions.getNoticesReportPath().map(Path::toFile)
                    .orElse(directoryManager.getSourceDirectory()));
            }
            return Optional.empty();
        });
    }

    public Optional<File> calculateRiskReportFileLocation() throws OperationException { //TODO Should be a decision in boot
        return auditLog.namedInternal("Decide Risk Report Path", () -> {
            BlackDuckPostOptions postOptions = detectConfigurationFactory.createBlackDuckPostOptions();
            if (postOptions.shouldGenerateRiskReport()) {
                return Optional.of(postOptions.getRiskReportPdfPath().map(Path::toFile)
                    .orElse(directoryManager.getSourceDirectory()));
            }
            return Optional.empty();
        });
    }

    public void waitForCodeLocations(BlackDuckRunData blackDuckRunData, CodeLocationWaitData codeLocationWaitData, NameVersion projectNameVersion)
        throws OperationException {
        auditLog.namedPublic("Wait for Code Locations", () -> {
            //TODO fix this when NotificationTaskRange doesn't include task start time
            // ekerwin - The start time of the task is the earliest time a code location was created.
            // In order to wait the full timeout, we have to not use that start time and instead use now().
            NotificationTaskRange notificationTaskRange = Optional.ofNullable(codeLocationWaitData.getNotificationRange())
                .map(notificationRange -> new NotificationTaskRange(
                    System.currentTimeMillis(),
                    codeLocationWaitData.getNotificationRange().getStartDate(),
                    codeLocationWaitData.getNotificationRange().getEndDate()
                ))
                .orElseThrow(() -> new DetectUserFriendlyException("Date range for notification range wasn't set.", ExitCodeType.FAILURE_UNKNOWN_ERROR));

            CodeLocationCreationService codeLocationCreationService = blackDuckRunData.getBlackDuckServicesFactory().createCodeLocationCreationService();
            CodeLocationWaitResult result = codeLocationCreationService.waitForCodeLocations(
                notificationTaskRange,
                projectNameVersion,
                codeLocationWaitData.getCodeLocationNames(),
                codeLocationWaitData.getExpectedNotificationCount(),
                detectConfigurationFactory.findTimeoutInSeconds()
            );
            if (result.getStatus() == CodeLocationWaitResult.Status.PARTIAL) {
                throw new DetectUserFriendlyException(
                    result.getErrorMessage().orElse("Timed out waiting for code locations to finish on the Black Duck server."),
                    ExitCodeType.FAILURE_TIMEOUT
                );
            }
        });
    }

    public AggregateCodeLocation createAggregateCodeLocation(ProjectDependencyGraph aggregateDependencyGraph, NameVersion projectNameVersion, GitInfo gitInfo)
        throws OperationException {
        return auditLog.namedInternal("Create Aggregate Code Location", () -> new CreateAggregateCodeLocationOperation(codeLocationNameManager)
            .createAggregateCodeLocation(
                directoryManager.getBdioOutputDirectory(),
                aggregateDependencyGraph,
                projectNameVersion,
                gitInfo,
                detectConfigurationFactory.createBdioOptions().getBdioFileName().orElse(null)
            ));
    }

    public ProjectDependencyGraph aggregateSubProject(NameVersion projectNameVersion, List<DetectCodeLocation> detectCodeLocations) throws OperationException {
        return auditLog.namedPublic("SubProject Aggregate", "SubProjectAggregate",
            () -> (new FullAggregateGraphCreator()).aggregateCodeLocations(
                directoryManager.getSourceDirectory(),
                projectNameVersion,
                detectCodeLocations
            )
        );
    }

    public void createAggregateBdio2File(String integratedMatchingCorrelationid, AggregateCodeLocation aggregateCodeLocation, Bdio.ScanType scanType) throws OperationException {
        auditLog.namedInternal(
            "Create Bdio Code Locations",
            () -> new CreateAggregateBdio2FileOperation(new Bdio2Factory(), detectInfo).writeAggregateBdio2File(integratedMatchingCorrelationid, aggregateCodeLocation, scanType)
        );
    }

    private ExecutorService createExecutorServiceForScanner() {
        return Executors.newFixedThreadPool(detectConfigurationFactory.createBlackDuckSignatureScannerOptions().getParallelProcessors());
    }

    public BlackDuckPostOptions createBlackDuckPostOptions() {
        return detectConfigurationFactory.createBlackDuckPostOptions();
    }

    public BinaryScanOptions calculateBinaryScanOptions() {
        return detectConfigurationFactory.createBinaryScanOptions();
    }

    public Optional<File> searchForBinaryTargets(Predicate<File> fileFilter, int searchDepth, boolean followSymLinks) throws OperationException {
        binaryScanFindMultipleTargetsOperation = new BinaryScanFindMultipleTargetsOperation(fileFinder, directoryManager);
        return auditLog.namedInternal(
            "Binary Search For Targets",
            () -> binaryScanFindMultipleTargetsOperation
                .searchForMultipleTargets(fileFilter, followSymLinks, searchDepth)
        );
    }
    
    public Optional<File> collectBinaryTargets(Set<String> targets) throws OperationException {
        return auditLog.namedInternal(
            "Binary Collection of Targets",
            () -> new BinaryScanFindMultipleTargetsOperation(directoryManager)
                .collectAutonomousTargets(targets)
        );
    }

    public void publishBinaryFailure(String message) {
        logger.error("Binary scan failure: {}", message);
        statusEventPublisher.publishStatusSummary(Status.forTool(DetectTool.BINARY_SCAN, StatusType.FAILURE));
        exitCodePublisher.publishExitCode(ExitCodeType.FAILURE_BLACKDUCK_FEATURE_ERROR, "BINARY_SCAN");
    }
    
    public void publishContainerTimeout(Exception e) {
        logger.error("Container scan timeout: {}", e.getMessage());
        statusEventPublisher.publishStatusSummary(Status.forTool(DetectTool.CONTAINER_SCAN, StatusType.FAILURE));
        exitCodePublisher.publishExitCode(ExitCodeType.FAILURE_TIMEOUT, "CONTAINER_SCAN");
    }

    public void publishContainerFailure(Exception e) {
        logger.error("Container scan failure: {}", e.getMessage());
        statusEventPublisher.publishStatusSummary(Status.forTool(DetectTool.CONTAINER_SCAN, StatusType.FAILURE));
        exitCodePublisher.publishExitCode(ExitCodeType.FAILURE_BLACKDUCK_FEATURE_ERROR, "CONTAINER_SCAN");
    }
    
    public void publishThreatIntelFailure(Exception e) {
        logger.error("Threat Intel scan failure: {}", e.getMessage());
        statusEventPublisher.publishStatusSummary(Status.forTool(DetectTool.THREAT_INTEL, StatusType.FAILURE));
        exitCodePublisher.publishExitCode(ExitCodeType.FAILURE_BLACKDUCK_FEATURE_ERROR, "THREAT_INTEL");
    }

    public void publishContainerSuccess() {
        statusEventPublisher.publishStatusSummary(Status.forTool(DetectTool.CONTAINER_SCAN, StatusType.SUCCESS));
    }
    
    public void publishThreatIntelSuccess() {
        statusEventPublisher.publishStatusSummary(Status.forTool(DetectTool.THREAT_INTEL, StatusType.SUCCESS));
    }

    public void publishImpactFailure(Exception e) {
        logger.error("Impact analysis failure: {}", e.getMessage());
        statusEventPublisher.publishStatusSummary(Status.forTool(DetectTool.IMPACT_ANALYSIS, StatusType.FAILURE));
        exitCodePublisher.publishExitCode(ExitCodeType.FAILURE_BLACKDUCK_FEATURE_ERROR, "IMPACT_ANALYSIS");
    }

    public void publishImpactSuccess() {
        statusEventPublisher.publishStatusSummary(Status.forTool(DetectTool.IMPACT_ANALYSIS, StatusType.SUCCESS));
    }
    
    public CodeLocationCreationData<BinaryScanBatchOutput> uploadLegacyBinaryScanFile(File binaryUpload, NameVersion projectNameVersion, BlackDuckRunData blackDuckRunData)
        throws OperationException {
        return auditLog.namedPublic("Binary Upload", "Binary",
            () -> new BinaryUploadOperation(statusEventPublisher)
                .uploadLegacyBinaryScanFile(binaryUpload, blackDuckRunData.getBlackDuckServicesFactory().createBinaryScanUploadService(), codeLocationNameManager, projectNameVersion)
        );
    }

    public BinaryUploadStatus uploadBinaryScanFile(File binaryUpload, NameVersion projectNameVersion, BlackDuckRunData blackDuckRunData)
        throws OperationException {        
        return auditLog.namedPublic("Binary Upload", "Binary",
            () -> {                
                return new BinaryUploadOperation(statusEventPublisher)
                        .uploadBinaryScanFile(binaryUpload, projectNameVersion, codeLocationNameManager, blackDuckRunData);
            });
    }

    public ProjectVersionWrapper syncProjectVersion(
        NameVersion projectNameVersion,
        ProjectGroupFindResult projectGroupFindResult,
        CloneFindResult cloneFindResult,
        ProjectVersionLicenseFindResult projectVersionLicensesFindResult,
        BlackDuckRunData blackDuckRunData
    ) throws OperationException {
        return auditLog.namedInternal(
            "Sync Project",
            () -> new SyncProjectOperation(blackDuckRunData.getBlackDuckServicesFactory().createProjectService())
                .sync(
                    projectNameVersion,
                    projectGroupFindResult,
                    cloneFindResult,
                    projectVersionLicensesFindResult,
                    detectConfigurationFactory.createDetectProjectServiceOptions()
                )
        );
    }

    public ParentProjectMapOptions calculateParentProjectMapOptions() {
        return detectConfigurationFactory.createParentProjectMapOptions();
    }

    public void mapToParentProject(
        String parentProjectName,
        String parentProjectVersionName,
        ProjectVersionWrapper projectVersion,
        BlackDuckRunData blackDuckRunData
    ) throws OperationException {
        auditLog.namedInternal("Map to Parent Project", () -> new MapToParentOperation(
            blackDuckRunData.getBlackDuckServicesFactory().getBlackDuckApiClient(),
            blackDuckRunData.getBlackDuckServicesFactory().createProjectService(),
            blackDuckRunData.getBlackDuckServicesFactory().createProjectBomService()
        ).mapToParentProjectVersion(parentProjectName, parentProjectVersionName, projectVersion));
    }

    public String calculateApplicationId() {
        return detectConfigurationFactory.createApplicationId();
    }

    public void setApplicationId(String applicationId, ProjectVersionWrapper projectVersion, BlackDuckRunData blackDuckRunData) throws OperationException {
        auditLog.namedInternal(
            "Sync Project",
            () -> new SetApplicationIdOperation(blackDuckRunData.getBlackDuckServicesFactory().createProjectMappingService())
                .setApplicationId(projectVersion.getProjectView(), applicationId)
        );
    }

    public CustomFieldDocument calculateCustomFields() throws DetectUserFriendlyException {
        return detectConfigurationFactory.createCustomFieldDocument();
    }

    public void updateCustomFields(CustomFieldDocument customFieldDocument, ProjectVersionWrapper projectVersion, BlackDuckRunData blackDuckRunData) throws OperationException {
        auditLog.namedInternal(
            "Update Custom Fields",
            () -> new UpdateCustomFieldsOperation(blackDuckRunData.getBlackDuckServicesFactory().getBlackDuckApiClient())
                .updateCustomFields(projectVersion, customFieldDocument)
        );
    }

    public List<String> calculateUserGroups() {
        return detectConfigurationFactory.createGroups();
    }

    public List<String> calculateTags() {
        return detectConfigurationFactory.createTags();
    }

    public void addUserGroups(List<String> userGroups, ProjectVersionWrapper projectVersion, BlackDuckRunData blackDuckRunData) throws OperationException {
        auditLog.namedInternal(
            "Add User Groups",
            () -> new AddUserGroupsToProjectOperation(blackDuckRunData.getBlackDuckServicesFactory().createProjectUsersService())
                .addUserGroupsToProject(projectVersion, userGroups)
        );
    }

    public void addTags(List<String> tags, ProjectVersionWrapper projectVersion, BlackDuckRunData blackDuckRunData) throws OperationException {
        auditLog.namedInternal(
            "Add Tags",
            () -> new AddTagsToProjectOperation(blackDuckRunData.getBlackDuckServicesFactory().createTagService())
                .addTagsToProject(projectVersion, tags)
        );
    }

    public boolean calculateShouldUnmap() {
        return detectConfigurationFactory.createShouldUnmapCodeLocations();
    }

    public void unmapCodeLocations(ProjectVersionWrapper projectVersion, BlackDuckRunData blackDuckRunData) throws OperationException {
        auditLog.namedInternal("Unmap Code Locations", () -> new UnmapCodeLocationsOperation(
            blackDuckRunData.getBlackDuckServicesFactory().getBlackDuckApiClient(),
            blackDuckRunData.getBlackDuckServicesFactory().createCodeLocationService()
        ).unmapCodeLocations(projectVersion.getProjectVersionView()));
    }

    public FindCloneOptions calculateCloneOptions() {
        return detectConfigurationFactory.createCloneFindOptions();
    }

    public ProjectGroupOptions calculateProjectGroupOptions() {
        return detectConfigurationFactory.createProjectGroupOptions();
    }

    public ProjectVersionLicenseOptions calculateProjectVersionLicenses() {
        return detectConfigurationFactory.createProjectVersionLicenseOptions();
    }

    public CloneFindResult findLatestProjectVersionCloneUrl(BlackDuckRunData blackDuckRunData, String projectName) throws OperationException {
        return auditLog.namedInternal("Find Clone Url By Latest", () -> new FindCloneByLatestOperation(
            blackDuckRunData.getBlackDuckServicesFactory().createProjectService(),
            blackDuckRunData.getBlackDuckServicesFactory().getBlackDuckApiClient()
        ).findLatestProjectVersionCloneUrl(projectName));
    }

    public CloneFindResult findNamedCloneUrl(BlackDuckRunData blackDuckRunData, String projectName, String cloneVersionName) throws OperationException {
        return auditLog.namedInternal(
            "Find Clone Url By Name",
            () -> new FindCloneByNameOperation(blackDuckRunData.getBlackDuckServicesFactory().createProjectService())
                .findNamedCloneUrl(projectName, cloneVersionName)
        );
    }

    public HttpUrl findProjectGroup(BlackDuckRunData blackDuckRunData, String projectGroupName) throws OperationException {
        return auditLog.namedInternal(
            "Find Project Group By Exact Name",
            () -> new FindProjectGroupOperation(
                blackDuckRunData.getBlackDuckServicesFactory().getBlackDuckApiClient(),
                blackDuckRunData.getBlackDuckServicesFactory().getApiDiscovery()
            ).findProjectGroup(projectGroupName)
        );
    }

    public String findLicenseUrl(BlackDuckRunData blackDuckRunData, String licenseName) throws OperationException {
        return auditLog.namedInternal(
            "Find License Urls By Name",
            "LicenseUrlLookup",
            () -> new FindLicenseUrlOperation(blackDuckRunData.getBlackDuckServicesFactory().createLicenseService())
                .findLicenseUrl(licenseName)
        );
    }

    public void publishDetectorFailure() {
        eventSystem.publishEvent(Event.ExitCode, new ExitCodeRequest(ExitCodeType.FAILURE_DETECTOR, "A detector failed."));
    }

    public Optional<File> findRapidScanConfig() throws OperationException {
        return auditLog.namedInternal(
            "Find Rapid Scan Config",
            () -> new RapidModeConfigFindOperation(fileFinder)
                .findRapidScanConfig(directoryManager.getSourceDirectory())
        );
    }

    private File findScanCliOutputLogFile() {
        File blackDuckScanOutputDirectory = this.fileFinder.findFile(directoryManager.getScanOutputDirectory(), "BlackDuckScanOutput");
        if (blackDuckScanOutputDirectory != null) {
            File scanIdDirectory = this.fileFinder.findFile(blackDuckScanOutputDirectory, "*");
            if (scanIdDirectory != null) {
                File scanLogDirectory = this.fileFinder.findFile(scanIdDirectory, "log");
                if (scanLogDirectory != null) {
                    return this.fileFinder.findFile(scanLogDirectory, "*.log");
                }
            }
        }
        return null;
    }

    private int countSignatureScannerBdioChunks() {
        File scanCliOutputLogFile = findScanCliOutputLogFile();
        if (scanCliOutputLogFile == null) {
            return 0;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(scanCliOutputLogFile))) {
            Pattern pattern = Pattern.compile("scanNodeList\\.size\\(\\)=(\\d+).*scanLeafList\\.size\\(\\)=(\\d+)");
            long scanNodeCount = 0;
            long scanLeafCount = 0;

            String line = reader.readLine();
            while (line != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find() && matcher.groupCount() == 2) {
                    scanNodeCount = Integer.parseInt(matcher.group(1));
                    scanLeafCount = Integer.parseInt(matcher.group(2));
                }
                line = reader.readLine();
            }
            long sumOfScanNodesAndLeaves = scanNodeCount + scanLeafCount;
            int bdioChunksCount = (int) Math.ceil(sumOfScanNodesAndLeaves / 30000D);
            return bdioChunksCount;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return 0;
        }
    }

    private int countDetectBdioEntryFiles() {
        try {
            File bdioFile = this.fileFinder.findFile(directoryManager.getBdioOutputDirectory(), "*.bdio");
            if (bdioFile == null) {
                return 0;
            }
            Bdio2ContentExtractor bdio2Extractor = new Bdio2ContentExtractor();
            return bdio2Extractor.extractContent(bdioFile).size() - 1;
        } catch (IntegrationException e) {
            return 0;
        }
    }

    public static int calculateMaxWaitInSeconds(int fibonacciSequenceIndex) {
        int fibonacciSequenceLastIndex = LIMITED_FIBONACCI_SEQUENCE.length - 1;
        if (fibonacciSequenceIndex > fibonacciSequenceLastIndex) {
            return LIMITED_FIBONACCI_SEQUENCE[fibonacciSequenceLastIndex];
        } else if (fibonacciSequenceIndex > 4) {
            return LIMITED_FIBONACCI_SEQUENCE[fibonacciSequenceIndex];
        }
        return MIN_POLLING_INTERVAL_THRESHOLD_IN_SECONDS;
    }

    public int getFibonacciSequenceIndex() {
        int bdioChunksCount = countSignatureScannerBdioChunks();
        return bdioChunksCount != 0 ? bdioChunksCount : countDetectBdioEntryFiles();
    }

    public BomStatusScanView waitForBomCompletion(BlackDuckRunData blackDuckRunData, HttpUrl scanUrl) throws OperationException {
        return auditLog.namedInternal("Wait for scan to potentially be included in BOM", () -> {
            BlackDuckServicesFactory blackDuckServicesFactory = blackDuckRunData.getBlackDuckServicesFactory();
            int fibonacciSequenceIndex = getFibonacciSequenceIndex();
            return new BomScanWaitOperation(blackDuckServicesFactory.getBlackDuckApiClient()).waitForScan(
                scanUrl,
                detectConfigurationFactory.findTimeoutInSeconds(),
                calculateMaxWaitInSeconds(fibonacciSequenceIndex)
            );
        });
    }

    public UUID getScanIdFromScanUrl(HttpUrl blackDuckScanUrl) {
        String url = blackDuckScanUrl.toString();
        UUID scanId = UUID.fromString(url.substring(url.lastIndexOf("/") + 1));

        return scanId;
    }
    
    public DetectConfigurationFactory getDetectConfigurationFactory() {
        return this.detectConfigurationFactory;
    }
    
    private void zipFileIfNecessaryAndComputeMD5Base64(File file, ScassScanInitiationResult initResult, File outputDirectory) throws IOException {
        MessageDigest md = DigestUtils.getMd5Digest();
        Encoder encoder = Base64.getEncoder();

        if (DetectZipUtil.isZipFile(file)) {
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    md.update(buffer, 0, bytesRead);
                }
                byte[] md5Bytes = md.digest();
                initResult.setZipFile(file);
                initResult.setZipMd5(encoder.encodeToString(md5Bytes));
            }
        } else {
            Map<String, Path> entries = new HashMap<>();
            entries.put(file.getName(), file.toPath());

            File zipFile = new File(outputDirectory, "scass-upload.zip");

            initResult.setZipFile(zipFile);

            try (FileOutputStream fos = new FileOutputStream(zipFile);
                    DigestOutputStream dos = new DigestOutputStream(fos, md)) {
                DetectZipUtil.zip(dos, entries);
                initResult.setZipMd5(encoder.encodeToString(md.digest()));
            }
        }
    }
}
