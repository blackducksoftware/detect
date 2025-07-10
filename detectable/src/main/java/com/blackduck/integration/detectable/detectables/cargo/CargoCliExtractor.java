package com.blackduck.integration.detectable.detectables.cargo;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
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

        // Adding the --edges exclusion option based on the CargoDetectableOptions to the cargo tree command
        // This excludes build, dev, and proc-macro dependencies if specified
        // Normal dependencies are handled separately later as a special case
        addEdgeExclusions(fullTreeCommand, cargoDetectableOptions);

        List<String> fullTreeOutput = runCargoTreeCommand(directory, cargoExe, fullTreeCommand);

        // Checking whether normal dependencies are to be excluded
        // if so, collect normal dependencies and subtract from the above tree
        boolean excludeNormal = cargoDetectableOptions.getDependencyTypeFilter().shouldExclude(CargoDependencyType.NORMAL);
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

        return subtractNormalDependencies(fullTreeOutput, normalTreeOutput);
    }

    private List<String> subtractNormalDependencies(List<String> fullTreeOutput, List<String> normalTreeOutput) {
        List<String> result = new ArrayList<>(fullTreeOutput);
        for (String normalLine : normalTreeOutput) {
            int index = result.indexOf(normalLine);
            if (index != -1) {
                result.remove(index);
            }
        }
        return result;
    }

    private void addEdgeExclusions(List<String> cargoTreeCommand, CargoDetectableOptions options) {
        Map<CargoDependencyType, String> exclusionMap = new EnumMap<>(CargoDependencyType.class);
        exclusionMap.put(CargoDependencyType.BUILD, "no-build");
        exclusionMap.put(CargoDependencyType.DEV, "no-dev");
        exclusionMap.put(CargoDependencyType.PROC_MACRO, "no-proc-macro");

        List<String> exclusions = new ArrayList<>();
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
