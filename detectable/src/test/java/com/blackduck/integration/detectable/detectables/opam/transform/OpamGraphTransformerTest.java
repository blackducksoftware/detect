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
    private final List<String> showCalls = new ArrayList<>();

    private OpamGraphTransformer createTransformer(Map<String, ExecutableOutput> outputsByPackage) throws ExecutableRunnerException {
        DetectableExecutableRunner runner = Mockito.mock(DetectableExecutableRunner.class);
        Mockito.when(runner.execute(ArgumentMatchers.any(Executable.class))).thenAnswer(invocation -> {
            Executable executable = invocation.getArgument(0);
            List<String> command = executable.getCommandWithArguments();
            String packageName = command.get(command.indexOf("show") + 1);
            showCalls.add(packageName);
            return outputsByPackage.getOrDefault(packageName, new ExecutableOutput(2, "", "error"));
        });
        return new OpamGraphTransformer(new File("."), new ExternalIdFactory(), runner);
    }

    private ExecutableOutput showOutput(String version, String... dependencies) {
        List<String> lines = new ArrayList<>();
        lines.add("version: \"" + version + "\"");
        if (dependencies.length > 0) {
            lines.add("depends: [");
            Arrays.stream(dependencies).forEach(dependency -> lines.add("  " + dependency));
            lines.add("]");
        }
        return new ExecutableOutput(0, String.join("\n", lines), "");
    }

    private List<String> graphNames(CodeLocation codeLocation) {
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

    @Test
    void constraintsInShowOutputAreNotQueriedAndOsSpecificDependenciesAreReported() throws ExecutableRunnerException {
        Map<String, ExecutableOutput> outputs = new HashMap<>();
        outputs.put("dune", showOutput("3.0.0",
            "\"ocaml\"",
            "  {>= \"3.08.0\" & (os != \"cygwin\" & os-distribution != \"msys2\" | < \"5.0\") &",
            "   < \"5.5.0~\"}",
            "\"flexdll\" {os = \"cygwin\"}",
            "\"conf-msvc64\" {os = \"win32\" & arch = \"x86_64\"}"
        ));
        outputs.put("ocaml", showOutput("4.14.0"));
        outputs.put("flexdll", showOutput("0.44"));
        outputs.put("conf-msvc64", showOutput("1"));
        OpamGraphTransformer transformer = createTransformer(outputs);

        OpamParsedResult parsed = new OpamParsedResult("project", "1.0", Collections.singletonList("dune"), new File("project.opam"));
        CodeLocation codeLocation = transformer.transform(ExecutableTarget.forCommand("opam"), parsed);

        Assertions.assertEquals(Arrays.asList("dune", "ocaml", "flexdll", "conf-msvc64"), showCalls);
        Assertions.assertEquals(Collections.singletonList("dune@3.0.0"), graphNames(codeLocation));
        Assertions.assertEquals(Arrays.asList("conf-msvc64@1", "flexdll@0.44", "ocaml@4.14.0"), childNames(codeLocation, "dune"));
    }

    @Test
    void failingOpamShowSkipsOnlyThatPackage() throws ExecutableRunnerException {
        Map<String, ExecutableOutput> outputs = new HashMap<>();
        outputs.put("good", showOutput("1.0.0", "\"missing\"", "\"child\""));
        outputs.put("child", showOutput("2.0.0"));
        outputs.put("other", showOutput("3.0.0"));
        OpamGraphTransformer transformer = createTransformer(outputs);

        OpamParsedResult parsed = new OpamParsedResult("project", "1.0", Arrays.asList("good", "absent", "other"), new File("project.opam"));
        CodeLocation codeLocation = transformer.transform(ExecutableTarget.forCommand("opam"), parsed);

        Assertions.assertTrue(showCalls.contains("missing"));
        Assertions.assertTrue(showCalls.contains("absent"));
        Assertions.assertEquals(Arrays.asList("good@1.0.0", "other@3.0.0"), graphNames(codeLocation));
        Assertions.assertEquals(Collections.singletonList("child@2.0.0"), childNames(codeLocation, "good"));
    }

    @Test
    void showOutputWithoutVersionIsSkipped() throws ExecutableRunnerException {
        Map<String, ExecutableOutput> outputs = new HashMap<>();
        outputs.put("noversion", new ExecutableOutput(0, "name: \"noversion\"", ""));
        outputs.put("other", showOutput("3.0.0"));
        OpamGraphTransformer transformer = createTransformer(outputs);

        OpamParsedResult parsed = new OpamParsedResult("project", "1.0", Arrays.asList("noversion", "other"), new File("project.opam"));
        CodeLocation codeLocation = transformer.transform(ExecutableTarget.forCommand("opam"), parsed);

        Assertions.assertEquals(Collections.singletonList("other@3.0.0"), graphNames(codeLocation));
    }

    @Test
    void failingPackageIsLookedUpOnlyOnce() throws ExecutableRunnerException {
        Map<String, ExecutableOutput> outputs = new HashMap<>();
        outputs.put("a", showOutput("1.0.0", "\"missing\""));
        outputs.put("b", showOutput("2.0.0", "\"missing\"", "\"noversion\""));
        outputs.put("c", showOutput("3.0.0", "\"noversion\""));
        outputs.put("noversion", new ExecutableOutput(0, "", ""));
        OpamGraphTransformer transformer = createTransformer(outputs);

        OpamParsedResult parsed = new OpamParsedResult("project", "1.0", Arrays.asList("a", "b", "missing", "c"), new File("project.opam"));
        CodeLocation codeLocation = transformer.transform(ExecutableTarget.forCommand("opam"), parsed);

        Assertions.assertEquals(Arrays.asList("a", "missing", "b", "noversion", "c"), showCalls);
        Assertions.assertEquals(Arrays.asList("a@1.0.0", "b@2.0.0", "c@3.0.0"), graphNames(codeLocation));
        Assertions.assertTrue(childNames(codeLocation, "a").isEmpty());
        Assertions.assertTrue(childNames(codeLocation, "b").isEmpty());
        Assertions.assertTrue(childNames(codeLocation, "c").isEmpty());
    }
}
