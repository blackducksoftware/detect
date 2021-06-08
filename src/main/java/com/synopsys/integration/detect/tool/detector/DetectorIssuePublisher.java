/*
 * synopsys-detect
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.tool.detector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.synopsys.integration.detect.workflow.report.ExceptionUtil;
import com.synopsys.integration.detect.workflow.report.util.DetectorEvaluationUtils;
import com.synopsys.integration.detect.workflow.status.DetectIssue;
import com.synopsys.integration.detect.workflow.status.DetectIssueType;
import com.synopsys.integration.detect.workflow.status.StatusEventPublisher;
import com.synopsys.integration.detector.base.DetectorEvaluation;
import com.synopsys.integration.detector.base.DetectorEvaluationTree;

public class DetectorIssuePublisher {

    public void publishEvents(StatusEventPublisher statusEventPublisher, DetectorEvaluationTree rootEvaluationTree) {
        publishEvents(statusEventPublisher, rootEvaluationTree.asFlatList());
    }

    private void publishEvents(StatusEventPublisher statusEventPublisher, List<DetectorEvaluationTree> trees) {
        String spacer = "\t";
        for (DetectorEvaluationTree tree : trees) {
            List<DetectorEvaluation> excepted = DetectorEvaluationUtils.filteredChildren(tree, DetectorEvaluation::wasExtractionException);
            List<DetectorEvaluation> failed = DetectorEvaluationUtils.filteredChildren(tree, DetectorEvaluation::wasExtractionFailure);
            List<DetectorEvaluation> notExtractable = DetectorEvaluationUtils.filteredChildren(tree, evaluation -> evaluation.isApplicable() && !evaluation.isExtractable());

            List<String> messages = new ArrayList<>();

            addIfNotEmpty(messages, "Not Extractable: ", spacer, notExtractable, DetectorEvaluation::getExtractabilityMessage);
            addIfNotEmpty(messages, "Failure: ", spacer, failed, detectorEvaluation -> detectorEvaluation.getExtraction().getDescription());
            addIfNotEmpty(messages, "Exception: ", spacer, excepted, detectorEvaluation -> ExceptionUtil.oneSentenceDescription(detectorEvaluation.getExtraction().getError()));

            if (messages.size() > 0) {
                messages.add(0, tree.getDirectory().toString());
                statusEventPublisher.publishIssue(new DetectIssue(DetectIssueType.DETECTOR, "Detector Issue", messages));
            }
        }
    }

    private void addIfNotEmpty(List<String> messages, String prefix, String spacer, List<DetectorEvaluation> evaluations, Function<DetectorEvaluation, String> reason) {
        if (evaluations.size() > 0) {
            evaluations.forEach(evaluation -> {
                messages.add(prefix + evaluation.getDetectorRule().getDescriptiveName());
                messages.add(spacer + reason.apply(evaluation));
            });
        }
    }

}
