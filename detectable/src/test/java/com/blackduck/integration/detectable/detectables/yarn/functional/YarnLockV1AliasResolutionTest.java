package com.blackduck.integration.detectable.detectables.yarn.functional;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.yarn.YarnDependencyType;
import com.blackduck.integration.detectable.detectables.yarn.YarnLockOptions;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.functional.DetectableFunctionalTest;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;

/**
 * IDETECT-5353: Yarn 1 has no "resolution:", so an npm alias is reported as the real package named in its "resolved" URL.
 */
public class YarnLockV1AliasResolutionTest extends DetectableFunctionalTest {

    public YarnLockV1AliasResolutionTest() throws IOException {
        super("yarn");
    }

    @Override
    protected void setup() throws IOException {
        addFile(
            Paths.get("yarn.lock"),
            "# yarn lockfile v1",
            "",
            "",
            "\"string-width-alias@npm:string-width@4.2.3\":",
            "  version \"4.2.3\"",
            "  resolved \"https://registry.yarnpkg.com/string-width/-/string-width-4.2.3.tgz#269c7117\"",
            "  dependencies:",
            "    emoji-regex \"^8.0.0\"",
            "",
            "\"scoped-alias@npm:@types/react-select@3.0.23\":",
            "  version \"3.0.23\"",
            "  resolved \"https://registry.yarnpkg.com/@types/react-select/-/react-select-3.0.23.tgz#67fa25e3\"",
            "",
            "emoji-regex@^8.0.0:",
            "  version \"8.0.0\"",
            "  resolved \"https://registry.yarnpkg.com/emoji-regex/-/emoji-regex-8.0.0.tgz#e818fd69\"",
            "",
            "left-pad@1.3.0:",
            "  version \"1.3.0\"",
            "  resolved \"https://registry.yarnpkg.com/left-pad/-/left-pad-1.3.0.tgz#5b8a3a77\""
        );

        addFile(
            Paths.get("package.json"),
            "{",
            "  \"name\": \"test-app\",",
            "  \"version\": \"1.0.0\",",
            "  \"dependencies\": {",
            "    \"string-width-alias\": \"npm:string-width@4.2.3\",",
            "    \"scoped-alias\": \"npm:@types/react-select@3.0.23\",",
            "    \"left-pad\": \"1.3.0\"",
            "  }",
            "}"
        );
    }

    @NotNull
    @Override
    public Detectable create(@NotNull DetectableEnvironment detectableEnvironment) {
        return detectableFactory.createYarnLockDetectable(
            detectableEnvironment,
            new YarnLockOptions(EnumListFilter.fromExcluded(YarnDependencyType.NON_PRODUCTION), new ArrayList<>(0), new ArrayList<>(0), false)
        );
    }

    @Override
    public void assertExtraction(@NotNull Extraction extraction) {
        Assertions.assertEquals(1, extraction.getCodeLocations().size());
        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.NPMJS, extraction.getCodeLocations().get(0).getDependencyGraph());

        graphAssert.hasNoDependency("string-width-alias", "4.2.3");
        graphAssert.hasNoDependency("scoped-alias", "3.0.23");

        graphAssert.hasRootDependency("string-width", "4.2.3");
        graphAssert.hasParentChildRelationship("string-width", "4.2.3", "emoji-regex", "8.0.0");
        graphAssert.hasRootDependency("@types/react-select", "3.0.23");

        // an ordinary Yarn 1 entry is unchanged
        graphAssert.hasRootDependency("left-pad", "1.3.0");
    }
}
