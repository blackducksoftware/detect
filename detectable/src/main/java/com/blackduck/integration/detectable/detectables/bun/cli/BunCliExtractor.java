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
    // Fallback path: fires when bun.lock is absent but a compatible lockfile is present
    // (package-lock.json, yarn.lock, or pnpm-lock.yaml). Bun auto-migrates these formats.
    //
    // Known limitation: Bun's flat/hoisted node_modules layout causes packages shared by
    // multiple parents to appear ONLY at level 0 in the tree output — their edges to logical
    // parents are not repeated. Filtering them from the root set would orphan their entire
    // subtree (BDIO has no "transitive root" concept). Hoisted transitives are therefore
    // labeled as direct deps in the SBOM. The Bun Lockfile Detector provides accurate
    // classification when bun.lock is present.
    private static final List<String> BUN_LIST_ALL_COMMAND = Arrays.asList("pm", "list", "--all");

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
            List<String> allLines = runBunListAll(projectDir, bunExe);
            DependencyGraph graph = bunCliParser.parse(allLines);

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

    private List<String> runBunListAll(File directory, ExecutableTarget bunExe) throws ExecutableFailedException {
        ExecutableOutput output = executableRunner.executeSuccessfully(
            ExecutableUtils.createFromTarget(directory, bunExe, BUN_LIST_ALL_COMMAND)
        );
        return output.getStandardOutputAsList();
    }
}
