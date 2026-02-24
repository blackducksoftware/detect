package com.blackduck.integration.detectable.detectables.conda.tree;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectables.conda.model.CondaListElement;
import com.blackduck.integration.detectable.detectables.conda.parser.CondaListParser;
import com.blackduck.integration.detectable.detectables.conda.parser.CondaTreeParser;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.executable.ExecutableOutput;
import com.blackduck.integration.executable.ExecutableRunnerException;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

public class CondaTreeExtractor {

    private final DetectableExecutableRunner executableRunner;
    private final CondaTreeParser condaTreeParser;
    private final CondaListParser condaListParser;

    public CondaTreeExtractor(DetectableExecutableRunner executableRunner, CondaTreeParser condaTreeParser, CondaListParser condaListParser) {
        this.executableRunner = executableRunner;
        this.condaTreeParser = condaTreeParser;
        this.condaListParser = condaListParser;
    }

    public Extraction extract(File directory, ExecutableTarget condaTreeExe, ExecutableTarget condaExe, File workingDirectory, String condaEnvironmentName) throws ExecutableRunnerException {
        try {
            List<String> condaListOptions = new ArrayList<>();
            condaListOptions.add("list");
            if (StringUtils.isNotBlank(condaEnvironmentName)) {
                condaListOptions.add("-n");
                condaListOptions.add(condaEnvironmentName);
            }
            condaListOptions.add("--json");
            ExecutableOutput condaListOutput = executableRunner.execute(ExecutableUtils.createFromTarget(directory, condaExe, condaListOptions));

            String listJsonText = condaListOutput.getStandardOutput();

            Map<String, CondaListElement> dependencies = condaListParser.getDependencies(listJsonText);

            List<String> condaTreeListOptions = new ArrayList<>();

            if (StringUtils.isNotBlank(condaEnvironmentName)) {
                condaTreeListOptions.add("-n");
                condaTreeListOptions.add(condaEnvironmentName);
            }
            condaTreeListOptions.add("deptree");
            condaTreeListOptions.add("--small");

            ExecutableOutput executableOutput = executableRunner.execute(ExecutableUtils.createFromTarget(workingDirectory, condaTreeExe, condaTreeListOptions));
            List<String> condaTreeOutput = executableOutput.getStandardOutputAsList();

            if (executableOutput.getReturnCode() == 0) {
                DependencyGraph dependencyGraph = condaTreeParser.parse(condaTreeOutput, dependencies);
                CodeLocation codeLocation = new CodeLocation(dependencyGraph);

                return new Extraction.Builder().success(codeLocation).build();
            } else {
                return new Extraction.Builder().failure("Error while running conda-tree command.").build();
            }

        } catch (Exception e) {
            return new Extraction.Builder().exception(e).build();
        }
    }
}
