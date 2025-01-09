package com.blackduck.integration.detect.lifecycle.run;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Map;
import java.util.HashSet;
import java.util.stream.Collectors;

import com.blackduck.integration.blackduck.exception.BlackDuckApiException;
import com.blackduck.integration.configuration.property.types.enumallnone.list.AllEnumList;
import com.blackduck.integration.detect.configuration.DetectConfigurationFactory;
import com.blackduck.integration.detect.configuration.DetectProperties;
import com.blackduck.integration.detect.configuration.enumeration.DetectTool;
import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.lifecycle.autonomous.AutonomousManager;
import com.blackduck.integration.detect.workflow.phonehome.PhoneHomeManager;
import com.blackduck.integration.detector.base.DetectorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.configuration.enumeration.DetectTargetType;
import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.data.ProductRunData;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.detect.lifecycle.run.singleton.BootSingletons;
import com.blackduck.integration.detect.lifecycle.run.singleton.EventSingletons;
import com.blackduck.integration.detect.lifecycle.run.singleton.SingletonFactory;
import com.blackduck.integration.detect.lifecycle.run.singleton.UtilitySingletons;
import com.blackduck.integration.detect.lifecycle.run.step.IntelligentModeStepRunner;
import com.blackduck.integration.detect.lifecycle.run.step.RapidModeStepRunner;
import com.blackduck.integration.detect.lifecycle.run.step.UniversalStepRunner;
import com.blackduck.integration.detect.lifecycle.run.step.utility.StepHelper;
import com.blackduck.integration.detect.lifecycle.shutdown.ExceptionUtility;
import com.blackduck.integration.detect.lifecycle.shutdown.ExitCodeManager;
import com.blackduck.integration.detect.tool.UniversalToolsResult;
import com.blackduck.integration.detect.tool.detector.factory.DetectorFactory;
import com.blackduck.integration.detect.workflow.bdio.BdioResult;
import com.blackduck.integration.detect.workflow.blackduck.integratedmatching.ScanCountsPayloadCreator;
import com.blackduck.integration.detect.workflow.report.util.ReportConstants;
import com.blackduck.integration.detect.workflow.status.OperationSystem;
import com.blackduck.integration.util.NameVersion;

public class DetectRun {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ExitCodeManager exitCodeManager;
    private final ExceptionUtility exceptionUtility;

    public DetectRun(ExitCodeManager exitCodeManager, ExceptionUtility exceptionUtility) {
        this.exitCodeManager = exitCodeManager;
        this.exceptionUtility = exceptionUtility;
    }

    private OperationRunner createOperationFactory(BootSingletons bootSingletons, UtilitySingletons utilitySingletons, EventSingletons eventSingletons)
        throws DetectUserFriendlyException {
        DetectorFactory detectorFactory = new DetectorFactory(bootSingletons, utilitySingletons);
        DetectFontLoaderFactory detectFontLoaderFactory = new DetectFontLoaderFactory(bootSingletons, utilitySingletons);
        return new OperationRunner(detectorFactory.detectDetectableFactory(), detectFontLoaderFactory, bootSingletons, utilitySingletons, eventSingletons);
    }

    public void run(BootSingletons bootSingletons) {
        Optional<OperationSystem> operationSystem = Optional.empty();
        try {
            SingletonFactory singletonFactory = new SingletonFactory(bootSingletons);
            EventSingletons eventSingletons = singletonFactory.createEventSingletons();
            UtilitySingletons utilitySingletons = singletonFactory.createUtilitySingletons(eventSingletons);
            operationSystem = Optional.of(utilitySingletons.getOperationSystem());

            ProductRunData productRunData = bootSingletons.getProductRunData(); //TODO: Remove run data from boot singletons

            Map<DetectTool, Set<String>> scanTypeEvidenceMap = productRunData.getDetectToolFilter().getExcludedIncludedFilter().getScanTypeEvidenceMap();

            OperationRunner operationRunner = createOperationFactory(bootSingletons, utilitySingletons, eventSingletons);
            StepHelper stepHelper = new StepHelper(utilitySingletons.getOperationSystem(), utilitySingletons.getOperationWrapper(), productRunData.getDetectToolFilter());
            AutonomousManager autonomousManager = bootSingletons.getAutonomousManager();

            UniversalStepRunner stepRunner = new UniversalStepRunner(operationRunner, stepHelper); //Product independent tools
            UniversalToolsResult universalToolsResult = stepRunner.runUniversalTools();

            // test to ensure image scan results are available (i.e. image scan success) throws an
            // exception if results components are empty and/or not found.  This will never throw an
            // exception for any other scan type
            imageScanCanScanFurther(universalToolsResult, bootSingletons);

            // combine: processProjectInformation() -> ProjectResult (nameversion, bdio)
            NameVersion nameVersion = stepRunner.determineProjectInformation(universalToolsResult);
            operationRunner.publishProjectNameVersionChosen(nameVersion);
            BdioResult bdio;
            Boolean forceBdio = bootSingletons.getDetectConfigurationFactory().forceBdio();
            logger.debug("Integrated Matching Correlation ID: {}", bootSingletons.getDetectRunId().getCorrelationId());
            String correlationId = getCorrelationId(operationRunner.getDetectConfigurationFactory(), bootSingletons);
            if (!universalToolsResult.getDetectCodeLocations().isEmpty()
                    || (productRunData.shouldUseBlackDuckProduct() && !productRunData.getBlackDuckRunData().isOnline() && forceBdio && !universalToolsResult.didAnyFail() && exitCodeManager.getWinningExitCode().isSuccess())) {
                bdio = stepRunner.generateBdio(correlationId, universalToolsResult, nameVersion);
            } else {
                bdio = BdioResult.none();
            }
            
            final Set<String> binaryTargets;
            if(autonomousManager.getAutonomousScanEnabled()) {
                SortedMap<String, SortedSet<String>> packageManagerTargets = stepRunner.getScanTargets(universalToolsResult);
                autonomousManager.updateScanTargets(packageManagerTargets, scanTypeEvidenceMap);
                binaryTargets = scanTypeEvidenceMap.get(DetectTool.BINARY_SCAN);
                SortedMap<String, String> defaultValueMap = DetectProperties.getDefaultValues();
                List<String> allPropertyKeys = DetectProperties.allProperties().getPropertyKeys();
                Set<String> decidedScanTypes = getDecidedTools(bootSingletons, scanTypeEvidenceMap);
                autonomousManager.updateScanSettingsProperties(defaultValueMap, decidedScanTypes, packageManagerTargets.keySet(), allPropertyKeys);
            } else {
                binaryTargets = Collections.EMPTY_SET;
            }

            if (productRunData.shouldUseBlackDuckProduct()) {
                BlackDuckRunData blackDuckRunData = productRunData.getBlackDuckRunData();

                blackDuckRunData.getPhoneHomeManager().ifPresent(
                    phoneHomeManager -> phoneHomeApplicableDetectorTypes(
                        phoneHomeManager, universalToolsResult.getApplicableDetectorTypes()
                    )
                );

                if (blackDuckRunData.isNonPersistent() && blackDuckRunData.isOnline()) {
                    RapidModeStepRunner rapidModeSteps = new RapidModeStepRunner(operationRunner, stepHelper, bootSingletons.getGson(), correlationId, bootSingletons.getDirectoryManager());
                    
                    Optional<String> scaaasFilePath = bootSingletons.getDetectConfigurationFactory().getScaaasFilePath();
                    rapidModeSteps.runOnline(blackDuckRunData, nameVersion, bdio, universalToolsResult.getDockerTargetData(), scaaasFilePath);
                } else if (blackDuckRunData.isNonPersistent()) {
                    logger.info("Rapid Scan is offline, nothing to do.");
                } else if (blackDuckRunData.isOnline()) {
                    IntelligentModeStepRunner intelligentModeSteps = new IntelligentModeStepRunner(
                            operationRunner, 
                            stepHelper, 
                            bootSingletons.getGson(), 
                            new ScanCountsPayloadCreator(),
                            correlationId);
                    intelligentModeSteps.runOnline(blackDuckRunData, bdio, nameVersion, productRunData.getDetectToolFilter(), universalToolsResult.getDockerTargetData(), binaryTargets);
                } else {
                    IntelligentModeStepRunner intelligentModeSteps = new IntelligentModeStepRunner(
                            operationRunner, 
                            stepHelper, 
                            bootSingletons.getGson(), 
                            new ScanCountsPayloadCreator(), 
                            correlationId);
                    intelligentModeSteps.runOffline(nameVersion, universalToolsResult.getDockerTargetData(), bdio);
                }
            }
            if(autonomousManager.getAutonomousScanEnabled()) {
                operationRunner.saveAutonomousScanSettingsFile(autonomousManager);
            }
        } catch (Exception e) {
            logger.error(ReportConstants.RUN_SEPARATOR);
            logger.error("Detect run failed.");
            exceptionUtility.logException(e);
            logger.debug("An exception was thrown during the detect run.", e);
            logger.error(ReportConstants.RUN_SEPARATOR);
            exitCodeManager.requestExitCode(e);
            /*if (e instanceof OperationException && e.getCause() instanceof BlackDuckApiException) {
                BlackDuckApiException apiException = (BlackDuckApiException) e.getCause();
                logger.info("API Exception Error Code: {}", apiException.getBlackDuckErrorCode());
                if (apiException.getBlackDuckErrorCode().contains("central.constraint_violation.project_name_duplicate_not_allowed")) {
                    exitCodeManager.requestExitCode(ExitCodeType.FAILURE_BLACKDUCK_DUPLICATE_PROJECT_ERROR);
                } else {
                    exitCodeManager.requestExitCode(e);
                }
            } else {
                exitCodeManager.requestExitCode(e);
            }*/

            checkForInterruptedException(e);
        } finally {
            operationSystem.ifPresent(OperationSystem::publishOperations);
        }
    }

    public void phoneHomeApplicableDetectorTypes(PhoneHomeManager phoneHomeManager, Set<DetectorType> applicableDetectorTypes) {
        Map<DetectorType, Long> detectorTimes = applicableDetectorTypes.stream().collect(Collectors.toMap(detectorType -> detectorType, detectorType -> 0L));
        phoneHomeManager.savePhoneHomeDetectorTimes(detectorTimes);
    }

    private String getCorrelationId(DetectConfigurationFactory configurationFactory, BootSingletons bootSingletons) {
        return configurationFactory.isCorrelatedScanningEnabled()? bootSingletons.getDetectRunId().getCorrelationId():null;
    }

    private Set<String> getDecidedTools(BootSingletons bootSingletons, Map<DetectTool, Set<String>> scanTypeEvidenceMap) {
        AllEnumList<DetectTool> userProvidedScanTypes = bootSingletons.getDetectConfiguration().getValue(DetectProperties.DETECT_TOOLS);
        Set<String> decidedScanTypes = new HashSet<>();
        scanTypeEvidenceMap.keySet().forEach(tool -> decidedScanTypes.add(tool.toString()));
        userProvidedScanTypes.representedValues().forEach(tool -> decidedScanTypes.add(tool.toString()));

        return decidedScanTypes;
    }

    /**
     * Image scan check to see if any docker information is there. If image isn't found for image scan, lists will
     * be empty and dockerTargetData will be null...  If not an image scan just return true
     * @param result
     * @param bootSingletons
     * @return
     */
    private void imageScanCanScanFurther(UniversalToolsResult result, BootSingletons bootSingletons) throws Exception {
        boolean canScanFurther = true;
        boolean isImageScan = bootSingletons.getDetectConfigurationFactory().createDetectTarget().equals(DetectTargetType.IMAGE);
        boolean hasDockerTargetData = result.getDockerTargetData() != null;
        
        if (isImageScan) {
            canScanFurther = (!result.didAnyFail() && 
                                    !result.getDetectToolProjectInfo().isEmpty() &&
                                    !result.getDetectCodeLocations().isEmpty() &&
                                    hasDockerTargetData
                );
        }        

        if (!canScanFurther) {
            throw new DetectUserFriendlyException("Cannot scan Docker image." , ExitCodeType.FAILURE_IMAGE_NOT_AVAILABLE);
        }

    }

    private void checkForInterruptedException(Exception e) {
        if (e instanceof InterruptedException) {
            // Restore interrupted state...
            Thread.currentThread().interrupt();
        }
    }
}
