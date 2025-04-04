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
    private final Map<NameVersion, List<GoGraphRelationship>> relationshipMapNEW;
    private final Set<String> excludedModules; // comes from detect property. to leave that unchanged, modules cannot have version specifier? only name?
    private static int count = 0;

    public GoRelationshipManager(List<GoGraphRelationship> goGraphRelationships, Set<String> excludedModules) {
        this.excludedModules = excludedModules;
        relationshipMapNEW = new HashMap<>();
        for (GoGraphRelationship goGraphRelationship : goGraphRelationships) {
            NameVersion parentNameVersion = goGraphRelationship.getParent();
            relationshipMapNEW.putIfAbsent(parentNameVersion, new LinkedList<>());
            relationshipMapNEW.get(parentNameVersion).add(goGraphRelationship);
        }
    }

    public boolean hasRelationshipsForNEW(NameVersion moduleNameVersion) {
        return relationshipMapNEW.containsKey(moduleNameVersion);
    }

    public List<GoGraphRelationship> getRelationshipsForNEW(NameVersion moduleNameVersion) {
        return Optional.ofNullable(relationshipMapNEW.get(moduleNameVersion)).orElse(Collections.emptyList());
    }

    public boolean isModuleExcluded(String moduleName) { // can correspond to "main module does not need xyz" but dont want to cause confusion with jus the output that says "module not used by main"
        return excludedModules.contains(moduleName);
    }
}
