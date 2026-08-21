package com.blackduck.integration.detectable.detectables.bun.lockfile;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunLockDependency;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunLockPackage;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunLockResult;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunLockfileData;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.NullSafePackageJson;

public class BunLockfileTransformer {
    private final ExternalIdFactory externalIdFactory;

    public BunLockfileTransformer(ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
    }

    public List<CodeLocation> generateCodeLocations(BunLockResult result, NullSafePackageJson packageJson) {
        BunLockfileData data = result.getData();
        Map<String, Map<String, String>> rangeToVersion = data.getRangeToVersion();
        BasicDependencyGraph graph = new BasicDependencyGraph();

        // Seed the graph root from package.json direct dependencies
        Map<String, String> rootDeps = new LinkedHashMap<>();
        rootDeps.putAll(packageJson.getDependencies());
        rootDeps.putAll(packageJson.getDevDependencies());
        for (Map.Entry<String, String> entry : rootDeps.entrySet()) {
            Dependency dep = resolve(entry.getKey(), entry.getValue(), rangeToVersion);
            if (dep != null) {
                graph.addDirectDependency(dep);
            }
        }

        // Wire all transitive edges; graph traversal at BDIO time enforces reachability from root
        for (BunLockPackage pkg : data.getPackages()) {
            Dependency parent = makeDep(pkg.getName(), pkg.getVersion());
            for (BunLockDependency dep : pkg.getDependencies()) {
                Dependency child = resolve(dep.getName(), dep.getRange(), rangeToVersion);
                if (child != null) {
                    graph.addChildWithParent(child, parent);
                }
            }
        }

        return Collections.singletonList(new CodeLocation(graph));
    }

    private Dependency resolve(String name, String range, Map<String, Map<String, String>> rangeToVersion) {
        Map<String, String> versions = rangeToVersion.get(name);
        if (versions == null) {
            return null;
        }
        String version = versions.get(range);
        if (version == null) {
            return null;
        }
        return makeDep(name, version);
    }

    private Dependency makeDep(String name, String version) {
        ExternalId externalId = externalIdFactory.createNameVersionExternalId(Forge.NPMJS, name, version);
        return new Dependency(name, version, externalId);
    }
}
