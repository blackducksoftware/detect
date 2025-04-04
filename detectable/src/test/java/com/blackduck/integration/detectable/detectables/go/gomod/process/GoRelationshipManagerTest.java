package com.blackduck.integration.detectable.detectables.go.gomod.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.detectables.go.gomod.model.GoGraphRelationship;
import com.blackduck.integration.util.NameVersion;

class GoRelationshipManagerTest {
    static NameVersion parent = new NameVersion("parent", "v1");
    static NameVersion child = new NameVersion("child, v2");
    static NameVersion child2 = new NameVersion("child2", "v4");
    static NameVersion grandchild = new NameVersion("grandchild1", "v3");
    static GoRelationshipManager goRelationshipManagerSimple;
    static GoRelationshipManager goRelationshipManagerEdgeCase1;
    static GoRelationshipManager goRelationshipManagerEdgeCase2;
    // IDETECT-4602 edge case
    static NameVersion parentv1 = new NameVersion("parent", "v1");
    static NameVersion parentv2 = new NameVersion("parent", "v2");
    static NameVersion transitiveAv1 = new NameVersion("transitiveA", "v1");
    static NameVersion transitiveBv1 = new NameVersion("transitiveB", "v1");
    static NameVersion transitiveCv1 = new NameVersion("transitiveC", "v1");
    static NameVersion transitiveCv2 = new NameVersion("transitiveC", "v2");
    // IDETECT-4180 edge case
    static NameVersion mainModule = new NameVersion("code.byted.org/tiktok/rpc_gen", "");
    static NameVersion trueDirectDep = new NameVersion("code.byted.org/tiktok/user_core_client", "v1.0.47");
    static NameVersion versionedMainModuleAsTransitiveDep = new NameVersion("code.byted.org/tiktok/rpc_gen", "v1.2.8");
    static NameVersion transitiveDep = new NameVersion("bou.ke/monkey", "v1.0.2");

    @BeforeAll
    static void init() {
        List<GoGraphRelationship> goGraphRelationshipsSimple = Arrays.asList(
            new GoGraphRelationship(parent, child),
            new GoGraphRelationship(parent, child2),
            new GoGraphRelationship(child, grandchild)
        );
        Set<String> excludedModules = new HashSet<>();
        excludedModules.add(child2.getName());
        goRelationshipManagerSimple = new GoRelationshipManager(goGraphRelationshipsSimple, excludedModules);

        List<GoGraphRelationship> goGraphRelationshipsEdgeCase1 = Arrays.asList(
            new GoGraphRelationship(parentv1, transitiveAv1),
            new GoGraphRelationship(parentv1, transitiveCv1),
            new GoGraphRelationship(parentv2, transitiveBv1),
            new GoGraphRelationship(parentv2, transitiveCv2)
        );
        goRelationshipManagerEdgeCase1 = new GoRelationshipManager(goGraphRelationshipsEdgeCase1, Collections.emptySet());

        List<GoGraphRelationship> goGraphRelationshipsEdgeCase2 = Arrays.asList(
                new GoGraphRelationship(mainModule, trueDirectDep),
                new GoGraphRelationship(trueDirectDep, versionedMainModuleAsTransitiveDep),
                new GoGraphRelationship(versionedMainModuleAsTransitiveDep, transitiveDep)
        );
        goRelationshipManagerEdgeCase2 = new GoRelationshipManager(goGraphRelationshipsEdgeCase2, Collections.emptySet());
    }


    @Test
    void testDifferentVersionsOfDirectAndTransitiveDependency() {
        // Test that parentv1 does not incorrectly get transitives of parentv2 assigned to it
        // Confirm parentv1 and parentv2 have distinct relationships
        assertTrue(goRelationshipManagerEdgeCase1.hasRelationshipsForNEW(parentv1));
        assertTrue(goRelationshipManagerEdgeCase1.hasRelationshipsForNEW(parentv2));

        List<GoGraphRelationship> parentV1Relationships = goRelationshipManagerEdgeCase1.getRelationshipsForNEW(parentv1);
        // check if any parentv2 relationships snuck in
        boolean parentv2Present = parentV1Relationships.stream().anyMatch(r -> parentv2.equals(r.getParent()));
        assertFalse(parentv2Present, "Expected only parentv1 relationships.");
    }

    @Test
    void testVersionedMainModuleAsTransitiveDependency() {
        // Test that bou.ke/monkey is not incorrectly classified as a direct dependency of the main module (rpc_gen)
        // Confirm that both main module and the versioned module with the same name as the main module have distinct relationships
        assertTrue(goRelationshipManagerEdgeCase2.hasRelationshipsForNEW(mainModule));
        assertTrue(goRelationshipManagerEdgeCase2.hasRelationshipsForNEW(versionedMainModuleAsTransitiveDep));

        List<GoGraphRelationship> directDependencies = goRelationshipManagerEdgeCase2.getRelationshipsForNEW(mainModule);
        boolean trueDirectDepPresent = directDependencies.stream().anyMatch(r -> trueDirectDep.equals(r.getChild()));
        boolean transitiveDepPresent = directDependencies.stream().anyMatch(r -> transitiveDep.equals(r.getChild()));
        assertTrue(trueDirectDepPresent && !transitiveDepPresent, "Expected only true-direct dependencies.");
    }
    @Test
    void parentRelationshipTest() {
        List<GoGraphRelationship> parentRelationships = goRelationshipManagerSimple.getRelationshipsForNEW(parent);
        assertEquals(2, parentRelationships.size());

        assertEquals(parent, parentRelationships.get(0).getParent());
        assertEquals(child, parentRelationships.get(0).getChild());

        assertEquals(parent, parentRelationships.get(1).getParent());
        assertEquals(child2, parentRelationships.get(1).getChild());
    }

    @Test
    void childRelationshipTest() {
        List<GoGraphRelationship> childRelationships = goRelationshipManagerSimple.getRelationshipsForNEW(child);
        assertEquals(1, childRelationships.size());
        assertEquals(child, childRelationships.get(0).getParent());
        assertEquals(grandchild, childRelationships.get(0).getChild());
    }

    @Test
    void noRelationshipTest() {
        boolean hasRelationships = goRelationshipManagerSimple.hasRelationshipsForNEW(grandchild);
        assertFalse(hasRelationships);

        List<GoGraphRelationship> childRelationships = goRelationshipManagerSimple.getRelationshipsForNEW(grandchild);
        assertEquals(0, childRelationships.size());
    }

    @Test
    void moduleUsageTest() {
        boolean isNotUsedByMainModule = goRelationshipManagerSimple.isModuleExcluded(child2.getName());
        assertTrue(isNotUsedByMainModule, child2.getName() + " should not be used by the main module according to exclusions.");
    }

}
