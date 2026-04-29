package com.blackduck.integration.detectable.detectables.uv.buildexe;

import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
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
import java.util.Set;
import java.util.stream.Collectors;

public class UVBuildExtractor {

    private static final String TREE_COMMAND = "tree";
    private static final String NO_DEDUPE_FLAG = "--no-dedupe";
    private static final String ALL_GROUPS_FLAG = "--all-groups";
    private static final String GROUP_FLAG = "--group";
    private static final String NO_GROUP_FLAG = "--no-group";
    private static final String ALL_GROUPS_KEYWORD = "all";

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
            List<String> arguments = buildTreeCommandArguments(uvDetectorOptions);

            // run uv tree command
            ExecutableOutput executableOutput = executableRunner.executeSuccessfully(ExecutableUtils.createFromTarget(sourceDirectory, uvExe, arguments));
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

    private List<String> buildTreeCommandArguments(UVDetectorOptions uvDetectorOptions) {
        List<String> arguments = new ArrayList<>();
        arguments.add(TREE_COMMAND);
        arguments.add(NO_DEDUPE_FLAG);

        Set<String> includedGroups = uvDetectorOptions.getIncludedDependencyGroups();
        Set<String> excludedGroups = uvDetectorOptions.getExcludedDependencyGroups();

        Set<String> conflictingGroups = includedGroups.stream()
                .filter(excludedGroups::contains)
                .collect(Collectors.toSet());

        if (!conflictingGroups.isEmpty()) {
            logger.warn("Dependency groups {} appear in both included and excluded sets. They will be excluded.", conflictingGroups);
        }

        if (!includedGroups.isEmpty()) {
            boolean includeAll = includedGroups.stream().anyMatch(group -> group.equalsIgnoreCase(ALL_GROUPS_KEYWORD));
            if (includeAll) {
                arguments.add(ALL_GROUPS_FLAG);
            } else {
                for (String group : includedGroups) {
                    if (!excludedGroups.contains(group)) {
                        arguments.add(GROUP_FLAG);
                        arguments.add(group);
                    }
                }
            }
        }

        for (String group : excludedGroups) {
            arguments.add(NO_GROUP_FLAG);
            arguments.add(group);
        }

        return arguments;
    }
}
