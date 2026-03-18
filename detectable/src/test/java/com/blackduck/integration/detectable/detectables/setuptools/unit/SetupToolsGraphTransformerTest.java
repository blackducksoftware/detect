package com.blackduck.integration.detectable.detectables.setuptools.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectables.setuptools.parse.SetupToolsParsedResult;
import com.blackduck.integration.detectable.detectables.setuptools.transform.SetupToolsGraphTransformer;
import com.blackduck.integration.detectable.python.util.PythonDependency;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;
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

    @Test
    public void testNoPipTransformWithExtrasTransitives() throws ExecutableRunnerException {
        List<PythonDependency> dependencies = new ArrayList<>();
        dependencies.add(new PythonDependency("requests", "2.28.2"));
        dependencies.add(new PythonDependency("httpx", "0.23.3"));

        Map<String, List<PythonDependency>> extrasTransitives = new HashMap<>();
        extrasTransitives.put("requests", Arrays.asList(
                new PythonDependency("charset-normalizer", "3.3.1"),
                new PythonDependency("idna", "3.11")
        ));
        extrasTransitives.put("httpx", Arrays.asList(
                new PythonDependency("certifi", "2025.11.12"),
                new PythonDependency("httpcore", "1.0.9")
        ));

        SetupToolsParsedResult parsedResult = new SetupToolsParsedResult("sample", "1.0.0", dependencies, extrasTransitives);

        SetupToolsGraphTransformer graphTransformer = new SetupToolsGraphTransformer(null, new ExternalIdFactory(), null);
        DependencyGraph dependencyGraph = graphTransformer.transform(null, parsedResult);

        assertNotNull(dependencyGraph);
        assertEquals(2, dependencyGraph.getRootDependencies().size());

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.PYPI, dependencyGraph);

        // Direct dependencies are at root
        graphAssert.hasRootDependency("requests", "2.28.2");
        graphAssert.hasRootDependency("httpx", "0.23.3");

        // Transitives exist in graph
        graphAssert.hasDependency("charset-normalizer", "3.3.1");
        graphAssert.hasDependency("idna", "3.11");
        graphAssert.hasDependency("certifi", "2025.11.12");
        graphAssert.hasDependency("httpcore", "1.0.9");

        // Verify parent-child relationships
        graphAssert.hasParentChildRelationship("requests", "2.28.2", "charset-normalizer", "3.3.1");
        graphAssert.hasParentChildRelationship("requests", "2.28.2", "idna", "3.11");
        graphAssert.hasParentChildRelationship("httpx", "0.23.3", "certifi", "2025.11.12");
        graphAssert.hasParentChildRelationship("httpx", "0.23.3", "httpcore", "1.0.9");
    }
}
