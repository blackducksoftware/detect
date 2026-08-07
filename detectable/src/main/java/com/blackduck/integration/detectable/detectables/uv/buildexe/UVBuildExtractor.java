package com.blackduck.integration.detectable.detectables.uv.buildexe;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
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
            Optional<List<String>> arguments = buildTreeCommandArguments(uvDetectorOptions);

            // If no valid groups remain after exclusion, return an empty BOM (a CodeLocation with zero dependencies)
            // rather than running "uv tree --no-dedupe" which would incorrectly return ALL dependencies.
            if (!arguments.isPresent()) {
                logger.warn(
                        "All dependency groups specified in 'detect.uv.dependency.groups.only' are also present in "
                        + "'detect.uv.dependency.groups.excluded'. No dependency groups remain to scan. Returning an empty BOM."
                );
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

    private Optional<List<String>> buildTreeCommandArguments(UVDetectorOptions uvDetectorOptions) {
        List<String> arguments = new ArrayList<>();
        arguments.add(TREE_COMMAND);
        arguments.add(NO_DEDUPE_FLAG);

        Set<String> onlyGroups = uvDetectorOptions.getOnlyDependencyGroups();
        Set<String> excludedGroups = uvDetectorOptions.getExcludedDependencyGroups();

        if (!onlyGroups.isEmpty()) {
            boolean hasEffectiveGroups = addOnlyGroupArguments(arguments, onlyGroups, excludedGroups);
            if (!hasEffectiveGroups) {
                return Optional.empty();
            }
        } else {
            addDefaultGroupArguments(arguments, excludedGroups);
        }

        return Optional.of(arguments);
    }

    // Computes effectiveOnlyGroups = onlyGroups minus excludedGroups.
    // Returns false if no groups remain (signals caller to produce an empty BOM).
    private boolean addOnlyGroupArguments(List<String> arguments, Set<String> onlyGroups, Set<String> excludedGroups) {
        Set<String> conflictingGroups = onlyGroups.stream()
                .filter(excludedGroups::contains)
                .collect(Collectors.toSet());

        if (!conflictingGroups.isEmpty()) {
            logger.warn(
                    "Dependency groups {} are present in both 'detect.uv.dependency.groups.only' and 'detect.uv.dependency.groups.excluded'. "
                    + "The exclusion setting takes precedence; these groups will be excluded.",
                    conflictingGroups
            );
        }

        Set<String> effectiveOnlyGroups = onlyGroups.stream()
                .filter(group -> !excludedGroups.contains(group))
                .collect(Collectors.toSet());

        if (effectiveOnlyGroups.isEmpty()) {
            return false;
        }

        for (String group : effectiveOnlyGroups) {
            arguments.add(ONLY_GROUP_FLAG);
            arguments.add(group);
        }

        return true;
    }

    private void addDefaultGroupArguments(List<String> arguments, Set<String> excludedGroups) {
        arguments.add(ALL_GROUPS_FLAG);

        for (String group : excludedGroups) {
            arguments.add(NO_GROUP_FLAG);
            arguments.add(group);
        }
    }
}
