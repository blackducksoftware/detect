package com.blackduck.integration.detectable.detectable.result;

import java.io.File;
import java.util.Collections;
import java.util.List;

import com.blackduck.integration.detectable.detectable.explanation.Explanation;

public class PassedDetectableResult implements DetectableResult {
    private final List<Explanation> explanations;
    private final List<File> relevantFiles;

    public PassedDetectableResult(List<Explanation> explanations, List<File> relevantFiles) {
        this.explanations = explanations;
        this.relevantFiles = relevantFiles;
    }

    public PassedDetectableResult(List<Explanation> explanations) {
        this.explanations = explanations;
        this.relevantFiles = Collections.emptyList();
    }

    public PassedDetectableResult() {
        this.explanations = Collections.emptyList();
        this.relevantFiles = Collections.emptyList();
    }

    public PassedDetectableResult(Explanation explanation) {
        this.explanations = Collections.singletonList(explanation);
        this.relevantFiles = Collections.emptyList();
    }

    public PassedDetectableResult(Explanation explanation, File relevantFile) {
        this.explanations = Collections.singletonList(explanation);
        this.relevantFiles = Collections.singletonList(relevantFile);
    }

    @Override
    public boolean getPassed() {
        return true;
    }

    @Override
    public String toDescription() {
        return "Passed";
    }

    @Override
    public List<Explanation> getExplanation() {
        return explanations;
    }

    @Override
    public List<File> getRelevantFiles() {
        return relevantFiles;
    }
}
