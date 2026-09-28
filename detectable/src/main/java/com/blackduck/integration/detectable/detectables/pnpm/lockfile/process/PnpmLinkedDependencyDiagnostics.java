package com.blackduck.integration.detectable.detectables.pnpm.lockfile.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmDependencyType;

/**
 * Tracks selected local links and successful root additions for one code location,
 * without changing dependency selection or graph construction.
 */
class PnpmLinkedDependencyDiagnostics {
    private final boolean enabled;
    private final EnumListFilter<PnpmDependencyType> dependencyTypeFilter;
    private final List<LinkedDependency> linkedDependencies = new ArrayList<>();
    private final Set<String> addedPackageIds = new HashSet<>();

    PnpmLinkedDependencyDiagnostics(boolean enabled, EnumListFilter<PnpmDependencyType> dependencyTypeFilter) {
        this.enabled = enabled;
        this.dependencyTypeFilter = dependencyTypeFilter;
    }

    void recordLink(
        String name,
        String version,
        String packageId,
        @Nullable Map<String, ?> devDependencies,
        @Nullable Map<String, ?> optionalDependencies
    ) {
        if (!enabled || !version.startsWith("link:")) {
            return;
        }
        // Match the putAll precedence used when selecting direct dependencies.
        String type = "DEPENDENCY";
        if (dependencyTypeFilter.shouldInclude(PnpmDependencyType.OPTIONAL)
            && optionalDependencies != null && optionalDependencies.containsKey(name)) {
            type = "OPTIONAL";
        } else if (dependencyTypeFilter.shouldInclude(PnpmDependencyType.DEV)
            && devDependencies != null && devDependencies.containsKey(name)) {
            type = "DEV";
        }
        linkedDependencies.add(new LinkedDependency(name + " (" + type + ", " + version + ")", packageId));
    }

    void recordAddedPackage(String packageId) {
        if (enabled && !linkedDependencies.isEmpty()) {
            addedPackageIds.add(packageId);
        }
    }

    void log(Logger logger, @Nullable String projectPath, BiPredicate<String, List<String>> matchesRootPackage) {
        if (!enabled || linkedDependencies.isEmpty()) {
            return;
        }
        // Reuse the transformer's matching rules, rather than comparing displayed dependency names.
        List<String> notAdded = linkedDependencies.stream()
            .filter(link -> addedPackageIds.stream().noneMatch(id -> matchesRootPackage.test(id, Collections.singletonList(link.packageId))))
            .map(link -> link.description)
            .sorted()
            .collect(Collectors.toList());
        if (!notAdded.isEmpty()) {
            String moduleLabel = projectPath == null || PnpmWorkspaceDependencySummary.IS_NODE_ROOT.evaluate(projectPath)
                ? PnpmWorkspaceDependencySummary.ROOT_MODULE_LABEL : projectPath;
            String dependencyLabel = notAdded.size() == 1 ? "dependency" : "dependencies";
            logger.info("Workspace module '{}': Detect could not match {} local linked {} to resolved lockfile package entries. "
                + "The corresponding direct dependency relationships could not be added to this module's BOM. "
                + "Names and link targets are available at DEBUG.", moduleLabel, notAdded.size(), dependencyLabel);
            if (logger.isDebugEnabled()) {
                logger.debug("Workspace module '{}': local linked dependencies that could not be added as direct dependencies in the BOM: {}. "
                    + "Detect could not match these links to resolved lockfile package entries.", moduleLabel, notAdded);
            }
        }
    }

    private static class LinkedDependency {
        private final String description;
        private final String packageId;

        private LinkedDependency(String description, String packageId) {
            this.description = description;
            this.packageId = packageId;
        }
    }
}
