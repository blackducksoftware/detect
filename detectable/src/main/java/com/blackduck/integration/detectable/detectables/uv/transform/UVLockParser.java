package com.blackduck.integration.detectable.detectables.uv.transform;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.uv.UVDetectorOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

public class UVLockParser {

    private static final Logger logger = LoggerFactory.getLogger(UVLockParser.class);

    private static final String NAME_KEY = "name";
    private static final String VERSION_KEY = "version";
    private static final String PACKAGE_KEY = "package";
    private static final String MANIFEST_KEY = "manifest";
    private static final String DEPENDENCIES_KEY = "dependencies";
    private static final String DEV_DEPENDENCIES_KEY = "dev-dependencies";
    private static final String OPTIONAL_DEPENDENCIES_KEY = "optional-dependencies";
    private static final String MEMBERS_KEY = "members";
    private DependencyGraph dependencyGraph;
    private final Set<String> workSpaceMembers = new HashSet<>();
    private final ExternalIdFactory externalIdFactory;
    private final List<CodeLocation> codeLocations = new ArrayList<>();
    private final Map<String, String> packageDependencyMap = new HashMap<>();
    private final Map<String, Set<String>> transitiveDependencyMap = new HashMap<>();
    private final Set<String> visitedDependencies = new HashSet<>();

    public UVLockParser(ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
    }


    public List<CodeLocation> parseLockFile(String lockFileContent, String rootName, UVDetectorOptions uvDetectorOptions) {
        TomlParseResult uvLockObject = Toml.parse(lockFileContent);

        collectWorkspaceMembers(uvLockObject);
        rootName = normalizePackageName(rootName);
        if(uvLockObject.get(PACKAGE_KEY) != null) {
            TomlArray dependencies = uvLockObject.getArray(PACKAGE_KEY);
            parseDependencies(dependencies, rootName, uvDetectorOptions);
        }

        return codeLocations;
    }

    // parse all the dependencies which are in random order two times:
    // In the first go, we will get all dependency and transitive dependencies information,
    // In the second go, we will recursively loop all the workspace members which will have their direct, transitives and so on to build the graph.
    private void parseDependencies(TomlArray dependencies, String rootName, UVDetectorOptions uvDetectorOptions) {
        for(int i = 0; i < dependencies.size(); i++) {
            TomlTable dependencyTable = dependencies.getTable(i);

            if(dependencyTable != null) {
                String dependencyName = dependencyTable.getString(NAME_KEY);
                String dependencyVersion = "defaultVersion"; // If version is not present, create a project with default version
                if(dependencyTable.contains(VERSION_KEY)) {
                    dependencyVersion = normalizeVersion(dependencyTable.getString(VERSION_KEY));
                }

                packageDependencyMap.put(dependencyName, dependencyVersion);

                // the way we are building the graph with different projects/workspace members is by finding direct dependencies first with the project name
                // we parse project name and workspace members first and add them to a list, while recursively looping over dependencies, we loop over all workspaceMembers list
                // which will in turn mean that their dependencies are direct since uv.lock always has one entry for the root project
                // Here if rootName from pyproject.toml or workspace member is encountered then we store it
                if(rootName.equals(dependencyName) || workSpaceMembers.contains(dependencyName)) {
                    workSpaceMembers.add(dependencyName);
                }

                //parse transitive dependencies section of current dependency
                parseDependenciesSection(dependencyTable, dependencyName, uvDetectorOptions);
            }
        }

        generateGraph(uvDetectorOptions);

    }

    private void parseDependenciesSection(TomlTable dependencyTable, String dependencyName, UVDetectorOptions uvDetectorOptions) {
        //parse dependencies section
        if(dependencyTable.contains(DEPENDENCIES_KEY)) {
            TomlArray directDependencyArray = dependencyTable.getArray(DEPENDENCIES_KEY);
            parseTransitiveDependencies(directDependencyArray, dependencyName);
        }

        //parse dev dependencies, it is a toml table with group name as the key and dependencies as list, check if that group is not included then do not parse them
        if(dependencyTable.contains(DEV_DEPENDENCIES_KEY)) {
            TomlTable devDependencyTable = dependencyTable.getTable(DEV_DEPENDENCIES_KEY);
            for(List<String> key: devDependencyTable.keyPathSet()) {
                if(!uvDetectorOptions.getExcludedDependencyGroups().contains(key.get(0))) {
                    TomlArray devDependencyArray = devDependencyTable.getArray(key.get(0));
                    parseTransitiveDependencies(devDependencyArray, dependencyName);
                }
            }
        }

        //parse optional dependencies which is part of uv tree command, it can be excluded by users using uv configuration
        if(dependencyTable.contains(OPTIONAL_DEPENDENCIES_KEY)) {
            TomlTable optionalDependencyTable = dependencyTable.getTable(OPTIONAL_DEPENDENCIES_KEY);
            for(List<String> key: optionalDependencyTable.keyPathSet()) {
                TomlArray optionalDependencyArray = optionalDependencyTable.getArray(key.get(0));
                parseTransitiveDependencies(optionalDependencyArray, dependencyName);
            }
        }
    }


    //store all transitive dependencies of current one in a Map
    private void parseTransitiveDependencies(TomlArray transitiveDependencyArray, String dependencyName) {
        for (int j = 0; j < transitiveDependencyArray.size(); j++) {
            TomlTable currentDependencyTable = transitiveDependencyArray.getTable(j);
            if (currentDependencyTable.contains(NAME_KEY)) {
                transitiveDependencyMap.computeIfAbsent(dependencyName, value -> new HashSet<>()).add(currentDependencyTable.getString(NAME_KEY));
            }
        }
    }

    //parse over all the workspace members, which will build the dependency graph
    private void generateGraph(UVDetectorOptions uvDetectorOptions) {
        for(String workSpaceMember: workSpaceMembers) {
            if(!checkIfMemberExcluded(workSpaceMember, uvDetectorOptions)) {
                initializeProject(createDependency(workSpaceMember, packageDependencyMap.get(workSpaceMember))); // a new workspace member, initialize new code location
                loopOverDependencies(workSpaceMember, null, uvDetectorOptions); //loop over all direct dependencies of root project
            } else {
                logger.info("Skipping member '{}' as set in the Detect workspace property.", workSpaceMember);
            }
        }
    }

    // parse all dependencies which we had stored in the Map for transitive dependency for each dependency while parsing it the first time
    private void loopOverDependencies(String dependency, Dependency parentDependency, UVDetectorOptions uvDetectorOptions) {

        if(visitedDependencies.contains(dependency)) {
            return;
        }

        visitedDependencies.add(dependency);

        if(transitiveDependencyMap.containsKey(dependency)) {
            for(String transitiveDependency: transitiveDependencyMap.get(dependency)) {
                if(!checkIfMemberExcluded(transitiveDependency, uvDetectorOptions)) {
                    handleTransitiveDependency(transitiveDependency, parentDependency, uvDetectorOptions);
                }
            }
        }
    }

    private void handleTransitiveDependency(String transitiveDependency, Dependency parentDependency, UVDetectorOptions uvDetectorOptions) {
        if(packageDependencyMap.containsKey(transitiveDependency)) {
            Dependency currentDependency = createDependency(transitiveDependency, packageDependencyMap.get(transitiveDependency));
            addDependencyToGraph(currentDependency,parentDependency);
            loopOverDependencies(transitiveDependency, currentDependency, uvDetectorOptions);
        } else {
            logger.warn("There seems to be a mismatch in the uv.lock. A dependency could not be found: " + transitiveDependency);
        }
    }

    private void addDependencyToGraph(Dependency currentDependency, Dependency parentDependency) {
        if(parentDependency == null) {
            dependencyGraph.addDirectDependency(currentDependency); // direct dependency
        } else {
            dependencyGraph.addChildWithParent(currentDependency, parentDependency); // transitive dependency
        }
    }

    //sometimes the version has extra information which we don't want, parse and remove that eg. 2.5.1+cuddjf (only this as far as I have seen in many projects)
    private String normalizeVersion(String dependencyVersion) {
        if(dependencyVersion.contains("+")) {
            return dependencyVersion.substring(0, dependencyVersion.indexOf("+"));
        }
        return dependencyVersion;
    }

    private String normalizePackageName(String packageName) {
        return packageName.replaceAll("[_.-]+", "-").toLowerCase();
    }

    // parse and store all the workspace members
    private void collectWorkspaceMembers(TomlParseResult uvLockObject) {
        if(uvLockObject.get(MANIFEST_KEY) != null) {
            TomlTable manifestTable = uvLockObject.getTable(MANIFEST_KEY);
            if(manifestTable.get(MEMBERS_KEY) != null) {
                TomlArray membersArray = manifestTable.getArray(MEMBERS_KEY);
                for(int i = 0; i < membersArray.size(); i++) {
                    workSpaceMembers.add(membersArray.getString(i));
                }
            }
        }
    }

    // check if workspace members are excluded or included
    private boolean checkIfMemberExcluded(String memberName, UVDetectorOptions detectorOptions) {
        if(!detectorOptions.getExcludedWorkspaceMembers().isEmpty() && detectorOptions.getExcludedWorkspaceMembers().contains(memberName)) { // checking if current member is excluded
            return true;
        } else if(!detectorOptions.getIncludedWorkspaceMembers().isEmpty()){
            return !detectorOptions.getIncludedWorkspaceMembers().contains(memberName) && workSpaceMembers.contains(memberName); // checking if current member is not included
        } else {
            return false;
        }
    }

    //create a new code location for a new workspace member
    private void initializeProject(Dependency projectDependency) {
        dependencyGraph = new BasicDependencyGraph();
        CodeLocation codeLocation = new CodeLocation(dependencyGraph, projectDependency.getExternalId(), new File(projectDependency.getName()));
        codeLocations.add(codeLocation);
    }
    
    private Dependency createDependency(String dependencyName, String dependencyVersion) {
        String normalizedDependencyName = normalizePackageName(dependencyName);
        ExternalId externalId = externalIdFactory.createNameVersionExternalId(Forge.PYPI, normalizedDependencyName, dependencyVersion);
        return new Dependency(normalizedDependencyName, dependencyVersion, externalId);
    }

}
