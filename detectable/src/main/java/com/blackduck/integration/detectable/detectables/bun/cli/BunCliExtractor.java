package com.blackduck.integration.detectable.detectables.bun.cli;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.NullSafePackageJson;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.PackageJsonFiles;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.executable.ExecutableOutput;

public class BunCliExtractor {
    // `bun pm list --all` outputs a hierarchical dependency tree: level-0 entries are direct
    // dependencies, level-1+ entries are their transitives nested under their actual parents.
    private static final List<String> BUN_LIST_COMMAND = Arrays.asList("pm", "list", "--all");

    private final DetectableExecutableRunner executableRunner;
    private final BunCliParser bunCliParser;
    private final PackageJsonFiles packageJsonFiles;

    public BunCliExtractor(DetectableExecutableRunner executableRunner, BunCliParser bunCliParser, PackageJsonFiles packageJsonFiles) {
        this.executableRunner = executableRunner;
        this.bunCliParser = bunCliParser;
        this.packageJsonFiles = packageJsonFiles;
    }

    public Extraction extract(File projectDir, File packageJsonFile, ExecutableTarget bunExe) {
        try {
            List<String> outputLines = runBunList(projectDir, bunExe);
            DependencyGraph graph = bunCliParser.parse(outputLines);

            NullSafePackageJson rootPackageJson = packageJsonFiles.read(packageJsonFile);
            return new Extraction.Builder()
                .projectName(rootPackageJson.getName().orElse(null))
                .projectVersion(rootPackageJson.getVersion().orElse(null))
                .success(new CodeLocation(graph))
                .build();
        } catch (Exception e) {
            return new Extraction.Builder().exception(e).build();
        }
    }

    private List<String> runBunList(File directory, ExecutableTarget bunExe) throws ExecutableFailedException {
        ExecutableOutput output = executableRunner.executeSuccessfully(
            ExecutableUtils.createFromTarget(directory, bunExe, BUN_LIST_COMMAND)
        );
        return output.getStandardOutputAsList();
    }
}
