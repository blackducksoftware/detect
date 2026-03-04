package com.blackduck.integration.detectable.detectables.conda.functional;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.resolver.CondaResolver;
import com.blackduck.integration.detectable.detectables.conda.CondaCliDetectableOptions;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.functional.DetectableFunctionalTest;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;
import com.blackduck.integration.executable.ExecutableOutput;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;

public class CondaDetectableTest  extends DetectableFunctionalTest {

        public CondaDetectableTest() throws IOException {
            super("conda");
        }

        @Override
        protected void setup() throws IOException {
            addFile("environment.yaml");

            ExecutableOutput condaListOutput = createStandardOutput(
                    "[",
                    "   {",
                    "       \"base_url\": null,",
                    "       \"build_number\": 0,",
                    "       \"build_string\": \"0\",",
                    "       \"channel\": \"defaults\",",
                    "       \"dist_name\": \"mkl-2017.0.3-0\",",
                    "       \"name\": \"mkl\",",
                    "       \"platform\": null,",
                    "       \"version\": \"2017.0.3\",",
                    "       \"with_features_depends\": null",
                    "   }",
                    "]"
            );
            addExecutableOutput(condaListOutput, "conda", "list", "-n", "conda-env", "--json");

            ExecutableOutput condaInfoOutput = createStandardOutput(
                    "{",
                    "   \"conda_build_version\": \"not installed\",",
                    "   \"conda_env_version\": \"4.3.22\",",
                    "   \"conda_location\": \"/usr/local/miniconda3/lib/python3.6/site-packages/conda\",",
                    "   \"conda_prefix\": \"/usr/local/miniconda3\",",
                    "   \"conda_private\": false,",
                    "   \"conda_version\": \"4.3.22\",",
                    "   \"default_prefix\": \"/usr/local/miniconda3\",",
                    "   \"platform\": \"win-64\"",
                    "}"
            );
            addExecutableOutput(getOutputDirectory(), condaInfoOutput, "conda", "info", "--json");
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
            return detectableFactory.createCondaCliDetectable(detectableEnvironment, new CondaResolverTest(), new CondaCliDetectableOptions("conda-env"));
        }

        @Override
        public void assertExtraction(@NotNull Extraction extraction) {
            Assertions.assertEquals(1, extraction.getCodeLocations().size());

            NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.ANACONDA, extraction.getCodeLocations().get(0).getDependencyGraph());
            graphAssert.hasRootSize(1);
            graphAssert.hasRootDependency("mkl", "2017.0.3-0-win-64");

        }
}
