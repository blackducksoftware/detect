package com.blackduck.integration.detectable.detectables.poetry.functional;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectables.poetry.PoetryOptions;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.functional.DetectableFunctionalTest;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;

public class PoetryDetectableTest extends DetectableFunctionalTest {

    PoetryDetectableTest() throws IOException {
        super("poetry");
    }

    @Override
    protected void setup() throws IOException {
        addFile(
            Paths.get("pyproject.toml"),
            "[tool.poetry]",
            "name=\"my-proj\"",
            "version=\"1.45\""
        );

        addFile(
            Paths.get("poetry.lock"),
            "[[package]]",
            "category = \"dev\"",
            "name = \"appdirs\"",
            "python-versions = \"*\"",
            "version = \"1.4.3\"\n",
            "",
            "[[package]]",
            "category = \"dev\"",
            "name = \"atomicwrites\"",
            "python-versions = \">=2.7, !=3.0.*, !=3.1.*, !=3.2.*, !=3.3.*\"",
            "version = \"1.4.0\"",
            "",
            "[package.dependencies]",
            "importLib-metadata = \"*\"",
            "",
            "[[package]]",
            "category = \"dev\"",
            "name = \"Importlib_Metadata\"",
            "python-versions = \"!=3.0.*,!=3.1.*,!=3.2.*,!=3.3.*,!=3.4.*,>=2.7\"",
            "version = \"1.6.0\""
        );
    }

    @NotNull
    @Override
    public Detectable create(@NotNull DetectableEnvironment detectableEnvironment) {
        return detectableFactory.createPoetryDetectable(detectableEnvironment, new PoetryOptions(Collections.emptyList()));
    }

    @Override
    public void assertExtraction(@NotNull Extraction extraction) {
        Assertions.assertEquals("my-proj", extraction.getProjectName());;
        Assertions.assertEquals("1.45", extraction.getProjectVersion());

        Assertions.assertEquals(1, extraction.getCodeLocations().size());

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.PYPI, extraction.getCodeLocations().get(0).getDependencyGraph());

        graphAssert.hasRootSize(2);
        graphAssert.hasRootDependency("appdirs", "1.4.3");
        graphAssert.hasRootDependency("atomicwrites", "1.4.0");
        graphAssert.hasParentChildRelationship("atomicwrites", "1.4.0", "importlib-metadata", "1.6.0");
    }
}
