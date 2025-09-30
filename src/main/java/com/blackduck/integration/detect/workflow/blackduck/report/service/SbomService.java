package com.blackduck.integration.detect.workflow.blackduck.report.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
        component.setGroup("com.example");
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
        for (Dependency dependency : allDependencies) {
            addDependencyToSbom(dependency, bom);
            // Also add transitive dependencies
            addTransitiveDependenciesToSbom(dependency, dependencyGraph, bom);
        }
    }

    private static void addDependencyToSbom(Dependency dependency, Bom bom) {
        Component component = new Component();
        component.setType(Component.Type.LIBRARY);
        component.setName(dependency.getName());
        component.setVersion(dependency.getVersion());

        // Extract group/namespace from external ID if available
        if (dependency.getExternalId() != null) {
            String[] moduleNames = dependency.getExternalId().getModuleNames();
            if (moduleNames != null && moduleNames.length > 0) {
                component.setGroup(moduleNames[0]);
            }

            // Generate appropriate PURL based on forge type
            String purl = generatePurl(dependency);
            if (purl != null) {
                component.setPurl(purl);
            }
        }

        bom.addComponent(component);
    }

    private static void addTransitiveDependenciesToSbom(Dependency parent, DependencyGraph dependencyGraph, Bom bom) {
        Set<Dependency> children = dependencyGraph.getChildrenForParent(parent);
        for (Dependency child : children) {
            addDependencyToSbom(child, bom);
            // Recursively add transitive dependencies
            addTransitiveDependenciesToSbom(child, dependencyGraph, bom);
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

        switch (forge.getName().toLowerCase()) {
            case "maven":
                return String.format("pkg:maven/%s/%s@%s", namespace, name, version);
            case "npm":
                return String.format("pkg:npm/%s@%s", name, version);
            case "pypi":
                return String.format("pkg:pypi/%s@%s", name, version);
            case "nuget":
                return String.format("pkg:nuget/%s@%s", name, version);
            default:
                return String.format("pkg:generic/%s@%s", name, version);
        }
    }
}