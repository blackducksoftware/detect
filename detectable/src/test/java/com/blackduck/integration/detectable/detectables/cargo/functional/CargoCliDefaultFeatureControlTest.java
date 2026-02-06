package com.blackduck.integration.detectable.detectables.cargo.functional;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;

import com.blackduck.integration.detectable.detectables.cargo.CargoDetectableOptions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.resolver.CargoResolver;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.functional.DetectableFunctionalTest;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;
import com.blackduck.integration.executable.ExecutableOutput;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;

/**
 * Tests default Cargo feature behavior when no feature properties are set.
 * Expected: Default features (pcre2, simd-accel, json) and their dependencies in BOM.
 */
public class CargoCliDefaultFeatureControlTest extends DetectableFunctionalTest {

    public CargoCliDefaultFeatureControlTest() throws IOException {
        super("cargo-features");
    }

    @Override
    protected void setup() throws IOException {
        addFile(
            Paths.get("Cargo.toml"),
            "[package]",
            "name = \"ripgrep\"",
            "version = \"14.1.1\"",
            "edition = \"2021\"",
            "",
            "[features]",
            "default = [\"pcre2\", \"simd-accel\", \"json\"]",
            "pcre2 = [\"dep:pcre2\"]",
            "simd-accel = [\"dep:encoding_rs_io\"]",
            "json = [\"dep:serde\", \"dep:serde_json\"]",
            "compressed = [\"gzip\", \"bzip2\"]",
            "gzip = [\"dep:flate2\"]",
            "bzip2 = [\"dep:bzip2\"]",
            "",
            "[dependencies]",
            "regex = \"1.10.6\"",
            "log = \"0.4.22\"",
            "pcre2 = { version = \"0.2.7\", optional = true }",
            "encoding_rs_io = { version = \"0.1.7\", optional = true }",
            "serde = { version = \"1.0.210\", optional = true }",
            "serde_json = { version = \"1.0.128\", optional = true }",
            "flate2 = { version = \"1.0\", optional = true }",
            "bzip2 = { version = \"0.4\", optional = true }"
        );

        ExecutableOutput cargoVersionOutput = createStandardOutput("cargo 1.85.0 (abc123 2020-06-17)");
        addExecutableOutput(cargoVersionOutput, "cargo", "--version");

        ExecutableOutput defaultOutput = createStandardOutput(
            "0ripgrep v14.1.1 (/path/to/ripgrep)",
            "1regex v1.10.6",
            "1log v0.4.22",
            "1pcre2 v0.2.7",
            "1encoding_rs_io v0.1.7",
            "1serde v1.0.210",
            "1serde_json v1.0.128"
        );
        addExecutableOutput(defaultOutput, "cargo", "tree", "--prefix", "depth", "--workspace");
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
            new CargoDetectableOptions(
                EnumListFilter.excludeNone(),
                false,
                false,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
            )
        );
    }

    @Override
    public void assertExtraction(@NotNull Extraction extraction) {
        Assertions.assertEquals(1, extraction.getCodeLocations().size());

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(
            Forge.CRATES,
            extraction.getCodeLocations().get(0).getDependencyGraph()
        );

        graphAssert.hasRootSize(6);
        graphAssert.hasRootDependency("regex", "1.10.6");
        graphAssert.hasRootDependency("log", "0.4.22");
        graphAssert.hasRootDependency("pcre2", "0.2.7");
        graphAssert.hasRootDependency("encoding_rs_io", "0.1.7");
        graphAssert.hasRootDependency("serde", "1.0.210");
        graphAssert.hasRootDependency("serde_json", "1.0.128");
    }
}
