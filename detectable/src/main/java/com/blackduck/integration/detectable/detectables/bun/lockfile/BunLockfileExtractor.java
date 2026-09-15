package com.blackduck.integration.detectable.detectables.bun.lockfile;

import java.io.File;
import java.util.List;

import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunLockResult;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.NullSafePackageJson;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.PackageJsonFiles;
import com.blackduck.integration.detectable.extraction.Extraction;

public class BunLockfileExtractor {
    private final BunLockJsonParser bunLockJsonParser;
    private final BunLockfileTransformer bunLockfileTransformer;
    private final PackageJsonFiles packageJsonFiles;

    public BunLockfileExtractor(BunLockJsonParser bunLockJsonParser, BunLockfileTransformer bunLockfileTransformer, PackageJsonFiles packageJsonFiles) {
        this.bunLockJsonParser = bunLockJsonParser;
        this.bunLockfileTransformer = bunLockfileTransformer;
        this.packageJsonFiles = packageJsonFiles;
    }

    public Extraction extract(File bunLockFile, File packageJsonFile) {
        try {
            NullSafePackageJson packageJson = packageJsonFiles.read(packageJsonFile);
            BunLockResult bunLockResult = bunLockJsonParser.parseBunLock(bunLockFile);
            List<CodeLocation> codeLocations = bunLockfileTransformer.generateCodeLocations(bunLockResult, packageJson);
            return new Extraction.Builder()
                .projectName(packageJson.getNameString())
                .projectVersion(packageJson.getVersionString())
                .success(codeLocations)
                .build();
        } catch (Exception e) {
            return new Extraction.Builder().exception(e).build();
        }
    }
}
