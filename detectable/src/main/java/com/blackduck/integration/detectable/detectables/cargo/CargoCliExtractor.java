package com.blackduck.integration.detectable.detectables.cargo;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.cargo.parse.CargoTomlParser;
import com.blackduck.integration.detectable.detectables.cargo.transform.CargoDependencyGraphTransformer;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.executable.ExecutableOutput;
import com.blackduck.integration.util.NameVersion;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Map;
import java.util.EnumMap;

public class CargoCliExtractor {
    private static final List<String> CARGO_TREE_COMMAND = Arrays.asList("tree", "--no-dedupe", "--prefix", "depth");
    private final DetectableExecutableRunner executableRunner;
    private final CargoDependencyGraphTransformer cargoDependencyTransformer;
    private final CargoTomlParser cargoTomlParser;

    public CargoCliExtractor(DetectableExecutableRunner executableRunner, CargoDependencyGraphTransformer cargoDependencyTransformer, CargoTomlParser cargoTomlParser) {
        this.executableRunner = executableRunner;
        this.cargoDependencyTransformer = cargoDependencyTransformer;
        this.cargoTomlParser = cargoTomlParser;
    }

    public Extraction extract(File directory, ExecutableTarget cargoExe, File cargoTomlFile, CargoDetectableOptions cargoDetectableOptions) throws ExecutableFailedException, IOException {
        List<String> fullTreeCommand = new ArrayList<>(CARGO_TREE_COMMAND);
        addEdgeExclusions(fullTreeCommand, cargoDetectableOptions);

        List<String> fullTreeOutput = runCargoTreeCommand(directory, cargoExe, fullTreeCommand);

        boolean excludeNormal = Optional.ofNullable(cargoDetectableOptions.getDependencyTypeFilter())
            .orElse(EnumListFilter.excludeNone())
            .shouldExclude(CargoDependencyType.NORMAL);

        if (excludeNormal) {
            fullTreeOutput = handleNormalDependencyExclusion(directory, cargoExe, fullTreeOutput);
        }

        DependencyGraph graph = cargoDependencyTransformer.transform(fullTreeOutput);

        Optional<NameVersion> projectNameVersion = Optional.empty();
        if (cargoTomlFile != null) {
            String cargoTomlContents = FileUtils.readFileToString(cargoTomlFile, StandardCharsets.UTF_8);
            projectNameVersion = cargoTomlParser.parseNameVersionFromCargoToml(cargoTomlContents);
        }

        CodeLocation codeLocation = new CodeLocation(graph);

        return new Extraction.Builder()
            .success(codeLocation)
            .nameVersionIfPresent(projectNameVersion)
            .build();
    }

    private List<String> runCargoTreeCommand(File directory, ExecutableTarget cargoExe, List<String> commandArgs) throws ExecutableFailedException {
        ExecutableOutput output = executableRunner.executeSuccessfully(
            ExecutableUtils.createFromTarget(directory, cargoExe, commandArgs)
        );
        return output.getStandardOutputAsList();
    }

    private List<String> handleNormalDependencyExclusion(File directory, ExecutableTarget cargoExe, List<String> fullTreeOutput) throws ExecutableFailedException {
        List<String> normalOnlyCommand = new ArrayList<>(CARGO_TREE_COMMAND);
        normalOnlyCommand.add("--edges");
        normalOnlyCommand.add("normal");

        List<String> normalTreeOutput = runCargoTreeCommand(directory, cargoExe, normalOnlyCommand);

        return diffExcludeNormal(fullTreeOutput, normalTreeOutput);
    }

    /**
     * Performs an sdiff-like merge: skips lines from fullTreeOutput that also appear
     * in the same position in normalTreeOutput.
     */
    private List<String> diffExcludeNormal(List<String> fullTreeOutput, List<String> normalTreeOutput) {
        List<String> result = new ArrayList<>();
        int i = 0;
        int j = 0;

        while (i < fullTreeOutput.size() && j < normalTreeOutput.size()) {
            String fullLine = fullTreeOutput.get(i);
            String normalLine = normalTreeOutput.get(j);

            if (fullLine.equals(normalLine)) {
                // This is a normal dependency line; skip it
                i++;
                j++;
            } else {
                // Keep from full tree; advance only full tree index
                result.add(fullLine);
                i++;
            }
        }

        // Add any remaining lines from the full tree
        while (i < fullTreeOutput.size()) {
            result.add(fullTreeOutput.get(i));
            i++;
        }

        return result;
    }

    private void addEdgeExclusions(List<String> cargoTreeCommand, CargoDetectableOptions options) {
        Map<CargoDependencyType, String> exclusionMap = new EnumMap<>(CargoDependencyType.class);
        exclusionMap.put(CargoDependencyType.BUILD, "no-build");
        exclusionMap.put(CargoDependencyType.DEV, "no-dev");
        exclusionMap.put(CargoDependencyType.PROC_MACRO, "no-proc-macro");

        List<String> exclusions = new LinkedList<>();
        for (Map.Entry<CargoDependencyType, String> entry : exclusionMap.entrySet()) {
            if (options.getDependencyTypeFilter().shouldExclude(entry.getKey())) {
                exclusions.add(entry.getValue());
            }
        }

        if (!exclusions.isEmpty()) {
            cargoTreeCommand.add("--edges");
            cargoTreeCommand.add(String.join(",", exclusions));
        }
    }
}
