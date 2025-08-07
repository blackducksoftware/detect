package com.blackduck.integration.detectable.detectables.poetry.functional;

import java.io.IOException;
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

public class Poetry2DetectableTest extends DetectableFunctionalTest {

    Poetry2DetectableTest() throws IOException {
        super("poetry2");
    }

    @Override
    protected void setup() throws IOException {
        Poetry2TestHelper.addPyProjectTomlFile(this);
        Poetry2TestHelper.addPoetryLockFile(this);
    }

    @NotNull
    @Override
    public Detectable create(@NotNull DetectableEnvironment detectableEnvironment) {
        return detectableFactory.createPoetryDetectable(detectableEnvironment, new PoetryOptions(Collections.emptyList()));
    }

    @Override
    public void assertExtraction(@NotNull Extraction extraction) {
        Assertions.assertEquals("my-project", extraction.getProjectName());
        Assertions.assertEquals("0.1.0", extraction.getProjectVersion());

        Assertions.assertEquals(1, extraction.getCodeLocations().size());

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.PYPI, extraction.getCodeLocations().get(0).getDependencyGraph());

        // Should have dependencies from both [project] section and [tool.poetry.dev.dependencies] section
        graphAssert.hasRootSize(6);
        
        // Dependencies from [project] section (Poetry 2.x PEP 621 format)
        graphAssert.hasRootDependency("requests", "2.28.1");
        graphAssert.hasRootDependency("click", "8.1.3");
        graphAssert.hasRootDependency("django", "4.1.0");
        graphAssert.hasRootDependency("pytest-cov", "3.0.0");
        graphAssert.hasRootDependency("python-dotenv", "0.20.0");
        
        // Dependencies from [tool.poetry.dev.dependencies] section
        graphAssert.hasRootDependency("numpy", "1.23.2");
    }
}