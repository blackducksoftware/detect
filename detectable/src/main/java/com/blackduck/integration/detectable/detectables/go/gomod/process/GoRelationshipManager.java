package com.blackduck.integration.detectable.detectables.go.gomod.process;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.blackduck.integration.detectable.detectables.go.gomod.model.GoGraphRelationship;
import com.blackduck.integration.util.NameVersion;

public class GoRelationshipManager {
//    private final Map<String, List<GoGraphRelationship>> relationshipMap;
    private final Map<NameVersion, List<GoGraphRelationship>> relationshipMapNEW;
    private final Set<String> excludedModules; // comes from detect property. to leave that unchanged, modules cannot have version specifier? only name?

    public GoRelationshipManager(List<GoGraphRelationship> goGraphRelationships, Set<String> excludedModules) {
        this.excludedModules = excludedModules;
//        relationshipMap = new HashMap<>();
        relationshipMapNEW = new HashMap<>();
        for (GoGraphRelationship goGraphRelationship : goGraphRelationships) {
            NameVersion parentNameVersion = goGraphRelationship.getParent();
            relationshipMapNEW.putIfAbsent(parentNameVersion, new LinkedList<>());
            relationshipMapNEW.get(parentNameVersion).add(goGraphRelationship);
        }
    }

//    public GoRelationshipManager(List<GoGraphRelationship> goGraphRelationships, Set<String> excludedModules) {
//        this.excludedModules = excludedModules;
//        relationshipMap = new HashMap<>();
//        for (GoGraphRelationship goGraphRelationship : goGraphRelationships) {
//            NameVersion parentName = goGraphRelationship.getParent();
//            relationshipMap.putIfAbsent(parentName, new LinkedList<>()); // TODO test to confirm new equals() works as expected
//            relationshipMap.get(parentName).add(goGraphRelationship);
//        }
//    }

//    public boolean hasRelationshipsFor(String moduleName) {
//        return relationshipMap.containsKey(moduleName);
//    }

    public boolean hasRelationshipsForNEW(NameVersion moduleNameVersion) {
        return relationshipMapNEW.containsKey(moduleNameVersion);
    }

//    public List<GoGraphRelationship> getRelationshipsFor(String moduleName) {
//        return Optional.ofNullable(relationshipMap.get(moduleName)).orElse(Collections.emptyList());
//    }

    public List<GoGraphRelationship> getRelationshipsForNEW(NameVersion moduleNameVersion) {
        return Optional.ofNullable(relationshipMapNEW.get(moduleNameVersion)).orElse(Collections.emptyList());
    }

    public boolean isNotUsedByMainModule(String moduleName) {
        return excludedModules.contains(moduleName);
    }

    public boolean isNotUsedByMainModuleNEW(NameVersion moduleNameVersion) {
        return excludedModules.contains(moduleNameVersion.getName());
    }
}
