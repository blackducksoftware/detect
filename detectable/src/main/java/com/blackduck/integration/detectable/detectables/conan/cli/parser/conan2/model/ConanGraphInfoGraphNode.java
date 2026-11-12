package com.blackduck.integration.detectable.detectables.conan.cli.parser.conan2.model;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.annotations.SerializedName;
import com.blackduck.integration.util.Stringable;

public class ConanGraphInfoGraphNode extends Stringable {
    
    private final String name;

    private final String version;

    @SerializedName("rrev")
    private final String recipeRevision;

    @SerializedName("prev")
    private final String packageRevision;

    @SerializedName("package_id")
    private final String packageId;

    private final String channel;

    private final String user;

    private final Map<Integer, ConanGraphInfoDependency> dependencies;

    public ConanGraphInfoGraphNode(
        String name,
        String version,
        String recipeRevision,
        String packageRevision,
        String packageId,
        String channel,
        String user,
        Map<Integer, ConanGraphInfoDependency> dependencies)
    {
        this.name = name;
        this.version = version;
        this.recipeRevision = recipeRevision;
        this.packageRevision = packageRevision;
        this.packageId = packageId;
        this.channel = channel;
        this.user = user;
        this.dependencies = dependencies;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String generateExternalIdVersion(boolean preferLongFormExternalIds) {
        String nameVersion = String.format(
            "%s@%s/%s#%s",
            version,
            (channel == null ? "_" : channel),
            (user == null ? "_" : user),
            recipeRevision
        );

        if (preferLongFormExternalIds && packageRevision != null) {
            nameVersion += String.format(
                ":%s#%s",
                packageId == null ? "0" : packageId,
                packageRevision
            );
        }
        return nameVersion;
    }

    public Set<Integer> getDirectDependencyIndices(boolean includeBuild, boolean includeTest) {
        Set<Integer> indices = new HashSet<>(dependencies.keySet());
        indices.removeIf(i -> {
            ConanGraphInfoDependency dep = dependencies.get(i);
            return !dep.isDirect()
                || (!includeTest && dep.isTest())
                || (!includeBuild && dep.isBuild());
        });
        return indices;
    }
}
