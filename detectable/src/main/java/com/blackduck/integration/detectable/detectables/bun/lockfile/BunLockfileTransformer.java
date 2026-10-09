package com.blackduck.integration.detectable.detectables.bun.lockfile;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunLockfileData;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunPackage;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunPackageDependency;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.DirectDependency;
import com.blackduck.integration.detectable.util.DependencyCreator;

public class BunLockfileTransformer {

    public List<CodeLocation> generateCodeLocations(BunLockfileData data) {
        Map<String, BunPackage> packagesByKey = buildPackageIndex(data.getPackages());
        BasicDependencyGraph graph = new BasicDependencyGraph();

        for (DirectDependency direct : data.getDirectDependencies()) {
            graph.addDirectDependency(makeDependency(direct.getName(), direct.getVersion()));
        }

        // Wire all transitive edges; graph traversal at BDIO time enforces reachability from root
        for (BunPackage pkg : data.getPackages()) {
            Dependency parent = makeDependency(pkg.getName(), pkg.getVersion());
            for (BunPackageDependency dep : pkg.getDependencies()) {
                BunPackage child = resolvePackage(pkg.getKey(), dep.getName(), packagesByKey);
                if (child != null) {
                    graph.addChildWithParent(makeDependency(child.getName(), child.getVersion()), parent);
                }
            }
        }

        return Collections.singletonList(new CodeLocation(graph));
    }

    private Map<String, BunPackage> buildPackageIndex(List<BunPackage> packages) {
        Map<String, BunPackage> index = new LinkedHashMap<>();
        for (BunPackage pkg : packages) {
            index.put(pkg.getKey(), pkg);
        }
        return index;
    }

    // Ancestor-walk: tries context/depName at each level up to the flat key.
    private BunPackage resolvePackage(String parentKey, String depName, Map<String, BunPackage> packagesByKey) {
        String context = parentKey;
        while (context != null) {
            BunPackage pkg = packagesByKey.get(context + "/" + depName);
            if (pkg != null) {
                return pkg;
            }
            context = parentContext(context);
        }
        return packagesByKey.get(depName);
    }

    // Strips the rightmost logical segment (one component for plain names, two for @scope/name).
    // Returns null when no further parent context exists (at the flat-key level).
    private static String parentContext(String key) {
        int lastSlash = key.lastIndexOf('/');
        if (lastSlash < 0) {
            return null;
        }
        // If the segment before lastSlash starts with '@', the tail is the second half of
        // @scope/name -- strip both components together so we land on the true parent key.
        int previousSlash = key.lastIndexOf('/', lastSlash - 1);
        int cutAt = (previousSlash >= 0 && key.charAt(previousSlash + 1) == '@') ? previousSlash : lastSlash;
        if (cutAt <= 0) {
            return null;
        }
        String parent = key.substring(0, cutAt);
        // "@scope" alone (no '/') is a fragment, not a valid key -- stop the walk.
        return (parent.startsWith("@") && !parent.contains("/")) ? null : parent;
    }

    private Dependency makeDependency(String name, String version) {
        return DependencyCreator.nameVersion(Forge.NPMJS, name, version);
    }
}
