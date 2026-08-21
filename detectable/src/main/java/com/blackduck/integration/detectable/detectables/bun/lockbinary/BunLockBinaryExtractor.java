package com.blackduck.integration.detectable.detectables.bun.lockbinary;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.NullSafePackageJson;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.PackageJsonFiles;
import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLock;
import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLockResult;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.executable.ExecutableOutput;

public class BunLockBinaryExtractor {
    private static final List<String> BUN_LOCKB_COMMAND = Collections.singletonList(BunLockBinaryDetectable.BUN_LOCKB_FILENAME);

    private final DetectableExecutableRunner executableRunner;
    private final BunLockBinaryParser bunLockBinaryParser;
    private final BunLockBinaryTransformer bunLockBinaryTransformer;
    private final PackageJsonFiles packageJsonFiles;

    public BunLockBinaryExtractor(
        DetectableExecutableRunner executableRunner,
        BunLockBinaryParser bunLockBinaryParser,
        BunLockBinaryTransformer bunLockBinaryTransformer,
        PackageJsonFiles packageJsonFiles
    ) {
        this.executableRunner = executableRunner;
        this.bunLockBinaryParser = bunLockBinaryParser;
        this.bunLockBinaryTransformer = bunLockBinaryTransformer;
        this.packageJsonFiles = packageJsonFiles;
    }

    public Extraction extract(File projectDir, File packageJsonFile, ExecutableTarget bunExe) {
        try {
            List<String> outputLines = runBunLockBinaryCommand(projectDir, bunExe);

            YarnLock yarnLock = bunLockBinaryParser.parseBunLockBinary(outputLines);
            NullSafePackageJson rootPackageJson = packageJsonFiles.read(packageJsonFile);
            YarnLockResult yarnLockResult = new YarnLockResult(rootPackageJson, yarnLock);
            List<CodeLocation> codeLocations = bunLockBinaryTransformer.generateCodeLocations(yarnLockResult, new ArrayList<>());

            return new Extraction.Builder()
                .projectName(rootPackageJson.getName().orElse(null))
                .projectVersion(rootPackageJson.getVersion().orElse(null))
                .success(codeLocations)
                .build();
        } catch (Exception e) {
            return new Extraction.Builder().exception(e).build();
        }
    }

    private List<String> runBunLockBinaryCommand(File directory, ExecutableTarget bunExe) throws ExecutableFailedException {
        ExecutableOutput output = executableRunner.executeSuccessfully(
            ExecutableUtils.createFromTarget(directory, bunExe, BUN_LOCKB_COMMAND)
        );
        return output.getStandardOutputAsList();
    }
}
