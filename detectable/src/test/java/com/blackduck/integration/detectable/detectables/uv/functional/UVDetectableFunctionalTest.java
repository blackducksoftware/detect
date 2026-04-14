package com.blackduck.integration.detectable.detectables.uv.functional;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.resolver.UVResolver;
import com.blackduck.integration.detectable.detectables.uv.UVDetectorOptions;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.functional.DetectableFunctionalTest;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;
import com.blackduck.integration.executable.ExecutableOutput;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;

public class UVDetectableFunctionalTest extends DetectableFunctionalTest {

    protected UVDetectableFunctionalTest() throws IOException {
        super("uv");
    }

    @Override
    protected void setup() throws IOException {
        addFile(Paths.get("pyproject.toml"),
                "[project]\n" +
                        "name = \"nvidia-nat\"\n" +
                        "dynamic = [\"version\"]\n" +
                        "dependencies = [\n" +
                        "  # `~=0.1.3.5`.\n" +
                        "  # Keep sorted!!!\n" +
                        "  \"aioboto3>=11.0.0\",\n" +
                        "  \"authlib~=1.3.1\",\n" +
                        "  \"click~=8.1\",\n" +
                        "]\n" +
                        "[tool.uv]\n" +
                        "managed = true\n" +
                        "config-settings = { editable_mode = \"compat\" }");


        ExecutableOutput uvTreeDependencyOutput = createStandardOutput("nvidia-nat-zep-cloud\n" +
                "├── nvidia-nat\n" +
                "│   ├── aioboto3 v15.1.0\n" +
                "│   │   ├── aiobotocore[boto3] v2.24.0\n" +
                "│   │   │   ├── aiohttp v3.12.15\n" +
                "│   │   │   │   ├── aiohappyeyeballs v2.6.1\n" +
                "│   │   │   │   ├── aiosignal v1.4.0\n" +
                "│   │   │   │   │   ├── frozenlist v1.7.0\n" +
                "│   │   │   │   │   └── typing-extensions v4.14.1\n" +
                "│   │   │   │   ├── attrs v25.3.0\n" +
                "│   │   │   │   ├── frozenlist v1.7.0\n" +
                "│   │   │   │   ├── multidict v6.6.4\n" +
                "│   │   │   │   ├── propcache v0.3.2\n" +
                "│   │   │   │   └── yarl v1.20.1\n" +
                "│   │   │   │       ├── idna v3.10\n" +
                "│   │   │   │       ├── multidict v6.6.4\n" +
                "│   │   │   │       └── propcache v0.3.2\n" +
                "│   │   │   ├── aioitertools v0.12.0\n" +
                "│   │   │   ├── botocore v1.39.11\n" +
                "│   │   │   │   ├── jmespath v1.0.1\n" +
                "│   │   │   │   ├── python-dateutil v2.9.0.post0\n" +
                "│   │   │   │   │   └── six v1.17.0\n" +
                "├── nvidia-nat-all (extra: all)\n" +
                "│   │   ├── gunicorn v23.0.0\n" +
                "│   │   │   └── packaging v25.0\n" +
                "│   │   ├── nvidia-nat (*)\n" +
                "│   │   ├── nvidia-nat-agno (*)\n" +
                "│   │   ├── nvidia-nat-crewai\n" +
                "│   │   │   ├── crewai v0.95.0\n" +
                "│   │   │   │   ├── appdirs v1.4.4\n" +
                "│   │   │   │   ├── auth0-python v4.10.0\n" +
                "│   │   │   │   │   ├── aiohttp v3.12.15 (*)\n" +
                "│   │   │   │   │   ├── cryptography v44.0.3 (*)");

        addExecutableOutput(uvTreeDependencyOutput, new File("uv").getAbsolutePath(), "tree", "--no-dedupe");
    }

    @NotNull
    @Override
    public Detectable create(@NotNull DetectableEnvironment detectableEnvironment) {
        class UVResolverTest implements UVResolver {
            @Override
            public ExecutableTarget resolveUV() throws DetectableException {
                return ExecutableTarget.forFile(new File("uv"));
            }
        }

        return detectableFactory.createUVBuildDetectable(detectableEnvironment, new UVResolverTest(), new UVDetectorOptions(Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));
    }

    @Override
    public void assertExtraction(@NotNull Extraction extraction) {
        Assertions.assertEquals(1, extraction.getCodeLocations().size(), "A code location should have been generated.");

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.PYPI, extraction.getCodeLocations().get(0).getDependencyGraph());
        graphAssert.hasRootSize(2);

        graphAssert.hasRootDependency("nvidia-nat-all", "defaultVersion");
        graphAssert.hasDependency("botocore", "1.39.11");
        graphAssert.hasRootDependency("nvidia-nat", "defaultVersion");
        graphAssert.hasDependency("nvidia-nat-crewai", "defaultVersion");
        graphAssert.hasDependency("aiohttp", "3.12.15");
        graphAssert.hasDependency("packaging", "25.0");
    }
}
