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
    private final Set<String> excludedModules; // comes from detect property. to leave that unchanged, modules cannot have version specifier? only name?

    public GoRelationshipManager(List<GoGraphRelationship> goGraphRelationships, Set<String> excludedModules) {
        this.excludedModules = excludedModules;
        relationshipMap = new HashMap<>();
        for (GoGraphRelationship goGraphRelationship : goGraphRelationships) {
            NameVersion parentNameVersion = goGraphRelationship.getParent(); // the version here isnt the shortened hash?
            relationshipMap.putIfAbsent(parentNameVersion, new LinkedList<>());
            relationshipMap.get(parentNameVersion).add(goGraphRelationship);
        }
    }

    public boolean hasRelationshipsFor(NameVersion moduleNameVersion) {
        return relationshipMap.containsKey(moduleNameVersion);
    }

    public List<GoGraphRelationship> getRelationshipsFor(NameVersion moduleNameVersion) {
        return Optional.ofNullable(relationshipMap.get(moduleNameVersion)).orElse(Collections.emptyList()); // why return empty list when we only ever call this method after checking hasRelationshipsForNEW()
    }

    public boolean isModuleExcluded(String moduleName) { // can correspond to "main module does not need xyz" but dont want to cause confusion with jus the output that says "module not used by main"
        return excludedModules.contains(moduleName);
    }
}
