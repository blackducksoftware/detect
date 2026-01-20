package com.blackduck.integration.detect.workflow.componentlocationanalysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.blackduck.integration.detect.workflow.file.DirectoryManager;
import com.blackduck.integration.detect.workflow.result.QuackPatchResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.componentlocator.ComponentLocator;
import static com.blackduck.integration.componentlocator.ComponentLocator.SUPPORTED_DETECTORS;
import com.blackduck.integration.componentlocator.beans.Component;
import com.blackduck.integration.componentlocator.beans.Input;
import com.blackduck.integration.detect.configuration.DetectConfigurationFactory;
import com.blackduck.integration.detect.configuration.DetectProperties;
import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.configuration.enumeration.DetectTool;
import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.detect.lifecycle.shutdown.ExitCodePublisher;
import com.blackduck.integration.detect.workflow.file.DetectFileUtils;
import com.blackduck.integration.detect.workflow.report.util.ReportConstants;
import com.blackduck.integration.detect.workflow.result.ComponentLocatorResult;
import com.blackduck.integration.detect.workflow.status.Status;
import com.blackduck.integration.detect.workflow.status.StatusEventPublisher;
import com.blackduck.integration.detect.workflow.status.StatusType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * This class will generate the appropriate input file for Component Locator, invoke the library's obfuscated JAR and
 * save the resulting output file in the appropriate output subdirectory.
 */
public class GenerateComponentLocationAnalysisOperation {
    
    public static final String OPERATION_NAME = "Generating Component Location Analysis File for All Components";
    private static final String LOCATOR_INPUT_FILE_NAME = "components-source.json";
    private static final String LOCATOR_OUTPUT_FILE_NAME = "components-with-locations.json";
    public static final String SUPPORTED_DETECTORS_LOG_MSG = "Component Location Analysis supports specific detectors ".concat(SUPPORTED_DETECTORS.toString()).concat(" only.");
    public static final String QUACKPATCH_SUBDIRECTORY_NAME = "quack-patch";
    public static final String INVOKED_DETECTORS_AND_RELEVANT_FILES_JSON = "invokedDetectorsAndTheirRelevantFiles.json";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DetectConfigurationFactory detectConfigurationFactory;
    private final StatusEventPublisher statusEventPublisher;
    private final ExitCodePublisher exitCodePublisher;

    public GenerateComponentLocationAnalysisOperation(DetectConfigurationFactory detectConfigurationFactory, StatusEventPublisher statusEventPublisher, ExitCodePublisher exitCodePublisher) {
        this.detectConfigurationFactory = detectConfigurationFactory;
        this.statusEventPublisher = statusEventPublisher;
        this.exitCodePublisher = exitCodePublisher;
    }

    /**
     * Given a BDIO, generates an output file consisting of the list of unique components detected and their declaration
     * locations.
     * @param componentsSet
     * @param scanOutputFolder Detect's output subdirectory where this file will be saved
     * @param projectSrcDir source directory of project being scanned
     * @return ComponentLocatorResult
     * @throws com.blackduck.integration.detect.workflow.componentlocationanalysis.ComponentLocatorException
     * @throws DetectUserFriendlyException
     */
    public ComponentLocatorResult locateComponents(Set<Component> componentsSet, File scanOutputFolder, File projectSrcDir) throws ComponentLocatorException, DetectUserFriendlyException {
        Input componentLocatorInput = new Input(projectSrcDir.getAbsolutePath(), new JsonObject(), componentsSet);
        String outputFilepath = scanOutputFolder + "/" + LOCATOR_OUTPUT_FILE_NAME;
        if (logger.isDebugEnabled()) {
            serializeInputToJson(scanOutputFolder, componentLocatorInput);
        }
        logger.info(ReportConstants.RUN_SEPARATOR);
        int status = ComponentLocator.locateComponents(componentLocatorInput, outputFilepath);
        if (status != 0) {
            logger.warn("Component Locator execution has failed.");
            logger.info(ReportConstants.RUN_SEPARATOR);
            failComponentLocationAnalysisOperation();
        }
        logger.info("Component Location Analysis file saved at: {}", outputFilepath);
        logger.info(ReportConstants.RUN_SEPARATOR);
        publishComponentLocatorSuccessIfEnabled();
        return new ComponentLocatorResult(outputFilepath);
    }

    public QuackPatchResult runQuackPatch(File scanOutputFolder, File rapidFullResultsFile, DetectConfigurationFactory configFactory) {
        logger.info("Attempting Quack Patch.");

            Map<String, List<String>> relevantDetectorsAndFiles = loadDetectorsAndFiles(scanOutputFolder.getAbsolutePath() + "/" + QUACKPATCH_SUBDIRECTORY_NAME + "/" + INVOKED_DETECTORS_AND_RELEVANT_FILES_JSON);
            String llmKey = configFactory.getDetectPropertyConfiguration().getValue(DetectProperties.DETECT_LLM_API_KEY);
            String llmName = configFactory.getDetectPropertyConfiguration().getValue(DetectProperties.DETECT_LLM_NAME);
            String llmURL = configFactory.getDetectPropertyConfiguration().getValue(DetectProperties.DETECT_LLM_API_ENDPOINT);

            ComponentLocator.runQuackPatch(rapidFullResultsFile, relevantDetectorsAndFiles, llmKey, llmName, llmURL, scanOutputFolder.getPath());
        return new QuackPatchResult();
    }

    public ComponentLocatorResult locateComponentsForOnlineIntelligentScan() throws ComponentLocatorException {
        logger.info(ReportConstants.RUN_SEPARATOR);
        logger.info("Intelligent Scan mode does not support Component Location Analysis.");
        failComponentLocationAnalysisOperation();

        // unreachable statements, mainly here so we don't forget to log a result if this function is ever implemented
        publishComponentLocatorSuccessIfEnabled();
        return new ComponentLocatorResult("change me");
    }

    /**
     * A ComponentLocatorException should be thrown in all cases where the output file is not created so that the
     * appropriate status can be logged for the requested {@link GenerateComponentLocationAnalysisOperation}.
     * @throws com.blackduck.integration.detect.workflow.componentlocationanalysis.ComponentLocatorException
     */
    public void failComponentLocationAnalysisOperation() throws ComponentLocatorException {
        publishComponentLocatorFailureIfEnabled();
        throw new ComponentLocatorException("Failed to generate Component Location Analysis file.");
    }

    public String getQuackPatchOutputPath() {
        Path relevantDetectorsAndFilesInfoPath = Paths.get(DirectoryManager.getScanDirectoryName())
                .resolve(QUACKPATCH_SUBDIRECTORY_NAME)
                .resolve(INVOKED_DETECTORS_AND_RELEVANT_FILES_JSON);
        return relevantDetectorsAndFilesInfoPath.toString();
    }


    private void publishComponentLocatorSuccessIfEnabled() {
        if (detectConfigurationFactory.doesComponentLocatorAffectStatus()) {
            statusEventPublisher.publishStatusSummary(Status.forTool(DetectTool.COMPONENT_LOCATION_ANALYSIS, StatusType.SUCCESS));
        }
    }

    private void publishComponentLocatorFailureIfEnabled() {
        if (detectConfigurationFactory.doesComponentLocatorAffectStatus()) {
            statusEventPublisher.publishStatusSummary(Status.forTool(DetectTool.COMPONENT_LOCATION_ANALYSIS, StatusType.FAILURE));
            exitCodePublisher.publishExitCode(ExitCodeType.FAILURE_COMPONENT_LOCATION_ANALYSIS);
        }
    }

    private File serializeInputToJson(File saveInputFileDir, Input libInput) throws DetectUserFriendlyException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String serializedLibInput = gson.toJson(libInput, Input.class);
        try {
            File componentsSourceInputFile =  new File (saveInputFileDir, LOCATOR_INPUT_FILE_NAME);
            DetectFileUtils.writeToFile(componentsSourceInputFile, serializedLibInput);
            logger.debug("Component Location Analysis input file written to {}", componentsSourceInputFile);
            return componentsSourceInputFile;
        } catch (IOException ex) {
            throw new DetectUserFriendlyException("Failed to create component location analysis output file", ex, ExitCodeType.FAILURE_UNKNOWN_ERROR);
        }
    }

    public Map<String, List<String>> loadDetectorsAndFiles(String jsonFilePath) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(new File(jsonFilePath),
                    new TypeReference<Map<String, List<String>>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to read detectors file: " + jsonFilePath, e);
        }
    }

}
