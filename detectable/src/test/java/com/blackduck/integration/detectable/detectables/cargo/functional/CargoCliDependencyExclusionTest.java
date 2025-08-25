package com.blackduck.integration.detectable.detectables.cargo.functional;

import java.io.IOException;
import java.nio.file.Paths;

import com.blackduck.integration.detectable.detectables.cargo.CargoDependencyType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.resolver.CargoResolver;
import com.blackduck.integration.detectable.detectables.cargo.CargoDetectableOptions;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.functional.DetectableFunctionalTest;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;
import com.blackduck.integration.executable.ExecutableOutput;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;

public class CargoCliDependencyExclusionTest extends DetectableFunctionalTest {

    public CargoCliDependencyExclusionTest() throws IOException {
        super("cargo");
    }

    @Override
    protected void setup() throws IOException {
        addFile(
            Paths.get("Cargo.toml"),
            "[package]",
            "name = \"sample-cargo-project\"",
            "version = \"0.1.0\"",
            "edition = \"2024\"",
            "build = \"build.rs\"",
            "",
            "[dependencies]",
            "rand = \"0.9.1\"",
            "time = \"0.3.41\"",
            "",
            "[build-dependencies]",
            "regex = \"1.0.1\"",
            "",
            "[dev-dependencies]",
            "openssl = \"0.10.73\"",
            "regex-lite = { git = \"https://github.com/rust-lang/regex.git\" }"
        );

        // Mocking "cargo --version" command output
        ExecutableOutput cargoVersionOutput = createStandardOutput("cargo 1.85.0 (abc123 2020-06-17)");
        addExecutableOutput(cargoVersionOutput, "cargo", "--version");

        // Mock "cargo tree --no-dedupe --prefix depth --edges no-normal" command output
        ExecutableOutput cargoTreeOutput = createStandardOutput(
            "0sample-cargo-project v0.1.0 (/path/to/project)",
            "1regex v1.11.1",
            "1openssl v0.10.73",
            "1regex-lite v0.1.6 (https://github.com/rust-lang/regex.git#1a069b92)"
        );
        addExecutableOutput(cargoTreeOutput, "cargo", "tree", "--no-dedupe", "--prefix", "depth", "--edges", "no-normal");
    }

    @NotNull
    @Override
    public Detectable create(@NotNull DetectableEnvironment detectableEnvironment) {
        class CargoResolverTest implements CargoResolver {
            @Override
            public ExecutableTarget resolveCargo(DetectableEnvironment environment) throws DetectableException {
                return ExecutableTarget.forCommand("cargo");
            }
        }
        return detectableFactory.createCargoCliDetectable(
            detectableEnvironment,
            new CargoResolverTest(),
            new CargoDetectableOptions(EnumListFilter.fromExcluded(CargoDependencyType.NORMAL))
        );
    }

    @Override
    public void assertExtraction(@NotNull Extraction extraction) {
        Assertions.assertEquals(1, extraction.getCodeLocations().size());

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.CRATES, extraction.getCodeLocations().get(0).getDependencyGraph());
        graphAssert.hasRootSize(3);
        graphAssert.hasRootDependency("regex", "1.11.1");
        graphAssert.hasRootDependency("openssl", "0.10.73");
        graphAssert.hasRootDependency("regex-lite", "0.1.6");
    }
}