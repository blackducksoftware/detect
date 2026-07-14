package com.blackduck.integration.detectable.detectables.pnpm.unit;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
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

/**
 * Tests that pnpm-lock.yaml files containing emojis and non-ASCII content
 * are parsed correctly when read with explicit UTF-8 encoding
 * (InputStreamReader + StandardCharsets.UTF_8).
 */
public class PnpmLockYamlParserUtf8Test {

    private List<CodeLocation> parseLockFile(String resourcePath) throws IOException, IntegrationException {
        File pnpmLockYaml = FunctionalTestFiles.asFile(resourcePath);

        EnumListFilter<PnpmDependencyType> dependencyTypeFilter = EnumListFilter.excludeNone();
        PnpmLockOptions pnpmLockOptions = new PnpmLockOptions(dependencyTypeFilter, Collections.emptyList(), Collections.emptyList());

        PnpmLockYamlParserInitial parser = new PnpmLockYamlParserInitial(pnpmLockOptions);
        PnpmLinkedPackageResolver linkedPackageResolver = new PnpmLinkedPackageResolver(
            pnpmLockYaml.getParentFile(),
            new PackageJsonFiles(new PackageJsonReader(new Gson()))
        );

        return parser.parse(pnpmLockYaml, new NameVersion("project", "1.0.0"), linkedPackageResolver);
    }

    @Test
    public void testParseV9WithEmojiComments() throws IOException, IntegrationException {
        // YAML contains emoji characters (🚀🎉✅❌🔥💡) in comments
        List<CodeLocation> codeLocations = parseLockFile("/pnpm/unicode/v9-emoji-comments/pnpm-lock.yaml");

        Assertions.assertNotNull(codeLocations, "Code locations should not be null when parsing YAML with emoji comments");
        Assertions.assertFalse(codeLocations.isEmpty(), "Should produce at least one code location");
    }

    @Test
    public void testParseV5WithAccentedComments() throws IOException, IntegrationException {
        // YAML contains accented (Ünïcödé, Ñoño, résumé, naïveté) and Greek characters in comments
        List<CodeLocation> codeLocations = parseLockFile("/pnpm/unicode/v5-accented/pnpm-lock.yaml");

        Assertions.assertNotNull(codeLocations, "Code locations should not be null when parsing YAML with accented comments");
        Assertions.assertFalse(codeLocations.isEmpty(), "Should produce at least one code location");
    }

}

