package com.blackduck.integration.detectable.detectables.poetry.functional;

import java.io.IOException;
import java.nio.file.Paths;

import com.blackduck.integration.detectable.functional.DetectableFunctionalTest;

public class Poetry2TestHelper {
    public static void addPyProjectTomlFile(DetectableFunctionalTest test) throws IOException {
        test.addFile(
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
            "numpy = \"^1.21.0\""
        );
    }

    public static void addPoetryLockFile(DetectableFunctionalTest test) throws IOException {
        test.addFile(
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
}
