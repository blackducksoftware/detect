package com.blackduck.integration.detectable.detectables.rush.functional;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.common.util.finder.SimpleFileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.npm.lockfile.parse.NpmLockFileProjectIdTransformer;
import com.blackduck.integration.detectable.detectables.npm.lockfile.parse.NpmLockfileGraphTransformer;
import com.blackduck.integration.detectable.detectables.npm.lockfile.parse.NpmLockfilePackager;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.PnpmLockOptions;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.process.PnpmLockYamlParserInitial;
import com.blackduck.integration.detectable.detectables.rush.RushDetectable;
import com.blackduck.integration.detectable.detectables.rush.RushExtractor;
import com.blackduck.integration.detectable.detectables.rush.RushOptions;
import com.blackduck.integration.detectable.detectables.rush.parse.RushJsonParser;
import com.blackduck.integration.detectable.detectables.rush.parse.RushLockFileParser;
import com.blackduck.integration.detectable.detectables.rush.util.RushTestHelper;
import com.blackduck.integration.detectable.detectables.yarn.YarnLockOptions;
import com.blackduck.integration.detectable.detectables.yarn.YarnDependencyType;
import com.blackduck.integration.detectable.detectables.yarn.YarnPackager;
import com.blackduck.integration.detectable.detectables.yarn.YarnTransformer;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.PackageJsonFiles;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.PackageJsonReader;
import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLockParser;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.functional.DetectableFunctionalTest;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

public class RushSubspacesFunctionalTest extends DetectableFunctionalTest {

    Gson gson = new GsonBuilder().setPrettyPrinting().setLenient().create();

    protected RushSubspacesFunctionalTest() throws IOException {
        super("rush-subspaces");
    }

    @Override
    protected void setup() throws IOException {
        addFile(Paths.get("rush.json"),
                "{\n" +
                        "  \"$schema\": \"https://developer.microsoft.com/json-schemas/rush/v5/rush.schema.json\",\n" +
                        "  \"rushVersion\": \"5.82.0\",\n" +
                        "  \"pnpmVersion\": \"8.6.0\",\n" +
                        "  \"nodeSupportedVersionRange\": \">=16.0.0 <21.0.0\",\n" +
                        "  \"subspacesEnabled\": true,\n" +
                        "  \"projects\": [\n" +
                        "    {\n" +
                        "      \"packageName\": \"@workspace/frontend\",\n" +
                        "      \"projectFolder\": \"apps/frontend\",\n" +
                        "      \"reviewCategory\": \"production\",\n" +
                        "      \"tags\": [\"frontend\"]\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"packageName\": \"@workspace/backend\",\n" +
                        "      \"projectFolder\": \"apps/backend\",\n" +
                        "      \"reviewCategory\": \"production\",\n" +
                        "      \"tags\": [\"backend\"]\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"packageName\": \"@workspace/shared\",\n" +
                        "      \"projectFolder\": \"libs/shared\",\n" +
                        "      \"reviewCategory\": \"production\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}");

        addFile(Paths.get("apps", "frontend", "package.json"),
                "{\n" +
                        "  \"name\": \"@workspace/frontend\",\n" +
                        "  \"version\": \"4.0.0\",\n" +
                        "  \"dependencies\": {\n" +
                        "    \"vue\": \"^3.3.4\",\n" +
                        "    \"vue-router\": \"^4.2.4\"\n" +
                        "  }\n" +
                        "}");

        addFile(Paths.get("apps", "backend", "package.json"),
                "{\n" +
                        "  \"name\": \"@workspace/backend\",\n" +
                        "  \"version\": \"4.0.0\",\n" +
                        "  \"dependencies\": {\n" +
                        "    \"fastify\": \"^4.21.0\"\n" +
                        "  }\n" +
                        "}");

        addFile(Paths.get("libs", "shared", "package.json"),
                "{\n" +
                        "  \"name\": \"@workspace/shared\",\n" +
                        "  \"version\": \"4.0.0\",\n" +
                        "  \"dependencies\": {\n" +
                        "    \"zod\": \"^3.22.2\"\n" +
                        "  }\n" +
                        "}");

        addFile(Paths.get("common", "config", "subspaces", "default", "pnpm-lock.yaml"),
                "lockfileVersion: 6.0\n" +
                        "\n" +
                        "settings:\n" +
                        "  autoInstallPeers: true\n" +
                        "  excludeLinksFromLockfile: false\n" +
                        "\n" +
                        "importers:\n" +
                        "\n" +
                        "  ../../../apps/frontend:\n" +
                        "    dependencies:\n" +
                        "      vue:\n" +
                        "        specifier: ^3.3.4\n" +
                        "        version: 3.3.4\n" +
                        "      vue-router:\n" +
                        "        specifier: ^4.2.4\n" +
                        "        version: 4.2.4(vue@3.3.4)\n" +
                        "\n" +
                        "  ../../../apps/backend:\n" +
                        "    dependencies:\n" +
                        "      fastify:\n" +
                        "        specifier: ^4.21.0\n" +
                        "        version: 4.21.0\n" +
                        "\n" +
                        "  ../../../libs/shared:\n" +
                        "    dependencies:\n" +
                        "      zod:\n" +
                        "        specifier: ^3.22.2\n" +
                        "        version: 3.22.2\n" +
                        "\n" +
                        "packages:\n" +
                        "\n" +
                        "  fastify@4.21.0:\n" +
                        "    resolution: {integrity: sha512-f+4kcr6x9O4yOmyJo0SxNRSctJZvv6D9OB9dP8oeGDPaAPTvt5GKL0MGiNK6aZzGQBm9Doe6CJJRvnB9P2O4XA==}\n" +
                        "    engines: {node: '>=14'}\n" +
                        "    dev: false\n" +
                        "\n" +
                        "  vue@3.3.4:\n" +
                        "    resolution: {integrity: sha512-VTyEYn3yvIeY1Py0WaYGZsXnz3y5UnGi62GjVEqvEGPl6nxbOrxcXzDUEPoEDbYtYby9PADv7cjlHypG4LJSBw==}\n" +
                        "    dev: false\n" +
                        "\n" +
                        "  vue-router@4.2.4(vue@3.3.4):\n" +
                        "    resolution: {integrity: sha512-9PISkmaCO02OzPVOMq2w82ilty6+xJmQrarYZDkjZBfl4RvYAlt4PKnEX21oW4KTtWfa9OuO/b3qxlnkbtrPK0==}\n" +
                        "    peerDependencies:\n" +
                        "      vue: ^3.2.0\n" +
                        "    dependencies:\n" +
                        "      vue: 3.3.4\n" +
                        "    dev: false\n" +
                        "\n" +
                        "  zod@3.22.2:\n" +
                        "    resolution: {integrity: sha512-wvWkphh5WQsJbVk1tbx1l1Ly4yg+XecD+Mq280uBGt9wa5BKSWf4Mhp6GmrkPixhMxmabYY7RbzlwVP32pbGCg==}\n" +
                        "    dev: false\n");
    }

    @NotNull
    @Override
    public Detectable create(@NotNull DetectableEnvironment detectableEnvironment) {
        RushJsonParser rushJsonParser = new RushJsonParser(gson);
        PnpmLockOptions pnpmLockOptions = new PnpmLockOptions(EnumListFilter.excludeNone(), Collections.emptyList(), Collections.emptyList());

        PnpmLockYamlParserInitial pnpmLockYamlParser = new PnpmLockYamlParserInitial(pnpmLockOptions);

        // Create NPM dependencies
        ExternalIdFactory externalIdFactory = new ExternalIdFactory();
        NpmLockfilePackager npmLockfilePackager = RushTestHelper.createNpmPackager(externalIdFactory, gson);


        YarnPackager yarnPackager = RushTestHelper.createYarnPackager(externalIdFactory);
        YarnLockParser yarnLockParser = RushTestHelper.createYarnLockParser();

        // Create shared dependencies
        PackageJsonFiles packageJsonFiles = new PackageJsonFiles(new PackageJsonReader(gson));
        RushOptions rushOptions = new RushOptions(Collections.emptyList(), Collections.emptyList());

        RushLockFileParser rushLockFileParser = new RushLockFileParser(
                npmLockfilePackager,
                pnpmLockYamlParser,
                yarnPackager,
                packageJsonFiles,
                yarnLockParser,
                rushOptions
        );
        RushExtractor rushExtractor = new RushExtractor(rushJsonParser, rushLockFileParser);

        return new RushDetectable(detectableEnvironment, new SimpleFileFinder(), rushExtractor);
    }

    @Override
    public void assertExtraction(@NotNull Extraction extraction) {
        Assertions.assertEquals(3, extraction.getCodeLocations().size(), "A code location should have been generated.");

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.NPMJS, extraction.getCodeLocations().get(0).getDependencyGraph());
        
        graphAssert.hasDependency("vue", "3.3.4");
        graphAssert.hasDependency("vue-router", "4.2.4");
    }
}