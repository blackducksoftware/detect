package com.blackduck.integration.detectable.detectables.yarn.functional;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.yarn.YarnDependencyType;
import com.blackduck.integration.detectable.detectables.yarn.YarnLockOptions;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.functional.DetectableFunctionalTest;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;

public class YarnLockUnresolvedVersionTest extends DetectableFunctionalTest {

    public YarnLockUnresolvedVersionTest() throws IOException {
        super("yarn");
    }

    @Override
    protected void setup() throws IOException {
        addFile(
                        Paths.get("yarn.lock"),
                        "\"lodash@npm:4.17.4\":",
                        "   version \"4.17.4\"",
                        "   dependencies:",
                        "     semver: \"npm:^6.3.1\"",
                        "     json5: \"npm:^1.2.0\"",
                        "",
                        "\"semver@npm:^7.5.4\":",
                        "   version: 7.5.4",
                        "\"json5@npm:^2.2.3\":",
                        "   version: 2.2.3",
                        "   dependencies:",
                        "     semver: \"npm:2 || 3 || ^5.2.0 \""
                );

        addFile(
                Paths.get("package.json"),
                "{",
                "   \"name\": \"client\",",
                "   \"version\": \"1.2.3\",",
                "   \"private\": true,",
                "   \"license\": \"MIT\",",
                "   \"dependencies\": { ",
                "       \"lodash\": \"4.17.4\",",
                "       \"semver\": \"7.5.4\",",
                "       \"json5\": \"2.2.3\"",
                "   }",
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
        CodeLocation codeLocation = extraction.getCodeLocations().get(0);

        Assertions.assertEquals("client", extraction.getProjectName());
        Assertions.assertEquals("1.2.3", extraction.getProjectVersion());

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.NPMJS, codeLocation.getDependencyGraph());
        graphAssert.hasRootSize(3);
        graphAssert.hasRootDependency("lodash", "4.17.4");
        graphAssert.hasDependency("semver", "7.5.4");
        graphAssert.hasParentChildRelationship("lodash", "4.17.4", "json5", "2.2.3");
        graphAssert.hasParentChildRelationship("json5", "2.2.3", "semver", "7.5.4");
    }
}
