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

public class RushNpmFunctionalTest extends DetectableFunctionalTest {

    Gson gson = new GsonBuilder().setPrettyPrinting().setLenient().create();

    protected RushNpmFunctionalTest() throws IOException {
        super("rush-npm");
    }

    @Override
    protected void setup() throws IOException {
        addFile(Paths.get("rush.json"),
                "{\n" +
                        "  \"rushVersion\": \"5.82.0\",\n" +
                        "  \"npmVersion\": \"8.19.2\",\n" +
                        "  \"nodeSupportedVersionRange\": \">=14.15.0 <19.0.0\",\n" +
                        "  \"projects\": [\n" +
                        "    {\n" +
                        "      \"packageName\": \"@example/api\",\n" +
                        "      \"projectFolder\": \"services/api\",\n" +
                        "      \"reviewCategory\": \"production\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"packageName\": \"@example/utils\",\n" +
                        "      \"projectFolder\": \"packages/utils\",\n" +
                        "      \"reviewCategory\": \"production\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}");

        addFile(Paths.get("services", "api", "package.json"),
                "{\n" +
                        "  \"name\": \"@example/api\",\n" +
                        "  \"version\": \"3.0.0\",\n" +
                        "  \"dependencies\": {\n" +
                        "    \"@example/utils\": \"workspace:*\",\n" +
                        "    \"express\": \"^4.18.2\",\n" +
                        "    \"helmet\": \"^7.0.0\"\n" +
                        "  }\n" +
                        "}");

        addFile(Paths.get("packages", "utils", "package.json"),
                "{\n" +
                        "  \"name\": \"@example/utils\",\n" +
                        "  \"version\": \"3.0.0\",\n" +
                        "  \"dependencies\": {\n" +
                        "    \"moment\": \"^2.29.4\"\n" +
                        "  }\n" +
                        "}");

        addFile(Paths.get("common", "config", "rush", "npm-shrinkwrap.json"),
                "{\n" +
                        "  \"name\": \"@example/monorepo\",\n" +
                        "  \"lockfileVersion\": 2,\n" +
                        "  \"requires\": true,\n" +
                        "  \"packages\": {\n" +
                        "    \"\": {\n" +
                        "      \"name\": \"@example/monorepo\",\n" +
                        "      \"workspaces\": [\n" +
                        "        \"services/*\",\n" +
                        "        \"packages/*\"\n" +
                        "      ]\n" +
                        "    },\n" +
                        "    \"node_modules/express\": {\n" +
                        "      \"version\": \"4.18.2\",\n" +
                        "      \"resolved\": \"https://registry.npmjs.org/express/-/express-4.18.2.tgz\",\n" +
                        "      \"integrity\": \"sha512-5/PsL6iGPdfQ/lKM1UuielYgv3BUoJfz1aUwU9vHZ+J7gyvwdQXFEBIEIaxeGf0GIcreATNyBExtalisDbuMqQ==\",\n" +
                        "      \"dependencies\": {\n" +
                        "        \"accepts\": \"~1.3.8\",\n" +
                        "        \"array-flatten\": \"1.1.1\",\n" +
                        "        \"body-parser\": \"1.20.1\"\n" +
                        "      },\n" +
                        "      \"engines\": {\n" +
                        "        \"node\": \">= 0.10.0\"\n" +
                        "      }\n" +
                        "    },\n" +
                        "    \"node_modules/helmet\": {\n" +
                        "      \"version\": \"7.0.0\",\n" +
                        "      \"resolved\": \"https://registry.npmjs.org/helmet/-/helmet-7.0.0.tgz\",\n" +
                        "      \"integrity\": \"sha512-MveXPCGcMkOmnJpxaf44+t7HTveO3xtrUVr8LZt9KNpxrZC9XO3FrAEwV4KyhPplr4beDQh9qUP3TczB8D96hg==\",\n" +
                        "      \"engines\": {\n" +
                        "        \"node\": \">=16.0.0\"\n" +
                        "      }\n" +
                        "    },\n" +
                        "    \"node_modules/moment\": {\n" +
                        "      \"version\": \"2.29.4\",\n" +
                        "      \"resolved\": \"https://registry.npmjs.org/moment/-/moment-2.29.4.tgz\",\n" +
                        "      \"integrity\": \"sha512-5LC9SOxjSc2HF6vO2CyuTDNivEdoz2IvyJJGj6X8DJ0eFyfszE0QiEd+iXmBvUP3WHxSjFH/vIsA0EN00cgr8w==\",\n" +
                        "      \"engines\": {\n" +
                        "        \"node\": \"*\"\n" +
                        "      }\n" +
                        "    }\n" +
                        "  }\n" +
                        "}");
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
        Assertions.assertEquals(1, extraction.getCodeLocations().size(), "A code location should have been generated.");

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.NPMJS, extraction.getCodeLocations().get(0).getDependencyGraph());
        graphAssert.hasRootSize(3);
        
        graphAssert.hasRootDependency("express", "4.18.2");
        graphAssert.hasDependency("helmet", "7.0.0");
        graphAssert.hasDependency("moment", "2.29.4");
    }
}