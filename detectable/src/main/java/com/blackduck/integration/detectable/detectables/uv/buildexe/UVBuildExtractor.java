package com.blackduck.integration.detectable.detectables.uv.buildexe;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectables.uv.UVDependencyGroupFilter;
import com.blackduck.integration.detectable.detectables.uv.UVDetectorOptions;
import com.blackduck.integration.detectable.detectables.uv.parse.UVTomlParser;
import com.blackduck.integration.detectable.detectables.uv.transform.UVTreeDependencyGraphTransformer;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.executable.ExecutableOutput;
import com.blackduck.integration.executable.ExecutableRunnerException;
import com.blackduck.integration.util.NameVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UVBuildExtractor {

    private static final String TREE_COMMAND = "tree";
    private static final String NO_DEDUPE_FLAG = "--no-dedupe";
    private static final String ALL_GROUPS_FLAG = "--all-groups";
    private static final String NO_GROUP_FLAG = "--no-group";
    private static final String ONLY_GROUP_FLAG = "--only-group";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DetectableExecutableRunner executableRunner;
    private final File sourceDirectory;
    private final UVTreeDependencyGraphTransformer uvTreeDependencyGraphTransformer;

    public UVBuildExtractor(DetectableExecutableRunner executableRunner, File sourceDirectory, UVTreeDependencyGraphTransformer uvTreeDependencyGraphTransformer) {
        this.executableRunner = executableRunner;
        this.sourceDirectory = sourceDirectory;
        this.uvTreeDependencyGraphTransformer = uvTreeDependencyGraphTransformer;
    }

    public Extraction extract(ExecutableTarget uvExe, UVDetectorOptions uvDetectorOptions, UVTomlParser uvTomlParser) throws ExecutableRunnerException {
        try {
            UVDependencyGroupFilter groupFilter = new UVDependencyGroupFilter(uvDetectorOptions);
            groupFilter.logGroupConflictWarnings(logger);

            Optional<List<String>> arguments = buildTreeCommandArguments(groupFilter);

            if (!arguments.isPresent()) {
                DependencyGraph emptyGraph = new BasicDependencyGraph();
                Optional<NameVersion> projectNameVersion = uvTomlParser.parseNameVersion();
                CodeLocation emptyCodeLocation = projectNameVersion
                        .map(nv -> new CodeLocation(emptyGraph, ExternalId.FACTORY.createNameVersionExternalId(Forge.PYPI, nv.getName(), nv.getVersion())))
                        .orElse(new CodeLocation(emptyGraph));
                return new Extraction.Builder()
                        .success(emptyCodeLocation)
                        .nameVersionIfPresent(projectNameVersion)
                        .build();
            }

            ExecutableOutput executableOutput = executableRunner.executeSuccessfully(ExecutableUtils.createFromTarget(sourceDirectory, uvExe, arguments.get()));
            List<String> uvTreeOutput = executableOutput.getStandardOutputAsList();

            List<CodeLocation> codeLocations = uvTreeDependencyGraphTransformer.transform(uvTreeOutput, uvDetectorOptions);

            Optional<NameVersion> projectNameVersion = uvTomlParser.parseNameVersion();

            return new Extraction.Builder()
                    .success(codeLocations)
                    .nameVersionIfPresent(projectNameVersion)
                    .build();
        } catch (Exception e) {
            return new Extraction.Builder().exception(e).build();
        }
    }

    private Optional<List<String>> buildTreeCommandArguments(UVDependencyGroupFilter groupFilter) {
        List<String> arguments = new ArrayList<>();
        arguments.add(TREE_COMMAND);
        arguments.add(NO_DEDUPE_FLAG);

        if (!groupFilter.getOnlyGroups().isEmpty()) {
            if (!groupFilter.hasEffectiveGroups()) {
                return Optional.empty();
            }
            for (String group : groupFilter.getEffectiveOnlyGroups()) {
                arguments.add(ONLY_GROUP_FLAG);
                arguments.add(group);
            }
        } else {
            arguments.add(ALL_GROUPS_FLAG);
            for (String group : groupFilter.getExcludedGroups()) {
                arguments.add(NO_GROUP_FLAG);
                arguments.add(group);
            }
        }

        return Optional.of(arguments);
    }
}
