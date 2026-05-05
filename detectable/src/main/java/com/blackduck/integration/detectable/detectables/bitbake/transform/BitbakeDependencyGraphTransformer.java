package com.blackduck.integration.detectable.detectables.bitbake.transform;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.bitbake.BitbakeDependencyType;
import com.blackduck.integration.detectable.detectables.bitbake.model.BitbakeGraph;
import com.blackduck.integration.detectable.detectables.bitbake.model.BitbakeNode;

public class BitbakeDependencyGraphTransformer {
    private static final String NATIVE_SUFFIX = "-native";
    public static final String VERSION_WITH_EPOCH_PREFIX_REGEX = "^[0-9]+:.*";
    public static final String VIRTUAL_PREFIX = "virtual/";
    public static final String AUTOINC_REGEX = "AUTOINC\\+[\\w|\\d]*";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final EnumListFilter<BitbakeDependencyType> dependencyTypeFilter;

    public BitbakeDependencyGraphTransformer(EnumListFilter<BitbakeDependencyType> dependencyTypeFilter) {
        this.dependencyTypeFilter = dependencyTypeFilter;
    }

    public DependencyGraph transform(BitbakeGraph bitbakeGraph, Map<String, List<String>> recipeLayerMap, Map<String, String> imageRecipes) {
        Map<String, Dependency> namesToExternalIds = generateExternalIds(bitbakeGraph, recipeLayerMap, imageRecipes);
        return buildGraph(bitbakeGraph, namesToExternalIds);
    }

    @NotNull
    private Map<String, Dependency> generateExternalIds(BitbakeGraph bitbakeGraph, Map<String, List<String>> recipeLayerMap, Map<String, String> imageRecipes) {
        Map<String, Dependency> namesToExternalIds = new HashMap<>();
        for (BitbakeNode bitbakeNode : bitbakeGraph.getNodes()) {
            String name = bitbakeNode.getName();

            if (bitbakeNode.getVersion().isPresent()) {
                String version = bitbakeNode.getVersion().get();
                Optional<String> actualLayer = bitbakeNode.getLayer();
                if (dependencyTypeFilter.shouldInclude(BitbakeDependencyType.BUILD) || !isBuildDependency(imageRecipes, name, version)) {
                    Optional<Dependency> dependency = generateExternalId(name, version, actualLayer.orElse(null), recipeLayerMap).map(Dependency::new);
                    dependency.ifPresent(value -> namesToExternalIds.put(bitbakeNode.getName(), value));
                } else {
                    logger.debug("Excluding BUILD dependency: {}:{}", name, version);
                }
            } else if (name.startsWith(VIRTUAL_PREFIX)) {
                logger.debug("Virtual component '{}' found. Excluding from graph.", name);
            } else {
                logger.debug("No version found for component '{}'. It is likely not a real component.", name);
            }
        }
        return namesToExternalIds;
    }

    @NotNull
    private DependencyGraph buildGraph(BitbakeGraph bitbakeGraph, Map<String, Dependency> namesToExternalIds) {
        DependencyGraph dependencyGraph = new BasicDependencyGraph();
        for (BitbakeNode bitbakeNode : bitbakeGraph.getNodes()) {
            String name = bitbakeNode.getName();

            if (namesToExternalIds.containsKey(name)) {
                Dependency dependency = namesToExternalIds.get(bitbakeNode.getName());
                dependencyGraph.addChildToRoot(dependency);

                for (String child : bitbakeNode.getChildren()) {
                    if (namesToExternalIds.containsKey(child)) {
                        Dependency childDependency = namesToExternalIds.get(child);
                        dependencyGraph.addParentWithChild(dependency, childDependency);
                    }
                }
            }
        }
        return dependencyGraph;
    }

    private boolean isBuildDependency(Map<String, String> imageRecipes, String recipeName, String recipeVersion) {
        if (foundInImageRecipes(imageRecipes, recipeName, recipeVersion)) {
            if (recipeName.endsWith(NATIVE_SUFFIX)) {
                logger.trace("{} classified as a build dependency due to it's '{}' suffix", recipeName, NATIVE_SUFFIX);
                return true;
            } else {
                return false;
            }
        } else {
            logger.trace("{} classified as a build dependency since it was not found in the license manifest", recipeName);
            return true;
        }
    }

    private boolean foundInImageRecipes(Map<String, String> imageRecipes, String recipeName, String recipeVersion) {
        if (imageRecipes.containsKey(recipeName)) {
            String imageRecipeVersion = imageRecipes.get(recipeName);
            String recipeWithoutEpoch = removeEpochPrefix(recipeVersion);
            if (recipeWithoutEpoch.startsWith(imageRecipeVersion)) {
                return true;
            } else {
                logger.debug("Recipe {}/{} is included in the image, but version {} is not.", recipeName, imageRecipeVersion, recipeVersion);
            }
        }
        return false;
    }

    private String removeEpochPrefix(String recipeVersion) {
        String recipeWithoutEpoch = recipeVersion;
        if (recipeVersion.matches(VERSION_WITH_EPOCH_PREFIX_REGEX)) {
            int colonPos = recipeVersion.indexOf(':');
            recipeWithoutEpoch = recipeVersion.substring(colonPos + 1);
            logger.trace("Recipe Version without epoch for {}: {}", recipeVersion, recipeWithoutEpoch);
        }
        return recipeWithoutEpoch;
    }

    private Optional<ExternalId> generateExternalId(String dependencyName, String dependencyVersion, @Nullable String dependencyLayer, Map<String, List<String>> recipeLayerMap) {
        List<String> recipeLayerNames = recipeLayerMap.get(dependencyName);
        ExternalId externalId = null;
        if (recipeLayerNames != null) {
            dependencyLayer = chooseRecipeLayer(dependencyName, dependencyLayer, recipeLayerNames);
            externalId = ExternalId.FACTORY.createYoctoExternalId(dependencyLayer, dependencyName, dependencyVersion);
        } else {
            logger.debug("Failed to find component '{}' in component layer map. [dependencyVersion: {}; dependencyLayer: {}", dependencyName, dependencyVersion, dependencyLayer);
            if (dependencyName.endsWith(NATIVE_SUFFIX)) {
                String alternativeName = dependencyName.replace(NATIVE_SUFFIX, "");
                logger.debug("Generating alternative component name '{}' for '{}=={}'", alternativeName, dependencyName, dependencyVersion);
                externalId = generateExternalId(alternativeName, dependencyVersion, dependencyLayer, recipeLayerMap).orElse(null);
            } else {
                logger.debug("'{}:{}' is not an actual component. Excluding from graph.", dependencyName, dependencyVersion);
            }
        }

        if (externalId != null && externalId.getVersion().contains("AUTOINC")) {
            externalId.setVersion(externalId.getVersion().replaceFirst(AUTOINC_REGEX, "X"));
        }

        return Optional.ofNullable(externalId);
    }

    private String chooseRecipeLayer(String dependencyName, @Nullable String dependencyLayer, List<String> recipeLayerNames) {
        // Validate: path-matched layer must exist in authoritative show-recipes list
        if (dependencyLayer != null && recipeLayerNames.contains(dependencyLayer)) {
            logger.trace("For dependency recipe {}: using layer {} parsed from task-depends.dot", dependencyName, dependencyLayer);
            return dependencyLayer;
        }

        // Path-matched layer is null or invalid - fall back to authoritative source
        if (dependencyLayer == null) {
            logger.warn(
                "Did not parse a layer for dependency {} from task-depends.dot; falling back to layer {} (first from show-recipes output)",
                dependencyName,
                recipeLayerNames.get(0)
            );
        } else {
            logger.debug(
                "Path-matched layer '{}' not valid for recipe '{}' (valid layers: {}); using '{}'",
                dependencyLayer,
                dependencyName,
                recipeLayerNames,
                recipeLayerNames.get(0)
            );
        }
        return recipeLayerNames.get(0);
    }
}
