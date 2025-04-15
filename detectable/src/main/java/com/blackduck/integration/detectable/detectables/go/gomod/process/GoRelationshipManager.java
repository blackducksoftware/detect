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
    private final Map<NameVersion, List<GoGraphRelationship>> relationshipMap;
    private final Set<String> excludedModules;

    public GoRelationshipManager(List<GoGraphRelationship> goGraphRelationships, Set<String> excludedModules) {
        this.excludedModules = excludedModules;
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

    public boolean isModuleExcluded(String moduleName) {
        return excludedModules.contains(moduleName);
    }
}
