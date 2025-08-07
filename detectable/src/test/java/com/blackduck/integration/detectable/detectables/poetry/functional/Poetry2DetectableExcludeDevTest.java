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

public class Poetry2DetectableExcludeDevTest extends DetectableFunctionalTest {

    Poetry2DetectableExcludeDevTest() throws IOException {
        super("poetry2_exclude_dev");
    }

    @Override
    protected void setup() throws IOException {
        addFile(
            Paths.get("pyproject.toml"),
            "[project]",
            "name = \"my-project\"",
            "version = \"0.1.0\"",
            "description = \"A sample Poetry 2.x project\"",
            "dependencies = [",
            "    \"requests>=2.25.1\",",
            "    \"click[colors]>=8.0.0\",",
            "    \"django (>=3.2.0)\",",
            "    \"pytest-cov; extra == 'test'\",",
            "    \"python-dotenv\"",
            "]",
            "",
            "[tool.poetry.dev.dependencies]",
            "python = \"^3.8\"",
            "numpy = \"^1.21.0\""
        );

        addFile(
            Paths.get("poetry.lock"),
            "[[package]]",
            "name = \"requests\"",
            "python-versions = \"*\"",
            "version = \"2.28.1\"",
            "",
            "[[package]]",
            "name = \"click\"",
            "python-versions = \">=3.7\"",
            "version = \"8.1.3\"",
            "",
            "[[package]]",
            "name = \"django\"",
            "python-versions = \">=3.8\"",
            "version = \"4.1.0\"",
            "",
            "[[package]]",
            "name = \"pytest-cov\"",
            "python-versions = \">=3.6\"",
            "version = \"3.0.0\"",
            "",
            "[[package]]",
            "name = \"python-dotenv\"",
            "python-versions = \">=3.5\"",
            "version = \"0.20.0\"",
            "",
            "[[package]]",
            "name = \"numpy\"",
            "python-versions = \">=3.8\"",
            "version = \"1.23.2\""
        );
    }

    @NotNull
    @Override
    public Detectable create(@NotNull DetectableEnvironment detectableEnvironment) {
        return detectableFactory.createPoetryDetectable(detectableEnvironment, new PoetryOptions(Collections.singletonList("dev")));
    }

    @Override
    public void assertExtraction(@NotNull Extraction extraction) {
        Assertions.assertEquals("my-project", extraction.getProjectName());
        Assertions.assertEquals("0.1.0", extraction.getProjectVersion());

        Assertions.assertEquals(1, extraction.getCodeLocations().size());

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.PYPI, extraction.getCodeLocations().get(0).getDependencyGraph());

        graphAssert.hasRootSize(5);
        
        // Dependencies from [project] section (Poetry 2.x PEP 621 format)
        graphAssert.hasRootDependency("requests", "2.28.1");
        graphAssert.hasRootDependency("click", "8.1.3");
        graphAssert.hasRootDependency("django", "4.1.0");
        graphAssert.hasRootDependency("pytest-cov", "3.0.0");
        graphAssert.hasRootDependency("python-dotenv", "0.20.0");
        
        // Dependencies from [tool.poetry.dev.dependencies]
        graphAssert.hasNoDependency("numpy", "1.23.2");
    }
}