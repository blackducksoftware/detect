package com.blackduck.integration.detect.lifecycle.run.step;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.SortedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.bdio2.Bdio;
import com.blackduck.integration.bdio.graph.ProjectDependencyGraph;
import com.blackduck.integration.blackduck.codelocation.upload.UploadTarget;
import com.blackduck.integration.detect.configuration.enumeration.DetectTool;
import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.detect.lifecycle.run.step.utility.StepHelper;
import com.blackduck.integration.detect.tool.DetectableTool;
import com.blackduck.integration.detect.tool.DetectableToolResult;
import com.blackduck.integration.detect.tool.UniversalToolsResult;
import com.blackduck.integration.detect.tool.UniversalToolsResultBuilder;
import com.blackduck.integration.detect.tool.detector.DetectorToolResult;
import com.blackduck.integration.detect.workflow.bdio.AggregateCodeLocation;
import com.blackduck.integration.detect.workflow.bdio.BdioResult;
import com.blackduck.integration.detect.workflow.blackduck.report.service.SbomService;
import com.blackduck.integration.detect.workflow.codelocation.DetectCodeLocation;
import com.blackduck.integration.detect.workflow.codelocation.DetectCodeLocationNamesResult;
import com.blackduck.integration.detect.workflow.report.util.ReportConstants;
import com.blackduck.integration.util.NameVersion;

public class UniversalStepRunner {
    private final OperationRunner operationRunner;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final StepHelper stepHelper;

    public UniversalStepRunner(OperationRunner operationRunner, StepHelper stepHelper) {
        this.operationRunner = operationRunner;
        this.stepHelper = stepHelper;
    }

    public UniversalToolsResult runUniversalTools() throws OperationException {
        UniversalToolsResultBuilder resultBuilder = new UniversalToolsResultBuilder();

        stepHelper.runToolIfIncluded(DetectTool.DOCKER, "Docker", this::runDocker)
            .ifPresent(resultBuilder::addDetectableToolResult);

        stepHelper.runToolIfIncluded(DetectTool.BAZEL, "Bazel", this::runBazel)
            .ifPresent(resultBuilder::addDetectableToolResult);

        stepHelper.runToolIfIncluded(DetectTool.DETECTOR, "Detectors", this::runDetectors)
            .ifPresent(resultBuilder::addDetectorToolResult);

        return resultBuilder.build();
    }

    private DetectableToolResult runDocker() throws OperationException {
        Optional<DetectableTool> potentialTool = operationRunner.checkForDocker();
        if (potentialTool.isPresent()) {
            return operationRunner.executeDocker(potentialTool.get());
        } else {
            return DetectableToolResult.skip();
        }
    }

    private DetectableToolResult runBazel() throws OperationException {
        Optional<DetectableTool> potentialTool = operationRunner.checkForBazel();
        if (potentialTool.isPresent()) {
            return operationRunner.executeBazel(potentialTool.get());
        } else {
            return DetectableToolResult.skip();
        }
    }

    private DetectorToolResult runDetectors() throws OperationException {
        DetectorToolResult result = operationRunner.executeDetectors();
        if (result.anyDetectorsFailed()) {
            operationRunner.publishDetectorFailure();
        }
        return result;
    }
    
    public SortedMap<String, SortedSet<String>> getScanTargets(UniversalToolsResult universalToolsResult) {
        SortedMap<String, SortedSet<String>> scanTargetMap = new TreeMap<>();
        for(DetectCodeLocation detectCodeLocation: universalToolsResult.getDetectCodeLocations()) {
            if(detectCodeLocation.getCreatorName().isPresent()) {
                scanTargetMap.computeIfAbsent(detectCodeLocation.getCreatorName().get(), value -> new TreeSet<>()).add(detectCodeLocation.getSourcePath().toString());
            }
        }
        return scanTargetMap;
    }

    public BdioResult generateBdio(String integratedMatchingCorrelationId, UniversalToolsResult universalToolsResult, NameVersion projectNameVersion) throws OperationException {
        ProjectDependencyGraph aggregateDependencyGraph = operationRunner.aggregateSubProject(projectNameVersion, universalToolsResult.getDetectCodeLocations());

        // Generate SBOM from in-memory dependency graph before it gets serialized to BDIO
        try {
            SbomService.generateSbom(aggregateDependencyGraph);
        } catch (Exception e) {
            logger.warn("Failed to generate SBOM from dependency graph", e);
        }

        AggregateCodeLocation aggregateCodeLocation = operationRunner.createAggregateCodeLocation(
            aggregateDependencyGraph,
            projectNameVersion,
            universalToolsResult.getDetectToolGitInfo()
        );
        operationRunner.createAggregateBdio2File(integratedMatchingCorrelationId, aggregateCodeLocation, Bdio.ScanType.PACKAGE_MANAGER);

        List<UploadTarget> uploadTargets = new ArrayList<>();
        Map<DetectCodeLocation, String> codeLocationNamesResult = new HashMap<>();
        universalToolsResult.getDetectCodeLocations().forEach(cl -> codeLocationNamesResult.put(
            cl,
            aggregateCodeLocation.getCodeLocationName()
        )); //TODO: This doesn't seem right, it should just be the aggregate CL name right?

        uploadTargets.add(UploadTarget.createDefault(projectNameVersion, aggregateCodeLocation.getCodeLocationName(), aggregateCodeLocation.getAggregateFile()));

        return new BdioResult(uploadTargets, new DetectCodeLocationNamesResult(codeLocationNamesResult), universalToolsResult.getApplicableDetectorTypes());
    }

    public NameVersion determineProjectInformation(UniversalToolsResult universalToolsResult) throws OperationException {
        logger.info(ReportConstants.RUN_SEPARATOR);
        logger.debug("Completed code location tools.");

        logger.debug("Determining project info.");

        NameVersion projectNameVersion = operationRunner.createProjectDecisionOperation(universalToolsResult.getDetectToolProjectInfo());

        logger.info(String.format("Project name: %s", projectNameVersion.getName()));
        logger.info(String.format("Project version: %s", projectNameVersion.getVersion()));

        return projectNameVersion;
    }

}
