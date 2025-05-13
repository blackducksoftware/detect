package com.blackduck.integration.detectable.detectables.sbt.dot;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.executable.Executable;
import com.blackduck.integration.executable.ExecutableOutput;

public class SbtDotExtractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DetectableExecutableRunner executableRunner;
    private final SbtDotOutputParser sbtDotOutputParser;
    private final SbtRootNodeFinder sbtRootNodeFinder;
    private final SbtGraphParserTransformer sbtGraphParserTransformer;
    private final SbtDotGraphNodeParser graphNodeParser;
    private final SbtCommandArgumentGenerator sbtCommandArgumentGenerator;

    public SbtDotExtractor(
        DetectableExecutableRunner executableRunner,
        SbtDotOutputParser sbtDotOutputParser,
        SbtRootNodeFinder sbtRootNodeFinder,
        SbtGraphParserTransformer sbtGraphParserTransformer,
        SbtDotGraphNodeParser graphNodeParser,
        SbtCommandArgumentGenerator sbtCommandArgumentGenerator
    ) {
        this.executableRunner = executableRunner;
        this.sbtDotOutputParser = sbtDotOutputParser;
        this.sbtRootNodeFinder = sbtRootNodeFinder;
        this.sbtGraphParserTransformer = sbtGraphParserTransformer;
        this.graphNodeParser = graphNodeParser;
        this.sbtCommandArgumentGenerator = sbtCommandArgumentGenerator;
    }

    public Extraction extract(File directory, ExecutableTarget sbt, String sbtCommandAdditionalArguments) {
        try {
            List<String> sbtArgs = sbtCommandArgumentGenerator.generateSbtCmdArgs(sbtCommandAdditionalArguments, "dependencyDot");
            Executable dotExecutable = ExecutableUtils.createFromTarget(directory, sbt, sbtArgs);
            ExecutableOutput dotOutput = executableRunner.executeSuccessfully(dotExecutable);
            List<File> dotGraphs = sbtDotOutputParser.parseGeneratedGraphFiles(dotOutput.getStandardOutputAsList());

            Extraction.Builder extraction = new Extraction.Builder();
            for (File dotGraph : dotGraphs) {
                MutableGraph mutableGraph = new Parser().read(dotGraph);
                Set<String> rootIDs = sbtRootNodeFinder.determineRootIDs(mutableGraph);
                File projectFolder = dotGraph.getParentFile().getParentFile();//typically found in project-folder/target/<>.dot so .parent.parent == project folder

                if (rootIDs.size() == 1) {
                    String projectId = rootIDs.stream().findFirst().get();
                    DependencyGraph graph = sbtGraphParserTransformer.transformDotToGraph(projectId, mutableGraph);
                    Dependency projectDependency = graphNodeParser.nodeToDependency(projectId);
                    extraction.codeLocations(new CodeLocation(graph, projectDependency.getExternalId(), projectFolder));
                    if (projectFolder.equals(directory)) {
                        extraction.projectName(projectDependency.getName());
                        extraction.projectVersion(projectDependency.getVersion());
                    }
                } else {
                    logger.warn("Unable to determine which node was the project in an SBT graph: " + dotGraph.toString());
                    logger.warn("This may mean you have extraneous dependencies and should consider removing them. The dependencies are: " + String.join(",", rootIDs));
                    DependencyGraph graph = sbtGraphParserTransformer.transformDotToGraph(rootIDs, mutableGraph);
                    extraction.codeLocations(new CodeLocation(graph, projectFolder));
                }
            }
            return extraction.success().build();
        } catch (IOException | DetectableException | ExecutableFailedException e) {
            return new Extraction.Builder().exception(e).build();
        }
    }
}
