package com.blackduck.integration.detectable.detectables.setuptools.transform;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectables.setuptools.parse.SetupToolsParsedResult;
import com.blackduck.integration.detectable.python.util.PythonDependency;
import com.blackduck.integration.executable.ExecutableOutput;
import com.blackduck.integration.executable.ExecutableRunnerException;

public class SetupToolsGraphTransformer {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String REQUIRES = "Requires";
    private static final String UNEXPECTED_PIP_OUTPUT = "Unexpected empty pip show results. Please run pip install . on the project and try running Detect again.";
    
    private File sourceDirectory;
    private ExternalIdFactory externalIdFactory;
    private DetectableExecutableRunner executableRunner;

    public SetupToolsGraphTransformer(File sourceDirectory, ExternalIdFactory externalIdFactory, DetectableExecutableRunner executableRunner) {
        this.sourceDirectory = sourceDirectory;
        this.externalIdFactory = externalIdFactory;
        this.executableRunner = executableRunner;
    }

    public DependencyGraph transform(ExecutableTarget pipExe, SetupToolsParsedResult parsedResult) throws ExecutableRunnerException {
        DependencyGraph dependencyGraph = new BasicDependencyGraph();
        
        if (pipExe != null) {
            // Get dependencies by running pip show on each direct dependency
            for (PythonDependency directDependency : parsedResult.getDirectDependencies()) {
                handleShowDependency(pipExe, dependencyGraph, directDependency.getName(), null, directDependency.isConditional());
            }
        } else {
            // Unable to determine transitive dependencies, add parsed dependencies directly
            // to the root of the graph.
            handleParsedDependencies(parsedResult, dependencyGraph);
        }
        
        return dependencyGraph;
    }

    private void handleParsedDependencies(SetupToolsParsedResult parsedResult, DependencyGraph dependencyGraph) {
        List<PythonDependency> directDependencies = parsedResult.getDirectDependencies();
        Map<String, List<PythonDependency>> extrasTransitives = parsedResult.getExtrasTransitives();

        for (PythonDependency directDependency : directDependencies) {
            String name = directDependency.getName();
            String version = directDependency.getVersion();

            Dependency currentDependency;
            if (StringUtils.isEmpty(version)) {
                currentDependency = entryToDependency(name);
            } else {
                currentDependency = entryToDependency(name, version);
            }
            dependencyGraph.addChildrenToRoot(currentDependency);

            if (extrasTransitives.containsKey(name)) {
                for (PythonDependency transitiveDep : extrasTransitives.get(name)) {
                    String transName = transitiveDep.getName();
                    String transVersion = transitiveDep.getVersion();
                    Dependency transitiveDependency;
                    if (StringUtils.isEmpty(transVersion)) {
                        transitiveDependency = entryToDependency(transName);
                    } else {
                        transitiveDependency = entryToDependency(transName, transVersion);
                    }
                    dependencyGraph.addChildWithParent(transitiveDependency, currentDependency);
                }
            }
        }
    }
    
    private void handleShowDependency(ExecutableTarget pipExe, DependencyGraph dependencyGraph, String dependencyToSearch, Dependency parentDependency, boolean isConditionalDependency) throws ExecutableRunnerException {
        List<String> rawShowOutput = runPipShow(pipExe, dependencyToSearch, isConditionalDependency);
        
        if (rawShowOutput.isEmpty()) {
            return;
        }
        
        Map<String, String> showOutput = parsePipShow(rawShowOutput);
        
        Dependency currentDependency = entryToDependency(dependencyToSearch, showOutput.get("Version"));
        
        if (parentDependency == null) {
            // No parent, add to root of graph
            dependencyGraph.addChildrenToRoot(currentDependency);
        } else {
            dependencyGraph.addChildWithParent(currentDependency, parentDependency);
        }
        
        // See if we need to continue exploring this chain of dependencies
        if (showOutput.containsKey(REQUIRES)) {
            String[] requiredPackages = showOutput.get(REQUIRES).split(", ");
            for (String requiredPackage : requiredPackages) {
                handleShowDependency(pipExe, dependencyGraph, requiredPackage, currentDependency, false);
            }
        }
    }
    
    public List<String> runPipShow(ExecutableTarget pipExe, String dependencyToSearch, boolean isConditionalDependency) throws ExecutableRunnerException {      
        List<String> pipArguments = new ArrayList<>();
        pipArguments.add("show");
        pipArguments.add(dependencyToSearch);
        
        ExecutableOutput executableOutput = executableRunner.execute(ExecutableUtils.createFromTarget(sourceDirectory, pipExe, pipArguments));
        
        if (executableOutput.getReturnCode() == 0) {
            return executableOutput.getStandardOutputAsList();
        } else {
            // Don't consider it a failure if this is a conditional dependency, it might not have been installed intentionally
            if (isConditionalDependency) {
                logger.info(String.format("Dependency %s is not in the pip cache. Ignoring as a condition is specified on the dependency.", dependencyToSearch));
                return Collections.emptyList();
            } else {
                throw new ExecutableRunnerException(new Exception(UNEXPECTED_PIP_OUTPUT));
            }
        }
    }
    
    public Map<String, String> parsePipShow(List<String> rawShowOutput) {
        Map<String, String> showOutput = new HashMap<>();
        
        for (String line : rawShowOutput) {
            String[] parts = line.split(": ");
            if (parts.length >= 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();
                if (key.equals("Version") || key.equals(REQUIRES)) {
                    showOutput.put(key, value);
                }
            }
        }

        return showOutput;
    }
 
    private Dependency entryToDependency(String key) {
        ExternalId externalId = externalIdFactory.createNameVersionExternalId(Forge.PYPI, key);
        return new Dependency(externalId); 
    }
    
    private Dependency entryToDependency(String key, String value) {
        ExternalId externalId = externalIdFactory.createNameVersionExternalId(Forge.PYPI, key, value);
        return new Dependency(externalId);
    }
}