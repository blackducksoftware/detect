package com.blackduck.integration.detectable.detectables.pnpm.lockfile.process;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detectable.detectables.yarn.packagejson.NullSafePackageJson;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.PackageJsonFiles;

public class PnpmLinkedPackageResolver {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final File projectRoot;
    private final PackageJsonFiles packageJsonFiles;

    public PnpmLinkedPackageResolver(File projectRoot, PackageJsonFiles packageJsonFiles) {
        this.projectRoot = projectRoot;
        this.packageJsonFiles = packageJsonFiles;
    }

    @Nullable
    public String resolveVersionOfLinkedPackage(@Nullable String reportingProjectPackagePath, String relativePathToLinkedPackage) {
        File reportingPackage;
        if (reportingProjectPackagePath != null) {
            reportingProjectPackagePath = reportingProjectPackagePath.replaceAll("^(\\.\\./|\\./)++", "");
            reportingPackage = new File(projectRoot, reportingProjectPackagePath);
        } else {
            reportingPackage = projectRoot;
        }

        relativePathToLinkedPackage = relativePathToLinkedPackage.replaceAll("^(\\.\\./|\\./)++", "");

        File linkedPackage = new File(projectRoot, relativePathToLinkedPackage);
        File packageJsonFile = new File(linkedPackage, "package.json");

        if (packageJsonFile.exists()) {
            try {
                NullSafePackageJson packageJson = packageJsonFiles.read(packageJsonFile);
                return packageJson.getVersionString();
            } catch (IOException e) {
                logger.debug(String.format("Unable to parse package.json: %s", packageJsonFile.getAbsolutePath()));
                return null;
            }
        } else {
            linkedPackage = new File(reportingPackage, relativePathToLinkedPackage);
            packageJsonFile = new File(linkedPackage, "package.json");

            if (!packageJsonFile.exists()) {
                logger.debug(String.format("Unable to resolve version for linked package: %s", linkedPackage));
                return null;
            }

            try {
                NullSafePackageJson packageJson = packageJsonFiles.read(packageJsonFile);
                return packageJson.getVersionString();
            } catch (IOException e) {
                logger.debug(String.format("Unable to parse package.json: %s", packageJsonFile.getAbsolutePath()));
                return null;
            }
        }


    }

}
