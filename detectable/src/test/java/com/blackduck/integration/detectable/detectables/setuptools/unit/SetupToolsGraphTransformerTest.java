package com.blackduck.integration.detectable.detectables.setuptools.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectables.setuptools.parse.SetupToolsParsedResult;
import com.blackduck.integration.detectable.detectables.setuptools.transform.SetupToolsGraphTransformer;
import com.blackduck.integration.detectable.python.util.PythonDependency;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;
import com.blackduck.integration.executable.Executable;
import com.blackduck.integration.executable.ExecutableOutput;
import com.blackduck.integration.executable.ExecutableRunnerException;

public class SetupToolsGraphTransformerTest {
    @Test
    public void testNoPipTransform() throws ExecutableRunnerException {
        // Create a list of PythonDependency objects
        List<PythonDependency> dependencies = new ArrayList<>();
        dependencies.add(new PythonDependency("requests", "2.25.1"));
        dependencies.add(new PythonDependency("numpy", "1.21.0"));

        // Create a SetupToolsParsedResult object
        SetupToolsParsedResult parsedResult = new SetupToolsParsedResult("setuptools", "1.0.0", dependencies);

        // Create an instance of SetupToolsGraphTransformer
        SetupToolsGraphTransformer graphTransformer = new SetupToolsGraphTransformer(null, new ExternalIdFactory(), null);

        // Call the transform method
        DependencyGraph dependencyGraph = graphTransformer.transform(null, parsedResult);

        assertNotNull(dependencyGraph);
        
        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.PYPI, dependencyGraph);

        graphAssert.hasRootDependency("requests", "2.25.1");
        graphAssert.hasRootDependency("numpy", "1.21.0");
        
        assertEquals(2, dependencyGraph.getRootDependencies().size());
    }
    
    @Test
    public void testPipTransform() throws ExecutableRunnerException {
        // Create a list of PythonDependency objects
        List<PythonDependency> dependencies = Arrays.asList(
                new PythonDependency("certifi", "2022.6.15")
        );

        // Create a SetupToolsParsedResult object
        SetupToolsParsedResult parsedResult = new SetupToolsParsedResult("setuptools", "1.0.0", dependencies);

        // Mock the DetectableExecutableRunner
        DetectableExecutableRunner executableRunner = Mockito.mock(DetectableExecutableRunner.class);
        ExecutableOutput executableOutput = Mockito.mock(ExecutableOutput.class);
        when(executableOutput.getReturnCode()).thenReturn(0);
        when(executableOutput.getStandardOutputAsList()).thenReturn(Arrays.asList(
                "Name: certifi",
                "Version: 2022.6.15",
                "Requires: "
        ));
        when(executableRunner.execute(Mockito.any())).thenReturn(executableOutput);

        // Create an instance of SetupToolsGraphTransformer
        SetupToolsGraphTransformer graphTransformer = new SetupToolsGraphTransformer(null, new ExternalIdFactory(), executableRunner);

        // Call the transform method
        DependencyGraph dependencyGraph = graphTransformer.transform(ExecutableTarget.forCommand("pip"), parsedResult);

        // Assert that the returned DependencyGraph is not null and contains the expected number of dependencies
        assertNotNull(dependencyGraph);
        assertEquals(1, dependencyGraph.getRootDependencies().size());
        
        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.PYPI, dependencyGraph);

        graphAssert.hasRootDependency("certifi", "2022.6.15");
    }

    // Reproduces a diamond dependency pattern:
    // panel -> mdit-py-plugins -> markdown-it-py
    // panel -> markdown-it-py (encountered a second time via a different path)
    @Test
    public void testDiamondDependencyIsOnlyQueriedOnce() throws ExecutableRunnerException {
        List<PythonDependency> dependencies = Arrays.asList(
                new PythonDependency("panel", "1.9.4")
        );

        SetupToolsParsedResult parsedResult = new SetupToolsParsedResult("setuptools", "1.0.0", dependencies);

        Map<String, List<String>> pipShowOutputByPackage = new HashMap<>();
        pipShowOutputByPackage.put("panel", Arrays.asList(
                "Name: panel",
                "Version: 1.9.4",
                "Requires: mdit-py-plugins, markdown-it-py"
        ));
        pipShowOutputByPackage.put("mdit-py-plugins", Arrays.asList(
                "Name: mdit-py-plugins",
                "Version: 0.4.2",
                "Requires: markdown-it-py"
        ));
        pipShowOutputByPackage.put("markdown-it-py", Arrays.asList(
                "Name: markdown-it-py",
                "Version: 3.0.0",
                "Requires: mdurl"
        ));
        pipShowOutputByPackage.put("mdurl", Arrays.asList(
                "Name: mdurl",
                "Version: 0.1.2",
                "Requires: "
        ));

        // Counts calls to "pip show markdown-it-py" specifically, since that's the package
        // reached via two paths (directly from panel, and via mdit-py-plugins). Before the fix
        // this would be 2; the fix should bring it down to 1.
        AtomicInteger markdownItPyQueryCount = new AtomicInteger(0);

        // The transformer invokes executableRunner.execute(Executable) once per "pip show <package>"
        // call, so the mock routes each invocation to canned output by reading the package name
        // back off the Executable's argument list (its last argument) rather than matching on
        // toString(), which would be fragile for short/overlapping package names.
        DetectableExecutableRunner executableRunner = Mockito.mock(DetectableExecutableRunner.class);
        when(executableRunner.execute(Mockito.any())).thenAnswer((InvocationOnMock invocation) -> {
            Executable executable = invocation.getArgument(0);
            String packageName = lastArgument(executable);
            if (!pipShowOutputByPackage.containsKey(packageName)) {
                throw new AssertionError("Unexpected pip show invocation for package: " + packageName);
            }

            if (packageName.equals("markdown-it-py")) {
                markdownItPyQueryCount.incrementAndGet();
            }

            ExecutableOutput executableOutput = Mockito.mock(ExecutableOutput.class);
            when(executableOutput.getReturnCode()).thenReturn(0);
            when(executableOutput.getStandardOutputAsList()).thenReturn(pipShowOutputByPackage.get(packageName));
            return executableOutput;
        });

        SetupToolsGraphTransformer graphTransformer = new SetupToolsGraphTransformer(null, new ExternalIdFactory(), executableRunner);

        DependencyGraph dependencyGraph = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> graphTransformer.transform(ExecutableTarget.forCommand("pip"), parsedResult));

        assertEquals(1, markdownItPyQueryCount.get(), "markdown-it-py should only be queried via pip show once, even though it is reached through two paths");

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.PYPI, dependencyGraph);
        graphAssert.hasRootDependency("panel", "1.9.4");
        graphAssert.hasDependency("mdit-py-plugins", "0.4.2");
        graphAssert.hasDependency("markdown-it-py", "3.0.0");
        graphAssert.hasDependency("mdurl", "0.1.2");

        graphAssert.hasParentChildRelationship("panel", "1.9.4", "mdit-py-plugins", "0.4.2");
        graphAssert.hasParentChildRelationship("panel", "1.9.4", "markdown-it-py", "3.0.0");
        graphAssert.hasParentChildRelationship("mdit-py-plugins", "0.4.2", "markdown-it-py", "3.0.0");
        graphAssert.hasParentChildRelationship("markdown-it-py", "3.0.0", "mdurl", "0.1.2");
    }

    // Reproduces a cycle in the "Requires" chain (e.g. a <-> b), which without cycle detection
    // would recurse forever.
    @Test
    public void testCyclicalDependencyDoesNotInfinitelyRecurse() throws ExecutableRunnerException {
        List<PythonDependency> dependencies = Arrays.asList(
                new PythonDependency("a", "1.0.0")
        );

        SetupToolsParsedResult parsedResult = new SetupToolsParsedResult("setuptools", "1.0.0", dependencies);

        Map<String, List<String>> pipShowOutputByPackage = new HashMap<>();
        pipShowOutputByPackage.put("a", Arrays.asList("Name: a", "Version: 1.0.0", "Requires: b"));
        pipShowOutputByPackage.put("b", Arrays.asList("Name: b", "Version: 2.0.0", "Requires: a"));

        DetectableExecutableRunner executableRunner = Mockito.mock(DetectableExecutableRunner.class);
        when(executableRunner.execute(Mockito.any())).thenAnswer((InvocationOnMock invocation) -> {
            Executable executable = invocation.getArgument(0);
            String packageName = lastArgument(executable);
            if (!pipShowOutputByPackage.containsKey(packageName)) {
                throw new AssertionError("Unexpected pip show invocation for package: " + packageName);
            }

            ExecutableOutput executableOutput = Mockito.mock(ExecutableOutput.class);
            when(executableOutput.getReturnCode()).thenReturn(0);
            when(executableOutput.getStandardOutputAsList()).thenReturn(pipShowOutputByPackage.get(packageName));
            return executableOutput;
        });

        SetupToolsGraphTransformer graphTransformer = new SetupToolsGraphTransformer(null, new ExternalIdFactory(), executableRunner);

        // assertTimeoutPreemptively is the real safety net here: without the fix, "a" requiring
        // "b" requiring "a" sends handleShowDependency into unbounded recursion, so the test would
        // hang rather than fail fast on a bad assertion.
        DependencyGraph dependencyGraph = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> graphTransformer.transform(ExecutableTarget.forCommand("pip"), parsedResult));

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.PYPI, dependencyGraph);
        graphAssert.hasRootDependency("a", "1.0.0");
        graphAssert.hasDependency("b", "2.0.0");
        graphAssert.hasParentChildRelationship("a", "1.0.0", "b", "2.0.0");
        graphAssert.hasParentChildRelationship("b", "2.0.0", "a", "1.0.0");
    }

    // The package name is always the final argument of "pip show <package>".
    private String lastArgument(Executable executable) {
        List<String> commandWithArguments = executable.getCommandWithArguments();
        return commandWithArguments.get(commandWithArguments.size() - 1);
    }
}
