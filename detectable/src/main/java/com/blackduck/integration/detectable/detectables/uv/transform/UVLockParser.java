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


import javax.annotation.Nullable;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
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
    private static final String MEMBERS_KEY = "members";
    private DependencyGraph dependencyGraph;
    private final Set<String> workSpaceMembers = new HashSet<>();
    private final ExternalIdFactory externalIdFactory;
    private final List<CodeLocation> codeLocations = new ArrayList<>();
    private final Map<String, String> packageDependencyMap = new HashMap<>();
    private final Map<String, Set<String>> transitiveDependencyMap = new HashMap<>();

    public UVLockParser(ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
    }


    public List<CodeLocation> parseLockFile(String lockFileContent, String rootName, UVDetectorOptions uvDetectorOptions) {
        TomlParseResult uvLockObject = Toml.parse(lockFileContent);

        collectWorkspaceMembers(uvLockObject);
        if(uvLockObject.get(PACKAGE_KEY) != null) {
            TomlArray dependencies = uvLockObject.getArray(PACKAGE_KEY);
            parseDependencies(dependencies, rootName, uvDetectorOptions);
        }

        return codeLocations;
    }

    private void parseDependencies(TomlArray dependencies, String rootName, UVDetectorOptions uvDetectorOptions) {
        for(int i = 0; i < dependencies.size(); i++) {
            TomlTable dependencyTable = dependencies.getTable(i);

            if(dependencyTable != null) {
                String dependencyName = dependencyTable.getString(NAME_KEY);
                String dependencyVersion = dependencyTable.getString(VERSION_KEY);

                packageDependencyMap.put(dependencyName, dependencyVersion);

                if(rootName.equals(dependencyName) || workSpaceMembers.contains(dependencyName)) {
                    workSpaceMembers.add(dependencyName);
                }

                parseDependenciesSection(dependencyTable, dependencyName, uvDetectorOptions);
            }
        }
        
        generateGraph(uvDetectorOptions);

    }

    private boolean checkIfMemberExcluded(String memberName, UVDetectorOptions detectorOptions) {
        if(!detectorOptions.getExcludedWorkspaceMembers().isEmpty() && detectorOptions.getExcludedWorkspaceMembers().contains(memberName)) { // checking if current member is excluded
            return true;
        } else if(!detectorOptions.getIncludedWorkspaceMembers().isEmpty()){
            return !detectorOptions.getIncludedWorkspaceMembers().contains(memberName); // checking if current member is not included
        } else {
            return false;
        }
    }
    
    private void generateGraph(UVDetectorOptions uvDetectorOptions) {
        for(String workSpaceMember: workSpaceMembers) {
            if(!checkIfMemberExcluded(workSpaceMember, uvDetectorOptions)) {
                initializeProject(createDependency(workSpaceMember, packageDependencyMap.get(workSpaceMember)));
                generateTreeOutput(workSpaceMember, null, uvDetectorOptions);
            }
        }
    }

    private void generateTreeOutput(String dependency, Dependency parentDependency, UVDetectorOptions uvDetectorOptions) {
        if(transitiveDependencyMap.containsKey(dependency)) {
            for(String transitiveDependency: transitiveDependencyMap.get(dependency)) {
                if(!checkIfMemberExcluded(transitiveDependency, uvDetectorOptions)) {
                    Dependency childDependency = createDependency(transitiveDependency, packageDependencyMap.get(transitiveDependency));
                    if(parentDependency == null) {
                        dependencyGraph.addDirectDependency(childDependency);
                    } else {
                        dependencyGraph.addChildWithParent(childDependency, parentDependency);
                    }

                    generateTreeOutput(transitiveDependency, childDependency, uvDetectorOptions);
                }
            }
        }
    }

    private void parseDependenciesSection(TomlTable dependencyTable, String dependencyName, UVDetectorOptions uvDetectorOptions) {
        if(dependencyTable.contains(DEPENDENCIES_KEY)) {
            TomlArray directDependencyArray = dependencyTable.getArray(DEPENDENCIES_KEY);
            parseTransitiveDependencies(directDependencyArray, dependencyName);
        }

        if(dependencyTable.contains(DEV_DEPENDENCIES_KEY)) {
            TomlTable devDependencyTable = dependencyTable.getTable(DEV_DEPENDENCIES_KEY);
            for(List<String> key: devDependencyTable.keyPathSet()) {
                if(!uvDetectorOptions.getExcludedDependencyGroups().contains(key.get(0))) {
                    TomlArray devDependencyArray = devDependencyTable.getArray(key.get(0));
                    parseTransitiveDependencies(devDependencyArray, dependencyName);
                }
            }
        }
    }


    private void parseTransitiveDependencies(TomlArray transitiveDependencyArray, String dependencyName) {
        for (int j = 0; j < transitiveDependencyArray.size(); j++) {
            TomlTable currentDependencyTable = transitiveDependencyArray.getTable(j);
            if (currentDependencyTable.contains(NAME_KEY)) {
                transitiveDependencyMap.computeIfAbsent(dependencyName, value -> new HashSet<>()).add(currentDependencyTable.getString(NAME_KEY));
            }
        }
    }

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

    //create a new code location for a new workspace member
    private void initializeProject(Dependency projectDependency) {
        dependencyGraph = new BasicDependencyGraph();
        CodeLocation codeLocation = new CodeLocation(dependencyGraph, projectDependency.getExternalId(), new File(projectDependency.getName()));
        codeLocations.add(codeLocation);
    }

    private List<String> extractList(@Nullable TomlTable list) {
        List<String> extractedList = new ArrayList<>();

        if(list != null) {
            for (List<String> key : list.keyPathSet()) {
                extractedList.add(key.get(0));
            }
        }

        return extractedList;
    }
    
    private Dependency createDependency(String dependencyName, String dependencyVersion) {
        ExternalId externalId = externalIdFactory.createNameVersionExternalId(Forge.PYPI, dependencyName, dependencyVersion);
        return new Dependency(dependencyName, dependencyVersion, externalId);
    }

}
