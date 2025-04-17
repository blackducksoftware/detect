package com.blackduck.integration.detectable.detectables.go.functional;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Paths;

import org.jetbrains.annotations.NotNull;

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
import org.junit.jupiter.api.Disabled;

@Disabled
public class GoModDetectableVendoredTest extends DetectableFunctionalTest {
    public GoModDetectableVendoredTest() throws IOException {
        super("gomod");
    }

    @Override
    protected void setup() throws IOException {
        addFile(Paths.get("go.mod"));

        ExecutableOutput goListOutput = createStandardOutputFromResource("/go/gomod-test1/go-list.xout");
        addExecutableOutput(goListOutput, "go", "list", "-m", "-json");

        ExecutableOutput goListMainOutput = createStandardOutputFromResource("/go/gomod-test1/go-mod-get-main.xout");
        addExecutableOutput(goListMainOutput, "go", "list", "-mod=readonly", "-m", "-f", "{{if (.Main)}}{{.Path}}{{end}}", "all");

        ExecutableOutput goListDirectMods = createStandardOutputFromResource("/go/gomod-test1/go-mod-list-directs.xout");
        addExecutableOutput(goListDirectMods, "go", "list", "-mod=readonly", "-m", "-f", "{{if not (or .Indirect .Main)}}{{.Path}}@{{.Version}}{{end}}", "all");

        ExecutableOutput goVersionOutput = createStandardOutput(
            "go version go1.16.5 darwin/amd64"
        );
        addExecutableOutput(goVersionOutput, "go", "version");

        ExecutableOutput goListUJsonOutput = createStandardOutputFromResource("/go/gomod-test1/go-list-all.xout");
        addExecutableOutput(goListUJsonOutput, "go", "list", "-mod=readonly", "-m", "-json", "all");

        ExecutableOutput goModGraphOutput = createStandardOutput(
            "github.com/gin-gonic/gin golang.org/x/text@v0.3.0",
            "github.com/gin-gonic/gin sigs.k8s.io/yaml@v1.2.0",
            "golang.org/x/text@v0.3.0 golang.org/x/tools@v0.0.0-20180917221912-90fa682c2a6e",
            "gopkg.in/yaml.v2@v2.2.8 gopkg.in/check.v1@v0.0.0-20161208181325-20d25e280405",
            "sigs.k8s.io/yaml@v1.2.0 github.com/davecgh/go-spew@v1.1.1",
            "sigs.k8s.io/yaml@v1.2.0 gopkg.in/yaml.v2@v2.2.8"
        );
        addExecutableOutput(goModGraphOutput, "go", "mod", "graph");

        ExecutableOutput goModWhyOutput = createStandardOutput(
            "# github.com/gin-gonic/gin",
            "github.com/gin-gonic/gin",
            "",
            "# github.com/davecgh/go-spew",
            "(main module does not need to vendor module github.com/davecgh/go-spew)",
            "",
            "# golang.org/x/text",
            "github.com/gin-gonic/gin",
            "golang.org/x/text/language",
            "",
            "# golang.org/x/tools",
            "(main module does not need to vendor module golang.org/x/tools)",
            "",
            "# gopkg.in/check.v1",
            "(main module does not need to vendor module gopkg.in/check.v1)",
            "",
            "# gopkg.in/yaml.v2",
            "(main module does not need to vendor module gopkg.in/yaml.v2)",
            "",
            "# sigs.k8s.io/yaml",
            "(main module does not need to vendor module sigs.k8s.io/yaml)"
        );

        addExecutableOutput(goModWhyOutput, "go", "mod", "why", "-m", "-vendor", "all");

        ExecutableOutput goModWhyNvOutput = createStandardOutput("/go/gomod-test1/gomodwhy.xout");
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
        GoModCliDetectableOptions goModCliDetectableOptions = new GoModCliDetectableOptions(GoModDependencyType.VENDORED);
        return detectableFactory.createGoModCliDetectable(detectableEnvironment, new GoResolverTest(), goModCliDetectableOptions);
    }

    @Override
    public void assertExtraction(@NotNull Extraction extraction) {
        assertSuccessfulExtraction(extraction);
        assertEquals(1, extraction.getCodeLocations().size());

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.GOLANG, extraction.getCodeLocations().get(0).getDependencyGraph());
        graphAssert.hasRootSize(1);

        graphAssert.hasRootDependency("golang.org/x/text", "v0.3.6");

        // This version should be replaced with a v0.3.6
        graphAssert.hasNoDependency("golang.org/x/text", "v0.3.0");

        graphAssert.hasNoDependency("golang.org/x/tools", "90fa682c2a6e");

        // sigs.k8s.io/yaml and it's transitives are unused as per `go mod why`
        graphAssert.hasNoDependency("sigs.k8s.io/yaml", "v1.2.0");
        graphAssert.hasNoDependency("github.com/davecgh/go-spew", "v1.1.1");
        graphAssert.hasNoDependency("gopkg.in/yaml.v2", "v2.2.8");
    }

}
