package com.blackduck.integration.detectable.detectables.pnpm.functional;

import java.io.File;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.PnpmLockExtractor;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.PnpmLockOptions;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmDependencyType;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.process.PnpmLinkedPackageResolver;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.process.PnpmLockYamlParserInitial;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.PackageJsonFiles;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.PackageJsonReader;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.util.FunctionalTestFiles;

public class PnpmLockExtractorTest {
    @Test
    public void testNoFailureOnNullPackageJsonv5() {
        PackageJsonFiles packageJsonFiles = new PackageJsonFiles(new PackageJsonReader(new Gson()));
        EnumListFilter<PnpmDependencyType> dependencyTypeFilter = EnumListFilter.excludeNone();
        PnpmLockOptions pnpmLockOptions = new PnpmLockOptions(dependencyTypeFilter, Collections.emptyList(), Collections.emptyList());
        
        PnpmLockExtractor extractor = new PnpmLockExtractor(new PnpmLockYamlParserInitial(pnpmLockOptions), packageJsonFiles);

        File pnpmLockYaml = FunctionalTestFiles.asFile("/pnpm/v5/pnpm-lock.yaml");
        Extraction extraction = extractor.extract(pnpmLockYaml, null, new PnpmLinkedPackageResolver(new File(""), packageJsonFiles));
        Assertions.assertTrue(extraction.isSuccess());
    }
    
    @Test
    public void testNoFailureOnNullPackageJsonv6() {
        PackageJsonFiles packageJsonFiles = new PackageJsonFiles(new PackageJsonReader(new Gson()));
        EnumListFilter<PnpmDependencyType> dependencyTypeFilter = EnumListFilter.excludeNone();
        PnpmLockOptions pnpmLockOptions = new PnpmLockOptions(dependencyTypeFilter, Collections.emptyList(), Collections.emptyList());
        
        PnpmLockExtractor extractor = new PnpmLockExtractor(new PnpmLockYamlParserInitial(pnpmLockOptions), packageJsonFiles);

        File pnpmLockYaml = FunctionalTestFiles.asFile("/pnpm/v6/pnpm-lock.yaml");
        Extraction extraction = extractor.extract(pnpmLockYaml, null, new PnpmLinkedPackageResolver(new File(""), packageJsonFiles));
        Assertions.assertTrue(extraction.isSuccess());
    }
    
    @Test
    public void testNoFailureOnNullPackageJsonv9() {
        PackageJsonFiles packageJsonFiles = new PackageJsonFiles(new PackageJsonReader(new Gson()));
        EnumListFilter<PnpmDependencyType> dependencyTypeFilter = EnumListFilter.excludeNone();
        PnpmLockOptions pnpmLockOptions = new PnpmLockOptions(dependencyTypeFilter, Collections.emptyList(), Collections.emptyList());
        
        PnpmLockExtractor extractor = new PnpmLockExtractor(new PnpmLockYamlParserInitial(pnpmLockOptions), packageJsonFiles);

        File pnpmLockYaml = FunctionalTestFiles.asFile("/pnpm/v9/pnpm-lock.yaml");
        Extraction extraction = extractor.extract(pnpmLockYaml, null, new PnpmLinkedPackageResolver(new File(""), packageJsonFiles));
        Assertions.assertTrue(extraction.isSuccess());
    }
}
