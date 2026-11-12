package com.blackduck.integration.detectable.detectables.opam.transform;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectables.opam.parse.OpamParsedResult;
import com.blackduck.integration.executable.Executable;
import com.blackduck.integration.executable.ExecutableOutput;
import com.blackduck.integration.executable.ExecutableRunnerException;

public class OpamGraphTransformerTest {
    private final Map<String, ExecutableOutput> showOutputs = new HashMap<>();
    private final List<String> showCalls = new ArrayList<>();

    private OpamGraphTransformer createTransformer() throws ExecutableRunnerException {
        DetectableExecutableRunner runner = Mockito.mock(DetectableExecutableRunner.class);
        Mockito.when(runner.execute(ArgumentMatchers.any(Executable.class))).thenAnswer(invocation -> {
            List<String> command = ((Executable) invocation.getArgument(0)).getCommandWithArguments();
            String packageName = command.get(command.indexOf("show") + 1);
            showCalls.add(packageName);
            return showOutputs.getOrDefault(packageName,
                new ExecutableOutput(2, "", "opam: PACKAGES... arguments: Invalid character in package name \"" + packageName + "\""));
        });
        return new OpamGraphTransformer(new File("."), new ExternalIdFactory(), runner);
    }

    // Detect reads the output line by line, split on the platform's line separator.
    private void addShowOutput(String packageName, String... lines) {
        showOutputs.put(packageName, new ExecutableOutput(0, String.join(System.lineSeparator(), lines), ""));
    }

    private CodeLocation transform(String... directDependencies) throws ExecutableRunnerException {
        OpamParsedResult parsed = new OpamParsedResult("project", "1.0", Arrays.asList(directDependencies), new File("project.opam"));
        return createTransformer().transform(ExecutableTarget.forCommand("opam"), parsed);
    }

    private List<String> rootNames(CodeLocation codeLocation) {
        return codeLocation.getDependencyGraph().getRootDependencies().stream()
            .map(dependency -> dependency.getName() + "@" + dependency.getVersion())
            .sorted()
            .collect(Collectors.toList());
    }

    private List<String> childNames(CodeLocation codeLocation, String parentName) {
        Dependency parent = codeLocation.getDependencyGraph().getRootDependencies().stream()
            .filter(dependency -> dependency.getName().equals(parentName))
            .findFirst()
            .orElseThrow(() -> new AssertionError(parentName + " is not a direct dependency"));
        return codeLocation.getDependencyGraph().getChildrenForParent(parent).stream()
            .map(dependency -> dependency.getName() + "@" + dependency.getVersion())
            .sorted()
            .collect(Collectors.toList());
    }

    // The reported issue: opam 2.1.6 wraps this filter over two lines, and "opam show 5.5.0~ --raw" was run.
    @Test
    void versionInsideAWrappedFilterIsNotLookedUp() throws ExecutableRunnerException {
        addShowOutput("ocamlfind",
            "version: \"1.9.8\"",
            "depends: [",
            "  \"ocaml\"",
            "    {>= \"3.08.0\" & (os != \"cygwin\" & os-distribution != \"msys2\" | < \"5.0\") &",
            "     < \"5.5.0~\"}",
            "]"
        );
        addShowOutput("ocaml", "version: \"5.2.1\"");

        CodeLocation codeLocation = transform("ocamlfind");

        Assertions.assertEquals(Arrays.asList("ocamlfind", "ocaml"), showCalls);
        Assertions.assertEquals(Collections.singletonList("ocamlfind@1.9.8"), rootNames(codeLocation));
        Assertions.assertEquals(Collections.singletonList("ocaml@5.2.1"), childNames(codeLocation, "ocamlfind"));
    }

    @Test
    void osSpecificDependenciesAreReported() throws ExecutableRunnerException {
        addShowOutput("dune",
            "version: \"3.0.0\"",
            "depends: [",
            "  \"flexdll\" {os = \"cygwin\"}",
            "  \"conf-msvc64\" {os = \"win32\" & arch = \"x86_64\"}",
            "  \"js_of_ocaml\"",
            "    {with-dev-setup & >= \"6.1.0\" & < \"6.4\" & os != \"win32\"}",
            "]"
        );
        addShowOutput("flexdll", "version: \"0.44\"");
        addShowOutput("conf-msvc64", "version: \"1\"");
        addShowOutput("js_of_ocaml", "version: \"6.0.1\"");

        CodeLocation codeLocation = transform("dune");

        Assertions.assertEquals(Arrays.asList("dune", "flexdll", "conf-msvc64", "js_of_ocaml"), showCalls);
        Assertions.assertEquals(Arrays.asList("conf-msvc64@1", "flexdll@0.44", "js_of_ocaml@6.0.1"), childNames(codeLocation, "dune"));
    }

    @Test
    void singleDependencyWithoutBrackets() throws ExecutableRunnerException {
        addShowOutput("parent",
            "version: \"1.0\"",
            "depends: \"ocaml\" {>= \"4.08\"}",
            "url {",
            "  src: \"https://example.com/archive.tar.gz\"",
            "}"
        );
        addShowOutput("ocaml", "version: \"4.14.0\"");

        CodeLocation codeLocation = transform("parent");

        Assertions.assertEquals(Arrays.asList("parent", "ocaml"), showCalls);
        Assertions.assertEquals(Collections.singletonList("ocaml@4.14.0"), childNames(codeLocation, "parent"));
    }

    // A package opam show cannot resolve still fails the extraction, as before this change.
    @Test
    void failedLookupStillFailsTheExtraction() throws ExecutableRunnerException {
        addShowOutput("parent", "version: \"1.0\"", "depends: [ \"missing\" ]");

        ExecutableRunnerException error = Assertions.assertThrows(ExecutableRunnerException.class, () -> transform("parent"));

        Assertions.assertTrue(error.getMessage().contains("There was an error running opam show on the missing package"), error.getMessage());
        Assertions.assertEquals(Arrays.asList("parent", "missing"), showCalls);
    }

    @Test
    void descriptionAndCommentDependenciesAreNotLookedUp() throws ExecutableRunnerException {
        addShowOutput("parent",
            "version: \"1.0\"",
            "description: \"\"\"", "depends: [ \"description-fake\" ]", "\"\"\"",
            "(* outer (* inner *)", "depends: [ \"comment-fake\" ]", "*)",
            "url {", "depends: [ \"section-fake\" ]", "}",
            "depends (* comment *) : [ \"\"\"ocaml\"\"\" {>= \"5.5.0~\"} ]");
        addShowOutput("ocaml", "version: \"5.2.1\"");

        CodeLocation codeLocation = transform("parent");

        Assertions.assertEquals(Arrays.asList("parent", "ocaml"), showCalls);
        Assertions.assertEquals(Collections.singletonList("ocaml@5.2.1"), childNames(codeLocation, "parent"));
    }

    @Test
    void descriptionWithoutDependencyFieldDoesNotTriggerLookups() throws ExecutableRunnerException {
        addShowOutput("parent", "version: \"1.0\"", "description: \"\"\"", "depends: [ \"fake\" ]", "\"\"\"");

        CodeLocation codeLocation = transform("parent");

        Assertions.assertEquals(Collections.singletonList("parent"), showCalls);
        Assertions.assertEquals(Collections.singletonList("parent@1.0"), rootNames(codeLocation));
        Assertions.assertEquals(Collections.emptyList(), childNames(codeLocation, "parent"));
    }
}
