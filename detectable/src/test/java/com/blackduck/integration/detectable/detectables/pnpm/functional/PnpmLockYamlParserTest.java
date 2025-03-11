package com.blackduck.integration.detectable.detectables.pnpm.functional;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.MultipleFailuresError;

import com.google.gson.Gson;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.PnpmLockOptions;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmDependencyType;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.process.PnpmLinkedPackageResolver;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.process.PnpmLockYamlParserInitial;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.PackageJsonFiles;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.PackageJsonReader;
import com.blackduck.integration.detectable.util.FunctionalTestFiles;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.util.NameVersion;

public class PnpmLockYamlParserTest {

    @Test
    public void testParsev5() throws IOException, IntegrationException {
        File pnpmLockYaml = FunctionalTestFiles.asFile("/pnpm/v5/pnpm-lock.yaml");
        evaluatePnpmLockYamlParsing(pnpmLockYaml);
    }
    
    @Test
    public void testParsev6() throws IOException, IntegrationException {
        File pnpmLockYaml = FunctionalTestFiles.asFile("/pnpm/v6/pnpm-lock.yaml");
        evaluatePnpmLockYamlParsing(pnpmLockYaml);
    }

    /**
     * At a high level the v5 and v6 yaml files contain the same information just in different
     * formats. Validate that the parsing has removed these differences and that the same
     * code location information is created.
     */
    private void evaluatePnpmLockYamlParsing(File pnpmLockYaml)
            throws IOException, IntegrationException, MultipleFailuresError {
        EnumListFilter<PnpmDependencyType> dependencyTypeFilter = EnumListFilter.excludeNone();
        PnpmLockOptions pnpmLockOptions = new PnpmLockOptions(dependencyTypeFilter, Collections.emptyList(), Collections.emptyList());
        
        PnpmLockYamlParserInitial pnpmLockYamlParser = new PnpmLockYamlParserInitial(pnpmLockOptions);
        PnpmLinkedPackageResolver pnpmLinkedPackageResolver = new PnpmLinkedPackageResolver(
            FunctionalTestFiles.asFile("/pnpm"),
            new PackageJsonFiles(new PackageJsonReader(new Gson()))
        );

        List<CodeLocation> codeLocations = pnpmLockYamlParser.parse(pnpmLockYaml, new NameVersion("project", "version"), pnpmLinkedPackageResolver);
        Assertions.assertEquals(2, codeLocations.size());

        // Did we correctly identify root project package in "importers"?
        Assertions.assertTrue(codeLocations.stream()
            .map(CodeLocation::getExternalId)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .anyMatch(
                externalId -> externalId.getName().equals("project") && externalId.getVersion().equals("version")
            )
        );

        // Do all code locations have a source path?
        Assertions.assertAll(codeLocations.stream()
            .map(codeLocation -> () -> Assertions.assertTrue(codeLocation.getSourcePath().isPresent(), String.format(
                "Expected source path to be present for all code locations. But code location with id %s does not have one set.",
                codeLocation.getExternalId().map(ExternalId::createExternalId).orElse("N/A")
            ))));

        // Did we generate a unique source path for each code location?
        Map<String, List<File>> collect = codeLocations.stream()
            .map(CodeLocation::getSourcePath)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.groupingBy(File::getAbsolutePath));

        Assertions.assertAll(collect.entrySet().stream()
            .map(codeLocationGrouping -> () -> {
                int numberOfCodeLocations = codeLocationGrouping.getValue().size();
                Assertions.assertEquals(
                    1,
                    numberOfCodeLocations,
                    String.format("Expected unique code locations paths. But found %d with that same path of %s", numberOfCodeLocations, codeLocationGrouping.getKey())
                );
            }));
    }
}
