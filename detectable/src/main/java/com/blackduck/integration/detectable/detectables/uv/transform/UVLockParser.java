package com.blackduck.integration.detectable.detectables.uv.transform;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.uv.UVDependencyGroupFilter;
import com.blackduck.integration.detectable.detectables.uv.UVDetectorOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Parses a uv.lock file into one dependency graph (code location) per project / workspace member.
 *
 * Packages are identified by their normalized name AND version (never by name alone), so a package that is
 * resolved to several versions in uv.lock (for example different Python or platform resolution forks) is
 * reported once per version, and each version keeps its own transitive dependency tree.
 * The detector stays buildless: every variant found in uv.lock is reported, no marker is evaluated.
 */
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
    private static final String DEFAULT_VERSION = "defaultVersion"; // used when a [[package]] entry has no version
    private DependencyGraph dependencyGraph;
    private final Set<String> workSpaceMembers = new HashSet<>();
    private final ExternalIdFactory externalIdFactory;
    private final List<CodeLocation> codeLocations = new ArrayList<>();
    // every [[package]] entry of the lock file, keyed by normalized name + version
    private final Map<String, PackageNode> packageNodes = new LinkedHashMap<>();
    // every version found in the lock file for a given normalized package name
    private final Map<String, Set<String>> versionsByName = new HashMap<>();
    // keys (name + version) of the root project and of the workspace member entries
    private final Set<String> projectNodeKeys = new LinkedHashSet<>();

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
    // In the first go, we will get all packages (name + version) and the dependency references each of them declares,
    // In the second go, we will recursively loop all the workspace members which will have their direct, transitives and so on to build the graph.
    private void parseDependencies(TomlArray dependencies, String rootName, UVDetectorOptions uvDetectorOptions) {
        UVDependencyGroupFilter groupFilter = new UVDependencyGroupFilter(uvDetectorOptions);
        groupFilter.logGroupConflictWarnings(logger);

        if (!groupFilter.hasEffectiveGroups()) {
            return;
        }

        Set<String> effectiveOnlyGroups = new TreeSet<>(groupFilter.getEffectiveOnlyGroups());
        if (!effectiveOnlyGroups.isEmpty()) {
            logger.info("UV Lock detector: scanning only dependency groups: {}", effectiveOnlyGroups);
        }

        Set<String> onlyGroups = groupFilter.getOnlyGroups();
        Set<String> excludedGroups = groupFilter.getExcludedGroups();

        for(int i = 0; i < dependencies.size(); i++) {
            TomlTable dependencyTable = dependencies.getTable(i);

            if(dependencyTable != null) {
                String rawName = dependencyTable.getString(NAME_KEY);
                if(rawName == null) {
                    logger.warn("Skipping a [[package]] entry of the uv.lock that has no name.");
                    continue;
                }
                String dependencyName = normalizePackageName(rawName);
                String dependencyVersion = DEFAULT_VERSION; // If version is not present, create a project with default version
                if(dependencyTable.contains(VERSION_KEY)) {
                    dependencyVersion = normalizeVersion(dependencyTable.getString(VERSION_KEY));
                }

                // a package is identified by name + version, so several versions of the same name are all kept
                PackageNode packageNode = registerPackage(dependencyName, dependencyVersion);

                // the way we are building the graph with different projects/workspace members is by finding direct dependencies first with the project name
                // we parse project name and workspace members first and add them to a list, while recursively looping over dependencies, we loop over all workspaceMembers list
                // which will in turn mean that their dependencies are direct since uv.lock always has one entry for the root project
                // Here if rootName from pyproject.toml or workspace member is encountered then we store it
                boolean isProjectEntry = rootName.equals(dependencyName) || workSpaceMembers.contains(dependencyName);
                if(isProjectEntry) {
                    workSpaceMembers.add(dependencyName);
                    projectNodeKeys.add(packageNode.key);
                }

                //parse transitive dependencies section of current dependency
                parseDependenciesSection(dependencyTable, packageNode, onlyGroups, excludedGroups, isProjectEntry);
            }
        }

        generateGraph(uvDetectorOptions);

    }

    // Registers a [[package]] entry. If the very same name + version is present more than once
    // (e.g. only the "+local" suffix differs, which is stripped) the entries share one node.
    private PackageNode registerPackage(String name, String version) {
        versionsByName.computeIfAbsent(name, key -> new LinkedHashSet<>()).add(version);
        return packageNodes.computeIfAbsent(packageKey(name, version), key -> new PackageNode(key, name, version));
    }

    // isProjectEntry is true only for the root project's own [[package]] entry and for
    // workspace member entries (identified via [manifest].members, collected up-front by
    // collectWorkspaceMembers before this loop runs). It is false for every other (regular,
    // third-party/transitive) package pulled in as a dependency.
    private void parseDependenciesSection(TomlTable dependencyTable, PackageNode packageNode, Set<String> onlyGroups, Set<String> excludedGroups, boolean isProjectEntry) {

        // [dependencies] — for the root project / workspace members this is [project.dependencies]
        // (the main project's regular deps), which is skipped when onlyGroups is set, mirroring CLI
        // behaviour where --only-group does not include regular dependencies.
        // For every OTHER package (e.g. a package pulled in via a selected dev group), [dependencies]
        // is that package's OWN runtime requirements and must always be parsed — otherwise the
        // transitive closure of a selected group's dependencies would be silently dropped.
        if ((!isProjectEntry || onlyGroups.isEmpty()) && dependencyTable.contains(DEPENDENCIES_KEY)) {
            parseTransitiveDependencies(dependencyTable.getArray(DEPENDENCIES_KEY), packageNode);
        }

        // [dev-dependencies] — when onlyGroups is set, include a group only if it is
        // in onlyGroups AND not in excludedGroups. Excluded always wins.
        if (dependencyTable.contains(DEV_DEPENDENCIES_KEY)) {
            parseFilteredGroupDependencies(dependencyTable.getTable(DEV_DEPENDENCIES_KEY), packageNode, onlyGroups, excludedGroups);
        }

        // [optional-dependencies] — same rationale as [dependencies] above: skipped only for the
        // root project / workspace members (their own [project.optional-dependencies] extras),
        // always parsed for any other package so extras needed to resolve that dependency remain.
        if ((!isProjectEntry || onlyGroups.isEmpty()) && dependencyTable.contains(OPTIONAL_DEPENDENCIES_KEY)) {
            parseFilteredGroupDependencies(dependencyTable.getTable(OPTIONAL_DEPENDENCIES_KEY), packageNode, onlyGroups, excludedGroups);
        }
    }

    // Iterates over a TOML group table (dev-dependencies or optional-dependencies) and
    // includes only groups that pass both the onlyGroups allowlist and the excludedGroups denylist.
    private void parseFilteredGroupDependencies(TomlTable groupTable, PackageNode packageNode, Set<String> onlyGroups, Set<String> excludedGroups) {
        for (List<String> keyPath : groupTable.keyPathSet()) {
            String rawGroupName = keyPath.get(0);
            String groupName = rawGroupName.toLowerCase();
            boolean groupAllowed = onlyGroups.isEmpty() || onlyGroups.contains(groupName);
            if (groupAllowed && !excludedGroups.contains(groupName)) {
                parseTransitiveDependencies(groupTable.getArray(rawGroupName), packageNode);
            }
        }
    }


    //store all dependency references of the current package (name + version) on its node
    //uv.lock adds a version to a reference when the referenced package exists in more than one version, e.g. { name = "ipython", version = "8.18.1" }
    private void parseTransitiveDependencies(TomlArray transitiveDependencyArray, PackageNode packageNode) {
        for (int j = 0; j < transitiveDependencyArray.size(); j++) {
            TomlTable currentDependencyTable = transitiveDependencyArray.getTable(j);
            if (currentDependencyTable != null && currentDependencyTable.contains(NAME_KEY)) {
                String referencedName = normalizePackageName(currentDependencyTable.getString(NAME_KEY));
                String referencedVersion = null; // null means the reference does not specify a version
                if (currentDependencyTable.contains(VERSION_KEY)) {
                    referencedVersion = normalizeVersion(currentDependencyTable.getString(VERSION_KEY));
                }
                packageNode.references.add(new DependencyReference(referencedName, referencedVersion));
            }
        }
    }

    //parse over all the workspace members, which will build the dependency graph
    private void generateGraph(UVDetectorOptions uvDetectorOptions) {
        for(String projectNodeKey: projectNodeKeys) {
            PackageNode project = packageNodes.get(projectNodeKey);
            if(!checkIfMemberExcluded(project.name, uvDetectorOptions)) {
                initializeProject(createDependency(project.name, project.version)); // a new workspace member, initialize new code location
                // every graph (code location) is traversed on its own, so every member gets the complete tree of its dependencies
                loopOverDependencies(project, null, new HashSet<>(), uvDetectorOptions); //loop over all direct dependencies of root project
            } else {
                logger.info("Skipping member '{}' as set in the Detect workspace property.", project.name);
            }
        }
    }

    // parse all dependency references which we had stored on the node of each package while parsing it the first time
    // parentDependency is the graph dependency of the given package, null for the root project (its dependencies are direct ones)
    private void loopOverDependencies(PackageNode packageNode, Dependency parentDependency, Set<String> visitedPackages, UVDetectorOptions uvDetectorOptions) {

        // visited is tracked by name + version, so another version of the same package is still expanded
        if(!visitedPackages.add(packageNode.key)) {
            return;
        }

        for(DependencyReference reference: packageNode.references) {
            for(PackageNode referencedPackage: resolveReference(reference, packageNode)) {
                if(!checkIfMemberExcluded(referencedPackage.name, uvDetectorOptions)) {
                    handleTransitiveDependency(referencedPackage, parentDependency, visitedPackages, uvDetectorOptions);
                }
            }
        }
    }

    private void handleTransitiveDependency(PackageNode referencedPackage, Dependency parentDependency, Set<String> visitedPackages, UVDetectorOptions uvDetectorOptions) {
        Dependency currentDependency = createDependency(referencedPackage.name, referencedPackage.version);
        // the edge is always added, a name + version shared by multiple parents is one node referenced by all of them
        addDependencyToGraph(currentDependency, parentDependency);
        loopOverDependencies(referencedPackage, currentDependency, visitedPackages, uvDetectorOptions);
    }

    // Finds the package(s) of the lock file a dependency reference points to:
    //  - the reference has a version: exactly that name + version
    //  - the reference has no version and the name exists in one version only: that version
    //  - the reference has no version and the name exists in several versions: ALL of them, we don't pick one arbitrarily
    private List<PackageNode> resolveReference(DependencyReference reference, PackageNode referrer) {
        Set<String> availableVersions = versionsByName.get(reference.name);
        if(availableVersions == null || availableVersions.isEmpty()) {
            logger.warn("There seems to be a mismatch in the uv.lock. A dependency could not be found: {}", reference.name);
            return Collections.emptyList();
        }

        if(reference.version != null) {
            PackageNode referencedPackage = packageNodes.get(packageKey(reference.name, reference.version));
            if(referencedPackage == null) {
                logger.warn("There seems to be a mismatch in the uv.lock. A dependency could not be found: {} (version {}), available versions: {}", reference.name, reference.version, availableVersions);
                return Collections.emptyList();
            }
            return Collections.singletonList(referencedPackage);
        }

        List<PackageNode> referencedPackages = new ArrayList<>();
        for(String version: availableVersions) {
            referencedPackages.add(packageNodes.get(packageKey(reference.name, version)));
        }
        if(referencedPackages.size() > 1) {
            logger.warn("The uv.lock entry {} {} depends on '{}' without specifying a version, but '{}' exists in multiple versions {}. The dependency is attached to all of these versions.",
                referrer.name, referrer.version, reference.name, reference.name, availableVersions);
        }
        return referencedPackages;
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

    // unique identity of a package: normalized name + version (neither can contain "==")
    private String packageKey(String normalizedName, String version) {
        return normalizedName + "==" + version;
    }

    // parse and store all the workspace members
    private void collectWorkspaceMembers(TomlParseResult uvLockObject) {
        if(uvLockObject.get(MANIFEST_KEY) != null) {
            TomlTable manifestTable = uvLockObject.getTable(MANIFEST_KEY);
            if(manifestTable.get(MEMBERS_KEY) != null) {
                TomlArray membersArray = manifestTable.getArray(MEMBERS_KEY);
                for(int i = 0; i < membersArray.size(); i++) {
                    workSpaceMembers.add(normalizePackageName(membersArray.getString(i)));
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
        CodeLocation codeLocation = new CodeLocation(dependencyGraph, projectDependency.getExternalId());
        codeLocations.add(codeLocation);
    }

    private Dependency createDependency(String dependencyName, String dependencyVersion) {
        String normalizedDependencyName = normalizePackageName(dependencyName);
        ExternalId externalId = externalIdFactory.createNameVersionExternalId(Forge.PYPI, normalizedDependencyName, dependencyVersion);
        return new Dependency(normalizedDependencyName, dependencyVersion, externalId);
    }

    // A package of the lock file, identified by normalized name + version, together with the dependencies it declares.
    private static final class PackageNode {
        private final String key;
        private final String name;
        private final String version;
        private final Set<DependencyReference> references = new LinkedHashSet<>();

        private PackageNode(String key, String name, String version) {
            this.key = key;
            this.name = name;
            this.version = version;
        }
    }

    // A dependency as it is written in the lock file. The version is only present when uv.lock disambiguates a package that exists in several versions.
    private static final class DependencyReference {
        private final String name;
        private final String version; // null if the reference does not specify one

        private DependencyReference(String name, String version) {
            this.name = name;
            this.version = version;
        }

        @Override
        public boolean equals(Object other) {
            if(this == other) {
                return true;
            }
            if(other == null || getClass() != other.getClass()) {
                return false;
            }
            DependencyReference that = (DependencyReference) other;
            return name.equals(that.name) && Objects.equals(version, that.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, version);
        }
    }

}

