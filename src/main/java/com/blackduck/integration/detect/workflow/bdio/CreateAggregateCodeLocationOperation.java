package com.blackduck.integration.detect.workflow.bdio;

import java.io.File;

import org.apache.commons.lang3.StringUtils;

import com.blackduck.integration.bdio.graph.ProjectDependencyGraph;
import com.blackduck.integration.blackduck.bdio2.model.GitInfo;
import com.blackduck.integration.detect.workflow.codelocation.CodeLocationNameManager;
import com.blackduck.integration.util.IntegrationEscapeUtil;
import com.blackduck.integration.util.NameVersion;

public class CreateAggregateCodeLocationOperation {
    private final CodeLocationNameManager codeLocationNameManager;

    public CreateAggregateCodeLocationOperation(CodeLocationNameManager codeLocationNameManager) {
        this.codeLocationNameManager = codeLocationNameManager;
    }

    public AggregateCodeLocation createAggregateCodeLocation(
        File bdioOutputDirectory,
        ProjectDependencyGraph aggregateDependencyGraph,
        NameVersion projectNameVersion,
        GitInfo gitInfo,
        String providedFileName
    ) {
        String codeLocationName = codeLocationNameManager.createAggregateCodeLocationName(projectNameVersion);

        String defaultFileName = new IntegrationEscapeUtil().replaceWithUnderscore(projectNameVersion.getName() + "_" + projectNameVersion.getVersion());
        String bdioFileName = StringUtils.defaultIfBlank(providedFileName, defaultFileName);
        bdioFileName = bdioFileName + ".bdio";
        File aggregateBdioFile = new File(bdioOutputDirectory, bdioFileName);

        return new AggregateCodeLocation(aggregateBdioFile, codeLocationName, projectNameVersion, gitInfo, aggregateDependencyGraph);
    }
}
