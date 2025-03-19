package com.blackduck.integration.detectable.detectables.go.functional;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.resolver.GoResolver;
import com.blackduck.integration.detectable.detectables.go.gomod.GoModCliDetectableOptions;
import com.blackduck.integration.detectable.detectables.go.gomod.GoModDependencyType;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.functional.DetectableFunctionalTest;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;
import com.blackduck.integration.executable.ExecutableOutput;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

// recreates IDETECT-4602
public class GoModDetectableTestCOPY extends DetectableFunctionalTest {
    public GoModDetectableTestCOPY() throws IOException {
        super("gomod");
    }

    @Override
    protected void setup() throws IOException {
        addFile(Paths.get("go.mod"));

        ExecutableOutput goListOutput = createStandardOutputFromResource("/go/go-fixed/go-list.xout"); // detectable/src/test/java/com/blackduck/integration/detectable/detectables/go/functional/GoModDetectableTestCOPY.java
        addExecutableOutput(goListOutput, "go", "list", "-m", "-json");

        ExecutableOutput goVersionOutput = createStandardOutput(
            "go version go1.16.5 darwin/amd64"
        );
        addExecutableOutput(goVersionOutput, "go", "version");

        ExecutableOutput goListUJsonOutput = createStandardOutputFromResource("/go/go-fixed/go-list-all.xout");
        addExecutableOutput(goListUJsonOutput, "go", "list", "-mod=readonly", "-m", "-json", "all");

        ExecutableOutput goModGraphOutput = createStandardOutput(
            "commons-service github.com/spf13/viper@v1.8.1",
            "github.com/spf13/viper@v1.8.1 github.com/bketelsen/crypt@v0.0.4",
            "github.com/spf13/viper@v1.8.1 github.com/fsnotify/fsnotify@v1.4.9", // fsnotify version could be a problem
            "github.com/spf13/viper@v1.7.0 github.com/coreos/bbolt@v1.3.2",
            "github.com/spf13/viper@v1.7.0 github.com/fsnotify/fsnotify@v1.4.7", // fsnotify version could be a problem
            "github.com/spf13/viper@v1.7.0 github.com/gogo/protobuf@v1.2.1"
        );
        addExecutableOutput(goModGraphOutput, "go", "mod", "graph");

        ExecutableOutput goListMainOutput = createStandardOutputFromResource("/go/go-fixed/go-mod-get-main.xout");
        addExecutableOutput(goListMainOutput, "go", "list", "-mod=readonly", "-m", "-f", "{{if (.Main)}}{{.Path}}{{end}}", "all");

        ExecutableOutput goListDirectMods = createStandardOutputFromResource("/go/go-fixed/go-mod-list-directs.xout");
        addExecutableOutput(goListDirectMods, "go", "list", "-mod=readonly", "-m", "-f", "{{if not (or .Indirect .Main)}}{{.Path}}@{{.Version}}{{end}}", "all");

        ExecutableOutput goModWhyNvOutput = createStandardOutput("/go/go-fixed/go-mod-why.xout");
        addExecutableOutput(goModWhyNvOutput, "go", "mod", "why", "-m", "all");
    }

    @NotNull
    @Override
    public Detectable create(@NotNull DetectableEnvironment detectableEnvironment) {
        class GoResolverTest implements GoResolver {
            @Override
            public ExecutableTarget resolveGo() throws DetectableException {
                return ExecutableTarget.forCommand("go");
            }
        }
        GoModCliDetectableOptions goModCliDetectableOptions = new GoModCliDetectableOptions(GoModDependencyType.NONE);
        return detectableFactory.createGoModCliDetectable(detectableEnvironment, new GoResolverTest(), goModCliDetectableOptions);
    }

    @Override
    public void assertExtraction(@NotNull Extraction extraction) {
        assertSuccessfulExtraction(extraction);
        assertEquals(1, extraction.getCodeLocations().size());

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.GOLANG, extraction.getCodeLocations().get(0).getDependencyGraph());
        graphAssert.hasRootSize(1); // number of direct dependencies

        // viper 1.8.1 is a direct dep
        graphAssert.hasRootDependency("github.com/spf13/viper", "v1.8.1");
        // true transitive dep of viper 1.8.1
        graphAssert.hasDependency("github.com/bketelsen/crypt", "v0.0.4");
        graphAssert.hasParentChildRelationship("github.com/spf13/viper", "v1.8.1", "github.com/bketelsen/crypt", "v0.0.4");
        // viper 1.7.0 and its transitives should not be present
        graphAssert.hasNoDependency("github.com/spf13/viper", "v1.7.0");
        graphAssert.hasNoDependency("github.com/coreos/bbolt", "v1.3.2");
        graphAssert.hasNoDependency("github.com/gogo/protobuf", "v1.2.1");

        // come back to sort out fsnotify version (aka how should conflicting versions be resolved vs how it is done today)
    }

}
