package com.blackduck.integration.detectable.detectables.go.gomodfile.parse;

import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoDependencyNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GoDependencyNode equality and hashCode behavior.
 *
 * These tests ensure:
 * 1. The equals/hashCode contract is maintained for node identity
 *    (used by caches and sets in the resolver).
 * 2. Hash code stability when children are added or removed — this is
 *    important because nodes may be used as keys in hash-based collections.
 *
 * Note: These are unit tests focused on node identity semantics. They do NOT
 * perform network or end-to-end resolution and therefore do not prove the
 * full resolver handles all cyclic graphs;
 */
class GoModDependencyResolverInfiniteLoopTest {

    @Test
    void testGoDependencyNode_EqualsHashCodeContract() {
        // Create dependencies with same name/version but different children
        Dependency dep1 = createDependency("golang.org/x/text", "v0.3.7");
        Dependency dep2 = createDependency("golang.org/x/text", "v0.3.7");

        GoDependencyNode node1 = new GoDependencyNode(false, dep1, new ArrayList<>());
        GoDependencyNode node2 = new GoDependencyNode(false, dep2, new ArrayList<>());

        // These should be equal: equality is defined on the underlying Dependency
        assertEquals(node1, node2, "Nodes with same dependency should be equal");
        assertEquals(node1.hashCode(), node2.hashCode(),
                    "Equal nodes must have equal hash codes (equals-hashCode contract)");

        // Add children to node1; this mutates traversal structure but should not
        // affect equality/hashCode because identity is based on the dependency field only.
        Dependency childDep = createDependency("golang.org/x/tools", "v0.1.12");
        GoDependencyNode childNode = new GoDependencyNode(false, childDep, new ArrayList<>());
        node1.addChild(childNode);

        // Even with different children, nodes should still be equal and have same hash code
        assertEquals(node1, node2, "Nodes with same dependency but different children should be equal");
        assertEquals(node1.hashCode(), node2.hashCode(),
                    "Hash code must not change when children change (critical for HashMap)");
    }

    @Test
    void testGoDependencyNode_HashCodeStability() {
        // Test that hash code doesn't change when children are added
        Dependency dep = createDependency("golang.org/x/text", "v0.3.7");
        GoDependencyNode node = new GoDependencyNode(false, dep, new ArrayList<>());

        int initialHashCode = node.hashCode();

        // Add multiple children - this exercises mutation while ensuring stable hash
        for (int i = 0; i < 5; i++) {
            Dependency childDep = createDependency("test/module" + i, "v1.0.0");
            GoDependencyNode childNode = new GoDependencyNode(false, childDep, new ArrayList<>());
            node.addChild(childNode);
        }

        int finalHashCode = node.hashCode();

        assertEquals(initialHashCode, finalHashCode,
                    "Hash code must remain stable when children are added (critical for HashMap cache)");
    }

    @Test
    void testGoDependencyNode_DifferentDependenciesNotEqual() {
        Dependency dep1 = createDependency("golang.org/x/text", "v0.3.7");
        Dependency dep2 = createDependency("golang.org/x/text", "v0.4.0");
        Dependency dep3 = createDependency("golang.org/x/tools", "v0.3.7");

        GoDependencyNode node1 = new GoDependencyNode(false, dep1, new ArrayList<>());
        GoDependencyNode node2 = new GoDependencyNode(false, dep2, new ArrayList<>());
        GoDependencyNode node3 = new GoDependencyNode(false, dep3, new ArrayList<>());

        assertNotEquals(node1, node2, "Nodes with different versions should not be equal");
        assertNotEquals(node1, node3, "Nodes with different names should not be equal");
        assertNotEquals(node2, node3, "Nodes with different dependencies should not be equal");

        // These assertions are 'ideal' (different dependencies likely produce different hash codes),
        // but we keep them rather than making a hard guarantee about absence of collisions.
        assertNotEquals(node1.hashCode(), node2.hashCode(),
                       "Different dependencies should have different hash codes (ideally)");
        assertNotEquals(node1.hashCode(), node3.hashCode(),
                       "Different dependencies should have different hash codes (ideally)");
    }

    @Test
    void testGoDependencyNode_NullDependency() {
        // Nodes representing a root or placeholder with null dependency should still be equal
        GoDependencyNode node1 = new GoDependencyNode(true, null, new ArrayList<>());
        GoDependencyNode node2 = new GoDependencyNode(true, null, new ArrayList<>());

        assertEquals(node1, node2, "Nodes with null dependencies should be equal");
        assertEquals(node1.hashCode(), node2.hashCode(),
                    "Nodes with null dependencies should have same hash code");
    }

    private Dependency createDependency(String name, String version) {
        return new Dependency(name, version, null, null);
    }
}
