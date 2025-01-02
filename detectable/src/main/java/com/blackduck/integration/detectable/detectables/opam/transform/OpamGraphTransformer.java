package com.blackduck.integration.detectable.detectables.opam.transform;

import com.blackduck.integration.detectable.detectables.opam.parse.OpamFileParser;
import com.blackduck.integration.detectable.detectables.opam.parse.OpamParsedResult;
import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.executable.ExecutableOutput;
import com.blackduck.integration.executable.ExecutableRunnerException;

import java.io.File;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpamGraphTransformer {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String VERSION = "version";
    private static final String DEPENDS = "depends";
    private final File sourceDirectory;
    private final ExternalIdFactory externalIdFactory;
    private final DetectableExecutableRunner executableRunner;
    Map<Dependency, Set<Dependency>> visitedDependenciesGraph = new HashMap<>();
    private static final String ORPHAN_PARENT_NAME = "Additional_Components";
    private static final String ORPHAN_PARENT_VERSION = "none";

    public OpamGraphTransformer(File sourceDirectory, ExternalIdFactory externalIdFactory, DetectableExecutableRunner executableRunner) {
        this.sourceDirectory = sourceDirectory;
        this.externalIdFactory = externalIdFactory;
        this.executableRunner = executableRunner;
    }

    public CodeLocation transform(ExecutableTarget opamExe, OpamParsedResult opamParsedResult) throws ExecutableRunnerException {
        DependencyGraph dependencyGraph = new BasicDependencyGraph();

        if(opamExe != null) {
            for(String dependency: opamParsedResult.getParsedDirectDependencies()) {
                findTransitiveDependencies(dependency, opamExe, dependencyGraph, new OpamFileParser(), null); // recursively run opam show to get all dependencies
            }
            // if version is empty, try to find version with opam exe and project name
            if(opamParsedResult.getProjectVersion().isEmpty() && !opamParsedResult.getProjectName().isEmpty()) {
                opamParsedResult.setProjectVersion(getProjectVersion(opamExe, opamParsedResult.getProjectName()));
            }
        } else {
            addDirectDependenciesToGraph(opamParsedResult, dependencyGraph); // no transitive dependencies will be detected
            if(opamParsedResult.getProjectVersion().isEmpty()) {
                opamParsedResult.setProjectVersion(opamParsedResult.getProjectName());
            }
        }
        // as we are parsing this for each project, we will create id for each project and add it its own code location
        ExternalId projectId = createExternalId(opamParsedResult.getProjectName(), opamParsedResult.getProjectVersion()).getExternalId();

        return new CodeLocation(dependencyGraph, projectId, opamParsedResult.getSourceCode());
    }

    public String getProjectVersion(ExecutableTarget opamExe, String projectName) throws ExecutableRunnerException {
        List<String> arguments = new ArrayList<>();
        arguments.add("show");
        arguments.add(projectName);
        arguments.add("--field=version"); // command to get just version

        String version = "";

        //run opam show on project to get version
        ExecutableOutput executableOutput = executableRunner.execute(ExecutableUtils.createFromTarget(sourceDirectory, opamExe, arguments));

        if(executableOutput.getReturnCode() == 0) {
            List<String> output = executableOutput.getStandardOutputAsList();
            if(!output.isEmpty()) {
                return output.get(0); // parse the single line version output
            }
        } else {
            logger.warn("Did not get the version successfully, please include version in the opam file.");
            return projectName;
        }
        return version;
    }

    private void addDirectDependenciesToGraph(OpamParsedResult opamParsedResult, DependencyGraph dependencyGraph) {
        List<String> directDependencies = opamParsedResult.getParsedDirectDependencies();
        Map<String, String> opamLockDependencies = opamParsedResult.getLockFileDependencies();

        Dependency orphanParentDependency = createExternalId(ORPHAN_PARENT_NAME, ORPHAN_PARENT_VERSION);

        //match direct dependencies found in opam files with all the resolved packages from lock files to generate graph
        for(String dependency: opamLockDependencies.keySet()) {
            if(directDependencies.contains(dependency)) {
                String packageVersion = opamLockDependencies.get(dependency);
                Dependency directDependency = createExternalId(dependency, packageVersion); //convert to OPAM dependency
                dependencyGraph.addDirectDependency(directDependency); //add Dependency to root
            } else {
                if(!dependencyGraph.hasDependency(orphanParentDependency)) {
                    dependencyGraph.addChildrenToRoot(orphanParentDependency);
                }
                String packageVersion = opamLockDependencies.get(dependency);
                Dependency transitiveDependency = createExternalId(dependency, packageVersion);
                dependencyGraph.addChildWithParent(transitiveDependency, orphanParentDependency);
            }
        }
    }

    private void findTransitiveDependencies(String dependency, ExecutableTarget opamExe, DependencyGraph dependencyGraph, OpamFileParser parser, Dependency parentDependency) throws ExecutableRunnerException {
        // check if the dependency is already been visited first
        Optional<Dependency> visitedDependencyOptional = visitedDependenciesGraph.keySet().stream().filter(dep -> dep.getName().equals(dependency)).findFirst();

        if(visitedDependencyOptional.isPresent() && visitedDependenciesGraph.containsKey(visitedDependencyOptional.get())) {
            Dependency visitedDependency = visitedDependencyOptional.get();
            if(parentDependency == null) {
                dependencyGraph.addDirectDependency(visitedDependency);
            }
            dependencyGraph.addParentWithChildren(visitedDependency, visitedDependenciesGraph.get(visitedDependency)); // add child of that dependency in the graph
            // add child dependencies for all the transitives already visited in the graph to skip running show
            addChildDependencies(dependencyGraph, visitedDependenciesGraph.get(visitedDependency));
        } else {
            List<String> output = runOpamShow(opamExe, dependency);

            if (output.isEmpty()) {
                return;
            }

            Map<String, String> parsedOutput = parser.parseData(output); // parse opam show output

            String version = parsedOutput.get(VERSION).trim().replace("\"","");

            Dependency createdDependency = createExternalId(dependency, version); // create dependency with name and version

            visitedDependenciesGraph.put(createdDependency, new HashSet<>()); // put dependency in graph

            if (parentDependency == null) {
                dependencyGraph.addDirectDependency(createdDependency);
            } else {
                dependencyGraph.addChildWithParent(createdDependency, parentDependency);
                visitedDependenciesGraph.get(parentDependency).add(createdDependency); // put current dependency as child of parent to memoize
            }

            //parse the chain of dependencies to get transitives
            if (parsedOutput.containsKey(DEPENDS)) {
                String[] dependentPackages = parsedOutput.get(DEPENDS).split(", ");
                for (String dependencyPackage : dependentPackages) {
                    findTransitiveDependencies(dependencyPackage, opamExe, dependencyGraph, parser, createdDependency);
                }
            }
        }
    }

    private List<String> runOpamShow(ExecutableTarget opamExe, String dependency) throws ExecutableRunnerException {
        List<String> arguments = new ArrayList<>();
        arguments.add("show");
        arguments.add(dependency);
        arguments.add("--raw"); // adding this will show the output as file.

        ExecutableOutput executableOutput = executableRunner.execute(ExecutableUtils.createFromTarget(sourceDirectory, opamExe, arguments));

        if(executableOutput.getReturnCode() == 0) {
            return executableOutput.getStandardOutputAsList();
        } else {
            throw new ExecutableRunnerException(new Exception("There was an error running opam show on the "+ dependency +" package. Please run opam install . and try running the scan again."));
        }
    }

    private Dependency createExternalId (String name, String version) {
        ExternalId externalId = externalIdFactory.createNameVersionExternalId(Forge.OPAM, name, version);
        return new Dependency(externalId);
    }

    private void addChildDependencies(DependencyGraph dependencyGraph, Set<Dependency> setOfChildren) {
        for(Dependency childDependency: setOfChildren) {
            // recursively add visited child dependencies in the graph
            if(visitedDependenciesGraph.containsKey(childDependency) && !visitedDependenciesGraph.get(childDependency).isEmpty()) {
                dependencyGraph.addParentWithChildren(childDependency, visitedDependenciesGraph.get(childDependency));
                addChildDependencies(dependencyGraph, visitedDependenciesGraph.get(childDependency));
            }
        }
    }
}
