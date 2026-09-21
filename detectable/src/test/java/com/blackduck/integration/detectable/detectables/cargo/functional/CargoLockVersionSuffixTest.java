package com.blackduck.integration.detectable.detectables.cargo.functional;

import java.io.IOException;
import java.nio.file.Paths;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.functional.DetectableFunctionalTest;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;

public class CargoLockVersionSuffixTest extends DetectableFunctionalTest {

    public CargoLockVersionSuffixTest() throws IOException {
        super("cargo-lock-version-suffix");
    }

    @Override
    protected void setup() throws IOException {
        addFile(
            Paths.get("Cargo.toml"),
            "[package]",
            "name = \"myapp\"",
            "version = \"1.0.0\"",
            "",
            "[dependencies]",
            "mimalloc-rust-sys = \"1.7-source\""
        );

        addFile(
            Paths.get("Cargo.lock"),
            "[[package]]",
            "name = \"mimalloc-rust-sys\"",
            "version = \"1.7.6-source\""
        );
    }

    @NotNull
    @Override
    public Detectable create(@NotNull DetectableEnvironment detectableEnvironment) {
        return detectableFactory.createCargoLockfileDetectable(detectableEnvironment);
    }

    @Override
    public void assertExtraction(@NotNull Extraction extraction) {
        Assertions.assertEquals(1, extraction.getCodeLocations().size());

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(
            Forge.CRATES,
            extraction.getCodeLocations().get(0).getDependencyGraph()
        );

        graphAssert.hasRootSize(1);
        graphAssert.hasRootDependency("mimalloc-rust-sys", "1.7.6-source");
    }
}
