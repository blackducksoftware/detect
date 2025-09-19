package com.blackduck.integration.detect.tool.impactanalysis.service;

import java.util.List;

import org.slf4j.Logger;

import com.blackduck.integration.blackduck.codelocation.CodeLocationBatchOutput;
import com.blackduck.integration.blackduck.exception.BlackDuckIntegrationException;

public class ImpactAnalysisBatchOutput extends CodeLocationBatchOutput<ImpactAnalysisOutput> {
    public ImpactAnalysisBatchOutput(List<ImpactAnalysisOutput> outputs) {
        super(outputs);
    }

    public void throwExceptionForError(Logger logger) throws BlackDuckIntegrationException {
        for (ImpactAnalysisOutput impactAnalysisOutput : this) {
            if (impactAnalysisOutput.getStatusCode() == 404) {
                logger.error("Impact analysis upload failed with 404. Your version of Black Duck SCA may not support Vulnerability Impact Analysis.");
            } else if (impactAnalysisOutput.getStatusCode() < 200 || impactAnalysisOutput.getStatusCode() >= 300) {
                logger.error(String.format("Unexpected status code: %d", impactAnalysisOutput.getStatusCode()));
                throw new BlackDuckIntegrationException(
                    String.format("Unexpected status code when uploading impact analysis: %d, %s [Black Duck SCA response content: %s]",
                        impactAnalysisOutput.getStatusCode(),
                        impactAnalysisOutput.getStatusMessage(),
                        impactAnalysisOutput.getContentString()
                    ));
            }
        }
    }

}
