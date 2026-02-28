package com.blackduck.integration.detectable.detectables.conda.functional;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.resolver.CondaResolver;
import com.blackduck.integration.detectable.detectable.executable.resolver.CondaTreeResolver;
import com.blackduck.integration.detectable.detectables.conda.CondaCliDetectableOptions;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.functional.DetectableFunctionalTest;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;
import com.blackduck.integration.executable.ExecutableOutput;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;

public class CondaTreeDetectableTest extends DetectableFunctionalTest {

    public CondaTreeDetectableTest() throws IOException {
        super("conda-tree");
    }

    @Override
    protected void setup() throws IOException {
        addFile("environment.yml");

        ExecutableOutput condaListOutput = createStandardOutput(
                "[",
                "   {\n" +
                    "    \"base_url\": \"https://conda.anaconda.org/conda-forge\",\n" +
                    "    \"build_number\": 0,\n" +
                    "    \"build_string\": \"h7139b31_0_cpython\",\n" +
                    "    \"channel\": \"conda-forge\",\n" +
                    "    \"dist_name\": \"python-3.9.23-h7139b31_0_cpython\",\n" +
                    "    \"name\": \"python\",\n" +
                    "    \"platform\": \"osx-arm64\",\n" +
                    "    \"version\": \"3.9.23\"\n" +
                "    },",
                "   {\n" +
                    "    \"base_url\": \"https://conda.anaconda.org/conda-forge\",\n" +
                    "    \"build_number\": 1,\n" +
                    "    \"build_string\": \"pyhd8ed1ab_1\",\n" +
                    "    \"channel\": \"conda-forge\",\n" +
                    "    \"dist_name\": \"requests-file-2.1.0-pyhd8ed1ab_1\",\n" +
                    "    \"name\": \"requests-file\",\n" +
                    "    \"platform\": \"noarch\",\n" +
                    "    \"version\": \"2.1.0\"\n" +
                "   },",
                "   {\n" +
                    "    \"base_url\": \"https://conda.anaconda.org/conda-forge\",\n" +
                    "    \"build_number\": 0,\n" +
                    "    \"build_string\": \"pyhd8ed1ab_0\",\n" +
                    "    \"channel\": \"conda-forge\",\n" +
                    "    \"dist_name\": \"requests-2.32.5-pyhd8ed1ab_0\",\n" +
                    "    \"name\": \"requests\",\n" +
                    "    \"platform\": \"noarch\",\n" +
                    "    \"version\": \"2.32.5\"\n" +
                "   },",
                "   {\n" +
                    "    \"base_url\": \"https://repo.anaconda.com/pkgs/main\",\n" +
                    "    \"build_number\": 0,\n" +
                    "    \"build_string\": \"py39hca03da5_0\",\n" +
                    "    \"channel\": \"pkgs/main\",\n" +
                    "    \"dist_name\": \"certifi-2025.10.5-py39hca03da5_0\",\n" +
                    "    \"name\": \"certifi\",\n" +
                    "    \"platform\": \"osx-arm64\",\n" +
                    "    \"version\": \"2025.10.5\"\n" +
                "   },",
                "   {\n" +
                    "    \"base_url\": \"https://conda.anaconda.org/conda-forge\",\n" +
                    "    \"build_number\": 0,\n" +
                    "    \"build_string\": \"pyhd8ed1ab_0\",\n" +
                    "    \"channel\": \"conda-forge\",\n" +
                    "    \"dist_name\": \"charset-normalizer-3.4.3-pyhd8ed1ab_0\",\n" +
                    "    \"name\": \"charset-normalizer\",\n" +
                    "    \"platform\": \"noarch\",\n" +
                    "    \"version\": \"3.4.3\"\n" +
                    "  }",
                "]"
        );
        addExecutableOutput(condaListOutput, "conda", "list", "-n", "conda-env", "--json");

        ExecutableOutput condaTreeOutput = createStandardOutput(
                "requests-file==2.1.0\n" +
                        "  ├─ python 3.9.23 [required: >=3.9]\n" +
                        "  └─ requests 2.32.5 [required: >=1.0.0]\n" +
                        "     ├─ certifi 2025.10.5 [required: >=2017.4.17]\n" +
                        "     │  └─ python 3.9.23 [required: >=3.9,<3.10.0a0]\n" +
                        "     ├─ charset-normalizer 3.4.3 [required: >=2,<4]\n" +
                        "     │  └─ python 3.9.23 [required: >=3.9]\n"
        );
        addExecutableOutput(getOutputDirectory(), condaTreeOutput, "conda-tree", "-n", "conda-env","deptree","--full");
    }

    @NotNull
    @Override
    public Detectable create(@NotNull DetectableEnvironment detectableEnvironment) {
        class CondaResolverTest implements CondaResolver {

            @Override
            public ExecutableTarget resolveConda() throws DetectableException {
                return ExecutableTarget.forCommand("conda");
            }
        }

        class CondaTreeResolverTest implements CondaTreeResolver {

            @Override
            public ExecutableTarget resolveCondaTree() throws DetectableException {
                return ExecutableTarget.forCommand("conda-tree");
            }
        }
        return detectableFactory.createCondaTreeDetectable(detectableEnvironment, new CondaTreeResolverTest(), new CondaResolverTest(), new CondaCliDetectableOptions("conda-env"));
    }

    @Override
    public void assertExtraction(@NotNull Extraction extraction) {
        Assertions.assertEquals(1, extraction.getCodeLocations().size());

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.ANACONDA, extraction.getCodeLocations().get(0).getDependencyGraph());
        graphAssert.hasRootSize(1);
        graphAssert.hasRootDependency("requests-file", "2.1.0-pyhd8ed1ab_1-noarch");
        graphAssert.hasDependency("certifi", "2025.10.5-py39hca03da5_0-osx-arm64");
        graphAssert.hasParentChildRelationship("requests", "2.32.5-pyhd8ed1ab_0-noarch", "charset-normalizer", "3.4.3-pyhd8ed1ab_0-noarch");
    }
}
