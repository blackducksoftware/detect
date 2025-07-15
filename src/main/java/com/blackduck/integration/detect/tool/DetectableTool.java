package com.blackduck.integration.detect.tool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.google.gson.JsonSyntaxException;
import com.blackduck.integration.bdio.graph.builder.MissingExternalIdException;
import com.blackduck.integration.detect.configuration.enumeration.DetectTool;
import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.detect.lifecycle.run.data.DockerTargetData;
import com.blackduck.integration.detect.lifecycle.shutdown.ExitCodePublisher;
import com.blackduck.integration.detect.lifecycle.shutdown.ExitCodeRequest;
import com.blackduck.integration.detect.tool.detector.CodeLocationConverter;
import com.blackduck.integration.detect.tool.detector.extraction.ExtractionEnvironmentProvider;
import com.blackduck.integration.detect.workflow.codelocation.DetectCodeLocation;
import com.blackduck.integration.detect.workflow.project.DetectToolProjectInfo;
import com.blackduck.integration.detect.workflow.status.DetectIssue;
import com.blackduck.integration.detect.workflow.status.DetectIssueType;
import com.blackduck.integration.detect.workflow.status.Status;
import com.blackduck.integration.detect.workflow.status.StatusEventPublisher;
import com.blackduck.integration.detect.workflow.status.StatusType;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectable.result.ExceptionDetectableResult;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;
import com.blackduck.integration.detectable.util.CycleDetectedException;
import com.blackduck.integration.detector.base.DetectableCreatable;
import com.blackduck.integration.executable.ExecutableRunnerException;
import com.blackduck.integration.util.NameVersion;

public class DetectableTool {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DetectableCreatable detectableCreatable;
    private final ExtractionEnvironmentProvider extractionEnvironmentProvider;
    private final CodeLocationConverter codeLocationConverter;
    private final String name;
    private final DetectTool detectTool;
    private final StatusEventPublisher statusEventPublisher;
    private final ExitCodePublisher exitCodePublisher;

    //TODO: Move docker/bazel out of detectable and drop this notion of a detctable tool. Will simplify this logic and make this unneccessary.
    private Detectable detectable;
    private File sourcePath;

    public DetectableTool(
        DetectableCreatable detectableCreatable,
        ExtractionEnvironmentProvider extractionEnvironmentProvider,
        CodeLocationConverter codeLocationConverter,
        String name,
        DetectTool detectTool,
        StatusEventPublisher statusEventPublisher,
        ExitCodePublisher exitCodePublisher
    ) {
        this.codeLocationConverter = codeLocationConverter;
        this.name = name;
        this.detectableCreatable = detectableCreatable;
        this.extractionEnvironmentProvider = extractionEnvironmentProvider;
        this.detectTool = detectTool;
        this.statusEventPublisher = statusEventPublisher;
        this.exitCodePublisher = exitCodePublisher;
    }

    public boolean initializeAndCheckForApplicable(File sourcePath) { //TODO: Move docker/bazel out of detectable and drop this notion of a detctable tool. Will simplify this logic and make this unneccessary.
        logger.trace("Starting a detectable tool.");
        this.sourcePath = sourcePath;

        DetectableEnvironment detectableEnvironment = new DetectableEnvironment(sourcePath);
        detectable = detectableCreatable.createDetectable(detectableEnvironment);
        DetectableResult applicable = detectable.applicable();

        if (!applicable.getPassed()) {
            logger.debug("Was not applicable.");
            return false;
        }

        logger.debug("Applicable passed.");
        return true;
    }

    public DetectableToolResult extract() { //TODO: Move docker/bazel out of detectable and drop this notion of a detctable tool. Will simplify this logic and make this unneccessary.
        DetectableResult extractable;
        try {
            extractable = detectable.extractable();
        } catch (DetectableException e) {
            extractable = new ExceptionDetectableResult(e);
        }

        if (!extractable.getPassed()) {
            logger.error(String.format("Was not extractable: %s", extractable.toDescription()));
            statusEventPublisher.publishIssue(new DetectIssue(DetectIssueType.DETECTABLE_TOOL, "Detectable Tool Issue", Arrays.asList(extractable.toDescription())));
            statusEventPublisher.publishStatusSummary(new Status(name, StatusType.FAILURE));
            exitCodePublisher.publishExitCode(ExitCodeType.FAILURE_GENERAL_ERROR);
            return DetectableToolResult.failed(extractable);
        }

        logger.debug("Extractable passed.");

        ExtractionEnvironment extractionEnvironment = extractionEnvironmentProvider.createExtractionEnvironment(name);
        Extraction extraction;
        try {
            extraction = detectable.extract(extractionEnvironment);
        } catch (ExecutableFailedException | ExecutableRunnerException | JsonSyntaxException | IOException | CycleDetectedException | DetectableException | MissingExternalIdException | ParserConfigurationException | SAXException e) {
            extraction = new Extraction.Builder().exception(e).build();
        }

        if (!extraction.isSuccess()) {
            logger.error("Extraction was not success.");
            List<String> errorMessages = collectErrorMessages(extraction);
            statusEventPublisher.publishIssue(new DetectIssue(DetectIssueType.DETECTABLE_TOOL, "Detectable Tool Issue", errorMessages));
            statusEventPublisher.publishStatusSummary(new Status(name, StatusType.FAILURE));
            exitCodePublisher.publishExitCode(ExitCodeType.FAILURE_GENERAL_ERROR);
            return DetectableToolResult.failed();
        } else {
            logger.debug("Extraction success.");
            statusEventPublisher.publishStatusSummary(new Status(name, StatusType.SUCCESS));
        }

        Map<CodeLocation, DetectCodeLocation> detectCodeLocationMap = codeLocationConverter.toDetectCodeLocation(sourcePath, extraction, sourcePath, name);
        List<DetectCodeLocation> detectCodeLocations = new ArrayList<>(detectCodeLocationMap.values());

        DockerTargetData dockerTargetData = DockerTargetData.fromExtraction(extraction);

        DetectToolProjectInfo projectInfo = null;
        if (StringUtils.isNotBlank(extraction.getProjectName()) || StringUtils.isNotBlank(extraction.getProjectVersion())) {
            NameVersion nameVersion = new NameVersion(extraction.getProjectName(), extraction.getProjectVersion());
            projectInfo = new DetectToolProjectInfo(detectTool, nameVersion);
        }

        logger.debug("Tool finished.");

        return DetectableToolResult.success(detectCodeLocations, projectInfo, dockerTargetData);
    }

    @NotNull
    private List<String> collectErrorMessages(Extraction extraction) {
        List<String> errorMessages = new LinkedList<>();
        if (StringUtils.isNotBlank(extraction.getDescription())) {
            errorMessages.add(extraction.getDescription());
        }
        if (extraction.getError() != null && StringUtils.isNotBlank(extraction.getError().getMessage())) {
            errorMessages.add(extraction.getError().getMessage());
        }
        return errorMessages;
    }
}
