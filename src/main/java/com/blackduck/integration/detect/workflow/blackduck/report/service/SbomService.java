package com.blackduck.integration.detect.workflow.blackduck.report.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cyclonedx.Version;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.bdio.BdioReader;
import com.blackduck.integration.bdio.BdioTransformer;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.graph.ProjectDependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.SimpleBdioDocument;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.blackduck.codelocation.upload.UploadTarget;
import com.google.gson.Gson;

public class SbomService {
    private static final Logger logger = LoggerFactory.getLogger(SbomService.class);

    // TODO working hardcoded version
    public static void generateSbom() throws Exception {
        Bom bom = new Bom();

        Component component = new Component();
        component.setType(Component.Type.LIBRARY);
        component.setName("example-lib");
        component.setVersion("1.2.3");
        component.setPurl("pkg:maven/com.example/example-lib@1.2.3");
        bom.addComponent(component);

        // Specify schema version explicitly
        BomJsonGenerator generator = BomGeneratorFactory.createJson(Version.VERSION_16, bom);

        String jsonBom = generator.toJsonString();

        try (FileOutputStream out = new FileOutputStream("bom.json")) {
            out.write(jsonBom.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static void generateSbom(List<UploadTarget> uploadTargets) throws Exception {
        Bom bom = new Bom();

        // Process each BDIO file to extract component information
        for (UploadTarget uploadTarget : uploadTargets) {
            try {
                extractComponentsFromBdio(uploadTarget.getUploadFile(), bom);
            } catch (Exception e) {
                logger.warn("Failed to extract components from BDIO file: {}", uploadTarget.getUploadFile().getName(), e);
            }
        }

        // Specify schema version explicitly
        BomJsonGenerator generator = BomGeneratorFactory.createJson(Version.VERSION_16, bom);

        String jsonBom = generator.toJsonString();

        try (FileOutputStream out = new FileOutputStream("bom.json")) {
            out.write(jsonBom.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static void generateSbom(ProjectDependencyGraph projectDependencyGraph) throws Exception {
        Bom bom = new Bom();

        // Extract components directly from the in-memory dependency graph
        // ProjectDependencyGraph extends DependencyGraph, so we can use it directly
        Set<Dependency> allDependencies = projectDependencyGraph.getDirectDependencies();
        Set<Dependency> visited = new HashSet<>();

        for (Dependency dependency : allDependencies) {
            addDependencyToSbom(dependency, bom, visited);
            // Also add transitive dependencies
            addTransitiveDependenciesToSbom(dependency, projectDependencyGraph, bom, visited);
        }

        // Specify schema version explicitly
        BomJsonGenerator generator = BomGeneratorFactory.createJson(Version.VERSION_16, bom);

        String jsonBom = generator.toJsonString();

        try (FileOutputStream out = new FileOutputStream("bom.json")) {
            out.write(jsonBom.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void extractComponentsFromBdio(File bdioFile, Bom bom) throws IOException {
        Gson gson = new Gson();
        BdioTransformer bdioTransformer = new BdioTransformer();

        SimpleBdioDocument simpleBdioDocument;
        try (InputStream bdioInputStream = new FileInputStream(bdioFile);
             BdioReader bdioReader = new BdioReader(gson, bdioInputStream)) {
            simpleBdioDocument = bdioReader.readSimpleBdioDocument();
        }

        // Transform BDIO to dependency graph to extract all components
        DependencyGraph dependencyGraph = bdioTransformer.transformToDependencyGraph(
            null,
            simpleBdioDocument.getProject(),
            simpleBdioDocument.getComponents()
        );

        // Add all dependencies to the SBOM
        Set<Dependency> allDependencies = dependencyGraph.getDirectDependencies();
        Set<Dependency> visited = new HashSet<>();
        for (Dependency dependency : allDependencies) {
            addDependencyToSbom(dependency, bom, visited);
            // Also add transitive dependencies
            addTransitiveDependenciesToSbom(dependency, dependencyGraph, bom, visited);
        }
    }

    private static void addDependencyToSbom(Dependency dependency, Bom bom, Set<Dependency> visited) {
        // Skip if already visited to avoid duplicates and infinite recursion
        if (visited.contains(dependency)) {
            return;
        }
        visited.add(dependency);

        Component component = new Component();
        component.setType(Component.Type.LIBRARY);
        component.setName(dependency.getName());
        component.setVersion(dependency.getVersion());

        // Extract group/namespace from external ID if available
        if (dependency.getExternalId() != null) {
//            String[] moduleNames = dependency.getExternalId().getModuleNames();
//            if (moduleNames != null && moduleNames.length > 0) {
//                component.setGroup(moduleNames[0]);
//            }

            // Generate appropriate PURL based on forge type
            String purl = generatePurl(dependency);
            if (purl != null) {
                component.setPurl(purl);
            }
        }

        bom.addComponent(component);
    }

    private static void addTransitiveDependenciesToSbom(Dependency parent, DependencyGraph dependencyGraph, Bom bom, Set<Dependency> visited) {
        Set<Dependency> children = dependencyGraph.getChildrenForParent(parent);
        for (Dependency child : children) {
            // Only recurse if we haven't already visited this child
            if (!visited.contains(child)) {
                addDependencyToSbom(child, bom, visited);
                // Recursively add transitive dependencies
                addTransitiveDependenciesToSbom(child, dependencyGraph, bom, visited);
            }
        }
    }

    private static String generatePurl(Dependency dependency) {
        if (dependency.getExternalId() == null || dependency.getExternalId().getForge() == null) {
            return null;
        }

        Forge forge = dependency.getExternalId().getForge();
        String[] moduleNames = dependency.getExternalId().getModuleNames();

        if (moduleNames == null || moduleNames.length < 2) {
            return null;
        }

        String namespace = moduleNames[0];
        String name = dependency.getName();
        String version = dependency.getVersion();

        // Case names must match Forge.getName() values from integration-bdio Forge.class
        // PURL format strings follow the PURL specification: https://github.com/package-url/purl-spec
        switch (forge.getName().toLowerCase()) {
            case "maven":      // Forge.MAVEN
                return String.format("pkg:maven/%s/%s@%s", namespace, name, version);
            case "npmjs":      // Forge.NPMJS
                return String.format("pkg:npm/%s@%s", name, version);
            case "pypi":       // Forge.PYPI
                return String.format("pkg:pypi/%s@%s", name, version);
            case "nuget":      // Forge.NUGET
                return String.format("pkg:nuget/%s@%s", name, version);
            case "rubygems":   // Forge.RUBYGEMS
                return String.format("pkg:gem/%s@%s", name, version);
            case "golang":     // Forge.GOLANG
                return String.format("pkg:golang/%s@%s", name, version);
            case "crates":     // Forge.CRATES
                return String.format("pkg:cargo/%s@%s", name, version);
            case "cocoapods":  // Forge.COCOAPODS
                return String.format("pkg:cocoapods/%s@%s", name, version);
            case "packagist":  // Forge.PACKAGIST
                return String.format("pkg:composer/%s/%s@%s", namespace, name, version);
            case "alpine":     // Forge.ALPINE
                return String.format("pkg:alpine/%s@%s", name, version);
            case "debian":     // Forge.DEBIAN
                return String.format("pkg:deb/%s@%s", name, version);
            case "ubuntu":     // Forge.UBUNTU
                return String.format("pkg:deb/ubuntu/%s@%s", name, version);
            case "centos":     // Forge.CENTOS
                return String.format("pkg:rpm/centos/%s@%s", name, version);
            case "redhat":     // Forge.REDHAT
                return String.format("pkg:rpm/redhat/%s@%s", name, version);
            case "fedora":     // Forge.FEDORA
                return String.format("pkg:rpm/fedora/%s@%s", name, version);
            case "hex":        // Forge.HEX
                return String.format("pkg:hex/%s@%s", name, version);
            case "dart":       // Forge.DART
                return String.format("pkg:pub/%s@%s", name, version);
            case "bower":      // Forge.BOWER
                return String.format("pkg:bower/%s@%s", name, version);
            case "github":     // Forge.GITHUB
                return String.format("pkg:github/%s/%s@%s", namespace, name, version);
            case "cpan":       // Forge.CPAN
                return String.format("pkg:cpan/%s@%s", name, version);
            case "cran":       // Forge.CRAN
                return String.format("pkg:cran/%s@%s", name, version);
            default:
                return String.format("pkg:generic/%s@%s", name, version);
        }
    }
}