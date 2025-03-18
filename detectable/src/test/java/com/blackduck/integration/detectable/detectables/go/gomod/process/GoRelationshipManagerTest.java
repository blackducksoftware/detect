package com.blackduck.integration.detectable.detectables.go.gomod.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.detectables.go.gomod.model.GoGraphRelationship;
import com.blackduck.integration.detectable.detectables.go.gomod.process.GoRelationshipManager;
import com.blackduck.integration.util.NameVersion;

class GoRelationshipManagerTest {
    static NameVersion parent = new NameVersion("parent", "v1");
    static NameVersion child = new NameVersion("child, v2");
    static NameVersion child2 = new NameVersion("child2", "v4");
    static NameVersion grandchild = new NameVersion("grandchild1", "v3");
    static GoRelationshipManager goRelationshipManager;
    // 4602 use case
    static NameVersion parentv1 = new NameVersion("parent", "v1");
    static NameVersion parentv2 = new NameVersion("parent", "v2");
    static NameVersion transitiveAv1 = new NameVersion("transitiveA", "v1");
    static NameVersion transitiveBv1 = new NameVersion("transitiveB", "v1");
    static NameVersion transitiveCv1 = new NameVersion("transitiveC", "v1");
    static NameVersion transitiveCv2 = new NameVersion("transitiveC", "v2");


    @BeforeAll
    static void init() {
        List<GoGraphRelationship> goGraphRelationships = Arrays.asList(
//            new GoGraphRelationship(parent, child),
//            new GoGraphRelationship(parent, child2),
//            new GoGraphRelationship(child, grandchild),
                new GoGraphRelationship(parentv1, transitiveAv1),
                new GoGraphRelationship(parentv1, transitiveCv1),
                new GoGraphRelationship(parentv2, transitiveBv1),
                new GoGraphRelationship(parentv2, transitiveCv2)
        );

        Set<String> excludedModules = new HashSet<>();
        excludedModules.add(child2.getName());

        goRelationshipManager = new GoRelationshipManager(goGraphRelationships, excludedModules);
    }


    @Test
    void testIncorrectDirectDepTransitives() {
        // test that parentv1 incorrectly gets transitives of parentv2 assigned to it
        // parentv1 and v2 are present in relationship map (indistinguishable due to version being stripped)
        assertTrue(goRelationshipManager.hasRelationshipsFor(parentv1.getName()));
        assertTrue(goRelationshipManager.hasRelationshipsFor(parentv2.getName()));
        List<GoGraphRelationship> parentRelationships = goRelationshipManager.getRelationshipsFor(parentv1.getName());
            // getRelationshipsFor(parentv1) returns alllll relationships for v1 and v2.
    }

    @Test
    void parentRelationshipTest() {
        List<GoGraphRelationship> parentRelationships = goRelationshipManager.getRelationshipsFor(parent.getName());
        assertEquals(2, parentRelationships.size());

        assertEquals(parent, parentRelationships.get(0).getParent());
        assertEquals(child, parentRelationships.get(0).getChild());

        assertEquals(parent, parentRelationships.get(1).getParent());
        assertEquals(child2, parentRelationships.get(1).getChild());
    }

    @Test
    void childRelationshipTest() {
        List<GoGraphRelationship> childRelationships = goRelationshipManager.getRelationshipsFor(child.getName());
        assertEquals(1, childRelationships.size());
        assertEquals(child, childRelationships.get(0).getParent());
        assertEquals(grandchild, childRelationships.get(0).getChild());
    }

    @Test
    void noRelationshipTest() {
        boolean hasRelationships = goRelationshipManager.hasRelationshipsFor(grandchild.getName());
        assertFalse(hasRelationships);

        List<GoGraphRelationship> childRelationships = goRelationshipManager.getRelationshipsFor(grandchild.getName());
        assertEquals(0, childRelationships.size());
    }

    @Test
    void moduleUsageTest() {
        boolean isNotUsedByMainModule = goRelationshipManager.isNotUsedByMainModule(child2.getName());
        assertTrue(isNotUsedByMainModule, child2.getName() + " should not be used by the main module according to exclusions.");
    }

}
