package com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.bdio.model.dependency.Dependency;

import static org.junit.jupiter.api.Assertions.*;


public class GoDependencyNodeTest {

    @Test
    public void testGoDependencyNodeCreation() {
        GoDependencyNode node = new GoDependencyNode(
            false,
            new Dependency("example.com/module", "v1.0.0", null, null),
            new java.util.ArrayList<>()
        );
        
        assertTrue(node.getDependency().getName().equals("example.com/module"));
        assertTrue(node.getDependency().getVersion().equals("v1.0.0"));
    }

    @Test
    public void testGoDependencyNodeEquality() {
        GoDependencyNode node1 = new GoDependencyNode(
            false,
            new Dependency("example.com/module", "v1.0.0", null, null),
            new java.util.ArrayList<>()
        );
        GoDependencyNode node2 = new GoDependencyNode(
            false,
            new Dependency("example.com/module", "v1.0.0", null, null),
            new java.util.ArrayList<>()
        );

        assertTrue(node1.equals(node2));
    }

    @Test
    public void testGoDependencyNodeNotEqual() {
        GoDependencyNode node1 = new GoDependencyNode(
            false,
            new Dependency("example.com/module", "v1.0.0", null, null),
            new java.util.ArrayList<>()
        );
        GoDependencyNode node2 = new GoDependencyNode(
            false,
            new Dependency("example.com/other", "v1.0.0", null, null),
            new java.util.ArrayList<>()
        );

        assertTrue(!node1.equals(node2));
    }
}