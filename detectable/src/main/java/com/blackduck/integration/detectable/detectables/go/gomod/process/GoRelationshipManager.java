package com.blackduck.integration.detectable.detectables.go.gomod.process;

import java.util.*;

import com.blackduck.integration.detectable.detectables.go.gomod.model.GoGraphRelationship;
import com.blackduck.integration.util.NameVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoRelationshipManager {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<NameVersion, List<GoGraphRelationship>> relationshipMap;
    private final Set<String> excludedModules;
    private final Set<String> excludedChildModules;
    private final Set<NameVersion> visitedModules = new HashSet<>();


    public GoRelationshipManager(List<GoGraphRelationship> goGraphRelationships, Set<String> excludedModules) {
        this.excludedModules = excludedModules;
        this.excludedChildModules = new HashSet<>();
        relationshipMap = new HashMap<>();
        for (GoGraphRelationship goGraphRelationship : goGraphRelationships) {
            NameVersion parentNameVersion = goGraphRelationship.getParent();
            relationshipMap.putIfAbsent(parentNameVersion, new LinkedList<>());
            relationshipMap.get(parentNameVersion).add(goGraphRelationship);
        }
    }

    public boolean hasRelationshipsFor(NameVersion moduleNameVersion) {
        return relationshipMap.containsKey(moduleNameVersion);
    }

    public List<GoGraphRelationship> getRelationshipsFor(NameVersion moduleNameVersion) {
        return Optional.ofNullable(relationshipMap.get(moduleNameVersion)).orElse(Collections.emptyList());
    }

    public boolean isModuleExcluded(NameVersion moduleNameVersion) {
        return excludedModules.contains(moduleNameVersion.getName());
    }

    public boolean isParentExcluded(NameVersion childModuleNameVersion) {
        // child is not really an orphan, its parent is just excluded
        return excludedChildModules.contains(childModuleNameVersion.getName());
    }

    public void addChildrenToExcludedModules(NameVersion parentModuleNameVersion) {
        if (hasRelationshipsFor(parentModuleNameVersion)) {
            List<GoGraphRelationship> relationships = getRelationshipsFor(parentModuleNameVersion);
            // When excluding a module during graph building, ensure all its children are also excluded so they do
            // not remain in the graph as orphans
            for (GoGraphRelationship r : relationships) {
               NameVersion childModuleNameVersion = r.getChild();
               if (!visitedModules.contains(childModuleNameVersion)) {
                   visitedModules.add(childModuleNameVersion);
                   excludedChildModules.add(childModuleNameVersion.getName());
                   addChildrenToExcludedModules(childModuleNameVersion);
               }

            }
        }
    }
}
